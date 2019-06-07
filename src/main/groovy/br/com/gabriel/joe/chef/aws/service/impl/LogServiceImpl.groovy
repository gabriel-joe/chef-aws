package br.com.gabriel.joe.chef.aws.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.LogLevel;
import br.com.gabriel.joe.chef.aws.domain.Log
import br.com.gabriel.joe.chef.aws.domain.MessageHeader;
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.SqsService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class LogServiceImpl implements LogService {
	
	@Autowired
	SqsService sqsService
	
	
	@Override
	public void info(MessageHeader messageHeader,def message) {
		createAndSendLogMessage(messageHeader,LogLevel.INFO,message)
	}

	@Override
	public void warn(MessageHeader messageHeader,def message) {
		createAndSendLogMessage(messageHeader,LogLevel.WARN,message)
	}

	@Override
	public void error(MessageHeader messageHeader,def message) {
		createAndSendLogMessage(messageHeader,LogLevel.ERROR,message)
	}
	
	
	
	/**
	 *
	 * @param messageHeader
	 * @param serverJson
	 * @param response
	 * @param result
	 * @return
	 */
	private void createAndSendLogMessage(MessageHeader messageHeader,LogLevel dsNivel,def message) {
		
		def result = [:]
		
		Log log = new Log(
				cdCliente:messageHeader.clientId,
				dsIdentificador:messageHeader.executionPlan,
				snSincronizado:"S",
				dhLog: new Date().toTimestamp().toString(),
				cdTipoLog:"plano.execucao",
				dsNivel:dsNivel.name,
				loConteudo:message
				)

		result.log = []
		result.log.add(log)
		
		sqsService.generateAndSendMessage(messageHeader,result)
		
	}

}
