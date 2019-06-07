package br.com.gabriel.joe.chef.aws.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.CommandType
import br.com.gabriel.joe.chef.aws.domain.Balancer
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.domain.Server
import br.com.gabriel.joe.chef.aws.domain.ServicePort
import br.com.gabriel.joe.chef.aws.service.BalancerService
import br.com.gabriel.joe.chef.aws.service.InfraService
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
@Service
class BalancerServiceImpl implements BalancerService {

	
	@Autowired
	InfraService infraService
	
	@Autowired
	PropertiesConfig propertiesConfig
	
	@Override
	public List<Balancer> getListBalancerInstallation(String environmentName, CommandType command, installation) {
		
		if(command == CommandType.DOWNLOAD) {
			return []
		}
		
		List<Balancer> listBalancer = []
		installation.listBalancer.each { balancer ->
			Balancer b = new Balancer(host:balancer.host, port:balancer.port, envName: environmentName.toLowerCase())
			b.servers = []
			listBalancer << b
		}
		
		
		listBalancer.each { balancer ->
			
			installation.list_server.each { serverJson ->
				Server server = new Server(host: serverJson.soulmvlinux.serverHost)
				server.ports = []
				serverJson.tomcatinstance.port.each { port ->
					ServicePort sP = new ServicePort(port:port)
					sP.products = []
					server.ports << sP
				}
				
				serverJson.app_list.each { product ->
					loadServer(server, product, balancer)
				}
				
			}
			
		}
		
		removeDuplicateRegistry(listBalancer)
		
		
		return listBalancer	
	}

	
	@Override
	public List<Balancer> filterBalancerInformation(String environmentName, CommandType command, List<Balancer> listBalancerInstallation) {
		
		if(command == CommandType.DOWNLOAD) {
			return []
		}
		
		String balancer = ""
		File balancerDir = new File("${propertiesConfig.workingDir}/balancer")
		File fileBalancer = new File("${propertiesConfig.workingDir}/balancer/balancer-${environmentName}.json")
		
		if(!balancerDir.exists()) {
			balancerDir.mkdir()
		}
		
		if(fileBalancer.exists()) {
			infraService.copyFile("${propertiesConfig.workingDir}/balancer/balancer-${environmentName}.json", "${propertiesConfig.workingDir}/balancer/balancer-backup-${environmentName}.json")
			def balancerFile = new JsonSlurper().parseText(fileBalancer.text);
			collectAllInformationBalancer(balancerFile, listBalancerInstallation)
			balancer = new JsonBuilder(listBalancerInstallation).toPrettyString()
			listBalancerInstallation.removeAll { it.servers.size() == 0 }
		} else {
			balancer = new JsonBuilder(listBalancerInstallation).toPrettyString()
		}
		
		infraService.saveFile(fileBalancer, balancer)
		
		return listBalancerInstallation
	}
	
	
	/**
	 * 
	 * @param server
	 * @param product
	 * @param balancer
	 * @return
	 */
	private loadServer(Server server, product,Balancer balancer) {
		
		if(product.balancer_host == balancer.host && product.balancer_port == balancer.port) {
			
			server.ports.each { p ->
				if(product.port == p.port) {
					Product pr = new Product(name:product.productname, context: product.context, type: product.context == "soul-product-reports" ? "reports" : product.type)
					p.products << pr
				}
			}
			
			balancer.servers << server
		}
		
	}
	
	/**
	 * This method is responsible to correct duplicate records for each balancer
	 * @param listBalancer
	 * @return
	 */
	private removeDuplicateRegistry(List<Balancer> listBalancer) {
		
		listBalancer.each { balancer ->
			List<Server> listToAdd = []
			Server s = new Server()
			balancer.servers.sort { it.host }
			balancer.servers.each { server ->
				if(server.host != s.host) {
					listToAdd << server
				}
				s = server
			}
			
			listToAdd.each { server ->
				List<Server> listPortToAdd = []
				ServicePort portA = new ServicePort()
				server.ports.removeAll { it.products.size == 0 }
				server.ports.sort { it.port }
				server.ports.each { port ->
					if(port.port != portA.port) {
						listPortToAdd << port
					}
					portA = port
				}
				
				server.ports.clear()
				server.ports.addAll listPortToAdd
				
			}
			
			balancer.servers.clear()
			balancer.servers.addAll listToAdd
		}
	}
	
	/**
	 * This method is responsible to do a merge with the installation and the balancer file
	 * For get every products include the products that don't be in the actual installation list
	 * @param balancerFile
	 * @param listBalancerInstallation
	 * @return
	 */
	private collectAllInformationBalancer(balancerFile, List<Balancer> listBalancerInstallation) {
		
		listBalancerInstallation.each { balancer ->

			def balancerFileFilter = balancerFile.find { it.host == balancer.host && it.port == balancer.port }

			if(balancerFileFilter) {
				balancerFileFilter.servers.each { server ->
					def serverAction = balancer.servers.find { it.host == server.host }
					if(serverAction) {
						server.ports.each { port ->

							def portAction = serverAction.ports.find { it.port == port.port }
							if(portAction) {

								port.products.each { product ->

									def productAction = portAction.products.find { it.name == product.name && it.type == product.type }
									if(!productAction) {
										portAction.products << product
									} 
								}
								serverAction.ports.remove { it.port == port.port }
								serverAction.ports << portAction
							} else {
								serverAction.ports << port
							}
						}
						
						balancer.servers.remove server
						balancer.servers << serverAction
						
					} else {
						balancer.servers << server
					}
				}
			} 
		}
		
		removeDuplicateRegistry(listBalancerInstallation)

	}

}
