package br.com.gabriel.joe.chef.aws.service.impl


import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.MachineType
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.BootstrapService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.KnifeService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.util.InstallationUtil
import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

@Slf4j
@Service
class BootstrapServiceImpl implements BootstrapService {

	@Autowired
	PropertiesConfig propertiesConfig

	@Autowired
	LogService logService
	
	@Autowired
	InfraService infraService
	
	@Autowired
	KnifeService knifeService
	
	
	private static final String BOOTSTRAP_VERSION = "14.3.37"

	/** 
	 * 
	 * @param machine 
	 * @param messageHeader
	 */
	public void bootstrapNode(Machine machine,MessageHeader messageHeader) throws BusinessException {
		bootstrapMachine(machine,messageHeader)
	}
	

	/**
	 * this method will be connect with the machine 
	 * by ssh and will run the bootstrap's proccess
	 */
	private void bootstrapMachine(Machine machine,MessageHeader messageHeader) throws BusinessException {

		def message = [:]
			
		SshClient ssh = infraService.getChefServerConnection()
		log.info "Chef machine password - ${propertiesConfig.chefServerPassword}"
		log.info "Machine Password - ${machine.userPassword}"
		ssh.connect();
		
		ExecResponse response;

		if(machine.type.toString().equalsIgnoreCase(MachineType.LINUX.name)) {
			//Test connection with server
			SshClient sshTest = infraService.getConnection(machine.host,machine.userName,machine.userPassword,22)
			sshTest.connect();
			response = runBootstrapCommandLinux(machine, ssh);
			sshTest.disconnect()
		} else {
			response = runBootstrapCommandWindows(machine, ssh);
		}

			
		if(response.exitStatus == 0) {
			message = executeInitialRunList(machine)
		} else {
			message = new Result(result:ResultType.ERROR, message:response.output)
		}
		
		if(message.result == ResultType.ERROR) {
			logService.error(messageHeader,message)
		} else {
			logService.info(messageHeader,message)
		}
				
		ssh.disconnect();
		
	}


	/**
	 * This method is responsible to do the bootstrap in the windows platform machine
	 * @param bootstrap
	 * @param ssh
	 * @return
	 */
	private ExecResponse runBootstrapCommandWindows(Machine machine, SshClient ssh)  {
		String rawString = knifeService.getKnifeCommandBootstrapWinrm(machine, BOOTSTRAP_VERSION)
		log.info "Command bootstrap windows [${rawString.toString()}]"
		ssh.put("/root/chef-bootstrap.sh", rawString.toString());
		ExecResponse result = ssh.exec(" bash /root/chef-bootstrap.sh")

		return result
	}


	/**
	 * This method is responsible to do the bootstrap in the linux platform machine
	 * @param bootstrap
	 * @param ssh
	 * @return
	 */
	private ExecResponse runBootstrapCommandLinux(Machine machine, SshClient ssh)  {

		String rawString = knifeService.getKnifeCommandBootstrapLinux(machine, BOOTSTRAP_VERSION)
		log.info "Command bootstrap linux [${InstallationUtil.getStringWithoutPassword(rawString, machine.userPassword)}]"
		ssh.put("/root/chef-bootstrap.sh", rawString)
		ExecResponse result = ssh.exec(" bash /root/chef-bootstrap.sh")

		return result
	}
	
	/**
	 * Execute the initial recipes list on the machine
	 * @param machine
	 * @return
	 */
	private Result executeInitialRunList(Machine machine){
		
		ExecResponse response;
		Result message;
		
		String runListPretty = new JsonBuilder(machine.initialExecution).toPrettyString()
		String runList = new JsonBuilder(machine.initialExecution).toString()
		String path = InstallationUtil.getPathExecution(machine.userName);
		
		if(machine.type.toString().equalsIgnoreCase(MachineType.LINUX.name)) {
			response = runInitialRunListLinux("${path}/chef-installation.json", runListPretty, "sudo -S <<< \"${machine.userPassword}\" chef-client -j ${path}/chef-installation.json -L ${path}/chef-client.log", machine)
		} else {
			response = runInitialRunListWindows(runList, machine)
		}
		
		if(response.exitStatus == 0) {
			message = new Result(result:ResultType.SUCCESS,message:response.output)
		} else {
			message = new Result(result:ResultType.ERROR,message:response.output)
		}
		
		return message
			
	}
	
	/**
	 * Runs initial recipe list on linux platform
	 * @param ssh
	 * @param path
	 * @param commandPlace
	 * @param commandExec
	 * @return
	 */
	private ExecResponse runInitialRunListLinux(String path,String commandPlace,String commandExec, Machine machine)  {
		
		SshClient ssh = infraService.getConnection(machine.host, machine.userName, machine.userPassword, 22)
		ssh.connect();
		log.info("Run initial list with command [${commandExec}]")
		ssh.put(path,commandPlace);
		ExecResponse result = ssh.exec(commandExec)
		ssh.disconnect();
		
		return result
	}
	
	
	/**
	 * Runs initial recipe list on windows platform
	 * @param ssh
	 * @param path
	 * @param commandPlace
	 * @param commandExec
	 * @return
	 */
	private ExecResponse runInitialRunListWindows(String commandPlace, Machine machine)  {
		
		SshClient ssh = infraService.getChefServerConnection()
		ssh.connect()
		
		ssh.put("/root/chef-windows.json", commandPlace)
		String rawString = knifeService.getKnifeCommandWinrmExecSetAttributes(machine, false)
		log.info("Run knife winrm set attributes with command [${rawString.toString()}]")
		ssh.put("/root/chef-initial-windows.sh", rawString);
		ExecResponse result = ssh.exec(" bash /root/chef-initial-windows.sh")
		
		if(result.exitStatus == 0) {
			rawString = knifeService.getKnifeCommandWirmPutFile(machine)
			log.info("Run recipe create_installation_file with command [${InstallationUtil.getStringWithoutPassword(rawString, machine.userPassword)}]")
			ssh.put("/root/chef-initial-windows.sh", rawString)
			result = ssh.exec(" bash /root/chef-initial-windows.sh")
		}
		
		if(result.exitStatus == 0) {
			rawString = knifeService.getKnifeCommandWirmChefClient(machine)
			log.info("Run initial list with command [${InstallationUtil.getStringWithoutPassword(rawString, machine.userPassword)}]")
			ssh.put("/root/chef-initial-windows.sh", rawString);
			result = ssh.exec(" bash /root/chef-initial-windows.sh ")
		}
		
		ssh.disconnect();

		return result
	}
	
	
}
