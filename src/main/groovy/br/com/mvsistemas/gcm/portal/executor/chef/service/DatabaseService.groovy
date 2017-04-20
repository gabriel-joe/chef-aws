package br.com.mvsistemas.gcm.portal.executor.chef.service

import groovy.sql.Sql
import br.com.mvsistemas.gcm.portal.executor.chef.domain.DatabaseInformation
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader

public interface DatabaseService {

	public void checkConnection(DatabaseInformation database,MessageHeader messageHeader)
	
	public Sql getInstallConnection(DatabaseInformation database)
	
	public Sql getUserApplicationConnection(DatabaseInformation database)
	
	public String getJdbcUrl(DatabaseInformation database)
	
	public void runScripts(MessageHeader messageHeader, def scriptPackage, DatabaseInformation databaseInformation)
	
	public void compileDatabase(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void runGrantsAndSynonyms(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
	public void createIndexesAndFks(MessageHeader messageHeader, DatabaseInformation databaseInformation)
	
}