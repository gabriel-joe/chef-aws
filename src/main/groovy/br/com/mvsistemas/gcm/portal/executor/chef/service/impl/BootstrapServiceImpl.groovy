package br.com.mvsistemas.gcm.portal.executor.chef.service.impl


import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j

import org.jclouds.chef.ChefContext
import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig
import br.com.mvsistemas.gcm.portal.executor.chef.constants.LogLevel
import br.com.mvsistemas.gcm.portal.executor.chef.domain.Machine
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MachineType
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.domain.Result
import br.com.mvsistemas.gcm.portal.executor.chef.exception.BusinessException
import br.com.mvsistemas.gcm.portal.executor.chef.service.BootstrapService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService

@Slf4j
@SuppressWarnings("deprecation")
@Service
class BootstrapServiceImpl implements BootstrapService {

	@Autowired
	PropertiesConfig propertiesConfig

	@Autowired
	LogService logService
	
	@Autowired
	InfraService infraService

	/** 
	 * 
	 * @param machine 
	 * @param messageHeader
	 */
	public void bootstrapNode(Machine machine,MessageHeader messageHeader) throws BusinessException {
		
		ChefContext ctx = infraService.getChefContext()
		
		bootstrapMachineLinux(ctx, machine,messageHeader)
		
	}

	

	/**
	 * Do bootstrap machine linux, that will be connect with the machine 
	 * by ssh and will run the processes to do bootstrap
	 */
	private void bootstrapMachineLinux(ChefContext ctx, Machine machine,MessageHeader messageHeader) throws BusinessException {

		
		def result = [:]
		def message = [:]
		try {
			
			SshClient ssh = infraService.getConnection(propertiesConfig.chefServerHost,propertiesConfig.chefServerUser,propertiesConfig.chefServerPassword,22)
			
			ssh.connect();
	
			ExecResponse response;
	
			if(machine.type == MachineType.linux)
				response = runBootstrapCommandLinux(machine, ssh);
			else
				response = runBootstrapCommandWindows(machine, ssh);
	
				
			if(response.exitStatus == 0)
				message = executeInitialRunList(machine)
			else
				message = new Result(result:response.exitStatus.toString(),message:response.output)
			
			logService.info(messageHeader,message)
					
			ssh.disconnect();
			
		} catch (Exception e){
			
			log.error e.printStackTrace()
			
			logService.error(messageHeader,new Result(result:"error",message:e.getMessage()))
			
		}
		
		
	}


	/**
	 *
	 * @param bootstrap
	 * @param ssh
	 * @return
	 */
	private ExecResponse runBootstrapCommandWindows(Machine machine,SshClient ssh)  {
		
		StringBuilder rawString = new StringBuilder()
		
		rawString.append("/opt/opscode/bin/knife bootstrap windows winrm ${machine.host} -x ${machine.userName} -P ${machine.userPassword} --node-ssl-verify-mode none -c /root/knife.rb -N ${machine.host} --yes --bootstrap-version 12.5 \n")
		
		ssh.put("/root/chef-bootstrap.sh",rawString.toString());
		ExecResponse result = ssh.exec(" bash /root/chef-bootstrap.sh")

		return result
	}


	/**
	 * 
	 * @param bootstrap
	 * @param ssh
	 * @return
	 */
	private ExecResponse runBootstrapCommandLinux(Machine machine,SshClient ssh)  {
		
		StringBuilder rawString = new StringBuilder()
		
		rawString.append("/opt/opscode/bin/knife bootstrap ${machine.userName}@${machine.host} --sudo -x ${machine.userName} -P ${machine.userPassword} --node-ssl-verify-mode none -c /root/knife.rb -N ${machine.host} --yes --bootstrap-version 12.5 \n")
		
		ssh.put("/root/chef-bootstrap.sh",rawString.toString());
		ExecResponse result = ssh.exec(" bash /root/chef-bootstrap.sh")

		return result
	}
	
	/**
	 *
	 * @param bootstrap
	 * @param ssh
	 * @return
	 */
	private ExecResponse runInitialRunList(SshClient ssh,String path,String commandPlace,String commandExec)  {
		
		StringBuilder rawString = new StringBuilder()
		
		rawString.append(commandPlace)
		
		ssh.put(path,rawString.toString());
		ExecResponse result = ssh.exec(commandExec)

		return result
	}
	

	
	private Result executeInitialRunList(Machine machine){
		
		ExecResponse response;
		def result = [:]
		def message = [:]
		
		SshClient ssh = infraService.getConnection(machine.host,machine.userName,machine.userPassword,22)
		
		ssh.connect();
		
		String runList = new JsonBuilder(machine.initialExecution).toPrettyString()
		
		if(machine.type == MachineType.linux)
			response = runInitialRunList(ssh,"/root/chef-installation.json",runList.toString(),"chef-client -j /root/chef-installation.json")
		else
			response = runInitialRunList(ssh,"/root/chef-installation.json",runList.toString(),"chef-client -j /root/chef-installation.json")
		
			
		if(response.exitStatus == 0)
			message = new Result(result:"ok",message:response.output)
		else
			message = new Result(result:response.exitStatus.toString(),message:response.output)
			
		ssh.disconnect();
		
		return message
			
	}
	
}
