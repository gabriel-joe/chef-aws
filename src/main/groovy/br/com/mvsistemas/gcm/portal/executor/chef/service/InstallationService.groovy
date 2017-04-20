package br.com.mvsistemas.gcm.portal.executor.chef.service

import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader

public interface InstallationService {

	
	public void executeInstallation(def installation,String message,MessageHeader messageHeader)
	
}