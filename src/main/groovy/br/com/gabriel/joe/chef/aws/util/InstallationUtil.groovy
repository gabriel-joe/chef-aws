package br.com.gabriel.joe.chef.aws.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.amazonaws.ClientConfiguration
import com.amazonaws.Protocol

import br.com.gabriel.joe.chef.aws.domain.ProxyConfiguration


class InstallationUtil {
	
	
	private final static PROXY_MATCHER_CREDENTIALS = /(.+):\/\/(.+):(.+)@(.+):(.+)/
	private final static PROXY_MATCHER_WITHOUT_CREDENTIALS = /(.+):\/\/(.+):(.+)/
	
	private static final Logger logger = LoggerFactory
	.getLogger(InstallationUtil.class);
	
	/**
	 * Return the path of file
	 * @param userName
	 * @return
	 */
	public static String getPathExecution(String userName) {
		
		if("root".equals(userName)) {
			return "/root"
		} else {
			return "/home/" + userName;
		}
		
	}
	
	
	/**
	 * 
	 * @return
	 */
	private static ProxyConfiguration getProxyConfigurations() {
		
		String httpsProxy = System.getenv("https_proxy")
		String httpProxy = System.getenv("http_proxy")
		def matcher = httpsProxy =~ PROXY_MATCHER_CREDENTIALS
		boolean credentials = true
		String protocol = "HTTPS"
		if(httpsProxy) {
			logger.info "Initialize https proxy with ${httpsProxy}"
		} else if (httpProxy) {
			protocol = "HTTP"
			logger.info "Initialize http proxy with ${httpProxy}"
		}
		
		if (matcher.size() == 0) {
			credentials = false
			matcher = httpsProxy =~ PROXY_MATCHER_WITHOUT_CREDENTIALS
		}
		
		logger.info "matcher size ${matcher.size()}"
		
		if(httpsProxy && matcher.size() > 0) {
			
			ProxyConfiguration proxy = new ProxyConfiguration()
			matcher.each { group ->
				
				if(credentials) {
					proxy.username = group[2]
					proxy.password = group[3]
					proxy.host = group[4]
					proxy.port = group[5]
				} else {
					proxy.host = group[2]
					proxy.port = group[3]
				}
				
				proxy.credentialsActive = credentials
				proxy.protocol = protocol
			}
			return proxy
		}
		
		
		return null
	}
	
	/**
	 * This method is responsible to initialize AWS Client configuration
	 * @return
	 */
	public static ClientConfiguration getClientConfiguration() {
		
		ClientConfiguration clientConfig = new ClientConfiguration()
		ProxyConfiguration proxy = InstallationUtil.getProxyConfigurations()
		
		if(!proxy) {
			return null
		}
		
		logger.info "Proxy informations user=${proxy.username}, password=${proxy.password}, host=${proxy.host}, port=${proxy.port}, protocol=${proxy.protocol}"
		
		clientConfig.setProxyHost(proxy.host)
		clientConfig.setProxyPort(Integer.parseInt(proxy.port))
		if(proxy.credentialsActive) {
			clientConfig.setProxyUsername(proxy.username)
			clientConfig.setProxyPassword(proxy.password)
		}
		clientConfig.setProtocol(proxy.protocol == "HTTPS" ? Protocol.valueOf("HTTPS") : Protocol.valueOf("HTTP"))
		clientConfig.setConnectionTimeout(10000);
		
		return clientConfig
	}
	
	
	public static String handlePasswordEscapeChars(String password) {
		return password.replace("&", "\\&").replace("!", "\\!")
	}
	
	
	/**
	 * Get MV_HOME from fileSystem attribute
	 * chef node
	 * @param fileSystem
	 * @return
	 */
	public static String getMvHomeFromFileSystem(fileSystem) {
		String mvHome = "C:"
		
		if(fileSystem.mvHome) {
			mvHome = fileSystem.mvHome.substring(0, fileSystem.mvHome.indexOf(":")+1)
		}
		
		return mvHome
	}
	
	/**
	 * Method responsible to handle a string command to remove password credentials
	 * @param command
	 * @param password
	 * @return
	 */
	public static getStringWithoutPassword(String command, String password) {
		String commandAux = command
		return commandAux.replaceAll("-P '${password}'", "-P ***")
	}
	
}
