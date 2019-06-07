package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.exception.BusinessException

public interface InstallationService {

	
	public void executeInstallation(Installation installation,String message,MessageHeader messageHeader) throws BusinessException, Exception
	
}