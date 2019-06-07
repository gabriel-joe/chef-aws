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
import groovy.sql.Sql
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ValidateDatabaseUserMasterController implements ValidateController {
	
	
	private static Map< String, String > RULE_MASTER =  ['PACKAGE':'UTL_RECOMP','ROLE':'DBA']
	
	
	@Autowired
	LogService logService
	
	@Autowired
	DatabaseService databaseService
	
	@Override
	public void executeValidation(Installation installation, MessageHeader messageHeader, String metric) throws BusinessException {
		
		DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Validando permissões master para o usuário ${database.cdUserInstall}..."))
		log.info "${ValidationType.CHECK_DATABASE_USER_MASTER} Started!"
		
		try {
			
			Sql sql = databaseService.getInstallConnection(database)
			
			def resultSet = databaseService.getRowsUserMasterValidation(RULE_MASTER, sql)
			
			RULE_MASTER.each{key,value ->
				
				log.debug "[${ValidationType.CHECK_DATABASE_USER_MASTER}] Verify ${key} : ${value}"
				def row = resultSet.find{it.NAME == key}
				if(!row){
					
					if(value == RULE_MASTER['PACKAGE']){
						throw new BusinessException("Usuário logado [${database.cdUserInstall}] não tem permissão na PACKAGE ${RULE_MASTER['PACKAGE']}")
					} else {
						throw new BusinessException("Usuário logado [${database.cdUserInstall}] não tem permissão de DBA [${value}]")
					}
					
				}
			}
			
		} catch(SQLException e) {
			throw new BusinessException("Usuário [${database.cdUserInstall}] não tem permissão nas tabelas [USER_TAB_PRIVS e USER_ROLE_PRIVS]!")
		} 
		
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Permissões master para o usuário ${database.cdUserInstall} validadas!"))
		log.info "${ValidationType.CHECK_DATABASE_USER_MASTER} Finished!"
	}
	
}
