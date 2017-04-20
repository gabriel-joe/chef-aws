package br.com.mvsistemas.gcm.portal.executor.chef.service.impl

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import net.schmizz.sshj.SSHClientimport.*

import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig
import br.com.mvsistemas.gcm.portal.executor.chef.domain.DatabaseInformation
import br.com.mvsistemas.gcm.portal.executor.chef.domain.ItemAmbiente
import br.com.mvsistemas.gcm.portal.executor.chef.domain.ItemServidor
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MachineType
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.service.DatabaseService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InstallationService
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService

@Service
class InstallationServiceImpl implements InstallationService {
	
	
	private static final Logger logger = LoggerFactory
	.getLogger(InstallationServiceImpl.class);
	
	@Autowired
	InfraService infraService

	@Autowired
	LogService logService

	@Autowired
	SqsService sqsService

	@Autowired
	DatabaseService databaseService

	@Autowired
	PropertiesConfig propertiesConfig

	boolean toContinue

	@Override
	public void executeInstallation(def installation,String message,MessageHeader messageHeader) {

		def result = [:]
		ExecResponse response
		def listItems = []
		String urlBase

		toContinue = true

		logger.info "Start installation"

		installation.list_server.each  { serverJson ->

			urlBase = serverJson.soulmvlinux.urlbase

			if(toContinue) {

				if(serverJson.soulmvlinux.serverType == MachineType.linux.toString())
					response = executeLinuxAction(serverJson,result,listItems, messageHeader, installation)
			}
		}

		if(toContinue)
			response = executeScriptAction(urlBase,installation,messageHeader)


		result = [:]

		result = getItemAmbienteResult(response,result,listItems,messageHeader)

		sqsService.generateAndSendMessage(messageHeader,result)
	}

	/**
	 * 
	 * @param serverJson
	 * @param result
	 * @param listItems
	 * @param response
	 * @param messageHeader
	 * @param continuous
	 * @param installation
	 * @return
	 */
	private ExecResponse executeLinuxAction(def serverJson,def result,def listItems,
			MessageHeader messageHeader,def installation){

		def runList = []
		ExecResponse response

		SshClient ssh = null

		// connect machine to do action
		ssh = infraService.getConnection(serverJson.soulmvlinux.serverHost,
				serverJson.soulmvlinux.serverUser,
				serverJson.soulmvlinux.serverPassword,22)

		ssh.connect()

		// send message to indicate the beginning of the process on the server
		logService.info(messageHeader,"Iniciando ${messageHeader.command} no servidor ${serverJson.soulmvlinux.serverHost}!")
		logger.info "Start ${messageHeader.command} in server ${serverJson.soulmvlinux.serverHost}!"

		// each in app_list to get all products and environment
		serverJson.app_list.each { product ->

			product.environment = serverJson.soulmvlinux.environment
			listItems.add(product)

		}

		// set the runList will be executed
		runList = serverJson.run_list

		runList.each { recipe ->

			serverJson.run_list = []

			if(recipe.contains("mv_balancer") && !serverIsBalancer(serverJson))
				return

			serverJson.run_list.add(recipe)

			if(toContinue){

				logger.info "Start recipe ${recipe}"

				logService.info(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command} | Executando ${recipe}!")

				if(recipe.contains("mv_balancer::create_jk") && serverIsBalancer(serverJson))
					response = executeBalancerRecipe(installation, ssh, recipe)
				else
					response = generateCommandToExecute(serverJson, ssh)

				logger.info "Recipe response ${response.exitStatus}"

				if(response.exitStatus == 0) {

					logService.info(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command} | ${recipe} finalizado no servidor com sucesso!")

				} else {

					logService.error(messageHeader,"${serverJson.soulmvlinux.serverHost} | ${serverJson.soulmvlinux.environment} | ${messageHeader.command} | ${recipe} finalizado no servidor com falha! codigo ${response.exitStatus}")
					// stop the execution
					toContinue = false

				}


			}
		}

		result = getItemServidorResult(response,result, serverJson, messageHeader)
		sqsService.generateAndSendMessage(messageHeader,result)

		ssh.disconnect()

		return response

	}


	/**
	 * 
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse generateCommandToExecute(def serverJson, SshClient ssh) {

		String path = "/root/chef-installation.json"
		String commandExecute = "chef-client -j /root/chef-installation.json -l info -L /var/log/chef-client.log"

		String serverMessage = new JsonBuilder(serverJson).toPrettyString()

		ExecResponse response = executeWithAttribute(ssh,serverMessage,path,commandExecute)

		return response
	}


	/**
	 *
	 * @param serverJson
	 * @param ssh
	 * @return
	 */
	private ExecResponse executeBalancerRecipe(def installation, SshClient ssh,String recipe) {

		String path = "/root/chef-installation-balancer.json"
		String commandExecute = "chef-client -o \"${recipe}\" -j /root/chef-installation-balancer.json -l info -L /var/log/chef-client.log"

		String serverMessage = new JsonBuilder(installation).toPrettyString()

		ExecResponse response = executeWithAttribute(ssh,serverMessage,path,commandExecute)

		return response
	}

