package br.com.gabriel.joe.chef.aws.domain

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@ToString(includeNames=true,includeFields=true,excludes="content")
@EqualsAndHashCode(includes=["name"])
class DBScript {
	String name, productId, productName, productVersion, productOldId, phase, errorCode
	def status
	String content


}
