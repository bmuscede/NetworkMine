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
	private String projectName;
	
	private final String FILE_PLACEHOLDER = "<F_PLACEHOLDER>";
	private final String CONTRIB_PLACEHOLDER = "<C_PLACEHOLDER>";
	private final String PLACEHOLDER = "<PLACEHOLDER>";
	
	private final String FIND_PROJ = "./scripts/find_projects.boa";
	private final String FIRST = "./scripts/project_id.boa";
	private final String SECOND = "./scripts/contributions.boa";
	
	public ContributionBuilder(){}
	
	public boolean login(String user, String pass){
		try {
			manager = new BoaManager(user, pass);
		} catch (LoginException e) {
			return false;
		}
		
		return true;
	}
	
	public boolean buildContributionNetwork(FinishedCallback cb){
		if (manager == null) return false;
		
		//We run a thread that gets all contributors and their commits.
		Thread builder = new Thread(){
			public void run(){
				startConstruction(cb);
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
	
	private void findProjects(FinishedCallback cb, int contrib, int files){
		//First, we load in the find project query.
		String query = loadQuery(FIND_PROJ);
		if (query == null){
			cb.onFinish(false);
			return;
		}
		query = query.replace(FILE_PLACEHOLDER, String.valueOf(files));
		query = query.replace(CONTRIB_PLACEHOLDER, String.valueOf(contrib));
		
		//We now run the query.
		String output = "";
		try {
			output = manager.runQueryBlocking(query);
		} catch (BoaException | JobErrorException e) {
			cb.onFinish(false);
			return;
		}
	}
	
	private void startConstruction(FinishedCallback cb){
		//First, we load in the first Boa query.
		String query = loadQuery(FIRST);
		if (query == null){ 
			cb.onFinish(false);
			return;
		}
		query = query.replace(PLACEHOLDER, projectName);
		
		//Runs the first query.
		String output = "";
		try {
			output = manager.runQueryBlocking(query);
		} catch (BoaException | JobErrorException e) {
			cb.onFinish(false);
			return;
		}
		
		//Next, we use the output to parse the project ID.
		Matcher matcher = Pattern.compile("\\d+").matcher(output);
		boolean success = matcher.find();
		if (!success){
			cb.onFinish(false);
			return;
		}
		output = matcher.group();
		
		//Now we load in the contribution file.
		query = loadQuery(SECOND);
		if (query == null){ 
			cb.onFinish(false);
			return;
		}
		query = query.replace(PLACEHOLDER, output);
		
		//Runs the contributions query.
		output = "";
		try {
			output = manager.runQueryBlocking(query);
		} catch (BoaException | JobErrorException e) {
			cb.onFinish(false);
			return;
		}
		
		//TODO Do something interesting with the output.
		
		//Finally, we set success.
		cb.onFinish(true);
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
