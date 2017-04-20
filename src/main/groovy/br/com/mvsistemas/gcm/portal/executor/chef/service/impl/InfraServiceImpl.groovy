package br.com.mvsistemas.gcm.portal.executor.chef.service.impl


import groovy.util.logging.Slf4j

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import org.jclouds.Constants
import org.jclouds.ContextBuilder
import org.jclouds.chef.ChefContext
import org.jclouds.chef.config.ChefProperties
import org.jclouds.domain.LoginCredentials
import org.jclouds.ssh.SshClient
import org.jclouds.sshj.config.SshjSshClientModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.mv.etl.entities.DBScript
import br.com.mv.etl.util.DBUtils
import br.com.mvsistemas.gcm.portal.executor.chef.config.PropertiesConfig
import br.com.mvsistemas.gcm.portal.executor.chef.domain.ChefDetails
import br.com.mvsistemas.gcm.portal.executor.chef.service.InfraService

import com.google.common.base.Charsets
import com.google.common.collect.ImmutableSet
import com.google.common.io.Files
import com.google.common.net.HostAndPort
import com.google.inject.Key
import com.google.inject.TypeLiteral

@SuppressWarnings("deprecation")
@Service
@Slf4j
class InfraServiceImpl implements InfraService {


	@Autowired
	PropertiesConfig propertiesConfig


	@Override
	public ChefContext getChefContext() {

		ChefDetails chefDetails = getChefDetails()

		String clientCredential = Files.toString(new File( chefDetails.home + "/" + chefDetails.clientName + ".pem"), Charsets.UTF_8);
		String validatorCredential = Files.toString(new File(chefDetails.home + "/" + chefDetails.orgValidationName + ".pem"), Charsets.UTF_8);

		Properties props = new Properties();
		props.put(ChefProperties.CHEF_VALIDATOR_NAME, chefDetails.orgValidationName);
		props.put(ChefProperties.CHEF_VALIDATOR_CREDENTIAL, validatorCredential);
		props.put(Constants.PROPERTY_RELAX_HOSTNAME, "true");
		props.put(Constants.PROPERTY_TRUST_ALL_CERTS, "true");

		/* *** First, create the context to connect to the Chef Server *** */

		// Create the context and configure the SSH driver to use. sshj in this example
		ChefContext ctx = ContextBuilder.newBuilder("chef")
				.endpoint(chefDetails.endPoint)
				.credentials(chefDetails.clientName, clientCredential)
				.overrides(props)
				.modules(ImmutableSet.of(new SshjSshClientModule())) //
				.buildApi(ChefContext.class)


		return ctx


	}

	/**
	 *
	 * @return
	 */
	private ChefDetails getChefDetails(){

		ChefDetails chefDetails = new ChefDetails()

		chefDetails.clientName = propertiesConfig.chefClientName
		chefDetails.orgValidationName = propertiesConfig.chefOrganizationValidatorName
		chefDetails.organizationName = propertiesConfig.chefOrganizationName
		chefDetails.endPoint = "https://${propertiesConfig.chefServerHost}/organizations/${chefDetails.organizationName}"
		chefDetails.home = propertiesConfig.chefHome

		return chefDetails
	}

	@Override
	public SshClient getConnection(String host, String login, String password,int port) {

		SshClient.Factory sshFactory = getChefContext().unwrap().utils()
				.injector().getInstance(Key.get(new TypeLiteral<SshClient.Factory>() {}));

		SshClient ssh = sshFactory.create(HostAndPort.fromParts(host, port),
				LoginCredentials.builder().user(login).password(password).build());

		return ssh
	}

	@Override
	public void downloadItem(String url, String nameItem) {

		url += "/gcmmv/artifacts/${nameItem}"

		File f = new File("${propertiesConfig.downloadDir}/${nameItem}")

		if(!f.exists())
			f.createNewFile()

		def file = new FileOutputStream("${propertiesConfig.downloadDir}/${nameItem}")
		def out = new BufferedOutputStream(file)
		out << new URL(url).openStream()
		out.close()
	}

	@Override
	public List getNotInstalledScripts(def listScriptsPackages) {

		Map map = getNotInstalledScriptsAsMap(listScriptsPackages)
		def values = map.values()

		if (values.empty)
			return []
		else {
			def scripts = []
			values.toList().each{ scripts.addAll(it) }
			return scripts
		}
	}



	/**
	 * 
	 * @param listScriptsPackages
	 * @return
	 */
	@Override
	public Map getNotInstalledScriptsAsMap(def listScriptsPackages) {

		Map scriptsNotInstalled = [:]

		listScriptsPackages.each { scriptPackage ->

			def zipName = "${propertiesConfig.downloadDir}/${scriptPackage.filename}"
			def preEntryName = "products/${scriptPackage.productName}/database/pre/"
			def zipFile = new ZipFile(zipName)


			zipFile.entries.each {ArchiveEntry entry ->
				if (!entry.isDirectory() && entry.name.startsWith(preEntryName)) {

					InputStream is = null;

					is = zipFile.getInputStream(entry)
					def content = IOUtils.toString(is, "ISO-8859-1");

					if (scriptsNotInstalled[scriptPackage] == null) {
						scriptsNotInstalled[scriptPackage] = []
						log.debug "Getting scripts for [${scriptPackage.filename}...]"
					}
					
					def scriptName = entry.name.replace(preEntryName, "")
					def script = new DBScript(
							name:scriptName,
							productId:scriptPackage.scriptId,
							productName:scriptPackage.productName,
							productVersion:scriptPackage.version,
							phase:"pre",
							content: content)

					scriptsNotInstalled[scriptPackage] << script

					if (scriptPackage.scripts == null)
						scriptPackage.scripts = new TreeMap<String, DBScript>()

					String nameScript = DBUtils.getScriptName(script.name)
					scriptPackage.scripts[nameScript] = script

					if (is != null)
						is.close();
						
				}
			}

			if (scriptsNotInstalled[scriptPackage] != null){
				log.debug "Scripts found for [${scriptPackage.scriptId}] (total): ${scriptsNotInstalled[scriptPackage].size()}"
			}

			if (zipFile != null)
				zipFile.close();
		}

		return scriptsNotInstalled
	}
}
