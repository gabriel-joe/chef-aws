package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.DatabaseInformation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import groovy.sql.Sql

public interface DatabaseService {

	public void checkConnection(DatabaseInformation database,MessageHeader messageHeader)
	
	public Sql getInstallConnection(DatabaseInformation database)
	
	public Sql getInstallConnectionMultipleStatements(DatabaseInformation database)
	
	public Sql getUserApplicationConnection(DatabaseInformation database)
	
	public String getJdbcUrl(DatabaseInformation database)
	
	public executeConnection(DatabaseInformation database,String type)
	
	public void execAuditPre(MessageHeader messageHeader, def databaseAuditProcess, DatabaseInformation databaseInformation)
	
	public void execAuditPos(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void getAuditInformation(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void runScripts(MessageHeader messageHeader, def scriptPackage, DatabaseInformation databaseInformation)
	
	public void compileDatabase(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void runGrantsAndSynonyms(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void createIndexesAndFks(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public List getNotInstalledScripts(def listScriptsPackages) throws BusinessException
	
	public Map getNotInstalledScriptsAsMap(def listScriptsPackages) throws BusinessException
	
	public void updateVersionsRegistry(Set<Product> listProduct, DatabaseInformation databaseInformation)
	
	public List getRowsTablespaceValidation(String tablespaces, Sql sql)
	
	public List getRowsUserMasterValidation(Map tablespaces, Sql sql)
}