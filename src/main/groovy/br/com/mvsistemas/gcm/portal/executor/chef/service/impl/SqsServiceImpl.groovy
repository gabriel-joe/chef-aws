package br.com.mvsistemas.gcm.portal.executor.chef.service.impl

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mvsistemas.gcm.portal.executor.chef.config.AwsCredentialsProvider
import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig
import br.com.mvsistemas.gcm.portal.executor.chef.constants.Constants
import br.com.mvsistemas.gcm.portal.executor.chef.domain.DatabaseInformation
import br.com.mvsistemas.gcm.portal.executor.chef.domain.Log
import br.com.mvsistemas.gcm.portal.executor.chef.domain.Machine
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.exception.BusinessException
import br.com.mvsistemas.gcm.portal.executor.chef.service.BootstrapService
import br.com.mvsistemas.gcm.portal.executor.chef.service.DatabaseService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InstallationService
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClient
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.MessageAttributeValue
import com.amazonaws.services.sqs.model.SendMessageRequest

@Slf4j
@Service
class SqsServiceImpl implements SqsService {

	@Autowired
	PropertiesConfig propertiesConfig
	
	@Autowired
	BootstrapService bootstrapService
	
	@Autowired
	DatabaseService databaseService
	
	@Autowired
	InfraService infraService
	
	@Autowired
	InstallationService installationService
	
	@Autowired
	LogService logService


	private AmazonSQS sqs;
	private AwsCredentialsProvider awsCredentialsProvider

	private void initVariables() {

		if(!awsCredentialsProvider)
			this.awsCredentialsProvider = new AwsCredentialsProvider(acessKey:propertiesConfig.awsAccessKey,passwordKey:propertiesConfig.awsPasswordKey)

		if(!sqs)
			this.sqs = new AmazonSQSClient(this.awsCredentialsProvider)
	}

	@Override
	public void send(String message,Map<String,MessageAttributeValue> headers) {

		initVariables()

		SendMessageRequest request = new SendMessageRequest(propertiesConfig.queueUrlSend, message)

		request.withMessageAttributes(headers)

		this.sqs.sendMessage(request);
	}
	
	@Override
	public void deleteMessage(String recipeHandler) {
		
		initVariables()
		
		this.sqs.deleteMessage(new DeleteMessageRequest(propertiesConfig.queueUrlListener, recipeHandler));
		
	}

	@Override
	public void generateAndSendMessage(MessageHeader messageHeader,def status) {

		Map<String,MessageAttributeValue> headers = [:]

		String resultSend = new JsonBuilder(status).toPrettyString()

		send(resultSend,headers)
		
	}

	@Override
	public void checkClientMessage(Map sqsHeaders, String message) {
		
		
		if(sqsHeaders.get("clientId") == propertiesConfig.clientId) {
			
			String recipe = sqsHeaders.get("ReceiptHandle")
			log.debug "Deleting message ${recipe}"
			deleteMessage(recipe)
			
			checkTypeMessage(sqsHeaders,message)
			
		} 
	}

	/**
	 *
	 * @param sqsHeaders
	 * @param message
	 * @return
	 */
	@Override
	public void checkTypeMessage(Map<String, Object> sqsHeaders,String message) throws BusinessException {

		MessageHeader messageHeader = loadHeaders(sqsHeaders)
		def result = [:]
		
		log.info "Start execution ${messageHeader.typeMessage} with ${message}"
		
		try {

			if( messageHeader.typeMessage == Constants.BOOTSTRAP)
				bootstrap(message,messageHeader)
			else if(messageHeader.typeMessage == Constants.CHECK_DATABASE)
				checkDatabaseInformations(message,messageHeader)
			else if(messageHeader.typeMessage == Constants.INSTALLATION)
				executeInstallation(message,messageHeader)
			
		} catch (Exception e) {
			
			log.error e.printStackTrace()
			
			logService.error(messageHeader,e.getMessage())
			
		}
		
		
	}

	/**
	 *
	 * @param message
	 * @return
	 */
	@Override
	public void bootstrap(String message,MessageHeader messageHeader) throws BusinessException {

		def machineJson = new JsonSlurper().parseText(message);

		Machine machine = new Machine(
				name:machineJson.name,
				host:machineJson.host,
				type:machineJson.type,
				userName:machineJson.userLogin,
				userPassword:machineJson.passwordUser,
				initialExecution:machineJson.initialExecution
				)
		
		log.info "Start bootstrap server - host [${machine.host}] type [${machine.type}] user [${machine.userName}] password [${machine.userPassword}]"
		
		bootstrapService.bootstrapNode(machine,messageHeader)
	}


	/**
	 *
	 * @param message
	 * @return
	 */
	@Override
	public void checkDatabaseInformations(String message,MessageHeader messageHeader) throws BusinessException {

		def database = new JsonSlurper().parseText(message);

		databaseService.checkConnection((DatabaseInformation) database,messageHeader)
	}
	
	
	@Override
	public void executeInstallation(String message,MessageHeader messageHeader) throws BusinessException {
		
		def installation = new JsonSlurper().parseText(message);
		
		installationService.executeInstallation(installation,message,messageHeader)
		
	}
	
	
	/**
	 *
	 * @param sqsHeaders
	 * @return
	 */
	private MessageHeader loadHeaders(Map<String, Object> sqsHeaders) throws BusinessException {

		MessageHeader messageHeaders = new MessageHeader(
				clientId:sqsHeaders.get("clientId"),
				environmentName:sqsHeaders.get("environmentName"),
				executionPlan:sqsHeaders.get("executionPlan"),
				typeMessage:sqsHeaders.get("typeMessage"),
				command:sqsHeaders.get("commandId"))


		return messageHeaders
		
	}
	
	
	
	/**
	 *
	 * @param messageHeader
	 * @param serverJson
	 * @param response
	 * @param result
	 * @return
	 */
	@Override
	public void createAndSendLogMessage(MessageHeader messageHeader,String dsNivel,def message) {
		
		def result = [:]
		
		Log log = new Log(
				cdCliente:messageHeader.clientId,
				dsIdentificador:messageHeader.executionPlan,
				snSincronizado:"S",
				cdTipoLog:"plano.execucao",
				dsNivel:dsNivel,
				loConteudo:message
				)

		result.log = []
		result.log.add(log)
		
		generateAndSendMessage(messageHeader,result)
		
	}

}
