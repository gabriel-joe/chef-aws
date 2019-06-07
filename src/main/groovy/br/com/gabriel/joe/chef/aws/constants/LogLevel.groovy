package br.com.gabriel.joe.chef.aws.constants

import groovy.transform.ToString;

@ToString
enum LogLevel {
			
	//------------------
	// Phase types
	//------------------
	
	INFO("INFO"),
	WARN("WARN"),
	ERROR("ERROR"),
	DEBUG("DEBUG")
	
	//-------------------
	// Private attributes
	//-------------------
	
	final String name
	
	//-------------------
	// Initialization
	//-------------------
	
	LogLevel(String name){
		this.name = name	
	}
	
}
