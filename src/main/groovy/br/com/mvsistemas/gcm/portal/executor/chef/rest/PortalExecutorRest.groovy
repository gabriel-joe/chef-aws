package br.com.mvsistemas.gcm.portal.executor.chef.rest

import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService

@RestController
class PortalExecutorRest {
	
	private static final String SYSTEM_PROPERTIES = "/system.properties";
	private static final String SYSTEM_VERSION_PROPERTY = "system.version";
	
	
	@RequestMapping(value = "/version", method = RequestMethod.GET)
	public String version() {
		
		Properties properties = new Properties();
		String version = "";

		try {
			properties.load(getClass().getResourceAsStream(SYSTEM_PROPERTIES));
			version = properties.getProperty(SYSTEM_VERSION_PROPERTY);
		} catch (IOException e) {
			version = e.getMessage();
			e.printStackTrace();
		}

		return version;
	}
	

}
