package br.com.mvsistemas.gcm.portal.executor.chef.config

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials

class AwsCredentialsProvider implements AWSCredentialsProvider {
	
	
	String acessKey,passwordKey
	
	public AWSCredentials getCredentials() {
		
		return new BasicAWSCredentials(acessKey,
			 passwordKey)
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub
	}

}
