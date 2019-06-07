package br.com.gabriel.joe.chef.aws.service.impl

import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.PhaseType
import br.com.gabriel.joe.chef.aws.domain.PhaseRecipe
import br.com.gabriel.joe.chef.aws.service.RecipeService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class RecipeServiceImpl implements RecipeService {

	@Override
	public List<PhaseRecipe> getListPhaseRecipe(def installation) {
		
		List<PhaseRecipe> list = []
		
		installation.executionPlan.each { action ->
			
			def serverPhase = []
			installation.list_server.each { serverJson ->
				serverJson.run_list.each {  runList -> if(runList.id == action.id) serverPhase << serverJson }
			}
			
			PhaseRecipe phaseRecipe = new PhaseRecipe(id:action.id,
				initialProgress:action.initialProgress,
				finalProgress:action.finalProgress,
				statusComplete:action.statusComplete,
				statusInitial:action.statusInitial,
				type:PhaseType.fromName(action.type),
				statusError:action.statusError,
				listServer:serverPhase,
				initialDescription:action.initialDescription,
				finalDescription:action.finalDescription
				)
			
			if(phaseRecipe.listServer.size > 0 || phaseRecipe.type.equals(PhaseType.AGENT))
				list << phaseRecipe
		}
		
		return list;
	}

}
