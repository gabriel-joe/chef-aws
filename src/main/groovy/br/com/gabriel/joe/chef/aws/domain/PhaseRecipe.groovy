package br.com.gabriel.joe.chef.aws.domain

import br.com.gabriel.joe.chef.aws.constants.PhaseType

class PhaseRecipe {
	
	
	String id, initialProgress, finalProgress, statusError, statusComplete, statusInitial, initialDescription, finalDescription
	PhaseType  type
	List listServer
	
	
}
