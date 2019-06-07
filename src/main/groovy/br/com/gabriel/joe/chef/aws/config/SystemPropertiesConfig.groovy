package br.com.gabriel.joe.chef.aws.config

import org.apache.commons.configuration.PropertiesConfiguration
import org.springframework.stereotype.Component

@Component
class SystemPropertiesConfig {
	
	private static final String SYSTEM_PROPERTIES = "/system.properties";
	private static final String SYSTEM_VERSION_PROPERTY = "system.version";
	private static final String COOKBOOK_VERSION_PROPERTY = "cookbook.version";
	
	
	public String agentVersion() {
		
		String version = "";
		version = loadProperties(SYSTEM_VERSION_PROPERTY);
		return version;
		
	}
	
	public String cookbookVersion() {
		
		String version = "";
		version = loadProperties(COOKBOOK_VERSION_PROPERTY);
		return version;
		
	}
	
	public updateCookbookVersion(String version) {
		PropertiesConfiguration config = new PropertiesConfiguration("system.properties")
		config.setProperty(COOKBOOK_VERSION_PROPERTY, version)
		config.save()
	}

	private String loadProperties(String version) {
		Properties properties = new Properties();
		properties.load(getClass().getResourceAsStream(SYSTEM_PROPERTIES));
		return properties.getProperty(version)
	}
	
	
}
