package ca.uwaterloo.cs.cs846Boa.bmuscede.mine;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kohsuke.github.*;

public class GitMiner {
	public enum StorageType{
		DATABASE("issues.db"), 
		TEXTFILE("issues.txt");
		
		private String storageLoc;
		StorageType(String loc){
			storageLoc = loc;
		}
		
		void setStorageLoc(String loc){
			storageLoc = loc;
		}
		
		String getStorageLoc(){
			return storageLoc;
		}
	}
	
	private final int TIMEOUT = 30;
	
	private GitHub client;
	private String[] repos;
	private String[] orgNames;
	private Map<Integer, List<GHIssue>> issueList;
	private Connection conn = null;
	
	public GitMiner(String username, String password) throws Exception{
		//Connects to GitHub.
		client = GitHub.connectUsingPassword(username, password);
		if (client.isCredentialValid() == false) throw new Exception("User not valid!");
		issueList = new HashMap<Integer, List<GHIssue>>();
	}
	
	public GitMiner(String[] orgNames, String[] repositoryNames) throws Exception{
		//Connects to GitHub.
		client = GitHub.connect();
		setNames(orgNames, repositoryNames);
		issueList = new HashMap<Integer, List<GHIssue>>();
	}
	
	public GitMiner(String username, String password,
			String[] orgNames, String[] repositoryNames) throws Exception{
		//Connects to GitHub.
		client = GitHub.connectUsingPassword(username, password);
		if (client.isCredentialValid() == false) throw new Exception("User not valid!");
		setNames(orgNames, repositoryNames);
		issueList = new HashMap<Integer, List<GHIssue>>();
	}
	
	public void setNames(String[] orgNames, String[] repositoryNames) throws Exception{
		//Checks if the organizations and repositories are the same.
		if (orgNames.length != repositoryNames.length)
			throw new Exception("The number of organizations and repositories " +
					"do not match!");
		
		repos = repositoryNames;
		this.orgNames = orgNames;
	}
	
	public boolean mineAllIssueData(GHIssueState state){
		//Iterate through all our issues.
		for (int i = 0; i < repos.length; i++){
			boolean succ;
			try {
				succ = mineIssues(orgNames[i], repos[i], state);
			} catch (IOException e) {
				return false;
			}
			
			if (succ == false) return false;
		}
		
		return true;
	}
	
	public boolean mineIssues(String orgName, String repoName, GHIssueState state) 
			throws IOException{
		//Search for that user.
		if (!Arrays.asList(orgNames).contains(orgName) ||
		    !Arrays.asList(repos).contains(repoName)){
			return false;
		}
		
		Integer repoID = client.getOrganization(orgName)
			.getRepository(repoName).getId();
		
		//Now we get all the isuses.
		issueList.put(repoID, client.getOrganization(orgName)
			.getRepository(repoName)
			.getIssues(state));
		
		return true;
	}
	
	public boolean storeAllData(StorageType storage){
		//We iterate through the repo hashmap.
		for (Integer key : issueList.keySet()) {
		    boolean success = storeRepo(key, storage);
		    if (!success) return false;
		}
		
		if (conn != null){
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public boolean storeRepo(int ID, StorageType storage){
		//First, check if the repo exists.
		if (!issueList.containsKey(ID)) return false;
		
		//Get the associated issue data.
		Iterator<GHIssue> issues = issueList.get(ID).iterator();
		while (issues.hasNext()){
			if (storage == StorageType.DATABASE)
				try {
					storeInDB(storage, issues.next());
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
			else 
				storeInText(issues.next());
		}
		
		return true;
	}
	
	private void storeInText(GHIssue next) {
		// TODO Write this method.
	}

	private void storeInDB(StorageType store, GHIssue issue) throws Exception{
		//Check if null
		if (conn == null) connectToDB(store);
		
		//Generate statements for query.
		int ID = issue.getId();
		String title = issue.getTitle();
		title = title.replace("\"", "\\\"");
		String createdAt = issue.getCreatedAt().toString();
		String closedAt = issue.getClosedAt().toString();
		GHUser createdBy = issue.getUser();
		GHUser closedBy = issue.getClosedBy();
		
		//Creates a query statement now.
		Statement state = conn.createStatement();
		state.setQueryTimeout(TIMEOUT);
		
		//Adds in the users.
		try {
		if (createdBy != null)
			state.executeUpdate("INSERT INTO User VALUES(" + createdBy.getId() + ",\"" +
				createdBy.getName() + "\");");
		if (closedBy != null)
			state.executeUpdate("INSERT INTO User VALUES(" + closedBy.getId() + ",\"" +
				closedBy.getName() + "\");");
		} catch (SQLException e){
			//Do nothing since it's likely that the
			//user already exists.
		}
		
		//Adds in the issue.
		String columns = "ID,Title";
		String values = ID + ",\"" + title + "\"";
		if (createdBy != null){
			columns += ",CreatedBy,CreatedDate";
			values += "," + createdBy.getId() + ",\"" + createdAt + "\"";
		}
		if (closedBy != null){
			columns += ",ClosedBy,ClosedDate";
			values += "," + closedBy.getId() + ",\"" + closedAt + "\"";
		}
		
		try {
			state.executeUpdate("INSERT INTO Issue(" + columns + ") VALUES(" +
				values + ");");
		} catch (SQLException e){
			if (e.getMessage().contains("syntax error")){
				System.out.println(title);
			}
		}
	}

	private void connectToDB(StorageType store){
		conn = null;
	    try {
	    	//Connect to the database.
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + store.getStorageLoc());
	    } catch (SQLException e){
	    	conn = null;
	    }
	}
	
	public int getRateLim(){
		try {
			return client.getRateLimit().limit;
		} catch (Exception e) {
			return 0;
		}
	}
}
