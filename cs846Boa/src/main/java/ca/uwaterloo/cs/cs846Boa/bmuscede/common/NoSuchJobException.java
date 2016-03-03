package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

/**
 * Exception that is thrown if a user tries to
 * look for a job that simply doesn't exist.
 * 
 * @author Bryan & Rafi
 */
public class NoSuchJobException extends Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for this exception.
	 * @param jobID The job ID that didn't exist.
	 */
	public NoSuchJobException(int jobID){
		super("Boa job " + jobID + " is not running or doesn't exist!");
	}
}
