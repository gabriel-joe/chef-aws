package br.com.gabriel.joe.chef.aws.service.impl

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.config.PropertiesConfig
import br.com.gabriel.joe.chef.aws.constants.MachineType
import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.service.KnifeService
import groovy.util.logging.Slf4j

@Slf4j
@Service
class KnifeServiceImpl implements KnifeService {
	
	@Autowired
	private PropertiesConfig propertiesConfig

	@Override
	public String getKnifeCommandCleanupAppList(String nodeName, boolean doubleQuotes) {
		StringBuilder knifeCommand = new StringBuilder()
		     
		knifeCommand.with { 
			append(" /opt/opscode/bin/knife exec -E ")
			if(doubleQuotes) {
				append(" \"nodes.transform(:name => \"${nodeName}\") ")
				append(" {|n|  n.normal[\"app_list\"] = \"\"; ")
				append(" n.normal[\"app_list\"] = \"\"; ")
				append(" }\" ")
			} else {
				append(" \"nodes.transform(:name => '${nodeName}') ")
				append(" {|n| n.normal['app_list'] =  ''; ")
				append(" n.normal['app_list'] = ''; ")
				append(" }\" ")
			}
		}
		return knifeCommand.toString()
	}

	@Override
	public String getKnifeCommandUpdateCredentials(String nodeName, boolean doubleQuotes) {
		StringBuilder knifeCommand = new StringBuilder()
		     
		knifeCommand.with { 
			append(" /opt/opscode/bin/knife exec -E ")
			if(doubleQuotes) {
				append(" \"nodes.transform(:name => \"${nodeName}\") ")
				append(" {|n|  n.normal[\"aws_access_key\"] = \"${propertiesConfig.awsAccessKey}\"; ")
				append(" n.normal[\"aws_secret_key\"] = \"${propertiesConfig.awsPasswordKey}\"; ")
				append(" }\" ")
			} else {
				append(" \"nodes.transform(:name => '${nodeName}') ")
				append(" {|n| n.normal['aws_access_key'] =  '${propertiesConfig.awsAccessKey}'; ")
				append(" n.normal['aws_secret_key'] = '${propertiesConfig.awsPasswordKey}'; ")
				append(" }\" ")
			}
			
		}
		return knifeCommand.toString()
	}

	@Override
	public String getKnifeCommandCleanupCredentials(String nodeName, boolean doubleQuotes) {
		StringBuilder knifeCommand = new StringBuilder()
		     
		knifeCommand.with { 
			append(" /opt/opscode/bin/knife exec -E ")
			if(doubleQuotes) {
				append(" \"nodes.transform(:name => \"${nodeName}\") ")
				append(" {|n|  n.normal[\"aws_access_key\"] = \"\"; ")
				append(" n.normal[\"aws_secret_key\"] = \"\"; ")
				append(" }\" ")
			} else {
				append(" \"nodes.transform(:name => '${nodeName}') ")
				append(" {|n| n.normal['aws_access_key'] =  ''; ")
				append(" n.normal['aws_secret_key'] = ''; ")
				append(" }\" ")
			}
		}
		
		return knifeCommand.toString()
	}

	@Override
	public String getKnifeCommandBootstrapLinux(Machine machine, String boostrapVersion) {
		
		StringBuilder rawString = new StringBuilder()
		
		rawString.with {
			append(" /opt/opscode/bin/knife bootstrap ")
			append(" ${machine.userName}@${machine.host} --sudo ")
			append(" -x ${machine.userName} -P '${machine.userPassword}' --use-sudo-password ")
			append(" --node-ssl-verify-mode none -c /root/knife.rb ")
			append(" -N ${machine.host} --yes --bootstrap-version ${boostrapVersion} \n ")
		}
		
		return rawString.toString()
	}

	@Override
	public String getKnifeCommandBootstrapWinrm(Machine machine, String boostrapVersion) {
		StringBuilder rawString = new StringBuilder()
		rawString.with {
			append(" /opt/opscode/bin/knife bootstrap windows winrm ")
			append(" ${machine.host} -x ${machine.userName} -P '${machine.userPassword}' ")
			append(" --node-ssl-verify-mode none -c /root/knife.rb ")
			append(" -N ${machine.host} --yes --bootstrap-version ${boostrapVersion} ")
			if(propertiesConfig.winrmShellActive)
				append " --winrm-shell ${propertiesConfig.winrmShellType} "
			if(propertiesConfig.winrmAuthenticationProtocol)
				append(" --winrm-authentication-protocol ${propertiesConfig.winrmAuthenticationProtocol} ")
			if(propertiesConfig.securityBootstrapProxy)
				append(" --bootstrap-proxy '${propertiesConfig.securityBootstrapProxy}' ")
	
		}
		
		return rawString.toString()
	}

