package br.com.gabriel.joe.chef.aws.service.impl

import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.CommandType
import br.com.gabriel.joe.chef.aws.constants.Constants
import br.com.gabriel.joe.chef.aws.constants.MachineType
import br.com.gabriel.joe.chef.aws.constants.PhaseType
import br.com.gabriel.joe.chef.aws.constants.RecipeType
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.Balancer
import br.com.gabriel.joe.chef.aws.domain.DatabaseInformation
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.PhaseRecipe
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.BalancerService
import br.com.gabriel.joe.chef.aws.service.DatabaseService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.InstallationService
import br.com.gabriel.joe.chef.aws.service.ItemService
import br.com.gabriel.joe.chef.aws.service.KnifeService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.ProductService
import br.com.gabriel.joe.chef.aws.service.RecipeService
import br.com.gabriel.joe.chef.aws.service.SqsService
import br.com.gabriel.joe.chef.aws.util.InstallationUtil
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

@Slf4j
@Service
class InstallationServiceImpl implements InstallationService {
	
	
	@Autowired
	InfraService infraService

	@Autowired
	LogService logService

	@Autowired
	SqsService sqsService

	@Autowired
	DatabaseService databaseService
	
	@Autowired
	RecipeService recipeService
	
	@Autowired
	ProductService productService
	
	@Autowired
	BalancerService balancerService
	
	@Autowired
	PropertiesConfig propertiesConfig
	
	@Autowired 
	KnifeService knifeService
	
	@Autowired
	ItemService itemService

	boolean toContinue
	
	String urlBase

	@Override
	public void executeInstallation(Installation installation,String message,MessageHeader messageHeader)  throws BusinessException, Exception {

		def result = [:]
		ExecResponse response

		toContinue = true
		urlBase = propertiesConfig.awsUrlBase
		List<PhaseRecipe> listPhase = recipeService.getListPhaseRecipe(installation)
		Set<Product> listProducts = productService.getProductsInstallation(installation)
		boolean downloadMode = messageHeader.command in [CommandType.DOWNLOAD, CommandType.DOWNLOAD_AND_INSTALL]
		boolean installMode = messageHeader.command in [CommandType.INSTALL, CommandType.DOWNLOAD_AND_INSTALL]
		productService.filterInstallationProducts(installation)
		List<Balancer> listBalancerInstallation = balancerService.getListBalancerInstallation(messageHeader.environmentName, messageHeader.command, installation)
		listBalancerInstallation = balancerService.filterBalancerInformation(messageHeader.environmentName, messageHeader.command, listBalancerInstallation)
		List<String> listServer = getListServer(installation)
		
		logService.info(messageHeader, "Realizando limpeza do cache [CHEF-SERVER]!")
		cleanUpCache(listServer)
		
		if(downloadMode) {
			logService.info(messageHeader, "Realizando configuracao [AWS_CREDENTIALS]...!")
			updateAwsCredentialsAttributes(listServer)
		}
		
		listPhase.sort { it.id }
		listPhase.each  { phaseRecipe ->
			
			if(toContinue) {
				
				result = [:]
				result = itemService.getInitialItemAmbiente(result, listProducts, phaseRecipe, messageHeader)
				sqsService.generateAndSendMessage(messageHeader, result)
				
				if(phaseRecipe.statusComplete in Constants.ACTIONS_DATABASE) {
					response = executeScriptAction(urlBase, installation, messageHeader, phaseRecipe.statusComplete)
				} else if(phaseRecipe.type.equals(PhaseType.CHEF)) {
					response = executeServerActions(phaseRecipe, response, messageHeader, listBalancerInstallation)
				}
				
				result = [:]
				result = itemService.getItemAmbienteResult(response, result, listProducts, phaseRecipe, messageHeader)
				sqsService.generateAndSendMessage(messageHeader, result)
				
			}
			
		}
		
		if(toContinue && installMode) {
			DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
			databaseService.updateVersionsRegistry(listProducts, database)
		}
		
		clearAwsCredentialsAttributes(listServer)
		cleanUpCache(listServer)
		
		if(toContinue)
			logService.info(messageHeader, "Processo concluido!")
			

	}

	
	/**
	 * Method responsible to reset app_list attributes
	 * @return
	 */
	private cleanUpCache(List<String> listServer) {
		
		SshClient ssh = infraService.getChefServerConnection()
		log.info "Clean up attributes"
		ExecResponse exec = null
		listServer.each { server ->
			exec = ssh.exec(knifeService.getKnifeCommandCleanupAppList(server, false))
			log.info "Response clean up ${server} - ${exec.exitStatus}"
			if(exec.exitStatus != 0) {
				exec = ssh.exec(knifeService.getKnifeCommandCleanupAppList(server, true))
				log.info "Response clean up 2 ${server} - ${exec.exitStatus}"
			}
		}
		
		ssh.disconnect()
		
	}
	
