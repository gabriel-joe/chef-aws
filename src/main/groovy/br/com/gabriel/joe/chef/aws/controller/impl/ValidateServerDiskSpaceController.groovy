package br.com.gabriel.joe.chef.aws.controller.impl

import org.jclouds.compute.domain.ExecResponse
import org.jclouds.ssh.SshClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import br.com.gabriel.joe.chef.aws.constants.MachineType
import br.com.gabriel.joe.chef.aws.constants.ResultType
import br.com.gabriel.joe.chef.aws.constants.ValidationType
import br.com.gabriel.joe.chef.aws.controller.ValidateController
import br.com.gabriel.joe.chef.aws.domain.Installation
import br.com.gabriel.joe.chef.aws.domain.Machine
import br.com.gabriel.joe.chef.aws.domain.MessageHeader
import br.com.gabriel.joe.chef.aws.domain.Result
import br.com.gabriel.joe.chef.aws.exception.BusinessException
import br.com.gabriel.joe.chef.aws.service.InfraService
import br.com.gabriel.joe.chef.aws.service.KnifeService
import br.com.gabriel.joe.chef.aws.service.LogService
import br.com.gabriel.joe.chef.aws.util.InstallationUtil
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

@Slf4j
@Service
class ValidateServerDiskSpaceController implements ValidateController {
	
	@Autowired
	InfraService infraService
	
	@Autowired
	LogService logService
	
	@Autowired
	KnifeService knifeService
	
	String metricValidation = null
	
	@Override
	public void executeValidation(Installation installation, MessageHeader messageHeader, String metric) throws BusinessException {
		SshClient ssh = infraService.getChefServerConnection()
		ssh.connect()
		
		log.info "${ValidationType.CHECK_SERVERS_DISK_SPACE} Started!"
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Iniciando validação do espaço em disco dos servidores..."))
		installation.list_server.each { serverJson ->
			
			Machine machine = new Machine(host: serverJson.soulmvlinux.serverHost, userName: serverJson.soulmvlinux.serverUser, userPassword: serverJson.soulmvlinux.serverPassword, type: MachineType.fromName(serverJson.soulmvlinux.serverType))
			
			logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Checando espaço em disco livre no servidor ${machine.host}"))
			
			try {
				metricValidation = metric
				checkMachineFreeDiskSpace(machine, ssh)
			} catch(Exception e) {
				log.error(e.getMessage(), e)
				throw new BusinessException("Falha ao checar espaço em disco livre do servidor ${machine.host} / Causa ${e.getMessage()}")
			}
			
			logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Servidor ${machine.host} validado!"))
			
		}
		
		ssh.disconnect()
		log.info "${ValidationType.CHECK_SERVERS_DISK_SPACE} Finished!"
		logService.info(messageHeader, new Result(result: ResultType.INFO, message: "Validação do espaço em disco finalizada!"))
		
	}
	
	/**
	 * Clear run_list recipes to execute a simple chef-client for only
	 * update some informations of the
	 * server
	 * @param machine
	 * @param chefServerSsh
	 * @return
	 */
	private void clearNodeRunList(Machine machine, SshClient chefServerSsh) throws BusinessException  {
		String knifeCommand = knifeService.getKnifeCommandClearNodeRunList(machine)
		ExecResponse exec = chefServerSsh.exec(knifeCommand)
		
		if(exec.exitStatus != 0) {
			throw new BusinessException(exec.output)
		}
		
	}
	
	/**
	 * Execute a simple windows chef-client execution to update all informations about the
	 * node
	 * @param machine
	 * @param chefServerSsh
	 * @return
	 */
	private void executeWindowsChefClient(Machine machine, SshClient chefServerSsh) throws BusinessException {
		String knifeCommand = knifeService.getKnifeCommandWirmChefClientZero(machine)
		ExecResponse exec = chefServerSsh.exec(knifeCommand)
		
		if(exec.exitStatus != 0) {
			throw new BusinessException(exec.output)
		}
	}
	
	/**
	 * Execute a simple windows chef-client execution to update all informations about
	 * server
	 * @param machine
	 * @param sshLinux
	 * @return
	 */
	private void executeLinuxChefClient(Machine machine) {
		SshClient ssh = infraService.getConnection(machine.host, machine.userName, machine.userPassword, 22)
		ssh.connect()
		ExecResponse exec = ssh.exec("sudo -S <<< \"${machine.userPassword}\" chef-client")
		ssh.disconnect()
		
		if(exec.exitStatus != 0) {
			throw new BusinessException(exec.output)
		}
	}
	
