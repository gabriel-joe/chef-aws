package br.com.gabriel.joe.chef.aws.config

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
	
	@Value ('${chef.downloadDir}')
	String downloadDir
	
	@Value ('${chef.working.dir}')
	String workingDir

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
	
	@Value ('${database.jdbcDialect}')
	String jdbcDialect
	
	@Value ('${database.jdbcDriverClassName}')
	String jdbcDriverClassName
	
	@Value('${security.api.url}')
	String securityApiUrl
	
	@Value('${security.api.userKey}')
	String securityApiUserKey
	
	@Value('${security.api.passwordKey}')
	String securityApiPasswordKey
	
	@Value('${security.api.active}')
	boolean securityApiActive
	
	@Value('${security.bootstrap.proxy}')
	String securityBootstrapProxy
	
	@Value('${aws.urlBase}')
	String awsUrlBase
	
	@Value('${aws.active}')
	boolean awsActive
	
	@Value('${winrm.shell.active}')
	boolean winrmShellActive
	
	@Value('${winrm.shell.type}')
	String winrmShellType
	
	@Value('${winrm.authentication.protocol}')
	String winrmAuthenticationProtocol
	
	@Value('${winrm.location.installation}')
	String winrmLocationInstallation
	
	@Value('${winrm.location.balancer}')
	String winrmLocationBalancer
	
	@Value('${node.recipeDefault}')
	String nodeRecipeDefault
}
