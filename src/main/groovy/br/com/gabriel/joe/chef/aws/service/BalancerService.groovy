package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.constants.CommandType
import br.com.gabriel.joe.chef.aws.domain.Balancer

public interface BalancerService {

	/**
	 * This method is responsible for going through all list of installation and mount
	 * the balancers informations, so then
	 * Return the list of balancer object
	 * @param environmentName
	 * @param command
	 * @param installation
	 * @return
	 */
	List<Balancer> getListBalancerInstallation(String environmentName, CommandType command, installation)
	
	/**
	 * 
	 * @param environmentName
	 * @param command
	 * @param listBalancerInstallation
	 * @return
	 */
	List<Balancer> filterBalancerInformation(String environmentName, CommandType command, List<Balancer> listBalancerInstallation)
}