package br.com.gabriel.joe.chef.aws.service

import org.apache.commons.configuration.PropertiesConfiguration
import org.jclouds.ssh.SshClient

import br.com.gabriel.joe.chef.aws.exception.BusinessException

public interface InfraService {

	/**
	 * 
	 * @param host
	 * @param login
	 * @param password
	 * @param port
	 * @return
	 */
	public SshClient getConnection(String host,String login,String password,int port) throws BusinessException
	
	/**
	 * 
	 * @param host
	 * @param login
	 * @param password
	 * @param port
	 * @return
	 * @throws BusinessException
	 */
	public SshClient getChefServerConnection() throws BusinessException
	
	
	/**
	 * 
	 * @param url
	 * @param nameItem
	 * @throws BusinessException
	 */
	public void downloadItem(String url,String nameItem) throws BusinessException
	
	/**
	 * 
	 * @param url
	 * @param nameItem
	 * @param version
	 * @throws BusinessException
	 */
	public void downloadAndExtractFile(String url,String nameItem, String version, String type) throws Exception
	
	/**
	 * 
	 * @param pathFrom
	 * @param pathTo
	 */
	public void copyFile(String pathFrom, String pathTo)
	
	/**
	 * 
	 * @param file
	 * @param content
	 */
	public void saveFile(File file, String content)
	
	/**
	 * 
	 * @param urlMd5
	 * @param fileName
	 */
	public boolean checksumFile(String urlMd5, String md5Name, String fileName)
	
	/**
	 * 
	 * @param nameItem
	 * @param url
	 * @return
	 * @throws BusinessException
	 */
	public File downloadFile(String nameItem, String url) throws BusinessException
	
	
	public PropertiesConfiguration getPropertyConfigFile() 
}