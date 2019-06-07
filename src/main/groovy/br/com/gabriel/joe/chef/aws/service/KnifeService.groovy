package br.com.gabriel.joe.chef.aws.service

import br.com.gabriel.joe.chef.aws.domain.Machine

public interface KnifeService {

	public String getKnifeCommandCleanupAppList(String nodeName, boolean doubleQuotes)
	
	public String getKnifeCommandUpdateCredentials(String nodeName, boolean doubleQuotes)
	
	public String getKnifeCommandCleanupCredentials(String nodeName, boolean doubleQuotes)
	
	public String getKnifeCommandBootstrapLinux(Machine machine, String boostrapVersion)
	
	public String getKnifeCommandBootstrapWinrm(Machine machine, String boostrapVersion)
	
	public String getKnifeCommandWirmPutFile(Machine machine)
	
	public String getKnifeCommandWirmChefClient(Machine machine)
	
	public String getKnifeCommandWirmChefClientZero(Machine machine)
	
	public String getKnifeCommandWinrmChefClientBalancer(Machine machine, String recipe)
	
	public String  getKnifeCommandWinrmExecSetAttributes(Machine machine, boolean setBalancer)
	
	public String getKnifeCommandClearNodeRunList(Machine machine)
	
	public String getKnifeLinuxDiskSpace(Machine machine)
	
	public String getKnifeWindowsDiskSpace(Machine machine)
	
}