	/**
	 * Method responsible to execute a simple chef-client 
	 * for get the last informations and check the result
	 * @param machine
	 * @param chefServerSsh
	 * @throws BusinessException
	 */
	private void checkMachineFreeDiskSpace(Machine machine, SshClient chefServerSsh) throws BusinessException  {
		
		clearNodeRunList(machine, chefServerSsh)
		
		log.info "Start check free disk space in machine ${machine.host}"
		
		if(machine.type == MachineType.LINUX) {
			execLinuxMachineFreeDiskSpace(machine, chefServerSsh)
		} else {
			execWindowsMachineFreeDiskSpace(machine, chefServerSsh)
		}
		
	}
	
	/**
	 * 
	 * @param machine
	 * @param chefServerSsh
	 * @throws BusinessException
	 */
	private void execWindowsMachineFreeDiskSpace(Machine machine, SshClient chefServerSsh) throws BusinessException {
		
		executeWindowsChefClient(machine, chefServerSsh)
		
		ExecResponse exec = chefServerSsh.exec(knifeService.getKnifeWindowsDiskSpace(machine))
		
		if(exec.exitStatus == 0) {
			checkWindowsResult(exec.output)
		} else {
			throw new BusinessException(exec.output)
		}
	}
	
	/**
	 * 
	 * @param machine
	 * @param chefServerSsh
	 * @return
	 * @throws BusinessException
	 */
	private ExecResponse execLinuxMachineFreeDiskSpace(Machine machine, SshClient chefServerSsh) throws BusinessException {
		
		executeLinuxChefClient(machine)
		
		ExecResponse exec = chefServerSsh.exec(knifeService.getKnifeLinuxDiskSpace(machine))
		
		if(exec.exitStatus == 0) {
			checkLinuxResult(exec.output)
		} else {
			throw new BusinessException(exec.output)
		}
		
	}
	
	
	/**
	 * Check windows result about disk space
	 * based on MV_HOME to get the correct partition
	 * @param result
	 * @throws BusinessException
	 */
	private void checkWindowsResult(String result) throws BusinessException {
		
		if(result) result = result.replaceAll("=>", ":").replaceAll(":nil", ":null")
		
		def fileSystem = new JsonSlurper().parseText(result)
		
		String mvHome = InstallationUtil.getMvHomeFromFileSystem(fileSystem)
		
		def mvFileSystem = fileSystem[mvHome]
		
		if(mvFileSystem) {
			checkResult(Long.parseLong(mvFileSystem.kb_available.toString()))
		} else {
			throw new BusinessException("MV_HOME [${mvHome}] não encontrada nos atributos do chef!")
		}
		
		log.info "Partition ${mvHome} with ${mvFileSystem.kb_available/1000} MB Available is valid!"
		
	}
	
	
	/**
	 * Check linux result about disk space
	 * based on partition space to get max
	 * and check metrics
	 * @param result
	 * @throws BusinessException
	 */
	private void checkLinuxResult(String result) throws BusinessException {
		
		if(result) result = result.replaceAll("=>", ":").replaceAll(":nil", ":null")
		
		def fSystemDevice = new JsonSlurper().parseText(result)
		
		def deviceList = fSystemDevice.findAll {k,v -> v.kb_size }
		
		def maxDevicePartition = deviceList.max { Long.parseLong(it.value.kb_size) }
		
		checkResult(Long.parseLong(maxDevicePartition.value.kb_available.toString()))
		
		log.info "Partition ${maxDevicePartition.key} with ${Long.parseLong(maxDevicePartition.value.kb_available)/1000} MB Available is valid!"
		
	}
	
	
	/**
	 * Method responsible to handle the result of knife 
	 * and return only numbers values
	 * @param result
	 * @return
	 */
	private void checkResult(Long kbFree) throws BusinessException {
		
		Long mbMetric = Long.parseLong(metricValidation)
		Long mbFree = (kbFree / 1000)
		
		if(mbFree < mbMetric) {
			throw new BusinessException("Servidor sem espaço em disco suficiente! Livre: ${mbFree} MB / Necessário: ${mbMetric} MB")
		}
		
	}
}
