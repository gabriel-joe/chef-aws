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
class ValidateTablespaceDatabaseController implements ValidateController {

	//Tablespaces to be validated
	private static Map< String, Integer > TABLESPACES = ['MV2000_D'  : 28672
														,'MV2000_I'  : 28672
														,'MV2000_L'  : 8192]
	@Autowired
	DatabaseService databaseService
	
	@Autowired
	LogService logService
	
	@Override
	public void executeValidation(Installation installation, MessageHeader messageHeader, String metric)
			throws BusinessException {
		
		DatabaseInformation database = (DatabaseInformation) installation.databaseInformation
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Validando tablespaces no banco de dados..."))
		log.info "${ValidationType.CHECK_DATABASE_TABLESPACE} Started!"
		
		try {
			String tablespacesAsString = TABLESPACES.collect { k,v -> "$k" }.join("','").toUpperCase()
			Sql sql = databaseService.getInstallConnection(database)
			StringBuilder tablespacesNotFound = new StringBuilder()
			StringBuilder tablespacesWrong = new StringBuilder()
			def resultTablespace =  databaseService.getRowsTablespaceValidation(tablespacesAsString, sql)
			
			TABLESPACES.each{ key,value ->
				
				log.debug "[${ValidationType.CHECK_DATABASE_TABLESPACE}] Verify ${key} : ${value}"
				def row = resultTablespace.find{it.TABLESPACE_NAME == key}
				if(row){
					if(row.VALUE < value){
						if(tablespacesWrong.size() > 0){
							tablespacesWrong.append(", ")
						}
						log.info "[${ValidationType.CHECK_DATABASE_TABLESPACE}] Tablespaces ${value} wrong!"
						tablespacesWrong.append(key+" : "+value)
					}
				} else {
					if(tablespacesNotFound.size() > 0){
						tablespacesNotFound.append(", ")
					}
					log.info "[${ValidationType.CHECK_DATABASE_TABLESPACE}] Tablespaces ${value} not found!"
					tablespacesNotFound.append(key+" : "+value)
				}
			}
			
			if(tablespacesNotFound.size() > 0){
				logService.warn(messageHeader, new Result(result: ResultType.INFO, message: "As Tablespaces ["+tablespacesNotFound.toString()+"] nao foram criadas!"))
			}
			
			if(tablespacesWrong.size() > 0){
				logService.warn(messageHeader, new Result(result: ResultType.INFO, message: "As Tablespaces ["+tablespacesWrong.toString()+"] foram criadas com tamanho errado!"))
			}
			
		} catch(SQLException e) {
			log.error(e.getMessage(), e)
			throw new BusinessException("Usuario [${database.cdUserInstall}] nao tem permissao para verificar as tablespaces [V\$TABLESPACES]!")
		}catch(Exception e) {
			log.error(e.getMessage(), e)
			throw new BusinessException("Falha para verificar a consistência das tablespaces! / Causa ${e.getMessage()}")
		}
		
		
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Tablespaces do banco de dados validadas!"))
		log.info "${ValidationType.CHECK_DATABASE_TABLESPACE} Finished!"
		
	}
	
}
