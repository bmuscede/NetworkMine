package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.iastate.cs.boa.BoaException;
import edu.iastate.cs.boa.LoginException;

public class ContributionBuilder extends Thread {
	private BoaManager manager;
	private String[] projects = null;
	
	private final String FILE_PLACEHOLDER = "<F_PLACEHOLDER>";
	private final String CONTRIB_PLACEHOLDER = "<C_PLACEHOLDER>";
	private final String PLACEHOLDER = "<PLACEHOLDER>";
	private final String PREFIX = "out[] = ";
	
	private final String FIND_PROJ = "./scripts/find_projects.boa";
	private final String CONTRIB = "./scripts/contributions.boa";
	
	public ContributionBuilder(){}
	
	public boolean login(String user, String pass){
		try {
			manager = new BoaManager(user, pass);
		} catch (LoginException e) {
			return false;
		}
		
		return true;
	}
	
	public boolean buildContributionNetwork(FinishedCallback cb, String[] ids){
		if (manager == null) return false;
		if (ids == null || ids.length == 0) return false;
		
		//We run a thread that gets all contributors and their commits.
		Thread builder = new Thread(){
			public void run(){
				startConstruction(cb, ids);
			}
		};
		
		//Starts the thread and r
		builder.start();
		return true;
	}
	
	public boolean getProjects(FinishedCallback cb, int contrib, int files,
			String dataset){
		if (manager == null) return false;
		
		//We run a thread that gets all projects of a particular size.
		Thread builder = new Thread(){
			public void run(){
				findProjects(cb, contrib, files);
			}
		};
		
		//Starts the thread.
		builder.start();
		return true;
	}
	
	public String[] collectProjects() throws Exception{
		if (projects == null) throw new Exception("No project query has been run yet.");
		return projects;
	}
	
	private void findProjects(FinishedCallback cb, int contrib, int files){
		//First, we load in the find project query.
		String query = loadQuery(FIND_PROJ);
		if (query == null){
			cb.onProjectFindFinish(false);
			return;
		}
		query = query.replace(FILE_PLACEHOLDER, String.valueOf(files));
		query = query.replace(CONTRIB_PLACEHOLDER, String.valueOf(contrib));
		
		//We now run the query.
		String output = "";
		try {
			output = manager.runQueryBlocking(query);
		} catch (BoaException | JobErrorException e) {
			cb.onProjectFindFinish(false);
			return;
		}
		
		//Parse the output.
		output = output.replace(PREFIX, "");
		projects = output.split("\n");

		
		//Notify that we are finished.
		cb.onProjectFindFinish(true);
	}
	
	private void startConstruction(FinishedCallback cb, String[] ids){
		//Iterate through all the supplied IDs.
		for (int i = 0; i < ids.length; i++){
			cb.informCurrentMine(ids[i]);
			
			boolean suc = runMine(ids[i]);
			if (!suc) cb.onNetworkFinish(false);
		}
		
		cb.onNetworkFinish(true);
	}

	private boolean runMine(String ID){
		//Now we load in the contribution file.
		String query = loadQuery(CONTRIB);
		if (query == null){ 
			return false;
		}
		query = query.replace(PLACEHOLDER, ID);
		
		//Runs the contributions query.
		String output = "";
		try {
			output = manager.runQueryBlocking(query);
		} catch (BoaException | JobErrorException e) {
			return false;
		}
		
		//TODO: Do something different with this.
		System.out.println(output);
		
		//Finally, we set success.
		return true;
	}

	private String loadQuery(String path) {
		//Reads in the lines.
		List<String> lines;
		try {
			lines = Files.readAllLines(Paths.get(path), 
					Charset.defaultCharset());
		} catch (IOException e) {
			return null;
		}
		
		//Iterates through all the lines.
		String output = "";
		for (String line : lines){
			output += line + "\n";
		}
		
		return output;
	}

	public String[] getDatasets() {
		return manager.getDatasets();
	};
}
