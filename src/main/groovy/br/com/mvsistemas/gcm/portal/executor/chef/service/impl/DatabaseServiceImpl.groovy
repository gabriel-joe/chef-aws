package br.com.mvsistemas.gcm.portal.executor.chef.service.impl

import groovy.sql.Sql
import groovy.util.logging.Slf4j

import java.sql.SQLException

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mv.etl.CustomerETLTool
import br.com.mv.etl.entities.DBScript
import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig
import br.com.mvsistemas.gcm.portal.executor.chef.constants.Constants
import br.com.mvsistemas.gcm.portal.executor.chef.domain.DatabaseInformation
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.domain.Result
import br.com.mvsistemas.gcm.portal.executor.chef.listener.impl.ScriptExecutionListenerImpl
import br.com.mvsistemas.gcm.portal.executor.chef.service.DatabaseService
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService

@Slf4j
@Service
class DatabaseServiceImpl implements DatabaseService {

	@Autowired
	PropertiesConfig propertiesConfig

	@Autowired
	LogService logService

	@Autowired
	InfraService infraService
	
	@Autowired
	SqsService sqsService


	@Override
	public void checkConnection(DatabaseInformation database,MessageHeader messageHeader) {

		Result result = new Result()
		def response = [:]

		String validate = ""

		try {

			validate = "app"
			executeConnection(database,validate)

			validate = "install"
			executeConnection(database,"install")

			result.message = "ok"
			result.result = "ok"
		} catch (SQLException e) {

			log.error e.printStackTrace()
			result.message = "Usuário/Senha invalidos - [${validate}] ${e.toString().trim()}"
			result.result = "error"
		} catch (ClassNotFoundException e) {

			log.error e.printStackTrace()
			result.message = "Erro generico causado por [${e.getMessage()}]"
			result.result = "error"
		}

		logService.info(messageHeader,result)
	}

	/**
	 * 
	 * @param database
	 * @param type
	 * @return
	 */
	private executeConnection(DatabaseInformation database,String type) {

		String userName = ""
		String password = ""

		log.info "Check connection with database [${database}] for type [${type}]"

		if(type == "app"){
			userName = database.cdUserApp
			password = database.passwordApp
		} else {
			userName = database.cdUserInstall
			password = database.passwordInstall
		}

		if(!database.jdbcUrl)
			database.jdbcUrl = "jdbc:oracle:thin:@${database.jdbcHost}:${database.jdbcPort}:${database.serviceName}"

		Sql.newInstance(
				database.jdbcUrl,
				userName,
				password,
				propertiesConfig.jdbcDriverClassName)
	}

	@Override
	public Sql getInstallConnection(DatabaseInformation database) {

		String userName = database.cdUserInstall
		String password = database.passwordInstall

		database.jdbcUrl = getJdbcUrl(database)

		return Sql.newInstance(
				database.jdbcUrl,
				userName,
				password,
				propertiesConfig.jdbcDriverClassName)
	}

	@Override
	public Sql getUserApplicationConnection(DatabaseInformation database) {

		String userName = database.cdUserApp
		String password = database.passwordApp

		database.jdbcUrl = getJdbcUrl(database)

		return Sql.newInstance(
				database.jdbcUrl,
				userName,
				password,
				propertiesConfig.jdbcDriverClassName)
	}

	@Override
	public String getJdbcUrl(DatabaseInformation database) {

		if(!database.jdbcUrl)
			database.jdbcUrl = "jdbc:oracle:thin:@${database.jdbcHost}:${database.jdbcPort}:${database.serviceName}"

		return database.jdbcUrl
	}




	/**
	 *
	 * @param messageHeader
	 * @param scriptPackage
	 * @param urlBase
	 */
	@Override
	public void runScripts(MessageHeader messageHeader, def scriptPackage, DatabaseInformation databaseInformation) {

		logService.info (messageHeader,"Atualizando banco de dados...")
		log.debug "Updating database..."

		Integer invalidObjects = CustomerETLTool.countInvalidObjects(getJdbcUrl(databaseInformation),
				databaseInformation.cdUserInstall,
				databaseInformation.passwordInstall)

		log.debug "Invalid objects before update database: " + invalidObjects
		logService.info (messageHeader,"Total de objetos invalidos antes da atualizacao: " + invalidObjects)

		def scripts = infraService.getNotInstalledScripts(scriptPackage)

		def listener = new ScriptExecutionListenerImpl(messageHeader,scriptPackage, infraService,logService,sqsService, databaseInformation.environmentName)

		def logFilePath = propertiesConfig.logFileDir

		if (scripts.empty) {

			logService.info (messageHeader,"Nenhum script encontrado")
		} else {

			CustomerETLTool.processScripts(
					listener,
					scripts,
					getJdbcUrl(databaseInformation),
					databaseInformation.cdUserInstall,
					databaseInformation.passwordInstall,
					logFilePath,
					messageHeader.clientId,
					messageHeader.environmentName)
		}
	}

