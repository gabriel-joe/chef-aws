package br.com.gabriel.joe.chef.aws.rest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.actuate.health.Health
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import groovy.util.logging.Slf4j

@Slf4j
@RestController
class PortalExecutorRest {
	
	private static final String SYSTEM_PROPERTIES = "/system.properties";
	private static final String SYSTEM_VERSION_PROPERTY = "system.version";
	
	@Autowired
	PropertiesConfig propertiesConfig
	
	@RequestMapping(value = "/version", method = RequestMethod.GET)
	public String version() {
		Properties properties = new Properties();
		String version = "";

		try {
			properties.load(getClass().getResourceAsStream(SYSTEM_PROPERTIES));
			version = properties.getProperty(SYSTEM_VERSION_PROPERTY);
		} catch (IOException e) {
			version = e.getMessage();
			log.error e
		}

		return version;
	}
	
	
	@RequestMapping(value = "/health", method = RequestMethod.GET)
	public Health health() {
		
		Health health = Health.up().
				withDetail("java", System.getProperty("java.version")).
				withDetail("sqsListener", propertiesConfig.queueUrlListener).
				withDetail("sqsSend", propertiesConfig.queueUrlSend).
				withDetail("securityApiActive", propertiesConfig.securityApiActive).
				withDetail("securityApiUrl", propertiesConfig.securityApiUrl).
				withDetail("clientId", propertiesConfig.clientId).
				withDetail("chefServerIp", propertiesConfig.chefServerHost).
				withDetail("awsActive", propertiesConfig.awsActive).
				withDetail("winrmShellActive", propertiesConfig.winrmShellActive).
				withDetail("winrmShellType", propertiesConfig.winrmShellType).
				withDetail("winrmLocationInstallation", propertiesConfig.winrmLocationInstallation).
				withDetail("winrmLocationBalancer", propertiesConfig.winrmLocationBalancer).
				withDetail("winrmAuthenticationProtocol", propertiesConfig.winrmAuthenticationProtocol ?: "default")
				.build();
		return health;
	}
	

}
