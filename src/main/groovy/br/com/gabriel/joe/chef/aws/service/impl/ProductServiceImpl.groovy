package br.com.gabriel.joe.chef.aws.service.impl

import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.ProductContextType
import br.com.gabriel.joe.chef.aws.constants.ProductGroupType
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.service.ProductService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ProductServiceImpl implements ProductService {

	@Override
	public void filterInstallationProducts(Installation installation) throws Exception {
		
		installation.list_server.each { serverJson ->
			def products = []
			serverJson.app_list.each { app -> if(app.productname in ProductGroupType.PRODUCTS.items) products << app }
			if(products.size() > 0) {
				def listProducts = getProductsFilter(products)
				serverJson.app_list.removeAll(products)
				serverJson.app_list.addAll(listProducts)
			}
		}
		
	}
	
	
	/**
	 * List all products from server json
	 * @param serverJson
	 * @return
	 */
	@Override
	public Set<Product> getProductsInstallation(Installation installation) {

		Set<Product> listProduct = []
		List<String> products = []
		installation.list_server.each { serverJson ->
			
			serverJson.app_list.each { product ->
				
				Product p = new Product(name:product.productname,
				version: product.version,
				itemVersion: product.itemVersion,
				environment: serverJson.soulmvlinux.environment,
				context: product.context)
				
				if(!products.contains(product.productname)) {
					log.info("Adding ${product.productname} to the installation list")
					listProduct << p
					products << p.name
				}

			}

		}

		return listProduct;
	}
	
	
	/**
	 * Filter products and add news to the list
	 * @param products
	 * @param environment
	 * @return
	 */
	private Object getProductsFilter(products) throws Exception {
		def list = []
		products.each { product ->
			
			String productName = handleProductName(product)
			ProductGroupType type = productName as ProductGroupType
			
			type.items.each { item ->
				def newProduct = [:]
				newProduct.productname = item
				newProduct.group = product.group
				newProduct.context = handleProductContext(item)
				newProduct.type = "forms"
				newProduct.version = product.version
				newProduct.itemVersion = product.itemVersion
				newProduct.port = product.port
				newProduct.serverId = product.serverId
				newProduct.balancer_host = product.balancer_host
				newProduct.balancer_port = product.balancer_port
				newProduct.serverInstance = product.serverInstance
				list << newProduct
			}
		}
		
		return list
		
	}

	/**
	 * handle the product name 
	 * and check the product type for comparison
	 * @param product
	 * @return
	 */
	private String handleProductName(product) throws Exception {
		
		String productName = product.productname.replaceAll("-", "_")
		if(product.type == "reports")
			productName += "_REPORTS"
			
		return productName
	}
	
	/**
	 * 
	 * @param productContext
	 * @return
	 */
	private String handleProductContext(String productName) throws Exception {
		productName = productName.replaceAll("-","_")
		ProductContextType pContext = productName as ProductContextType
		return pContext.context
	}

}
