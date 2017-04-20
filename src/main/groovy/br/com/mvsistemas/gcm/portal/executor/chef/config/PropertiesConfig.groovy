package br.com.mvsistemas.gcm.portal.executor.chef.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class PropertiesConfig {
	
	@Value('${chef.server.ip}')
	String chefServerHost
	
	@Value('${chef.server.user}')
	String chefServerUser
	
	@Value('${chef.server.password}')
	String chefServerPassword
	
	@Value('${chef.client.name}')
	String chefClientName
	
	@Value('${chef.organization.validator.name}')
	String chefOrganizationValidatorName
	
	@Value('${chef.organization.name}')
	String chefOrganizationName
	
	@Value('${client.id}')
	String clientId
	
	@Value('${chef.home}')
	String chefHome
	
	@Value ('${chef.downloadDir}')
	String downloadDir
	
	@Value ('${chef.logFileDir}')
	String logFileDir
	
	@Value ('${cloud.aws.credentials.accessKey}')
	String awsAccessKey
	
	@Value ('${cloud.aws.credentials.secretKey}')
	String awsPasswordKey
	
	@Value ('${address.queue.url.send}')
	String queueUrlSend
	
	@Value ('${address.queue.url.listener}')
	String queueUrlListener
	
	@Value ('${database.jdbc.dialect}')
	jdbcDialect
	
	@Value ('${database.jdbc.driverClassName}')
	jdbcDriverClassName
	
}
