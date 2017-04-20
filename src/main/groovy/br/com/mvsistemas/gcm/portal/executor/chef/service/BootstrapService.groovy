package br.com.mvsistemas.gcm.portal.executor.chef.service

import br.com.mvsistemas.gcm.portal.executor.chef.domain.Machine
import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader
import br.com.mvsistemas.gcm.portal.executor.chef.exception.BusinessException

public interface BootstrapService {

	public void bootstrapNode(Machine machine,MessageHeader messageHeader) throws BusinessException
	
}