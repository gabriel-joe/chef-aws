package br.com.gabriel.joe.chef.aws.service.impl

import org.apache.commons.configuration.PropertiesConfiguration
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.config.SystemPropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.service.AgentService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.LogService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class AgentServiceImpl implements AgentService {
	
	@Autowired
	private InfraService infraService
	
	@Autowired
	private LogService logService
	
	@Autowired
	PropertiesConfig propertiesConfig
	
	@Autowired
	SystemPropertiesConfig systemProperties
	
	@Override
	public void updateAgent(MessageHeader messageHeader, Object updateJson) throws Exception {
		
		infraService.downloadAndExtractFile(updateJson.urlDownload, updateJson.nameItem, updateJson.version, null)
		
		if(new File("${propertiesConfig.workingDir}/${systemProperties.agentVersion()}/config").exists()) {
			infraService.copyFile("${propertiesConfig.workingDir}/${systemProperties.agentVersion()}/config", "${propertiesConfig.workingDir}/${updateJson.version}/config")
		} else {
			infraService.copyFile("${propertiesConfig.workingDir}/config", "${propertiesConfig.workingDir}/${updateJson.version}/config")
		}
		String serviceName = updateAgentService(updateJson)
		executeRestartApp(messageHeader, serviceName)
		logService.info(messageHeader, new Result(result:ResultType.SUCCESS, message: "success!"))
		log.info "Update completed!"
		             
	}
	
	@Override
	public void checkAgentVersion(MessageHeader messageHeader, Object checkVersion) throws Exception {
		logService.info(messageHeader, new Result(result: ResultType.SUCCESS, message: systemProperties.agentVersion()))
	}
	
	/**
	 * 
	 * @param messageHeader
	 * @param updateJson
	 * @return
	 */
	private String updateAgentService(Object updateJson) throws Exception { 
		
		def confs = "${propertiesConfig.workingDir}/yajsw/conf/wrapper.conf"
		
		PropertiesConfiguration config = new PropertiesConfiguration(confs)
		
		config.setProperty("wrapper.java.app.jar", updateJson.jarName)
		
		config.setProperty("wrapper.working.dir", "${propertiesConfig.workingDir}/${updateJson.version}")
		
		config.save()
		
		log.info "Application actived!"
		
		return config.getProperty("wrapper.ntservice.name")
	}
	
	/**
	 * 
	 * @param serviceName
	 */
	private void executeRestartApp(MessageHeader messageHeader, String serviceName) {
		
		SshClient ssh = infraService.getChefServerConnection()
		ssh.connect();
		StringBuilder command = new StringBuilder()
		command.with {
			append "#!/bin/bash \n"
			append "PID=`ps -eaf | grep ${serviceName} | grep -v grep | awk '{print \$2}'` \n"
			append "if [[ \"\" !=  \"\$PID\" ]]; then \n"
			append " echo \"killing \$PID\" \n"
			append " kill -9 \$PID \n"
			append " fi \n \n"
			append "/etc/init.d/${serviceName} stop \n"
			append "sleep 5 \n"
			append "/etc/init.d/${serviceName} start"
		}
		ssh.put("/root/chef-update-agent.sh",command.toString());
		ssh.disconnect();
		logService.info(messageHeader, new Result(result:ResultType.SUCCESS, message: "success!"))
		" nohup bash /root/chef-update-agent.sh > /dev/null &".execute()
	}

}
