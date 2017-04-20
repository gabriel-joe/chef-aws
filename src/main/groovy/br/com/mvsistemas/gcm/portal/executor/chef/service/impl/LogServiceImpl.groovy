package br.com.mvsistemas.gcm.portal.executor.chef.service.impl

import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mvsistemas.gcm.portal.executor.chef.constants.LogLevel;
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader;
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService

@Slf4j
@Service
class LogServiceImpl implements LogService {
	
	@Autowired
	SqsService sqsService
	
	
	@Override
	public void info(MessageHeader messageHeader,def message) {
		
		sqsService.createAndSendLogMessage(messageHeader,LogLevel.INFO,message)
		
	}

	@Override
	public void warn(MessageHeader messageHeader,def message) {
		
		sqsService.createAndSendLogMessage(messageHeader,LogLevel.WARN,message)
		
	}

	@Override
	public void error(MessageHeader messageHeader,def message) {
		
		sqsService.createAndSendLogMessage(messageHeader,LogLevel.ERROR,message)
		
	}


}
