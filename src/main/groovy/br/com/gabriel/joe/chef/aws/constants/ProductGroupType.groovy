package br.com.gabriel.joe.chef.aws.constants

enum ProductGroupType {
	
	SOULMV_WI5(
		"SOULMV-WI5",
		["SOUL-INTEGRATED-WORKSPACE", "SOUL-INTEGRATED-SERVICES"],
		"SOUL-HTML5"),
	SOULMV_H5(
		"SOULMV-H5",
		["SOUL-PRODUCT-WORKSPACE", "SOUL-PRODUCT-FORMS"],
		"SOUL-HTML5"),
	SOULMV_H5_REPORTS(
		"SOULMV-H5",
		["SOUL-PRODUCT-REPORTS"],
		"SOUL-HTML5"),
	SOULMV_O5(
		"SOULMV-O5",
		["SOUL-PLANO-SAUDE-WORKSPACE", "SOUL-PLANO-SAUDE-FORMS"],
		"SOUL-HTML5"),
	SOULMV_O5_REPORTS(
		"SOULMV-O5",
		["SOUL-PLANO-SAUDE-REPORTS"],
		"SOUL-HTML5"),
	CLASRISCO(
		"CLASRISCO",
		["MVSACR", "MVGESTORFLUXO", "MVPAINEL", "MVTOTEMSENHA", "MVPAINELRECEPCAO"],
		"CLASRISCO"),
	PRODUCTS(
		"PRODUCTS",
		["SOULMV-O5", "SOULMV-H5", "SOULMV-WI5", "CLASRISCO"],
		"PRODUCTS")
	
	String description, platform
	
	List<String> items


	private ProductGroupType(String description, List<String> items, String platform){
		this.description = description
		this.items = items
		this.platform = platform
	}

	@Override
	String toString() {
		description
	}
}
