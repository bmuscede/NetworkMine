package uw.cs.cs846.bmuscede.common;

/**
 * Exception that is thrown when a job fails
 * that a user tries to look up.
 * 
 * @author Bryan & Rafi
 */
public class JobErrorException extends Exception {
	private static final long serialVersionUID = 1L;
	
	/**
	 * Constructor for this exception.
	 * Requires an ID is supplied whenever
	 * this error is thrown.
	 * @param id The failed job.
	 */
	public JobErrorException(int id){
		super("Boa job " + id + " has failed!");
	}
}
