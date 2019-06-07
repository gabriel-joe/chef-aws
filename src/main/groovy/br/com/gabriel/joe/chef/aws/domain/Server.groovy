package br.com.gabriel.joe.chef.aws.domain;

public class Server {

	String host
	List<ServicePort> ports
	
	@Override
	public boolean equals(Object obj) {
		return host == obj.host;
	}
}
