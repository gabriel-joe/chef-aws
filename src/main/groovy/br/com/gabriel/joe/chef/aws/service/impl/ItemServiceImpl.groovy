package br.com.gabriel.joe.chef.aws.service.impl

import org.jclouds.compute.domain.ExecResponse
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.domain.ItemAmbiente
import br.com.gabriel.joe.chef.aws.domain.ItemServidor
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.PhaseRecipe
import br.com.gabriel.joe.chef.aws.domain.Product
import br.com.gabriel.joe.chef.aws.service.ItemService

@Service
public class ItemServiceImpl implements ItemService {

	@Override
	public Object getInitialItemAmbiente(Object result, Set<Product> listProduct, PhaseRecipe phaseRecipe,
			MessageHeader messageHeader) {
		result.itemAmbiente = []
		result.itemAmbiente.addAll(getListItemAmbiente(listProduct,phaseRecipe.statusInitial,phaseRecipe.initialProgress, phaseRecipe.initialDescription, messageHeader))

		return result
	}

	@Override
	public Object getItemAmbienteResult(ExecResponse response, Object result, Set<Product> listProduct,
			PhaseRecipe phaseRecipe, MessageHeader messageHeader) {
		result.itemAmbiente = []

		if(response.exitStatus != 0){
			result.itemAmbiente.addAll(getListItemAmbiente(listProduct,phaseRecipe.statusError,phaseRecipe.finalProgress,"Erro - ${phaseRecipe.initialDescription}", messageHeader))
		} else {
			result.itemAmbiente.addAll(getListItemAmbiente(listProduct,phaseRecipe.statusComplete,phaseRecipe.finalProgress,phaseRecipe.finalDescription, messageHeader))
		}

		return result
	}

	@Override
	public Object getItemServidorResult(ExecResponse response, Object result, Object serverJson,
			PhaseRecipe phaseRecipe, MessageHeader messageHeader) {
		result.itemServidor = []

		if(response.exitStatus != 0){
			result.itemServidor.addAll(getListItemServidor(serverJson,phaseRecipe.statusError,messageHeader))
		} else {
			result.itemServidor.addAll(getListItemServidor(serverJson,phaseRecipe.statusComplete,messageHeader))
		}

		return result
	}
	
	/**
	 *
	 * @param listProduct
	 * @param cdItemStatus
	 * @param nrProgresso
	 * @param dsProgresso
	 * @param messageHeader
	 * @return
	 */
	private Set<ItemAmbiente> getListItemAmbiente(Set<Product> listProduct,String cdItemStatus,String nrProgresso,String dsProgresso,MessageHeader messageHeader) {

		Set<ItemAmbiente> list = new ArrayList<ItemAmbiente>()

		listProduct.each { product ->

			ItemAmbiente itemAmbiente = new ItemAmbiente(cdItem:product.itemVersion,
			cdItemStatus:cdItemStatus,
			cdCliente:messageHeader.clientId,
			cdAmbiente:product.environment,
			dhCriacao: new Date().toTimestamp().toString(),
			cdPlanoExecucao:messageHeader.executionPlan,
			nrProgressoAtualizacao:nrProgresso,
			dsProgressoAtualizacao:dsProgresso)

			list.add(itemAmbiente)
		}

		return list
	}
	
	
	/**
	 * 
	 * @param serverJson
	 * @param cdItemStatus
	 * @param envName
	 * @param messageHeader
	 * @return
	 */
	private Set<ItemServidor> getListItemServidor(def serverJson,String cdItemStatus,MessageHeader messageHeader) {

		Set<ItemServidor> list = new ArrayList<ItemServidor>()

		serverJson.app_list.each { product ->

			ItemServidor itemServidor = new ItemServidor(cdItem:product.productname + "." + product.version,
			cdItemStatus:cdItemStatus,
			cdCliente:messageHeader.clientId,
			cdAmbiente:serverJson.soulmvlinux.environment,
			cdPlanoExecucao:messageHeader.executionPlan,
			cdServidor: product.serverId)

			list.add(itemServidor)
		}

		return list
	}


	
}