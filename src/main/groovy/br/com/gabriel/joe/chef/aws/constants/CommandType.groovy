package br.com.gabriel.joe.chef.aws.constants

enum CommandType {

	INSTALL("install"),
	DOWNLOAD("download"),
	DOWNLOAD_AND_INSTALL("download-install"),
	UNKNOWN("")
	
	String name;
	
	CommandType(String name){
		this.name = name;
	}
	
	static CommandType fromName(String name) {
		CommandType m = CommandType.values().find {
			it.name == name
		}
		
		if(!m)
			return CommandType.UNKNOWN
			
		return m
	}
	
}
