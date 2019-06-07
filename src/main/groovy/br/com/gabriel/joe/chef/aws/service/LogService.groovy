package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.MessageHeader

public interface LogService {

	void info(MessageHeader messageHeader,def message) 
	
    void warn(MessageHeader messageHeader,def message) 
	
	void error(MessageHeader messageHeader,def message) 
	
}