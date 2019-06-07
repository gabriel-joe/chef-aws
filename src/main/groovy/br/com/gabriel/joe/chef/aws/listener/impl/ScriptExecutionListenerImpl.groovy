package br.com.gabriel.joe.chef.aws.listener.impl

import br.com.gabriel.joe.chef.aws.domain.DBScript
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.service.DatabaseService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.service.SqsService


class ScriptExecutionListenerImpl {
	
	
	DatabaseService databaseService
	
	LogService logService
	
	SqsService sqsService
	
	Map itemsByScriptName
	
	MessageHeader messageHeader
	
	String envName
	
	public ScriptExecutionListenerImpl(MessageHeader messageHeader,def listScripts,
		DatabaseService databaseService,LogService logService,SqsService sqsService,String envName) {
		
		this.databaseService = databaseService
		
		this.logService = logService
		
		this.messageHeader = messageHeader
		
		this.sqsService = sqsService
		
		this.envName = envName
		
		itemsByScriptName = [:]
		
		def scriptsMap = databaseService.getNotInstalledScriptsAsMap(listScripts)
		
		scriptsMap.each { item, scripts ->
			scripts.each {DBScript script ->
				itemsByScriptName[script.name] = item
			}
		}		
		
	} 

	public void beforeExecution(String scriptName) {		
		
		String msg = "Verificando status do script " + scriptName + "..."
		logService.info (messageHeader,msg)
		
	}

	public void afterExecution(String scriptName, def runningScript) {
		
		/*String msg = "Script " + scriptName + " -> " + runningScript.status
		
		if (runningScript.status in [StatusRunnigScript.ALREADY_SUCCESSFULLY_EXECUTED, StatusRunnigScript.ALREADY_EXECUTED_WITH_FAIL])
			msg = "Script " + scriptName + " -> ja executado no " + runningScript.product + "." + runningScript.version  + " com " + runningScript.status*/
				
		logService.info (messageHeader,"")
			
		
	}
	
	void compileDatabase(Long invalidObjects, Integer tries, Integer currentTry) {
		logService.info (messageHeader,"Compilando ${invalidObjects} objeto(s) invalido(s) - ${currentTry} de ${tries} tentativa(s)")
	}
	
	
}
