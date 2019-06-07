package br.com.gabriel.joe.chef.aws.service

import org.springframework.cloud.aws.messaging.listener.Acknowledgment

import com.amazonaws.services.sqs.model.MessageAttributeValue

import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.exception.BusinessException

public interface SqsService {

	
	public void send(String message,Map<String,MessageAttributeValue> headers)
	
	public void deleteMessage(String recipeHandler, Acknowledgment acknowledgment)
	
	public void generateAndSendMessage(MessageHeader messageHeader,def status) 
	
	public void checkClientMessage(Map sqsHeaders, String message, Acknowledgment acknowledgment)
	
	public void checkTypeMessage(MessageHeader messageHeader,String message) throws BusinessException
	
	
	
		
	
}