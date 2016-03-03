package ca.uwaterloo.cs.cs846Boa.bmuscede.helloworld;

import edu.iastate.cs.boa.*;

public class BoaSubmit {
	final private static int CHECK_TIME = 30;
	
	public static void main(String[] args) throws BoaException, InterruptedException{
		//Create the Boa object.
		final BoaClient client = new BoaClient();
		
		if (args.length != 3){
			System.err.println("Error: Username, password, and query must be supplied!");
			System.exit(-1);
		}
		
		//Logs the user in.
		try {
			client.login(args[0], args[1]);
		} catch (LoginException e) {
			System.err.println("Error: Invalid username and/or password!");
			System.exit(-1);
		}
		System.out.println("Successfully logged in as " + args[0] + "!");

		//We now want to run a query.
		final JobHandle j = client.query(args[2]);
		System.out.println("Submitted query as job: " + j.getId());
		
		//Iterates until job completes.
		boolean jobRunning = true;
		while(jobRunning){
			//Refreshes the variable.
			j.refresh();
			
			//Checks the execution status.
			if (j.getExecutionStatus() == ExecutionStatus.FINISHED){
				jobRunning = false;
				continue;
			} else if (j.getExecutionStatus() == ExecutionStatus.ERROR){
				System.err.println("Error: Job " + j.getId() + " has failed!");
				System.exit(1);
			}
			
			//Outputs the current status.
			System.out.println("Current status of job " + j.getId() + ": " 
					+ j.getExecutionStatus().toString());
			Thread.sleep(CHECK_TIME * 1000);
		}
		
		//Outputs job completion.
		System.out.println("\nJob " + j.getId() + " completed successfully!\nOutput:");
		
		String output = j.getOutput();
		System.out.println(output);
		
		//Afterwards, closes the connection.
		client.close();
	}
}
