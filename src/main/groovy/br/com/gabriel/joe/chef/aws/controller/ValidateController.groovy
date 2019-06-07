package br.com.gabriel.joe.chef.aws.controller

import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.exception.BusinessException

public interface ValidateController {

	void executeValidation(Installation installation, MessageHeader messageHeader, String metric) throws BusinessException
	
}