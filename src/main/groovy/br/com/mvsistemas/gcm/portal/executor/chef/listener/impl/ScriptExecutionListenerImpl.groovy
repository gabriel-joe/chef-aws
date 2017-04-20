package br.com.mvsistemas.gcm.portal.executor.chef.listener.impl

import br.com.mv.etl.entities.DBScript
import br.com.mv.etl.listener.ScriptExecutionListener
import br.com.mv.etl.loading.model.RunnigScript
import br.com.mv.etl.loading.model.StatusRunnigScript
import br.com.mv.etl.util.DBUtils
import br.com.mvsistemas.gcm.portal.executor.chef.domain.ItemAmbiente
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService
import br.com.mvsistemas.gcm.portal.executor.chef.service.LogService
import br.com.mvsistemas.gcm.portal.executor.chef.service.SqsService


class ScriptExecutionListenerImpl implements ScriptExecutionListener {
	
	
	
	InfraService infraService
	
	LogService logService
	
	SqsService sqsService
	
	Map itemsByScriptName
	
	MessageHeader messageHeader
	
	String envName
	
	public ScriptExecutionListenerImpl(MessageHeader messageHeader,def listScripts,
		InfraService infraService,LogService logService,SqsService sqsService,String envName) {
		
		this.infraService = infraService
		
		this.logService = logService
		
		this.messageHeader = messageHeader
		
		this.sqsService = sqsService
		
		this.envName = envName
		
		itemsByScriptName = [:]
		
		def scriptsMap = infraService.getNotInstalledScriptsAsMap(listScripts)
		
		scriptsMap.each { item, scripts ->
			scripts.each {DBScript script ->
				itemsByScriptName[script.name] = item
			}
		}		
		
	} 

	public void beforeExecution(String scriptName) {		
		
		def item = itemsByScriptName[scriptName]
		
		Integer currentProgress = getCurrentProgress(item, scriptName)
		
		String msg = "Verificando status do script " + scriptName + "..."
		logService.info (messageHeader,msg)
		
		println currentProgress
		
		//saveItemPhaseForEnvironment(item.scriptId,'install.atualizando.banco', currentProgress, msg )
		
		
	}

	private Integer getCurrentProgress(def item, String scriptName) {
		scriptName = DBUtils.getScriptName(scriptName)
		def position = item.scripts.findIndexOf {it.key.equals(scriptName)}+1
		def total = item.scripts.size()

		def Double percentage = (position*100)/total
		Integer currentProgress = percentage.round().intValue()
		return currentProgress
	}
 
	public void afterExecution(String scriptName, RunnigScript runningScript) {
		
		def item = itemsByScriptName[scriptName]
		
		Integer currentProgress = getCurrentProgress(item, scriptName)		

		String msg = "Script " + scriptName + " -> " + runningScript.status
		
		if (runningScript.status in [StatusRunnigScript.ALREADY_SUCCESSFULLY_EXECUTED, StatusRunnigScript.ALREADY_EXECUTED_WITH_FAIL])
			msg = "Script " + scriptName + " -> ja executado no " + runningScript.product + "." + runningScript.version  + " com " + runningScript.status
				
		logService.info (messageHeader,msg)
			
		//saveItemPhaseForEnvironment(item.scriptId,'install.atualizando.banco', currentProgress, msg )
		
	}
	
	void compileDatabase(Long invalidObjects, Integer tries, Integer currentTry) {
		logService.info (messageHeader,"Compilando ${invalidObjects} objeto(s) invalido(s) - ${currentTry} de ${tries} tentativa(s)")
	}
	
	/**
	 * 
	 * @param itemId
	 * @param phaseName
	 * @param currentProgress
	 * @param msg
	 * @return
	 */
	private saveItemPhaseForEnvironment(String itemId,String phaseName,Integer currentProgress,String msg){
		
		ItemAmbiente itemAmbiente = new ItemAmbiente(cdItem:itemId,
			cdItemStatus:phaseName,
			cdCliente:messageHeader.clientId,
			cdAmbiente:envName,
			cdPlanoExecucao:messageHeader.executionPlan,
			nrProgressoAtualizacao:currentProgress,
			dsProgressoAtualizacao:msg)
		
		def result = [:]
		result.itemAmbiente = []
		
		result.itemAmbiente.add(itemAmbiente)
		
		sqsService.generateAndSendMessage(messageHeader,result)
		
		
	}
	
	
}