	/**
	 * 
	 * @param response
	 * @param result
	 * @param serverJson
	 * @param messageHeader
	 * @return
	 */
	private Object getItemServidorResult(ExecResponse response,def result, def serverJson, MessageHeader messageHeader) {

		result.itemServidor = []

		if(response.exitStatus != 0){

			if(messageHeader.command == "download")
				result.itemServidor.addAll(getListItemServidor(serverJson,"download.aplicacao.copiada.com.erro",messageHeader))
			else if(messageHeader.command == "install")
				result.itemServidor.addAll(getListItemServidor(serverJson,"aplicacao.instalada.com.erro",messageHeader))
		} else {

			if(messageHeader.command == "download")
				result.itemServidor.addAll(getListItemServidor(serverJson,"download.aplicacao.copiada",messageHeader))
			else if(messageHeader.command == "install")
				result.itemServidor.addAll(getListItemServidor(serverJson,"aplicacao.instalada",messageHeader))
		}

		return result
	}


	/**
	 * 
	 * @param response
	 * @param result
	 * @param listItems
	 * @param messageHeader
	 * @return
	 */
	private Object getItemAmbienteResult(ExecResponse response,def result, def listItems, MessageHeader messageHeader) {

		result.itemAmbiente = []

		if(response.exitStatus != 0){

			if(messageHeader.command == "download")
				result.itemAmbiente.addAll(getListItemAmbiente(listItems,"download.aplicacao.copiada.com.erro","50","Download efetuado com erro",messageHeader))
			else if(messageHeader.command == "install")
				result.itemAmbiente.addAll(getListItemAmbiente(listItems,"aplicacao.instalada.com.erro","50","Instalação efetuada com erro",messageHeader))
				
		} else {

			if(messageHeader.command == "download")
				result.itemAmbiente.addAll(getListItemAmbiente(listItems,"download.aplicacao.copiada","100","Download efetuado com sucesso",messageHeader))
			else if(messageHeader.command == "install")
				result.itemAmbiente.addAll(getListItemAmbiente(listItems,"aplicacao.instalada","100","Aplicação instalada com sucesso",messageHeader))
			
			logService.info(messageHeader,"Processo concluido!")
			
		}

		return result
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

		ssh.put(path,commandPlace);
		ExecResponse result = ssh.exec(commandExecute)

		return result
	}


	/**
	 * 
	 * @param serverJson
	 * @param cdItemStatus
	 * @param messageHeader
	 * @return
	 */
	private Set<ItemAmbiente> getListItemAmbiente(def listItems,String cdItemStatus,String nrProgresso,String dsProgresso,MessageHeader messageHeader) {


		Set<ItemAmbiente> list = new ArrayList<ItemAmbiente>()

		listItems.each { product ->

			ItemAmbiente itemAmbiente = new ItemAmbiente(cdItem:product.productname + "." + product.version,
			cdItemStatus:cdItemStatus,
			cdCliente:messageHeader.clientId,
			cdAmbiente:product.environment,
			cdPlanoExecucao:messageHeader.executionPlan,
			nrProgressoAtualizacao:nrProgresso,
			dsProgressoAtualizacao:dsProgresso)


			list.add(itemAmbiente)
		}


		return list
	}


	/**
	 * 
	 * @param serverJson
	 * @param cdItemStatus
	 * @param envName
	 * @param messageHeader
	 * @return
	 */
	private Set<ItemServidor> getListItemServidor(def serverJson,String cdItemStatus,MessageHeader messageHeader) {

		Set<ItemServidor> list = new ArrayList<ItemServidor>()

		serverJson.app_list.each { product ->

			ItemServidor itemServidor = new ItemServidor(cdItem:product.productname + "." + product.version,
			cdItemStatus:cdItemStatus,
			cdCliente:messageHeader.clientId,
			cdAmbiente:serverJson.soulmvlinux.environment,
			cdPlanoExecucao:messageHeader.executionPlan,
			cdServidor: product.serverId)

			list.add(itemServidor)
		}

		return list
	}

	/**
	 * 
	 * @param serverJson
	 * @return
	 */
	private boolean serverIsBalancer(def serverJson){

		boolean response = false

		if(serverJson.soulmvlinux.serverHost == serverJson.config_variables.balancer_host)
			response = true


		return response
	}

	/**
	 * 
	 * @param urlBase
	 * @param scriptList
	 * @param messageHeader
	 * @return
	 */
	private executeScriptAction(String urlBase,def installation,MessageHeader messageHeader){

		ExecResponse response
		def result = [:]
		String scriptActual

		try {


			if(messageHeader.command == "download") {

				installation.scriptList.each { scriptPackage ->

					scriptActual = scriptPackage.filename

					downloadScripts(messageHeader, scriptPackage, urlBase)

				}

			} else if(messageHeader.command == "install") {
				
				DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
			
				databaseService.runScripts(messageHeader, installation.scriptList, database)
				databaseService.runGrantsAndSynonyms(messageHeader, database)
				databaseService.createIndexesAndFks(messageHeader, database)
				databaseService.compileDatabase(messageHeader, database)

			}

			response = new ExecResponse("ok","",0)

		} catch (Exception e){

			e.printStackTrace()

			logService.error(messageHeader,"${messageHeader.command} | Falha ao realizar ${messageHeader.command} causa [ ${e.getMessage()} ] !")
			response = new ExecResponse(e.getMessage(),e.getMessage(),1)

		}

		return response

	}

	/**
	 * 
	 * @param messageHeader
	 * @param scriptPackage
	 * @param urlBase
	 */
	private void downloadScripts(MessageHeader messageHeader, scriptPackage, String urlBase) {

		def result = [:]

		logService.info(messageHeader,"${messageHeader.command} | Executando download do pacote ${scriptPackage.filename}!")

		infraService.downloadItem(urlBase,scriptPackage.filename)

		logService.info(messageHeader,"${messageHeader.command} | Download do pacote ${scriptPackage.filename} realizado com sucesso!")


	}


	


}
