package br.com.mvsistemas.gcm.portal.executor.chef.listener.impl;

import java.io.IOException;
import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig;
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService;


@Service
class SqsListenerImpl {

	@Autowired 
	SqsService sqsService;
	
	@Autowired
	PropertiesConfig propertiesConfig;

	@SqsListener(value = "${address.queue.url.listener}",deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	public void queueListener(String message,
			@Headers Map<String, Object> sqsHeaders) throws JsonParseException,
			JsonMappingException, IOException, MessagingException, IllegalArgumentException,
	IllegalAccessException, NoSuchFieldException, SecurityException {
		
		if(sqsHeaders.get("clientId") != null)
			sqsService.checkClientMessage(sqsHeaders, message);
		
	}

	
}

