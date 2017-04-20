package br.com.mvsistemas.gcm.portal.executor.chef.service

import br.com.mvsistemas.gcm.portal.executor.chef.domain.DatabaseInformation
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader

public interface LogService {

	void info(MessageHeader messageHeader,def message) 
	
    void warn(MessageHeader messageHeader,def message) 
	
	void error(MessageHeader messageHeader,def message) 
	
}