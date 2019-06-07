package br.com.gabriel.joe.chef.aws.domain

class ItemAmbiente {
	
	String cdItem, cdAmbiente
	String cdCliente, cdPlanoExecucao
	String cdItemStatus
	String nrProgressoAtualizacao
	String dsProgressoAtualizacao
	String dhCriacao
	
	@Override
	public boolean equals(Object obj) {
		return (this.cdItem.equals(obj.cdItem) && this.cdAmbiente.equals(obj.cdAmbiente) && this.cdCliente.equals(obj.cdCliente))
	}
}
