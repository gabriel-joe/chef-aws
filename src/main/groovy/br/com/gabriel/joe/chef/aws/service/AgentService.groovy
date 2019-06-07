package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.MessageHeader

public interface AgentService {

	void updateAgent(MessageHeader messageHeader, def updateJson) throws Exception
	
	void checkAgentVersion(MessageHeader messageHeader, def checkJson) throws Exception
	
}