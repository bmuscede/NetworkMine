package ca.uwaterloo.cs.cs846Boa.bmuscede.gitmine;

import java.io.IOException;
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
	
	private GitHub client;
	private String[] repos;
	private String[] orgNames;
	private Map<Integer, List<GHIssue>> issueList;
	
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
		
		return true;
	}
	
	public boolean storeRepo(int ID, StorageType storage){
		//First, check if the repo exists.
		if (!issueList.containsKey(ID)) return false;
		
		//Get the associated issue data.
		Iterator<GHIssue> issues = issueList.get(ID).iterator();
		while (issues.hasNext()){
			if (storage == StorageType.DATABASE) 
				storeInDB(issues.next());
			else 
				storeInText(issues.next());
		}
		
		return true;
	}
	
	private void storeInText(GHIssue next) {
		// TODO Write this method.
	}

	private void storeInDB(GHIssue issue) {
		// TODO Write this method with SQLITE jdbc
	}

	public int getRateLim(){
		try {
			return client.getRateLimit().limit;
		} catch (Exception e) {
			return 0;
		}
	}
}
