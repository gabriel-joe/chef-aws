package br.com.gabriel.joe.chef.aws.domain;

public class Product {

	String name, version, itemVersion, environment, context, type
	
	
	@Override
	public boolean equals(Object obj) {
		return this.name == obj.name;
	}
}
