package br.com.mvsistemas.gcm.portal.executor.chef.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import com.amazonaws.services.sqs.AmazonSQSAsync


@Configuration
public class AwsConfig {

	@Autowired
	private AmazonSQSAsync amazonSqs

	@Bean
	public QueueMessagingTemplate queueMessagingTemplate() {
		QueueMessagingTemplate queueTemplate = new QueueMessagingTemplate(this.amazonSqs)
		queueTemplate.setDefaultDestinationName("https://sqs.us-west-2.amazonaws.com/527455963276/sqs_portal")
		return queueTemplate
	}
	
	@Bean
	public QueueMessageHandlerFactory queueMessageHandlerFactory() {
	    QueueMessageHandlerFactory factory = new QueueMessageHandlerFactory()
	    factory.setAmazonSqs(amazonSqs)
	    factory.setSendToMessagingTemplate(queueMessagingTemplate())
	    return factory
	}
	
	@Bean
	public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
	    SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory()        
	    factory.setAmazonSqs(amazonSqs)
	    factory.setQueueMessageHandler(queueMessageHandlerFactory().createQueueMessageHandler())
	    return factory
	}
}
