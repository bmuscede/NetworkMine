package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import edu.iastate.cs.boa.BoaException;
import edu.iastate.cs.boa.LoginException;

public class ContributionBuilder extends Thread {
	public enum Stage {
		PROJECT("Parsing project information."),
		FILES("Getting files in the project."),
		CONTRIB("Getting project contributors."),
		COMMIT("Getting commit information.");
		
		private String info;
		Stage(String describe){
			info = describe;
		}

		public String getName() {
			return info;
		}
	}

	
	private BoaManager manager;
	private String[] projects = null;
	
	private final String FILE_PLACEHOLDER = "<F_PLACEHOLDER>";
	private final String CONTRIB_PLACEHOLDER = "<C_PLACEHOLDER>";
	private final String PLACEHOLDER = "<PLACEHOLDER>";
	private final String PREFIX = "out[] = ";
	
	private final String FIND_PROJ = "./scripts/find_projects.boa";
	private final String FILES = "./scripts/list_files.boa";
	private final String CONTRIB = "./scripts/list_contributors.boa";
	private final String COMMITS = "./scripts/contributions.boa";
	
	private final String DB_LOC = "data/boa.db";
	private final int TIMEOUT = 30;
	
	public ContributionBuilder(){}
	
	public boolean login(String user, String pass){
		try {
			manager = new BoaManager(user, pass);
		} catch (LoginException e) {
			return false;
		}
		
		return true;
	}
	
	public String[] getDatasets() {
		return manager.getDatasets();
	}
	
	public boolean buildContributionNetwork(FinishedCallback cb, String[] ids){
		if (manager == null) return false;
		if (ids == null || ids.length == 0) return false;
		
		//We run a thread that gets all contributors and their commits.
		Thread builder = new Thread(){
			public void run(){
				try {
					startConstruction(cb, ids);
				} catch (SQLException e) {
					cb.onNetworkFinish(false);
				}
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
	
	private void startConstruction(FinishedCallback cb, String[] ids) throws SQLException{
		//Set the database file up.
		Connection conn = initializeDB();
	    
		//Iterate through all the supplied IDs.
		for (int i = 0; i < ids.length; i++){			
			//Stores the project information.
			cb.informCurrentMine(ids[i], Stage.PROJECT, i, ids.length * 4);
			if (!storeProject(ids[i], conn)) {
				conn.close();
				cb.onNetworkFinish(false);
			}
			
			//Gets the files in the project.
			cb.informCurrentMine(ids[i], Stage.FILES, i + 1, ids.length * 4);
			if (!runFiles(ids[i], conn)) {
				conn.close();
				cb.onNetworkFinish(false);
			}
			
			//Gets the contributors.
			cb.informCurrentMine(ids[i], Stage.CONTRIB, i + 2, ids.length * 4);
			if (!runContributors(ids[i], conn)) {
				conn.close();
				cb.onNetworkFinish(false);
			}
			
			//Gets all the commits.
			cb.informCurrentMine(ids[i], Stage.COMMIT, i + 3, ids.length * 4);
			if (!runCommits(ids[i], conn)) {
				conn.close();
				cb.onNetworkFinish(false);
			}
		}
		
		conn.close();
		cb.onNetworkFinish(true);
	}

	private Connection initializeDB() {	
		Connection conn = null;
	    try {
	    	//Connect to the database.
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
	    } catch (SQLException e){
	    	e.printStackTrace();
	    	conn = null;
	    }
	    
	    return conn;
	}

	private boolean storeProject(String ID, Connection conn) {
		//Add a row into the DB specifying the project.
		Statement state;
		try {
			state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		//See if we can get additional values for project.
		String name = "";
		if (projects != null){
			for (String val : projects){
				if (val.startsWith(ID)){
					name = val.split(",")[1];
				}
			}
		}
		
		//Builds the SQL statement.
		String sql = "INSERT INTO Project";
		if (name.equals("")){
			sql += "(ProjectID) VALUES(\"" + ID + "\");";
		} else {
			sql += " VALUES(\"" + ID + "\",\"" + name + "\");";
		}
		
		//Runs the query.
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			
			//Check the error message for unique.
			if (!e.getMessage().contains("UNIQUE"))
				return false;
		}
		
		return true;
	}

	private boolean runContributors(String ID, Connection conn) {
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
		
		//Parses and saves the output.
		output = output.replace(PREFIX, "");
		if (!saveContributors(ID, output.split("\n"), conn))
				return false;
		
		//Finally, we set success.
		return true;
	}

	private boolean saveContributors(String ID,
			String[] output, Connection conn) {
		//Set up the query parser.
		Statement state;
		try {
			state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		//For each file we build our own insert method.
		String sql, sqlOther;
		String[] values;
		for (String user : output){
			values = user.split(",");
			sql = "INSERT INTO User VALUES(\"" + values[0] + "\",\"" +
					values[1] + "\",\"" + values[2] + "\");";
			sqlOther = "INSERT INTO BelongsTo VALUES(\"" + values[0] + "\",\"" +
					ID + "\");";
			try {
				state.executeUpdate(sql);
				state.executeUpdate(sqlOther);
			} catch (SQLException e) {
				e.printStackTrace();
				if (!e.getMessage().contains("UNIQUE"))
					return false;
			}
		}
		return true;
	}

	private boolean runFiles(String ID, Connection conn) {
		//Now we load in the contribution file.
		String query = loadQuery(FILES);
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
		
		//Parses and saves the output.
		output = output.replace(PREFIX, "");
		if (!saveFiles(output.split("\n"), ID, conn))
				return false;
		
		return true;
	}

	private boolean saveFiles(String[] split, String ID, Connection conn) {
		//Prepares query executor.
		Statement state;
		try {
			state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		//Iterates through each of the files to build the SQL.
		String sql = "INSERT INTO File VALUES";
		for (String entry : split){
			sql += "(\"" + ID + "\",\"" + entry + "\"),";
		}
		sql = sql.substring(0, sql.length() - 1) + ";";
		
		//Runs the query.
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}	
		
		return true;
	}

	private boolean runCommits(String ID, Connection conn){
		//Now we load in the contribution file.
		String query = loadQuery(COMMITS);
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
		
		//Parses and saves the output.
		output = output.replace(PREFIX, "");
		if (!saveCommits(output.split("\n"), ID, conn))
				return false;
		
		//Finally, we set success.
		return true;
	}

	private boolean saveCommits(String[] output, String ID, Connection conn) {
		//Prepares query executor.
		Statement state;
		try {
			state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		//Goes through the output.
		String sql = "INSERT INTO CommitData VALUES";
		String[] values;
		for (String entry : output){
			values = entry.split(",");
			sql += "(\"" + ID + "\",\"" + values[0] + "\",\"" + values[1] + 
					"\",\"" + values[2] + "\"),";
		}
		sql = sql.substring(0, sql.length() - 1) + ";";
		
		//Runs the query.
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
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
}