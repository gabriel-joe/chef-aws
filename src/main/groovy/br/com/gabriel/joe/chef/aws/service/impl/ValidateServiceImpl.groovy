package br.com.gabriel.joe.chef.aws.service.impl

import javax.annotation.PostConstruct

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.constants.ValidationType
import br.com.gabriel.joe.chef.aws.controller.ValidateController
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.ValidateService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ValidateServiceImpl implements ValidateService {
	
	private static Map<ValidationType, ValidateController> registredValidations = [:]
	
	@Autowired
	@Qualifier('validateAgentController')
	ValidateController validateAgentController
	
	@Autowired
	@Qualifier('validateServerDiskSpaceController')
	ValidateController validateServerDiskSpaceController
	
	@Autowired
	@Qualifier('validateDatabaseConnectionController')
	ValidateController validateDatabaseConnectionController
	
	@Autowired
	@Qualifier('validateDatabaseUserMasterController')
	ValidateController validateDatabaseUserMasterController
	
	@Autowired
	@Qualifier('validateTablespaceDatabaseController')
	ValidateController validateTablespaceDatabaseController
	
	@Autowired
	LogService logService
	
	@PostConstruct
	public void initVariables() {
		registredValidations[ValidationType.CHECK_AGENT_VERSION] = validateAgentController
		registredValidations[ValidationType.CHECK_SERVERS_DISK_SPACE] = validateServerDiskSpaceController
		registredValidations[ValidationType.CHECK_DATABASE_CONNECTION] = validateDatabaseConnectionController
		registredValidations[ValidationType.CHECK_DATABASE_TABLESPACE] = validateTablespaceDatabaseController
		registredValidations[ValidationType.CHECK_DATABASE_USER_MASTER] = validateDatabaseUserMasterController
	}

	@Override
	public boolean executeValidation(Installation installation, MessageHeader messageHeader) {
		
		boolean validationResult = true

		installation.validations.each { vld ->
			ValidateController validate = registredValidations[ValidationType.valueOf(vld.name)]
			if(validate) {
				try {
					validate.executeValidation(installation,messageHeader, vld.metric)
				} catch (BusinessException e) {
					validationResult = false
					logService.error(messageHeader, new Result(result: ResultType.ERROR, message: e.getMessage()))
				}
			}	
		}
		
		
		return validationResult
		
	}
	
	
	

}
