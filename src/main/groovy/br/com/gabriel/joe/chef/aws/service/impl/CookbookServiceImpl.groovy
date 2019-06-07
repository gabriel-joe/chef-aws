package br.com.gabriel.joe.chef.aws.service.impl

import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.config.SystemPropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.service.CookbookService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.LogService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class CookbookServiceImpl implements CookbookService {
	
	@Autowired
	private InfraService infraService
	
	@Autowired
	private LogService logService
	
	@Autowired
	PropertiesConfig propertiesConfig
	
	@Autowired
	SystemPropertiesConfig systemProperties
	@Override
	public void updateCookbook(MessageHeader messageHeader, Object updateJson) throws Exception {
		infraService.downloadAndExtractFile(updateJson.urlDownload, updateJson.nameItem, updateJson.version, "cookbooks")
		executeUploadCookbooks(messageHeader, "${propertiesConfig.workingDir}/cookbooks/${updateJson.version}/")
		log.info "Update completed!"
	}
	
	
	/**
	 * Execute upload cookbooks
	 * @param path
	 */
	private void executeUploadCookbooks(MessageHeader messageHeader, String path) {
		log.info "Uploading cookbooks in path ${path}"
		SshClient ssh = infraService.getChefServerConnection()
		ssh.connect();
		ExecResponse result = ssh.exec("/opt/opscode/bin/knife upload /cookbooks --chef-repo-path ${path} " )
		ssh.disconnect();
		if(result.exitStatus == 0) {
			logService.info(messageHeader, new Result(result:ResultType.SUCCESS, message: "ok"))
		} else {
			logService.error(messageHeader, new Result(result:ResultType.ERROR, message: result.output))
		}	
		log.info "Upload completed!"
		
	}


	@Override
	public void checkCookbookVersion(MessageHeader messageHeader, Object checkJson) throws Exception {
		boolean checksum = infraService.checksumFile(checkJson.urlDownload, checkJson.nameItemMd5, checkJson.nameItem)
		if(checksum) {
			logService.info(messageHeader, new Result(result: ResultType.SUCCESS, message: checkJson.version))
		} else {
			logService.info(messageHeader, new Result(result: ResultType.SUCCESS, message: "Different version!"))
		}
	}
	

}
