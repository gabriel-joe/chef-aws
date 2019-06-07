package br.com.gabriel.joe.chef.aws.constants

import groovy.util.logging.Slf4j

@Slf4j
enum MessageType {

	BOOTSTRAP("bootstrap"),
	INSTALLATION("installation"),
	CHECK_DATABASE("check_database"),
	UPDATE_AGENT("update_agent"),
	UPDATE_COOKBOOKS("update_cookbooks"),
	CHECK_AGENT_VERSION("check_agent_version"),
	CHECK_COOKBOOK_VERSION("check_cookbook_version"),
	DATABASE_AUDIT_INFORMATION("database_audit_information"),
	UNKNOWN("unknown")
	
	String name;
	
	MessageType(String name){
		this.name = name;
	}
	
	static MessageType fromName(String name) {
		MessageType m = MessageType.UNKNOWN
		if(name == null) return m
		try {
			m = name.toUpperCase() as MessageType
		} catch(Exception e) {
			log.error("Falha na verificação do type ${name}", e)
		}
		
		return m
	}
	
}
