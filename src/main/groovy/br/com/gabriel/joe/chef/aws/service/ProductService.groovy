package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.Product

public interface ProductService {

	/**
	 * 
	 * @param installation
	 * @throws Exception
	 */
	void filterInstallationProducts(Installation installation) throws Exception
	
	/**
	 * 
	 * @param installation
	 * @return
	 */
	Set<Product> getProductsInstallation(Installation installation)
	
}