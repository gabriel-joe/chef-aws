package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.MessageHeader

public interface CookbookService {

	void updateCookbook(MessageHeader messageHeader, def updateJson) throws Exception
	
	void checkCookbookVersion(MessageHeader messageHeader, def checkJson) throws Exception
	
}