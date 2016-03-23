package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import edu.iastate.cs.boa.BoaException;
import edu.iastate.cs.boa.LoginException;

public class ContributionBuilder extends Thread implements FinishedQuery {
	private BoaManager manager;
	private String[] projects = null;
	
	private final String FILE_PLACEHOLDER = "<F_PLACEHOLDER>";
	private final String CONTRIB_PLACEHOLDER = "<C_PLACEHOLDER>";
	private final String PLACEHOLDER = "<PLACEHOLDER>";
	private final String PREFIX = "out[] = ";
	private final String SPLIT_DIV = "] = ";
	
	private final String COMMIT_FILE_PREFIX = "commitUserFile";
	private final String BUG_PREFIX = "bugs";
	private final String COMMIT_PREFIX = "commits";
	private final String USER_PREFIX = "userInfo";
	
	private final String FIND_PROJ = "./scripts/find_projects.boa";
	private final String NETWORK = "./scripts/contribution_net.boa";
	
	private final String DB_LOC = "data/boa.db";
	private final int TIMEOUT = 30;
	private final int CHECK_TIME = 1000;
	private AtomicInteger counter;
	private AtomicBoolean failureFlag;
	
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
	
	public boolean buildContributionNetwork(FinishedCallback cb, String[] ids, String dataset){
		if (manager == null) return false;
		if (ids == null || ids.length == 0) return false;
		
		//We run a thread that gets all contributors and their commits.
		Thread builder = new Thread(){
			public void run(){
				try {
					startConstruction(cb, ids, dataset);
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
				findProjects(cb, contrib, files, dataset);
			}
		};
		
		//Starts the thread.
		builder.start();
		return true;
	}
	
	public String[] collectProjects() throws Exception{
		if (projects == null) 
			throw new Exception("No project query has been run yet.");
		return projects;
	}
	
	private void findProjects(FinishedCallback cb, int contrib, int files,
			String dataset){
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
			output = manager.runQueryBlocking(query, dataset);
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
	
	private void startConstruction(FinishedCallback cb, String[] ids, String dataset) 
			throws SQLException{
		int numSubmitted = 0;
		counter = new AtomicInteger(0);
		failureFlag = new AtomicBoolean(false);
	    
		//We iterate through all the supplied IDs.
		for (int i = 0; i < ids.length; i++){			
			//Submits the job for the current project.
			if (!runProjectMine(ids[i], dataset)) {
				cb.onNetworkFinish(false);
				failureFlag.set(true);
				return;
			}
			
			//Increments number of submitted.
			numSubmitted++;
		}
		
		//Waits until all the projects are dealt with.
		boolean block = true;
		while(block){
			//Updates number of completed projects.
			cb.informCurrentMine(counter.get(), ids.length);
			
			//Checks if we can halt.
			if (numSubmitted != counter.get()){
				try {
					Thread.sleep(CHECK_TIME);
				} catch (InterruptedException e) {
					failureFlag.set(true);
					cb.onNetworkFinish(false);
					return;
				}
			} else if (failureFlag.get()) {
				cb.onNetworkFinish(false);
				return;
			} else {
				block = false;
			}
		}

		//Notifies of success.
		cb.onNetworkFinish(true);
	}

	private boolean runProjectMine(String ID, String dataset) {
		//Now we load in the contribution file.
		String query = loadQuery(NETWORK);
		if (query == null){ 
			return false;
		}
		query = query.replace(PLACEHOLDER, ID);
		
		//Submit the job async.
		try {
			manager.runQueryAsync(ID, query, dataset, this);
		} catch (BoaException e) {
			return false;
		}
		
		return true;
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

	private boolean saveUsers(ArrayList<String> users,
			String ID, Connection conn) {
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
		for (String user : users){
			user = user.replace(USER_PREFIX + "[] = ", "");
			values = user.split(",");
			sql = "INSERT INTO User VALUES(\"" + values[0] + "\",\"" +
					values[1] + "\",\"" + values[2] + "\");";
			sqlOther = "INSERT INTO BelongsTo VALUES(\"" + values[0] + "\",\"" +
					users + "\");";
			try {
				state.executeUpdate(sql);
				state.executeUpdate(sqlOther);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}

	private boolean saveFiles(ArrayList<String> files, String ID, Connection conn) {
		//Builds new hashmaps to store values.
		Map<String, Integer> bugs = new HashMap<String, Integer>();
		Map<String, Integer> commits = new HashMap<String, Integer>();
		
		//We need to specially parse this output.
		for (String entry : files){
			if (entry.startsWith(BUG_PREFIX)){
				//Removes keyword.
				entry = entry.replace(BUG_PREFIX + "[", "");
				String[] values = entry.split(SPLIT_DIV);
				
				//Adds into the bugs map.
				bugs.put(values[0], Integer.parseInt(values[1]));
			} else {
				//Removes keyword.
				entry = entry.replace(COMMIT_PREFIX + "[", "");
				String[] values = entry.split(SPLIT_DIV);
				
				//Adds into the bugs map.
				commits.put(values[0], Integer.parseInt(values[1]));
			}
		}
		
		//Now, we build the query statements.
		String values = "";
		for (Map.Entry<String, Integer> entry : commits.entrySet()){
			values += "(\"" + ID + "\",\"" + entry.getKey() + "\"," +
					entry.getValue() + ",";
			
			//Adds in the number of bugs.
			if (bugs.containsKey(entry.getKey())){
				values += bugs.get(entry.getKey());
			} else {
				values += "0";
			}
			
			//Finishes off the end statement.
			values += "),";
		}
		values = values.substring(0, values.length() - 1) + ";";
		
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
		sql += values;
		
		//Runs the query.
		try {
			state.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}	
		
		return true;
	}

	private boolean saveCommits(ArrayList<String> commits, String ID, Connection conn) {
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
		String[] valOne, valTwo;
		for (String entry : commits){
			entry = entry.replace(COMMIT_FILE_PREFIX + "[", "");
			valOne = entry.split("]\\[");
			valTwo = valOne[1].split("] = ");
			sql += "(\"" + ID + "\",\"" + valOne[0] + "\",\"" + valTwo[0] + 
					"\",\"" + valTwo[1] + "\"),";
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

	@Override
	public void finishedQuery(String ID, String results) {
		//First, check for failure.
		if (failureFlag.get()) return;
		
		//States that the data was received.
		
		//Set the database file up.
		Connection conn = initializeDB();
		
		//Stores the current project.
		if (!storeProject(ID, conn)) {
			fail(ID);
			failureFlag.set(true);
			return;
		}
		
		//Next, we divide the output.
		ArrayList<String> files = new ArrayList<String>();
		ArrayList<String> users = new ArrayList<String>();
		ArrayList<String> commits = new ArrayList<String>();
		for (String line : results.split("\n")){
			//Sees if we're dealing with file data.
			if (line.startsWith(COMMIT_PREFIX) || 
					line.startsWith(BUG_PREFIX)){
				files.add(line);
			} else if (line.startsWith(USER_PREFIX)){
				users.add(line);
			} else {
				commits.add(line);
			}
		}
		
		//Now, we write all this data to the database.
		if (!saveFiles(files, ID, conn)){
			fail(ID);
			failureFlag.set(true);
			return;
		}
		if (!saveUsers(users, ID, conn)){
			fail(ID);
			failureFlag.set(true);
			return;
		}
		if (!saveCommits(commits, ID, conn)){
			fail(ID);
			failureFlag.set(true);
			return;
		}
		
		//Once done, we increment our number of finished queries.
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		counter.incrementAndGet();
	}

	@Override
	public void fail(String ID) {
		//Changes the failure flag.
		failureFlag.set(true);
	}
}