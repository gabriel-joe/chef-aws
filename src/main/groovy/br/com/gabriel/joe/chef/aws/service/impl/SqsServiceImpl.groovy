package br.com.gabriel.joe.chef.aws.service.impl

import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.Interval
import org.joda.time.Period
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.listener.Acknowledgment
import org.springframework.stereotype.Service

import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.AmazonSQSClientBuilder
import com.amazonaws.services.sqs.model.DeleteMessageRequest
import com.amazonaws.services.sqs.model.MessageAttributeValue
import com.amazonaws.services.sqs.model.SendMessageRequest

import br.com.gabriel.joe.chef.aws.config.AwsCredentialsProvider
import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.CommandType
import br.com.gabriel.joe.chef.aws.constants.MachineType
import br.com.gabriel.joe.chef.aws.constants.MessageType
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.DatabaseInformation
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.AgentService
import br.com.gabriel.joe.chef.aws.service.BootstrapService
import br.com.gabriel.joe.chef.aws.service.CookbookService
import br.com.gabriel.joe.chef.aws.service.DatabaseService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.InstallationService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.SqsService
import br.com.gabriel.joe.chef.aws.service.ValidateService
import br.com.gabriel.joe.chef.aws.util.InstallationUtil
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

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

	@Autowired
	AgentService agentService

	@Autowired
	CookbookService cookbookService
	
	@Autowired
	ValidateService validationService
	
	private List<MessageHeader> pastDuplicityMessages = []

	private AmazonSQS sqs;
	private AwsCredentialsProvider awsCredentialsProvider

	private void initVariables() {

		if(!awsCredentialsProvider)
			this.awsCredentialsProvider = new AwsCredentialsProvider(acessKey:propertiesConfig.awsAccessKey,passwordKey:propertiesConfig.awsPasswordKey)
		
		if(!sqs)
			this.sqs = AmazonSQSClientBuilder.standard().withCredentials(awsCredentialsProvider).withClientConfiguration(InstallationUtil.getClientConfiguration()).withRegion(Regions.SA_EAST_1).build()
	}

	@Override
	public void send(String message,Map<String,MessageAttributeValue> headers) {

		initVariables()

		SendMessageRequest request = new SendMessageRequest(propertiesConfig.queueUrlSend, message)

		request.withMessageAttributes(headers)

		this.sqs.sendMessage(request);
	}

	@Override
	public void deleteMessage(String recipeHandler, Acknowledgment acknowledgment) {

		initVariables()
		this.sqs.deleteMessage(new DeleteMessageRequest(propertiesConfig.queueUrlListener, recipeHandler));
		acknowledgment.acknowledge().get()

	}

	@Override
	public void generateAndSendMessage(MessageHeader messageHeader,def status) {

		Map<String,MessageAttributeValue> headers = [:]

		String resultSend = new JsonBuilder(status).toPrettyString()

		send(resultSend,headers)

	}

	@Override
	public void checkClientMessage(Map sqsHeaders, String message, Acknowledgment acknowledgment) {
		MessageHeader messageHeader = loadHeaders(sqsHeaders)
		if(checkClientMessageOptions(messageHeader)) {
			String recipe = sqsHeaders.get("ReceiptHandle")
			log.debug "Deleting message ${recipe}"
			deleteMessage(recipe, acknowledgment)
			if(checkDuplicityMessageId(messageHeader)) {
				checkTypeMessage(messageHeader,message)
			} 
		} else {
			log.debug("Message received with id ${messageHeader.messageId}, but is not to me");
		}
	}

	/**
	 * This method is responsible to check these options:
	 * 1 - If the clientId in the message header is equals to properties clientId
	 * 2 - If the typeMessage in the message header is equals to bootstrap or installation, the checksum will be check
	 * @param sqsHeaders
	 * @return
	 */
	private boolean checkClientMessageOptions(MessageHeader messageHeader) {
		boolean isClientId = (messageHeader.clientId == propertiesConfig.clientId);

		if(!isClientId) {
			return false
		}

		if(messageHeader.typeMessage.equals(MessageType.CHECK_DATABASE) ) {
			return true
		}

		if(messageHeader.typeMessage in [MessageType.BOOTSTRAP, MessageType.INSTALLATION] && messageHeader.checksum != getPropertyConfigHash()) {
			return false
		}

		return true

	}
	
	/**
	 * This method is responsible to check the duplicity of messageId to agent
	 * do not execute the same message twice
	 * @param current
	 * @return
	 */
	private boolean checkDuplicityMessageId(MessageHeader current) {
		
		if(pastDuplicityMessages.contains(current)) {
			return false
		} 
		
		pastDuplicityMessages << current
		reducePastDuplicityValues()
		return true
		
	} 
	
	/**
	 * Method responsible to clean the pastDuplicityMessages if the size pass 
	 * 100 elements
	 * @return
	 */
	private reducePastDuplicityValues () {
		int length = pastDuplicityMessages.size()
		if(length == 100) {
			log.info "Cleaning the pastDuplicityMessages"
			pastDuplicityMessages = pastDuplicityMessages.takeRight(2)
		}
		
	}
	
	/**
	 * 
	 * @return
	 */
	private getPropertyConfigHash() {
		PropertiesConfiguration config = infraService.getPropertyConfigFile()
		return config.getProperty("client.hash")
	}

	/**
	 *
	 * @param sqsHeaders
	 * @param message
	 * @return
	 */
	@Override
	public void checkTypeMessage(MessageHeader messageHeader,String message) {

		log.info "Start execution ${messageHeader.typeMessage.name} with ${message}"

		try {

			logService.info(messageHeader, new Result(result: ResultType.CHECK, message: "Conexão com o agente feita com sucesso! ${InetAddress.getLocalHost()}"))
			switch (messageHeader.typeMessage) {

				case MessageType.BOOTSTRAP:
					bootstrap(message,messageHeader)
					break
				case MessageType.CHECK_DATABASE:
					checkDatabaseInformations(message,messageHeader)
					break
				case MessageType.INSTALLATION:
					executeInstallation(message,messageHeader)
					break
				case MessageType.UPDATE_AGENT:
					updateAgent(message, messageHeader)
					break
				case MessageType.UPDATE_COOKBOOKS:
					updateCookbooks(message, messageHeader)
					break
				case MessageType.CHECK_AGENT_VERSION:
					checkAgentVersion(message, messageHeader)
					break
				case MessageType.CHECK_COOKBOOK_VERSION:
					checkCookbookVersion(message, messageHeader)
					break
				case MessageType.DATABASE_AUDIT_INFORMATION:
					getDatabaseAuditInformation(message, messageHeader)
					break
				default:
					throw new Exception("Tipo da mensagem não identificado.")

			}

		} catch (BusinessException bE) {
			log.error("Falha na execução da ação", bE)
			logService.error(messageHeader,new Result(result: ResultType.ERROR, message: bE.getMessage()))
		} catch (Exception e) {
			log.error("Falha na execução da ação", e)
			logService.error(messageHeader,new Result(result: ResultType.ERROR, message: e.getMessage()))
		} 


	}

	/**
	 *
	 * @param message
	 * @return
	 */
	private void bootstrap(String message,MessageHeader messageHeader) {

		def machineJson = new JsonSlurper().parseText(message);
		Machine machine = new Machine(
				name:machineJson.name,
				host:machineJson.host,
				type:MachineType.fromName(machineJson.type),
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
	private void checkDatabaseInformations(String message,MessageHeader messageHeader) {
		def database = new JsonSlurper().parseText(message);
		databaseService.checkConnection((DatabaseInformation) database, messageHeader)
	}

	
	/**
	 * 
	 * @param message
	 * @param messageHeader
	 * @throws BusinessException
	 */
	private void executeInstallation(String message,MessageHeader messageHeader) {
		Date start = new Date()
		Installation installation = (Installation) new JsonSlurper().parseText(message)
		boolean validationResult = validationService.executeValidation(installation, messageHeader)
		
		if(validationResult) {
			installationService.executeInstallation(installation,message,messageHeader)
		} else {
			logService.error(messageHeader, new Result(result: ResultType.ERROR, message: "Requisitos para procedimentos no ambiente não atendidos, processo abortado!"))
		}
		
		Date stop = new Date()
		Period period = new Interval(start.getTime(), stop.getTime()).toPeriod()
		log.info "Installation finished with time ${period.getHours()} Hours / ${period.getMinutes()} Minutes / ${period.getSeconds()} Seconds"
	}


	/**
	 *
	 * @param sqsHeaders
	 * @return
	 */
	private MessageHeader loadHeaders(Map<String, Object> sqsHeaders) {

		MessageHeader messageHeaders = new MessageHeader(
				clientId:sqsHeaders.get("clientId"),
				environmentName:sqsHeaders.get("environmentName"),
				executionPlan:sqsHeaders.get("executionPlan"),
				typeMessage:MessageType.fromName(sqsHeaders.get("typeMessage")),
				command:CommandType.fromName(sqsHeaders.get("commandId")),
				checksum: sqsHeaders.get("checksum"),
				messageId: sqsHeaders.get("messageId"))


		return messageHeaders

	}

	/**
	 * 
	 * @param message
	 * @param messageHeader
	 * @throws Exception
	 */
	private void updateAgent(String message, MessageHeader messageHeader) throws Exception {
		def updateJson = new JsonSlurper().parseText(message);
		agentService.updateAgent(messageHeader, updateJson)
	}

	/**
	 *
	 * @param message
	 * @param messageHeader
	 * @throws Exception
	 */
	private void updateCookbooks(String message, MessageHeader messageHeader) throws Exception {
		def updateJson = new JsonSlurper().parseText(message);
		cookbookService.updateCookbook(messageHeader, updateJson)
	}
	
	/**
	 * 
	 * @param message
	 * @param messageHeader
	 * @throws Exception
	 */
	private void checkAgentVersion(String message, MessageHeader messageHeader) throws Exception {
		def checkJson = new JsonSlurper().parseText(message);
		agentService.checkAgentVersion(messageHeader, checkJson)
	}
	
	/**
	 * 
	 * @param message
	 * @param messageHeader
	 * @throws Exception
	 */
	private void checkCookbookVersion(String message, MessageHeader messageHeader) throws Exception {
		def checkJson = new JsonSlurper().parseText(message);
		cookbookService.checkCookbookVersion(messageHeader, checkJson)
	}
	
	/**
	 * 
	 * @param message
	 * @param messageHeader
	 * @throws Exception
	 */
	private void getDatabaseAuditInformation(String message, MessageHeader messageHeader) throws Exception {
		def jsonDatabaseInformation = new JsonSlurper().parseText(message);
		databaseService.getAuditInformation(messageHeader, (DatabaseInformation) jsonDatabaseInformation)
	}

}
