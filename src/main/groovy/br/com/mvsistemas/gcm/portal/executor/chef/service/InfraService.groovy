package br.com.mvsistemas.gcm.portal.executor.chef.service

import org.jclouds.chef.ChefContext
import org.jclouds.ssh.SshClient

import br.com.mvsistemas.gcm.portal.executor.chef.domain.MessageHeader

@SuppressWarnings("deprecation")
public interface InfraService {

	
	public ChefContext getChefContext()
	
	public SshClient getConnection(String host,String login,String password,int port)
	
	public void downloadItem(String url,String nameItem)
	
	public List getNotInstalledScripts(def listScriptsPackages)
	
	public Map getNotInstalledScriptsAsMap(def listScriptsPackages)
	
}