	@Override
	public void compileDatabase(MessageHeader messageHeader, DatabaseInformation databaseInformation) {

		/** List schemas to be compiled **/
		List<DBScript> scripts = new ArrayList<DBScript>()
		DBScript script
		String[] schemas = ["DBAMV", "DBASGU", "DBAPS", "MVINTEGRA"]
		int increment = 1

		Integer invalidObjectsTotal = CustomerETLTool.countInvalidObjects(getJdbcUrl(databaseInformation),
				databaseInformation.cdUserInstall,
				databaseInformation.passwordInstall)
		
		log.debug "Invalid objects after update database: " + invalidObjectsTotal
		logService.info (messageHeader,"Total de objetos invalidos apos fase de execucao dos scripts: " + invalidObjectsTotal)

		if (invalidObjectsTotal > 0) {

			logService.info (messageHeader,"Compilando os objetos invalidos do banco de dados...")
			log.debug "Compiling invalids database objects..."

			def listener = new ScriptExecutionListenerImpl(messageHeader,null, infraService,logService,sqsService,databaseInformation.environmentName)

			
			def logFilePath = propertiesConfig.logFileDir
			
			schemas.each { schema ->

				logService.info (messageHeader, "Compilando o esquema [${schema}]...")
				log.debug "Compiling schema [${schema}]..."

				script = new DBScript(
						name:"000000000${increment}.sql",
						productId:"INFRAGCM.00.000",
						productName:"INFRAGCM",
						productVersion:"00.000",
						phase:"pre",
						content: "BEGIN DBMS_UTILITY.COMPILE_SCHEMA ( '${schema}', FALSE); END;")

				increment++
				scripts << script
			}


			Integer invalidObjectsPartial = 0

			while (invalidObjectsTotal > 0) {
				
				CustomerETLTool.processScripts(null,scripts, 
					getJdbcUrl(databaseInformation), 
					databaseInformation.cdUserInstall, 
					databaseInformation.passwordInstall, 
					logFilePath, 
					false,
					messageHeader.clientId,
					messageHeader.environmentName)

				invalidObjectsPartial = CustomerETLTool.countInvalidObjects(
					getJdbcUrl(databaseInformation), 
					databaseInformation.cdUserInstall, 
					databaseInformation.passwordInstall)

				log.debug "Invalid objects after compile database: " + invalidObjectsPartial
				logService.info (messageHeader,"Restaram ${invalidObjectsPartial} objetos sem compilar!")

				if (invalidObjectsPartial != invalidObjectsTotal) {
					
					invalidObjectsTotal = invalidObjectsPartial
					logService.info (messageHeader,"Compilando os objetos invalidos do banco de dados...")
					log.debug "Compiling invalids database objects..."
					
				} else {
				
					log.debug "No more objetcs to compile."
					break
					
				}
			}

			log.debug "Compilation phase done!"
			logService.info (messageHeader,"Fase de compilacao dos objetos do banco de dados concluida.")
			
		}
	}

	@Override
	public void runGrantsAndSynonyms(MessageHeader messageHeader, DatabaseInformation databaseInformation) {
		
		String command = Constants.COMMAND_GRANT_SYNONYMS

		Sql sql = getInstallConnection(databaseInformation)
		
		log.debug("Running grants and synonyms command")
		logService.info(messageHeader,"Executando o comando de Grants e Sinonimos.")
		sql.execute(command)
		logService.info(messageHeader,"Comando de grants e sinonimos executado com sucesso.")
		
		
		
	}

	@Override
	public void createIndexesAndFks(MessageHeader messageHeader, DatabaseInformation databaseInformation) {

		log.debug("Creating foreign key indexes.")
		logService.info(messageHeader,"Criando os indices de FKs.")

		String query = Constants.QUERY_INDEX_FKS
		Sql sql = getInstallConnection(databaseInformation)
		
		def commands = getInstallConnection(databaseInformation).rows(query)
		
		commands.each { command ->
			
			if (command.FKs){
				
				try {
					sql.execute(command.FKs)
					
				} catch (SQLException ex) {
					
					if (ex.errorCode != 1408) {
						log.debug("Command [${command.FKs}] throws this exception [${ex.message}]")
					}
				}
				
			}
		}
	}
}
