package br.com.gabriel.joe.chef.aws.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSAsync
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder

import br.com.gabriel.joe.chef.aws.util.InstallationUtil


@Configuration
public class AwsConfig {

	@Autowired
	private AmazonSQSAsync amazonSQS
	
	@Autowired
	private PropertiesConfig propertiesConfig
	
	
	@Bean(name="amazonSQS")
	public AmazonSQSAsyncClient amazonSQSAsyncClient(AWSCredentialsProvider awsCredentialsProvider){
		return AmazonSQSAsyncClientBuilder.standard().withCredentials(awsCredentialsProvider).withClientConfiguration(awsClientConfig()).withRegion(Regions.SA_EAST_1).build();
	}
	
	
	@Bean
	public QueueMessagingTemplate queueMessagingTemplate() {
		QueueMessagingTemplate queueTemplate = new QueueMessagingTemplate(this.amazonSQS)
		queueTemplate.setDefaultDestinationName(propertiesConfig.queueUrlListener)
		return queueTemplate
	}
	
	@Bean
	public QueueMessageHandlerFactory queueMessageHandlerFactory() {
	    QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory()
	    factory.setAmazonSqs(amazonSQS)
	    factory.setSendToMessagingTemplate(queueMessagingTemplate())
	    return factory
	}
	
	@Bean
	public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
	    SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory()        
	    factory.setAmazonSqs(amazonSQS)
	    factory.setQueueMessageHandler(queueMessageHandlerFactory().createQueueMessageHandler())
	    return factory
	}
	
	@Bean
	public ClientConfiguration awsClientConfig() {
		ClientConfiguration clientConfig = InstallationUtil.getClientConfiguration()
		return clientConfig;
	}
	
	
	@Bean
	public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(AmazonSQSAsync amazonSQS) {
		SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
		factory.setAmazonSqs(amazonSQS);
		factory.setMaxNumberOfMessages(10)
		factory.setAutoStartup(true)
		factory.setWaitTimeOut(20)

		return factory;
	}
}
