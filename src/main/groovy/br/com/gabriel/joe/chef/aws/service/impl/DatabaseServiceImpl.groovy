package br.com.gabriel.joe.chef.aws.service.impl

import java.sql.SQLException

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mv.etl.CustomerETLTool
import br.com.mv.etl.entities.DBScript
import br.com.mv.etl.util.DBUtils
import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.Constants
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.domain.DatabaseInformation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.listener.impl.ScriptExecutionListenerImpl
import br.com.gabriel.joe.chef.aws.service.DatabaseService
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.SqsService
import groovy.sql.Sql
import groovy.util.logging.Slf4j

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
		String validate = ""

		try {

			validate = "app"
			executeConnection(database,validate)

			validate = "install"
			executeConnection(database, validate)
			
			result.message = "ok"
			result.result = ResultType.SUCCESS
			saveHashEnvironment(database.hashEnvironment)
			log.info("Connection successfully executed!");
			
		} catch (SQLException e) {
			log.error("Falha na conexão com o banco", e)
			result.message = "(Usuário/Senha) ou dados de conexão inválidos - [${validate}] ${e.toString().trim()}"
			result.result = ResultType.ERROR
		} catch (ClassNotFoundException er) {
			log.error("Falha na conexão com o banco", er)
			result.message = "Erro generico causado por [${er.getMessage()}]"
			result.result = ResultType.ERROR
		}

		logService.info(messageHeader,result)
	}

	/**
	 * 
	 * @param database
	 * @param type
	 * @return
	 */
	@Override
	public executeConnection(DatabaseInformation database,String type) {

		String userName;
		String password;

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

		Sql sql = Sql.newInstance(
					database.jdbcUrl,
					userName,
					password,
					propertiesConfig.jdbcDriverClassName)
		sql.close()
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
	public Sql getInstallConnectionMultipleStatements(DatabaseInformation database) {
		
		database.jdbcUrl = getJdbcUrl(database)
		def props = [user: database.cdUserInstall, password: database.passwordInstall, allowMultiQueries: 'true'] as Properties
		
		return Sql.newInstance(
				database.jdbcUrl,
				props,
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
		log.info "Updating database..."

		Integer invalidObjects = CustomerETLTool.countInvalidObjects(getJdbcUrl(databaseInformation),
				databaseInformation.cdUserInstall,
				databaseInformation.passwordInstall)

		log.info "Invalid objects before update database: " + invalidObjects
		logService.info (messageHeader,"Total de objetos invalidos antes da atualizacao: " + invalidObjects)

		def scripts = getNotInstalledScripts(scriptPackage)

		def listener = new ScriptExecutionListenerImpl(messageHeader,scriptPackage, this, logService, sqsService, databaseInformation.environmentName)

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
		
		log.info "Invalid objects after update database: " + invalidObjectsTotal
		logService.info (messageHeader,"Total de objetos invalidos apos fase de execucao dos scripts: " + invalidObjectsTotal)

		if (invalidObjectsTotal > 0) {

			logService.info (messageHeader,"Compilando os objetos invalidos do banco de dados...")
			log.info "Compiling invalids database objects..."

			def logFilePath = propertiesConfig.logFileDir
			
			schemas.each { schema ->

				logService.info (messageHeader, "Compilando o esquema [${schema}]...")
				log.info "Compiling schema [${schema}]..."

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

				log.info "Invalid objects after compile database: " + invalidObjectsPartial
				logService.info (messageHeader,"Restaram ${invalidObjectsPartial} objetos sem compilar!")

				if (invalidObjectsPartial != invalidObjectsTotal) {
					
					invalidObjectsTotal = invalidObjectsPartial
					logService.info (messageHeader,"Compilando os objetos invalidos do banco de dados...")
					log.info "Compiling invalids database objects..."
					
				} else {
				
					log.info "No more objetcs to compile."
					break
					
				}
			}

			log.info "Compilation phase done!"
			logService.info (messageHeader,"Fase de compilacao dos objetos do banco de dados concluida.")
			
		}
	}

	@Override
	public void runGrantsAndSynonyms(MessageHeader messageHeader, DatabaseInformation databaseInformation) {
		
		String command = Constants.COMMAND_GRANT_SYNONYMS

		Sql sql = getInstallConnection(databaseInformation)
		
		log.info("Running grants and synonyms command")
		logService.info(messageHeader,"Executando o comando de Grants e Sinonimos.")
		sql.execute(command)
		logService.info(messageHeader,"Comando de grants e sinonimos executado com sucesso.")
		sql.close()
		
		
		
	}

	@Override
	public void createIndexesAndFks(MessageHeader messageHeader, DatabaseInformation databaseInformation) {

		log.info("Creating foreign key indexes.")
		logService.info(messageHeader,"Criando os indices de FKs.")

		String query = Constants.QUERY_INDEX_FKS
		Sql sql = getInstallConnection(databaseInformation)
		
		def commands = sql.rows(query)
		
		commands.each { command ->
			
			if (command.FKs){
				
				try {
					sql.execute(command.FKs)
				} catch (SQLException ex) {
					if (ex.errorCode != 1408) {
						log.debug("Command [${command.FKs}] throws this exception [${ex.message}]")
					}
				} catch(Exception e) {
					log.debug("Command [${command.FKs}] throws this exception [${e.message}]")
				}
				
			}
		}
		
		sql.close()
	}
	
	@Override
	public List getNotInstalledScripts(def listScriptsPackages) throws BusinessException {

		Map map = getNotInstalledScriptsAsMap(listScriptsPackages)
		def values = map.values()

		if (values.empty)
			return []
		else {
			def scripts = []
			values.toList().each{ scripts.addAll(it) }
			return scripts
		}
	}



	/**
	 *
	 * @param listScriptsPackages
	 * @return
	 */
	@Override
	public Map getNotInstalledScriptsAsMap(def listScriptsPackages) throws BusinessException {

		Map scriptsNotInstalled = [:]

		listScriptsPackages.each { scriptPackage ->

			def zipName = "${propertiesConfig.downloadDir}/${scriptPackage.filename}"
			def preEntryName = "products/${scriptPackage.productName}/database/pre/"
			def zipFile = new ZipFile(zipName)
			def content = null
			zipFile.entries.each { ArchiveEntry entry ->
				if (!entry.isDirectory() && entry.name.startsWith(preEntryName)) {

					InputStream is = null;
					is = zipFile.getInputStream(entry)
					content = IOUtils.toString(is, "ISO-8859-1");

					if (scriptsNotInstalled[scriptPackage] == null) {
						scriptsNotInstalled[scriptPackage] = []
						log.debug "Getting scripts for [${scriptPackage.filename}...]"
					}
					
					def scriptName = entry.name.replace(preEntryName, "")
					def script = new DBScript(
							name:scriptName,
							productId:scriptPackage.scriptId,
							productName:scriptPackage.productName,
							productVersion:scriptPackage.version,
							phase:"pre",
							content: content)

					scriptsNotInstalled[scriptPackage] << script

					if (scriptPackage.scripts == null)
						scriptPackage.scripts = new TreeMap<String, DBScript>()

					String nameScript = DBUtils.getScriptName(script.name)
					scriptPackage.scripts[nameScript] = script

					if (is != null)
						is.close();
						
				}
			}

			if (scriptsNotInstalled[scriptPackage] != null){
				log.debug "Scripts found for [${scriptPackage.scriptId}] (total): ${scriptsNotInstalled[scriptPackage].size()}"
			}

			if (zipFile != null)
				zipFile.close();
		}

		return scriptsNotInstalled
	}
	
	@Override
	public void execAuditPre(MessageHeader messageHeader, databaseAuditProcess, DatabaseInformation databaseInformation)  {
		
		Sql sql = getInstallConnectionMultipleStatements(databaseInformation)
		
		try {
			File f = infraService.downloadFile(databaseAuditProcess.nameScript, databaseAuditProcess.urlScript )
			
			List<String> statements = splitStatements(f.text) 
			log.info "Execute ${f.name} [DatabaseAuditProcess (PRE)]"
			statements.each { command ->
				sql.execute(command)
			}
			
		} catch(Exception e) {
			log.info("Command [execAuditPre] throws this exception [${e.message}]")
		} finally {
			sql.close()
		}
		
	}

	@Override
	public void execAuditPos(MessageHeader messageHeader, DatabaseInformation databaseInformation) {
		Sql sql = getInstallConnection(databaseInformation)
		try {
			log.info "Execute [DatabaseAuditProcess (POS)]"
			sql.execute(Constants.ENABLE_AUDIT_TRIGGER)
		} catch(Exception e) {
			log.info("Command [execAuditPos] throws this exception [${e.message}]")
		}
		
	}

	@Override
	public void getAuditInformation(MessageHeader messageHeader, DatabaseInformation databaseInformation) {
		log.info "Execute [getAuditInformation]"
		Sql sql = getInstallConnection(databaseInformation)
		def list = sql.rows(Constants.SELECT_AUDIT_TABLE)
		log.info "[getAuditInformation] Executed with ${list.size()} rows!"
		logService.info (messageHeader, list)
		Set set = []
		set.c
	}
	
	/**
	 *
	 * @param hash
	 */
	private void saveHashEnvironment(String hash) {
		PropertiesConfiguration config = infraService.getPropertyConfigFile()
		if(config.getProperty("client.hash") == null) {
			config.addProperty("client.hash",hash)
		} else {
			config.setProperty("client.hash",hash)
		}
		
		config.save()
	}

	/**
	 * Split statements to execute
	 * @param script
	 * @return
	 */
	private List<String> splitStatements(String script) {

		List<String> statements = []
		StringBuilder buffer = new StringBuilder()
		script.eachLine { line ->
			if (line.trim().equals("/") && buffer.size() != 0) {
				statements << buffer.toString()
				buffer = new StringBuilder()
			} else {
				if (buffer.size() != 0) {
				  buffer.append(System.getProperty("line.separator"))
				}
				buffer.append( line )
			}
		}
		
		if ( statements.isEmpty() ) {
		  statements << buffer.toString()
		}	
			
		return statements
	}
	
	/**
	 * Update registry about product versions
	 * @param product
	 * @param databaseInformation
	 */
	@Override
	public void updateVersionsRegistry(Set<Product> listProduct, DatabaseInformation databaseInformation) {
		Sql installConnection = getInstallConnection(databaseInformation)
		
		String sql = Constants.SELECT_EXISTS_DBAMV_GCM_VERSAO
		
	    def rowCount = installConnection.firstRow(sql)
		
		if(rowCount.qtd > 0) {
			log.info("Updating DBAMV.GCM_VERSAO")
			updateVersions(listProduct, installConnection)
		} else {
		  log.info "Table DBAMV.GCM_VERSAO not exists"
		}
		
	}
	
	/**
	 * Clear table
	 * Insert the new value of product version
	 * @param listProduct
	 * @param sql
	 */
	private void updateVersions(Set<Product> listProduct, Sql sql) {
		
		try {
			
			listProduct.each { product ->
				
				String delete = Constants.DELETE_DBAMV_GCM_VERSAO
				sql.createPreparedQueryCommand(delete, [product.name]).execute()
				sql.commit()
				String insert = Constants.INSERT_DBAMV_GCM_VERSAO
				String version = "${product.name}.${product.version}"
				sql.execute(insert, [product.name, version])
				sql.commit()
				
			}
			
		} catch (Exception e) {
			log.error ("Update DBAMV.GCM_VERSAO failed", e)
		} finally {
			sql.close()
		}
		
		
	}

	@Override
	public List getRowsTablespaceValidation(String tablespaces, Sql sql) {
		
		StringBuilder query = new StringBuilder()
		
		query.with {
			append "SELECT X.TABLESPACE_NAME, SUM(X.MAX_MB) VALUE "
			append "  FROM  (SELECT D.TABLESPACE_NAME TABLESPACE_NAME, "
			append "                D.MAXBYTES/1024/1024 MAX_MB "
			append "           FROM SYS.DBA_DATA_FILES D, "
			append "                V\$DATAFILE V, "
			append "              (  SELECT FILE_ID, "
			append "                        SUM(BYTES) BYTES "
			append "                    FROM SYS.DBA_FREE_SPACE "
			append "                GROUP BY FILE_ID) S "
			append "        WHERE (S.FILE_ID (+)= D.FILE_ID) "
			append "          AND (D.FILE_NAME = V.NAME) "
			append "        UNION ALL "
			append "        SELECT D.TABLESPACE_NAME TABLESPACE_NAME, "
			append "               D.MAXBYTES/1024/1024 MAX_MB "
			append "          FROM SYS.DBA_TEMP_FILES D, "
			append "              V\$TEMP_SPACE_HEADER T, "
			append "              V\$TEMPFILE V "
			append "        WHERE (T.FILE_ID (+)= D.FILE_ID) "
			append "          AND (D.FILE_ID = V.FILE#)) X "
			append " WHERE X.TABLESPACE_NAME IN ( '${tablespaces}' ) "
			append " GROUP BY X.TABLESPACE_NAME "
			append " ORDER BY X.TABLESPACE_NAME "
		}
		
		def result = sql.rows(query.toString())
		
		return result
	}

	@Override
	public List getRowsUserMasterValidation(Map ruleMaster, Sql sql) {
		StringBuilder query = new StringBuilder()
		
		query.with {
			append " SELECT 'PACKAGE' NAME, PRIVILEGE VALUE "
			append "   FROM USER_TAB_PRIVS "
			append "  WHERE TABLE_NAME  = '${ruleMaster['PACKAGE']}' "
			append "  UNION ALL  "
			append " SELECT 'ROLE' NAME, GRANTED_ROLE VALUE "
			append "   FROM USER_ROLE_PRIVS "
			append "  WHERE GRANTED_ROLE = '${ruleMaster['ROLE']}' "
		}	
		
		def result = sql.rows(query.toString())
		
		return result
	}
}
