package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.PhaseRecipe

public interface RecipeService {

	List<PhaseRecipe> getListPhaseRecipe(def installation) 
	
}