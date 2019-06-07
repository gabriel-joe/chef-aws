package br.com.gabriel.joe.chef.aws.domain;

public class ServicePort {

	String port
	List<Product> products
	
	@Override
	public boolean equals(Object obj) {
		return port == obj.port;
	}
	
}
