package br.com.gabriel.joe.chef.aws.constants

enum MachineType {

	WINDOWS("windows"),LINUX("linux")

	String name;

	MachineType(String name){
		this.name = name;
	}

	static MachineType fromName(String name) {
		if(name.equalsIgnoreCase("linux")) {
			return MachineType.LINUX
		} else {
			return MachineType.WINDOWS
		}
	}

}
