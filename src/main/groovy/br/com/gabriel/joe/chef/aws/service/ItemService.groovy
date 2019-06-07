package br.com.gabriel.joe.chef.aws.service

import org.jclouds.compute.domain.ExecResponse

import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.PhaseRecipe
import br.com.gabriel.joe.chef.aws.domain.Product

public interface ItemService {

	Object getInitialItemAmbiente(def result, Set<Product> listProduct, PhaseRecipe phaseRecipe, MessageHeader messageHeader)
	
	Object getItemAmbienteResult(ExecResponse response,def result, Set<Product> listProduct, PhaseRecipe phaseRecipe, MessageHeader messageHeader)
	
	Object getItemServidorResult(ExecResponse response, def result, serverJson, PhaseRecipe phaseRecipe, MessageHeader messageHeader)
	
}