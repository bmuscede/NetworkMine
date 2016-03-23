package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

import java.util.HashMap;
import java.util.Map;
import edu.iastate.cs.boa.*;

/**
 * The main BoaManager class that is
 * used for connecting to the Boa service
 * and for running specified queries.
 * Simplifies having to deal with the
 * API directly.
 * 
 * @author Bryan & Rafi
 */
public class BoaManager extends Thread {
	private BoaClient client;
	private Map<Integer, JobHandle> runningJobs;
	private String username, password;
	
	private final int CHECK_TIME = 1000;
	
	/**
	 * Constructor that builds the BoaManager object.
	 * Connects to the Boa service with the supplied
	 * username and password.
	 * @param username The desired username.
	 * @param password The desired password.
	 * @throws LoginException 
	 */
	public BoaManager(String username, String password) throws LoginException{
		//Create the client object.
		client = new BoaClient();
		client.login(username, password);
		
		//Stores the username and password supplied.
		this.username = username;
		this.password = password;
		
		//Creates a vector to hold all running jobs.
		runningJobs = new HashMap<Integer, JobHandle>();
	}
	
	/**
	 * Closes the connection to the Boa server.
	 * @throws BoaException
	 */
	public void closeConnection() throws BoaException{
		client.close();
	}
	
	/**
	 * Runs a Boa query and blocks on the thread
	 * until the query is completed and the
	 * results are returned.
	 * @param boaQuery The query to be run.
	 * @return The results.
	 * @throws BoaException
	 * @throws JobErrorException 
	 */
	public String runQueryBlocking(String boaQuery, String dataset)
			throws BoaException, 
		JobErrorException {
		//First, submit the job.
		JobHandle curJob = submitJob(boaQuery, dataset);
		
		//Now we run the job.
		boolean jobRunning = true;
		while(jobRunning){
			//Refreshes the variable.
			curJob.refresh();
			
			//Checks the execution status.
			if (curJob.getExecutionStatus() == ExecutionStatus.FINISHED){
				jobRunning = false;
				continue;
			} else if (curJob.getExecutionStatus() == ExecutionStatus.ERROR){
				throw new JobErrorException(curJob.getId());
			}
			
			try {
				Thread.sleep(CHECK_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		//Get the output and return.
		String output = curJob.getOutput();
		return output;
	}
	
	/**
	 * Runs a query and returns the job ID. The
	 * job will run in the background on Boa's servers.
	 * @param boaQuery The query to be run.
	 * @return The job ID for the query.
	 * @throws BoaException
	 */
	public int runQuery(String boaQuery, String dataset) throws BoaException {
		//First, submit the job.
		JobHandle curJob = submitJob(boaQuery, dataset);
		
		//Add it to the HashMap
		int ID = curJob.getId();
		runningJobs.put(ID, curJob);
		
		return ID;
	}
	
	/**
	 * Runs the query but asynchronously in a new thread.
	 * Calls back the calling object.
	 * @param boaQuery The query to run.
	 * @param dataset The dataset to run on.
	 * @param callback The method to "call back" the function when complete.
	 * @throws BoaException
	 */
	public void runQueryAsync(String ID, String boaQuery, String dataset, 
			FinishedQuery callback) throws BoaException {
		//Starts a new thread.
		Thread builder = new Thread(){
			public void run(){
				try {
					//Runs the query but blocking.
					callback.finishedQuery(ID,
							runQueryBlocking(boaQuery, dataset));
				} catch (BoaException | JobErrorException e) {
					callback.fail(ID);
				}
			}
		};
		
		//Runs the thread.
		builder.start();
	}
	
	/**
	 * Gets the results of the query for
	 * the specified job ID.
	 * @param jobID The job ID for the query.
	 * @return Either a string of output or null. Null
	 * means that the query is still running.
	 * @throws JobErrorException
	 * @throws NoSuchJobException
	 * @throws BoaException
	 */
	public String getResults(int jobID) 
			throws JobErrorException, NoSuchJobException, BoaException{
		//Looks up that result in the job table.
		JobHandle job = runningJobs.get(jobID);
		if (job == null) throw new NoSuchJobException(jobID);
		try {
			job.refresh();
		} catch (NotLoggedInException e) {
			relogin();
			job.refresh();
		}
		
		//Check the status of the job.
		String output = null;
		if (job.getExecutionStatus() == ExecutionStatus.ERROR){
			runningJobs.remove(jobID);
			throw new JobErrorException(jobID);
		} else if (job.getExecutionStatus() == ExecutionStatus.FINISHED){
			output = job.getOutput();
			runningJobs.remove(jobID);
		}
		
		return output;
	}
	
	/**
	 * Submits a job to the Boa servers.
	 * @param boaQuery The query to be run.
	 * @return The job handle of the job.
	 * @throws BoaException
	 */
	private JobHandle submitJob(String boaQuery, String dataset) 
			throws BoaException{
		//Gets the Input handler.
		InputHandle data = client.getDataset(dataset);
		
		//Create the job handle.
		JobHandle j = null;
		try {
			if (data == null)
				j = client.query(boaQuery);
			else
				j = client.query(boaQuery, data);
		} catch (NotLoggedInException e){
			//Try to log in and resubmit.
			relogin();
			if (data == null)
				j = client.query(boaQuery);
			else
				j = client.query(boaQuery, data);
		}
		
		return j;
	}
	
	/**
	 * Relogs back in to Boa's servers. The
	 * username and password should be valid.
	 * The only exception to this is if Boa's
	 * servers are unreachable.
	 */
	private void relogin(){
		try {
			client.login(username, password);
		} catch (LoginException e) {
			//This should never happen since
			//the username and password are already
			//established valid.
			System.err.println("Unhandled exception occurred. Maybe "
					+ "Boa is down?");
		}
	}

	public String[] getDatasets() {
		String[] names = null;
		try {
			names = client.getDatasetNames();
		} catch (Exception e) {
			relogin();
		}
		return names;
	}
}
