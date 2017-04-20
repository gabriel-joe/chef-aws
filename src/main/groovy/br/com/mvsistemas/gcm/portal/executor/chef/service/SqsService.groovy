package br.com.mvsistemas.gcm.portal.executor.chef.service

import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.exception.BusinessException

import com.amazonaws.services.sqs.model.MessageAttributeValue

public interface SqsService {

	
	public void send(String message,Map<String,MessageAttributeValue> headers)
	
	public void deleteMessage(String recipeHandler)
	
	public void generateAndSendMessage(MessageHeader messageHeader,def status) 
	
	public void createAndSendLogMessage(MessageHeader messageHeader,String dsNivel,def message)
	
	public void checkClientMessage(Map sqsHeaders, String message)
	
	public void checkDatabaseInformations(String message,MessageHeader messageHeader) throws BusinessException
	
	public void bootstrap(String message,MessageHeader messageHeader) throws BusinessException
	
	public void checkTypeMessage(Map<String, Object> sqsHeaders,String message) throws BusinessException
	
	public void executeInstallation(String message,MessageHeader messageHeader) throws BusinessException
	
		
	
}