package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.MessageHeader

public interface ValidateService {

	boolean executeValidation(Installation installation, MessageHeader messageHeader) 
	
}