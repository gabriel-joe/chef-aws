package br.com.gabriel.joe.chef.aws.service.impl


import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile

import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.jclouds.domain.LoginCredentials
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.ssh.SshClient
import org.jclouds.sshj.config.SshjSshClientModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.DigestUtils

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.GetObjectRequest
import com.google.common.net.HostAndPort
import com.google.inject.Guice
import com.google.inject.Injector

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.config.SystemPropertiesConfig
import br.com.gabriel.joe.chef.aws.domain.UserSecurity
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.util.InstallationUtil
import groovy.util.logging.Slf4j
import groovyx.net.http.RESTClient

@Service
@Slf4j
class InfraServiceImpl implements InfraService {


	@Autowired
	PropertiesConfig propertiesConfig
	
	@Autowired
	SystemPropertiesConfig systemProperties

	@Override
	public SshClient getConnection(String host, String login, String password,int port) throws BusinessException {

		Injector i = Guice.createInjector(new SshjSshClientModule(), new SLF4JLoggingModule());
		SshClient.Factory factory = i.getInstance(SshClient.Factory.class);
		SshClient connection = factory.create(HostAndPort.fromParts(host, port),
				 LoginCredentials.builder().user(login).password(password).build());
		
		return connection;
		
	}
	
	@Override
	public SshClient getChefServerConnection()
			throws BusinessException {
		
		String user = propertiesConfig.chefServerUser
		String password = propertiesConfig.chefServerPassword		
				
		if(propertiesConfig.securityApiActive) {
			UserSecurity u = getUserFromApi()
			user = u.user
			password = u.password
		}		
				
		return getConnection(propertiesConfig.chefServerHost, user, password, 22);
	}

	@Override
	public void downloadItem(String url, String nameItem) throws BusinessException {
		url += "/gcmmv/artifacts/${nameItem}"
		if(propertiesConfig.awsActive) {
			downloadFile(nameItem, url)
		} else {
			downloadFileAnotherMirror(url, nameItem)
		}
		
	}
	
	@Override
	public void downloadAndExtractFile(String url,String nameItem, String version, String type) throws Exception {
		File file = downloadFile(nameItem, url)
		def zipFile = new ZipFile(file)
		if(type) {
		  cleanupDirectory("${type}/${version}")
		} else {
		  cleanupDirectory(version)
		}
		zipFile.entries().each { it ->
			def path = null
			
			if(type) {
				path = Paths.get("${propertiesConfig.workingDir}/${type}/${version}/${it.name}")
			} else {
				path = Paths.get("${propertiesConfig.workingDir}/${version}/${it.name}")
			}
			
			if(it.directory){
				Files.createDirectories(path)
			} else {
				def parentDir = path.getParent()
				if (!Files.exists(parentDir)) {
					Files.createDirectories(parentDir)
				}
				Files.copy(zipFile.getInputStream(it), path)
			}
		}
	}
	

	@Override
	public void copyFile(String pathFrom, String pathTo) {
		File scrDir = new File(pathFrom)
		File destDir = new File(pathTo)
		
		if(scrDir.isDirectory()) {
			FileUtils.copyDirectory(scrDir, destDir);
		} else {
			FileUtils.copyFile(scrDir, destDir);
		}
			
	}

	@Override
	public void saveFile(File file, String content) {
		
		if(!file.exists()) {
			file.createNewFile()
		}

		file.withWriter('UTF-8') { writer ->
			writer.write(content)
		}
		
	}

	@Override
	public boolean checksumFile(String urlMd5, String md5Name, String fileName) {
		File md5 = downloadFile(md5Name, urlMd5)
		File file = new File("${propertiesConfig.downloadDir}/${fileName}")
		
		if(!md5.exists() || !file.exists())
			return false
			
		String hash = ""
		md5.withReader { hash = it.readLine() }
		byte[] chk = Files.readAllBytes(file.toPath());
		String existingHash = DigestUtils.md5DigestAsHex(chk);
		return existingHash == hash
	}
	
	
	/**
	 *
	 * @param nameItem
	 * @param url
	 * @return
	 */
	@Override
	public File downloadFile(String nameItem, String url) throws BusinessException {

		File fDir = new File("${propertiesConfig.downloadDir}")
		File f = new File("${propertiesConfig.downloadDir}/${nameItem}")
		
		
		if(!fDir.exists()) {
			fDir.mkdir()
		}
		
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(propertiesConfig.awsAccessKey, propertiesConfig.awsPasswordKey);
		AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(awsCreds))
				.withClientConfiguration(InstallationUtil.getClientConfiguration())
				.withRegion("us-east-1")
				.build();

		URI fileToBeDownloaded = new URI(url);

		AmazonS3URI s3URI = new AmazonS3URI(fileToBeDownloaded);

		s3Client.getObject(new GetObjectRequest(s3URI.getBucket(), s3URI.getKey()), f);

		return f

	}
	
	/**
	 * Download item from mirror different of S3 AWS
	 * @param url
	 * @param nameItem
	 * @throws BusinessException
	 */
	public void downloadFileAnotherMirror(String url, String nameItem) throws BusinessException {
		
		File fDir = new File("${propertiesConfig.downloadDir}")
		
		if(!fDir.exists())
			fDir.mkdir()

		def file = new FileOutputStream("${propertiesConfig.downloadDir}/${nameItem}")
		def out = new BufferedOutputStream(file)
		out << new URL(url).openStream()
		out.close()
		file.close()
	}
	
	/**
	 *
	 * @param version
	 * @return
	 */
	private cleanupDirectory(String version) {
		File f = new File("${propertiesConfig.workingDir}/${version}");
		if(f.exists()) {
			FileUtils.cleanDirectory(f)
		}
	}
	
	
	/**
	 * This method is responsible to access 
	 * the user information and password through the security api
	 * @return
	 */
	private UserSecurity getUserFromApi() throws BusinessException {
		
		RESTClient client = new RESTClient(propertiesConfig.securityApiUrl)
		def response = client.get(path: null);
		
		String user = response.responseData["${propertiesConfig.securityApiUserKey}"]
		String password = response.responseData["${propertiesConfig.securityApiPasswordKey}"]
		
		if(!user || !password) {
			throw new BusinessException("Usuário/Senha da API inválidos")
		} 
		
		return new UserSecurity(user: user, password: password)
		
	}
	
	
	/**
	 *
	 * @return
	 */
	@Override
	public PropertiesConfiguration getPropertyConfigFile() {
		
		def versionPath = "${propertiesConfig.workingDir}/${systemProperties.agentVersion()}/config/application.properties"
		def confDefaultPath = "${propertiesConfig.workingDir}/config/application.properties"
		PropertiesConfiguration config = null
		if(new File(confDefaultPath).exists()) {
			config = new PropertiesConfiguration(confDefaultPath)
		} else {
			config = new PropertiesConfiguration(versionPath)
		}
		
		return config
	}
	
}
