package br.com.gabriel.joe.chef.aws.controller.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.SystemPropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.constants.ValidationType
import br.com.gabriel.joe.chef.aws.controller.ValidateController
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.LogService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ValidateAgentController implements ValidateController {
	
	@Autowired
	SystemPropertiesConfig systemConfig
	
	@Autowired
	LogService logService
	
	@Override
	public void executeValidation(Installation installation, MessageHeader messageHeader, String metric) throws BusinessException {
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Validando versão do agente..."))
		log.info "${ValidationType.CHECK_AGENT_VERSION} Started!"
		if(systemConfig.agentVersion() != metric) {
			throw new BusinessException("Agente desatualizado! Versão do agente atual [${systemConfig.agentVersion()}] / Versão do agente necessária ${metric} ")
		}
		log.info "${ValidationType.CHECK_AGENT_VERSION} Finished!"
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Versão do agente validada!"))
	}

}