	/**
	 * This method is responsible to update the credentials attributes that will be used
	 * on the recipes
	 * @return
	 */
	private updateAwsCredentialsAttributes(List<String> listServer) {
		
		SshClient ssh = infraService.getChefServerConnection()
		log.info "Clean up attributes"
		ExecResponse exec = null
		listServer.each { server ->
			exec = ssh.exec(knifeService.getKnifeCommandUpdateCredentials(server, false))
			log.info "Response update aws credentials ${server} - ${exec.exitStatus}"
			if(exec.exitStatus != 0) {
				exec = ssh.exec(knifeService.getKnifeCommandUpdateCredentials(server, true))
				log.info "Response update aws credentials 2 ${server} - ${exec.exitStatus}"
			}
		}
		
		ssh.disconnect()
	}
	
	/**
	 * 
	 * @return
	 */
	private clearAwsCredentialsAttributes(List<String> listServer) {
		
		SshClient ssh = infraService.getChefServerConnection()
		log.info "Clean up attributes"
		ExecResponse exec = null
		listServer.each { server ->
			exec = ssh.exec(knifeService.getKnifeCommandCleanupCredentials(server, false))
			log.info "Response clean up aws credentials ${server} - ${exec.exitStatus}"
			if(exec.exitStatus != 0) {
				exec = ssh.exec(knifeService.getKnifeCommandCleanupCredentials(server, true))
				log.info "Response clean up aws credentials 2 ${server} - ${exec.exitStatus}"
			}
		}
		
		ssh.disconnect()
		
	}
	
	/**
	 * Execute actions each server
	 * @param phaseRecipe
	 * @param response
	 * @param messageHeader
	 * @param installation
	 * @return
	 */
	private ExecResponse executeServerActions(PhaseRecipe phaseRecipe, ExecResponse response, MessageHeader messageHeader,List<Balancer> listBalancerInstallation) {
		try {
			phaseRecipe.listServer.each { serverJson ->
				urlBase = serverJson.soulmvlinux.urlbase
				def balancerAction = [:]
				if(messageHeader.command in [CommandType.INSTALL,CommandType.DOWNLOAD_AND_INSTALL])  {
					def balancerFilter = listBalancerInstallation.findAll { it.host == serverJson.soulmvlinux.serverHost}
					balancerAction.balancer = balancerFilter 
				}
				log.info "Start ${messageHeader.command.name} - ${phaseRecipe.id} in server ${serverJson.soulmvlinux.serverHost}!"
				if(toContinue) {
					response = checkMachineAndExecuteAction(response, phaseRecipe, serverJson, messageHeader, balancerAction)
				}
			}
		} catch (Exception e) {
			stopExecution(messageHeader, e, true)
			response = new ExecResponse("${e.getMessage()}","",1)
		}
		
		return response
	}

	
	
	/**
	 * 
	 * @param response
	 * @param phaseRecipe
	 * @param serverJson
	 * @param messageHeader
	 * @param balancerAction
	 * @return
	 * @throws Exception
	 */
	private ExecResponse checkMachineAndExecuteAction(ExecResponse response, PhaseRecipe phaseRecipe, serverJson, MessageHeader messageHeader, balancerAction) throws Exception {
		
		if(serverJson.soulmvlinux.serverType.toString().equalsIgnoreCase(MachineType.LINUX.name)) {
			response = executeLinuxAction(phaseRecipe, serverJson, messageHeader, balancerAction)
		} else {
			response = executeWindowsAction(phaseRecipe, serverJson, messageHeader, balancerAction)
		}
		
		return response
	}

