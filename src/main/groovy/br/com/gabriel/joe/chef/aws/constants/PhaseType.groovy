package br.com.gabriel.joe.chef.aws.constants

import groovy.util.logging.Slf4j

@Slf4j
enum PhaseType {

	CHEF("chef"),
	AGENT("agent"),
	UNKNOWN("unknown")
	
	String name;
	
	PhaseType(String name){
		this.name = name;
	}
	
	static PhaseType fromName(String name) {
		PhaseType p = PhaseType.UNKNOWN
		if(name == null) return p
		try {
			p = name.toUpperCase() as PhaseType
		} catch(Exception e) {
			log.error("Falha na verificação do type ${name}", e)
		}
		return p
	}
	
}
