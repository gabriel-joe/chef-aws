package br.com.mvsistemas.gcm.portal.executor.chef.exception;

public class BusinessException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1739226572585722254L;
	
	/**
	 * @param mensagem
	 *            A mensagem de excecao a ser apresentada.
	 */
	public BusinessException(String message) {
		super(message);
	}	
	
	public BusinessException(String message, Throwable t) {
		super(message, t);
	}

}