	/**
	 * 
	 * @param phaseRecipe
	 * @param serverJson
	 * @param messageHeader
	 * @param installation
	 * @return
	 * @throws Exception
	 */
	private ExecResponse executeLinuxAction(PhaseRecipe phaseRecipe, def serverJson,
			MessageHeader messageHeader, balancerAction)  throws Exception {

		SshClient ssh = infraService.getConnection(serverJson.soulmvlinux.serverHost,
				serverJson.soulmvlinux.serverUser,
				serverJson.soulmvlinux.serverPassword,22)

		ssh.connect()

		ExecResponse exec = executeAction(messageHeader, phaseRecipe, serverJson, ssh, balancerAction, MachineType.LINUX)

		ssh.disconnect()

		return exec

	}
	
	/**
	 * 
	 * @param phaseRecipe
	 * @param serverJson
	 * @param messageHeader
	 * @param balancerAction
	 * @return
	 * @throws Exception
	 */
	private ExecResponse executeWindowsAction(PhaseRecipe phaseRecipe, def serverJson,
			MessageHeader messageHeader, balancerAction)  throws Exception {

		// connect in chef server machine to do action
		SshClient ssh = infraService.getChefServerConnection()
		ssh.connect()
		
		ExecResponse exec = executeAction(messageHeader, phaseRecipe, serverJson, ssh, balancerAction, MachineType.WINDOWS)
		
		ssh.disconnect()
		
		return exec

	}
	
	/**
	 * 
	 * @param messageHeader
	 * @param phaseRecipe
	 * @param serverJson
	 * @param ssh
	 * @param installation
	 * @param machineType
	 * @param balancerAction
	 * @return
	 * @throws Exception
	 */
	private ExecResponse executeAction(MessageHeader messageHeader, PhaseRecipe phaseRecipe, serverJson, SshClient ssh, balancerAction, MachineType machineType) throws Exception {
		
		def result = [:]
		def runListOriginal = []
		def runList = []
		ExecResponse response

		// set the runList will be executed
		runListOriginal = serverJson.run_list
		runList = runListOriginal.findAll { it.id == phaseRecipe.id }

		runList.each { recipes ->
			
			recipes.recipes.each { recipe ->
				
				serverJson.run_list = []
				serverJson.run_list.add(recipe)
				
				if(toContinue){

					log.info "Start recipe ${recipe}"

					logService.info(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command.name} | Executando ${recipe}!")

					if(isBalancerRecipe(recipe)) {
						response = executeBalancerRecipe(serverJson, ssh, recipe, balancerAction, machineType)
					} else if(machineType == MachineType.WINDOWS) {
						response = generateWindowsCommandToExecute(serverJson, ssh)
					} else {
						response = generateLinuxCommandToExecute(serverJson, ssh)
					}

					log.info "Recipe response ${response.exitStatus}"

					if(response.exitStatus == 0) {
						log.info "Recipe executed!"
						logService.info(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command.name} | ${recipe} finalizado no servidor com sucesso!")
					} else {
						log.error "Recipe failed! ${response.output}"
						logService.error(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command.name} | ${recipe} finalizado no servidor com falha!")
						logService.error(messageHeader, new Result(result:ResultType.ERROR, message:response.output))
						// stop the execution
						stopExecution(messageHeader, null, false)
						return
					}

				}
			}
			
		}

		serverJson.run_list = runListOriginal
		result = itemService.getItemServidorResult(response, result, serverJson, phaseRecipe, messageHeader)
		sqsService.generateAndSendMessage(messageHeader,result)
		ssh.disconnect()

		return response
		
	}
	
	/**
	 * Check if the recipe is one of the options @RecipeType.BALANCER
	 * @param recipe
	 * @return
	 */
	private boolean isBalancerRecipe(String recipe) {

		if(recipe in RecipeType.BALANCER.recipes) {
			return true
		}
		
		return false
	}
	
	
	/**
	 * This method is responsible to run the balancer recipe in specified server type
	 * @return
	 */
	private ExecResponse executeBalancerRecipe(serverJson, ssh, recipe, balancerAction, MachineType machineType) {
		
		switch(machineType) {
			
			case MachineType.LINUX:
				return executeLinuxBalancerRecipe(serverJson, ssh, recipe, balancerAction)
			case MachineType.WINDOWS:
				return executeWindowsBalancerRecipe(serverJson, ssh, recipe, balancerAction)
			default:
				throw new BusinessException("Tipo da máquina não identificado para a execução do balanceador!")
				
		}
			
		
	}
	
	


