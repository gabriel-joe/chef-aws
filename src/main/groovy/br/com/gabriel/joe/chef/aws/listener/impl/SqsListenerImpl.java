package br.com.gabriel.joe.chef.aws.listener.impl;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Service;

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig;
import br.com.gabriel.joe.chef.aws.service.SqsService;


@Service
class SqsListenerImpl {

	@Autowired 
	SqsService sqsService;
	
	@Autowired
	PropertiesConfig propertiesConfig;

	@SqsListener(value = "${address.queue.url.listener}",deletionPolicy = SqsMessageDeletionPolicy.NEVER)
	public void queueListener(String message,
			@Headers Map<String, Object> sqsHeaders, Acknowledgment acknowledgment) throws InterruptedException, ExecutionException {
		if(sqsHeaders.get("clientId") != null)
			sqsService.checkClientMessage(sqsHeaders, message, acknowledgment);
	}

	
}

