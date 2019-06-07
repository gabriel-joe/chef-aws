package br.com.gabriel.joe.chef.aws.constants

enum RecipeType {
	
	BALANCER(['recipe[mv_balancer::create_jk]','recipe[mv_balancer::create_vhost]'])
	
	List<String> recipes

	private RecipeType(List<String> recipes){
		this.recipes = recipes
	}
	
}