	/**
	 * Generate linux command to execute 
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse generateLinuxCommandToExecute(def serverJson, SshClient ssh) {
		String serverMessage = getServerMessageAsStringWithoutPassword(serverJson)
		String path = "${InstallationUtil.getPathExecution(serverJson.soulmvlinux.serverUser)}"
		String pathInstallation = "${path}/chef-installation.json";
		String commandExecute = "sudo -S <<< \"${serverJson.soulmvlinux.serverPassword}\" chef-client -j ${pathInstallation} -l info -L ${path}/chef-client.log"
		
		ExecResponse response = executeWithAttribute(ssh,serverMessage,pathInstallation,commandExecute)

		return response
	}
	
	/**
	 * Generate windows command to execute
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse generateWindowsCommandToExecute(def serverJson, SshClient ssh) {
		
		String serverMessage = getServerMessageAsStringWithoutPassword(serverJson)
		Machine machine = new Machine(host: serverJson.soulmvlinux.serverHost, userName: serverJson.soulmvlinux.serverUser, userPassword: serverJson.soulmvlinux.serverPassword)
		
		ExecResponse response = commonExecutionWindows(ssh, serverMessage, machine, false, null)
		
		return response
	}
	
	/**
	 * Method responsible to hide server password in the file
	 * @param serverJson
	 * @return
	 */
	private String getServerMessageAsStringWithoutPassword(serverJson) {
		String passwordTemp = serverJson.soulmvlinux.serverPassword
		serverJson.soulmvlinux.serverPassword = ""
		String serverMessage = new JsonBuilder(serverJson).toString()
		serverJson.soulmvlinux.serverPassword = passwordTemp
		return serverMessage
	}
	

	/**
	 * Run Balancer recipe linux
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse executeLinuxBalancerRecipe(def serverJson, SshClient ssh,String recipe, def balancerAction)  {
		
		String path = "${InstallationUtil.getPathExecution(serverJson.soulmvlinux.serverUser)}"
		String pathInstallation = "${path}/chef-installation-balancer.json";
		String commandExecute = "sudo -S <<< \"${serverJson.soulmvlinux.serverPassword}\" chef-client -o \"${recipe}\" -j ${pathInstallation} -l info -L ${path}/chef-client.log"
		String serverMessage = new JsonBuilder(balancerAction).toPrettyString()

		ExecResponse response = executeWithAttribute(ssh,serverMessage,pathInstallation,commandExecute)

		return response
	}
	
	/**
	 * Run Balancer recipe windows
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse executeWindowsBalancerRecipe(def serverJson, SshClient ssh,String recipe, def balancerAction) {
		
		String balancerMessage = new JsonBuilder(balancerAction).toString()
		Machine machine = new Machine(host: serverJson.soulmvlinux.serverHost, userName: serverJson.soulmvlinux.serverUser, userPassword: serverJson.soulmvlinux.serverPassword)
		
		ExecResponse response = commonExecutionWindows(ssh, balancerMessage, machine, true, recipe)
		
		return response
	}

	
	/**
	 * Method responsible to execute the windows installation 
	 * products and balancer
	 * @param ssh
	 * @param message
	 * @param machine
	 * @param isBalancer
	 * @param recipe
	 * @return
	 */
	private ExecResponse commonExecutionWindows(SshClient ssh, String message, Machine machine, boolean isBalancer, String balancerRecipe) {
		ssh.put("/root/chef-windows.json", message)
		String commandExecute = knifeService.getKnifeCommandWinrmExecSetAttributes(machine, true)
		ssh.put("/root/chef-install-windows.sh", commandExecute)
		log.info "Execute knife set attributes file with command ${commandExecute}"
		log.info "IsBalancer execution ${isBalancer} / Balancer Recipe ${balancerRecipe}"
		ExecResponse response = ssh.exec(" bash /root/chef-install-windows.sh")
		if(response.exitStatus == 0) {
			commandExecute = knifeService.getKnifeCommandWirmPutFile(machine)
			ssh.put("/root/chef-install-windows.sh", commandExecute)
			log.info "Execute create installation file with command ${InstallationUtil.getStringWithoutPassword(commandExecute, machine.userPassword)}"
			response = ssh.exec(" bash /root/chef-install-windows.sh")
		}
		
		if(response.exitStatus == 0) {
			commandExecute = isBalancer ? knifeService.getKnifeCommandWinrmChefClientBalancer(machine, balancerRecipe) : knifeService.getKnifeCommandWirmChefClient(machine) 
			ssh.put("/root/chef-install-windows.sh", commandExecute)
			log.info "Execute windows recipe with ${InstallationUtil.getStringWithoutPassword(commandExecute, machine.userPassword)}"
			response = ssh.exec(" bash /root/chef-install-windows.sh")
		}
		
		return response
	}
	
	
	/**
	 * 
	 * @param ssh
	 * @param commandPlace
	 * @param path
	 * @param commandExecute
	 * @return
	 */
	private ExecResponse executeWithAttribute(SshClient ssh,String commandPlace,String path,String commandExecute)  {
		
		if(commandPlace != null) {
			ssh.put(path,commandPlace);
		}
		
		ExecResponse result = ssh.exec(commandExecute)

		return result
	}



