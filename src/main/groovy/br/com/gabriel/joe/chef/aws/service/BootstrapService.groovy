package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.exception.BusinessException

public interface BootstrapService {

	public void bootstrapNode(Machine machine,MessageHeader messageHeader) throws BusinessException
	
}