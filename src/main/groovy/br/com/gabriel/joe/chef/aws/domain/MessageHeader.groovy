package br.com.gabriel.joe.chef.aws.domain

import br.com.gabriel.joe.chef.aws.constants.CommandType
import br.com.gabriel.joe.chef.aws.constants.MessageType

class MessageHeader {

	
	String clientId,executionPlan,environmentName,messageId,checksum
	
	CommandType command
	
	MessageType typeMessage
	
	@Override
	public boolean equals(Object obj) {
		boolean response = false
		if(obj instanceof MessageHeader) {
			MessageHeader objM = (MessageHeader) obj
			if(objM.clientId == this.clientId 
				&& objM.messageId == this.messageId
				&& objM.typeMessage.equals(this.typeMessage)) {
				response =  true
			} 
		} 
		
		return response
	}
	
}