	/**
	 * 
	 * @param urlBase
	 * @param scriptList
	 * @param messageHeader
	 * @return
	 */
	private ExecResponse executeScriptAction(String urlBase,def installation,MessageHeader messageHeader, String statusDatabase){

		ExecResponse response

		try {

			if(messageHeader.command.equals(CommandType.DOWNLOAD)) {
				downloadScripts(messageHeader, installation.scriptList, urlBase)
			} else if(messageHeader.command.equals(CommandType.INSTALL)) {
				installScripts(installation, messageHeader)
			} else if (messageHeader.command.equals(CommandType.DOWNLOAD_AND_INSTALL)) {
				if(statusDatabase == Constants.ACTIONS_DATABASE_DOWNLOAD)
					downloadScripts(messageHeader, installation.scriptList, urlBase)
				else if (statusDatabase == Constants.ACTIONS_DATABASE_INSTALL)
					installScripts(installation, messageHeader)
			}

			response = new ExecResponse("ok","",0)

		} catch (Exception e){
			stopExecution(messageHeader, e, true)
			logService.error(messageHeader,"${messageHeader.command.name} | Falha ao realizar ${messageHeader.command.name} causa [ ${e.getMessage()} ] !")
			response = new ExecResponse(e.getMessage(),e.getMessage(),1)

		}

		return response

	}	
	
	/**
	 * Install Scripts
	 * @param installation
	 * @param messageHeader
	 * @return
	 */
	private installScripts(installation, MessageHeader messageHeader) {
		DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
		databaseService.execAuditPre(messageHeader, installation.databaseAuditProcess, database)
		databaseService.runScripts(messageHeader, installation.scriptList, database)
		databaseService.runGrantsAndSynonyms(messageHeader, database)
		databaseService.createIndexesAndFks(messageHeader, database)
		databaseService.compileDatabase(messageHeader, database)
		databaseService.execAuditPos(messageHeader, database)
	}
	
	/**
	 * Download scripts packages
	 * @param messageHeader
	 * @param scriptPackage
	 * @param urlBase
	 */
	private void downloadScripts(MessageHeader messageHeader, scriptList, String urlBase) throws BusinessException {

		scriptList.each { scriptPackage ->
			logService.info(messageHeader,"${messageHeader.command.name} | Executando download do pacote ${scriptPackage.filename}!")
			infraService.downloadItem(urlBase,scriptPackage.filename)
			logService.info(messageHeader,"${messageHeader.command.name} | Download do pacote ${scriptPackage.filename} realizado com sucesso!")
		}

	}
	
	/**
	 * @param installation
	 * @return
	 */
	private List<String> getListServer(installation) {
		List<String> listServer = new ArrayList<String>()
		installation.list_server.each { listServer << it.soulmvlinux.serverHost }
		return listServer
	}
	
	
	/**
	 * Stop Execution
	 */
	private void stopExecution(MessageHeader messageHeader, Exception e, boolean sendMessage) {
		if(sendMessage) logService.error(messageHeader, "Falha na execucao - ${e.getMessage()}")
		toContinue = false
	}
	

}
