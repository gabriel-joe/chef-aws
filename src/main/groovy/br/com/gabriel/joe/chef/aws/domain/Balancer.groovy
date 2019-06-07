package br.com.gabriel.joe.chef.aws.domain;

public class Balancer {

	String host,port,envName
	List<Server> servers
	
	
	@Override
	public boolean equals(Object obj) {
		return host == obj.host && port == obj.port;
	}	
}
