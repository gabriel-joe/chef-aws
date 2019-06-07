package br.com.gabriel.joe.chef.aws.controller.impl

import java.sql.SQLException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.constants.ValidationType
import br.com.gabriel.joe.chef.aws.controller.ValidateController
import br.com.gabriel.joe.chef.aws.domain.DatabaseInformation
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.DatabaseService
import br.com.gabriel.joe.chef.aws.service.LogService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ValidateDatabaseConnectionController implements ValidateController {

	@Autowired
	LogService logService
	
	@Autowired
	DatabaseService databaseService
	
	@Override
	public void executeValidation(Installation installation, MessageHeader messageHeader, String metric) throws BusinessException {
		
		
		DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
		String validate = ""
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Validando conexão com o banco de dados..."))
		log.info "${ValidationType.CHECK_DATABASE_CONNECTION} Started!"
		
		try {
			validate = "app"
			databaseService.executeConnection(database, "app")
			validate = "install"
			databaseService.executeConnection(database, "install")
		} catch (SQLException e) {
			log.error("Falha na conexão com o banco", e)
			throw new BusinessException("(Usuário/Senha) ou dados de conexão inválidos - [${validate}] ${e.toString().trim()}")
		} catch (ClassNotFoundException er) {
			log.error("Falha na conexão com o banco", er)
			throw new BusinessException("Falha na conexão com o banco / Erro generico causado por [${er.getMessage()}]")
		}
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Conexão com o banco de dados validada!"))
		log.info "${ValidationType.CHECK_DATABASE_CONNECTION} Finished!"
		
	}
}