	@Override
	public String getKnifeCommandWirmPutFile(Machine machine) {
		return getKnifeCommonWinrmExecution(machine, "chef-client -o soulmvwindows::create_installation_file")
	}

	@Override
	public String getKnifeCommandWirmChefClient(Machine machine) {
		return getKnifeCommonWinrmExecution(machine, "chef-client -j ${propertiesConfig.winrmLocationInstallation}")
	}

	@Override
	public String getKnifeCommandWinrmChefClientBalancer(Machine machine, String recipe) {
		return getKnifeCommonWinrmExecution(machine, "chef-client -o ${recipe} -j ${propertiesConfig.winrmLocationBalancer}")
	}
	
	@Override
	public String getKnifeCommandWirmChefClientZero(Machine machine) {
		return getKnifeCommonWinrmExecution(machine, "chef-client")
	}
	
	/**
	 * Common knife winrm execution
	 * @param machine
	 * @param winrmCommand
	 * @return
	 */
	private String getKnifeCommonWinrmExecution(Machine machine, String winrmCommand) {
		StringBuilder commandExecute = new StringBuilder()
		
		commandExecute.with {
			append " /opt/opscode/bin/knife winrm '${machine.host}' "
			append " '${winrmCommand}' "
			append " -m -x ${machine.userName} -P '${machine.userPassword}' "
			append " --winrm-ssl-verify-mode none --yes "
			if(propertiesConfig.winrmShellActive) {
				append " --winrm-shell ${propertiesConfig.winrmShellType} "
			}
			if(propertiesConfig.winrmAuthenticationProtocol) {
				append(" --winrm-authentication-protocol ${propertiesConfig.winrmAuthenticationProtocol} ")
			}
		}
		
		return commandExecute.toString()
	}

	@Override
	public String getKnifeCommandWinrmExecSetAttributes(Machine machine, boolean setBalancer) {
		StringBuilder rawString = new StringBuilder()
		rawString.with {
			append(" /opt/opscode/bin/knife exec -E ")
			append("  \"nodes.transform(:name => '${machine.host}') ")
			append(" { |n| n.rm_normal('attributes_file'); n.normal['attributes_file'] = File.read('/root/chef-windows.json'); ")
			if(setBalancer) {
				append(" n.normal['isBalancer'] = true; ")
			} else {
				append(" n.rm_normal('isBalancer'); ")
			}
			append(" n.save; }\" ")
			 
		}

	}

	@Override
	public String getKnifeCommandClearNodeRunList(Machine machine) {
		StringBuilder knifeCommand = new StringBuilder()
		     
		knifeCommand.with { 
			append(" /opt/opscode/bin/knife node run_list set ${machine.host} ")
			if(machine.type == MachineType.LINUX) {
				append(" \"\" ")
			} else {
				append(" \"${propertiesConfig.nodeRecipeDefault}\" ")
			}
		}
		
		return knifeCommand.toString()
	}

	@Override
	public String getKnifeLinuxDiskSpace(Machine machine) {
		StringBuilder rawString = new StringBuilder()
		
		rawString.with {
			append(" /opt/opscode/bin/knife exec -E ")
			append("  \"nodes.transform(:name => '${machine.host}') ")
			append(" { |n| puts n['filesystem']['by_device'] }\" ")
		}
		
		return rawString.toString()
	}

	@Override
	public String getKnifeWindowsDiskSpace(Machine machine) {
		StringBuilder rawString = new StringBuilder()
		
		rawString.with {
			append(" /opt/opscode/bin/knife exec -E ")
			append("  \"nodes.transform(:name => '${machine.host}') ")
			append(" { |n| n.rm_normal('filesystem','mvHome'); ")
			append("   n.normal['filesystem']['mvHome'] = n['soulmvwindows']['mvHome'];  ")
			append("   puts n['filesystem']; }\" ")
		}
		
		return rawString.toString()
	}

}
