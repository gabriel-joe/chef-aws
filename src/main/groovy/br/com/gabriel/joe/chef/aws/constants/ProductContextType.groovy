package br.com.gabriel.joe.chef.aws.constants

enum ProductContextType {
	
	SOUL_PRODUCT_FORMS(
		"soul-product-forms"),
	SOUL_PRODUCT_REPORTS(
		"soul-product-reports"),
	SOUL_PRODUCT_WORKSPACE(
		"soul-product-workspace"),
	SOUL_PLANO_SAUDE_FORMS(
		"soul-plano-saude-forms"),
	SOUL_PLANO_SAUDE_REPORTS(
		"soul-plano-saude-reports"),
	SOUL_PLANO_SAUDE_WORKSPACE(
		"soul-plano-saude-workspace"),
	SOUL_INTEGRATED_WORKSPACE(
		"soul-mv"),
	SOUL_INTEGRATED_SERVICES(
		"soul-integrated-services"),
	MVSACR(
		"mvsacr"),
	MVGESTORFLUXO(
		"mvgestorfluxo"),
	MVPAINELRECEPCAO(
		"mvpainelrecepcao"),
	MVTOTEMSENHA(
		"mvtotemsenha"),
	MVPAINEL(
		"mvpainel")
	
	String context

	private ProductContextType(String context){
		this.context = context
	}

	@Override
	String toString() {
		context
	}
}
