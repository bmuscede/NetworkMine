package ca.uwaterloo.cs.cs846Boa.bmuscede.gitmine;

import java.io.IOException;

import org.kohsuke.github.*;

import scala.actors.threadpool.Arrays;

public class GitMiner {
	GitHub client;
	String[] repos;
	String[] orgNames;
	
	public GitMiner(String username, String password) throws Exception{
		//Connects to GitHub.
		GitHub client = GitHub.connectUsingPassword(username, password);
	}
	
	public GitMiner(String[] orgNames, String[] repositoryNames) throws Exception{
		//Connects to GitHub.
		GitHub client = GitHub.connect();
		setNames(orgNames, repositoryNames);
	}
	
	public GitMiner(String username, String password,
			String[] orgNames, String[] repositoryNames) throws Exception{
		//Connects to GitHub.
		GitHub client = GitHub.connectUsingPassword(username, password);
		setNames(orgNames, repositoryNames);
	}
	
	private void setNames(String[] orgNames, String[] repositoryNames) throws Exception{
		//Checks if the organizations and repositories are the same.
		if (orgNames.length != repositoryNames.length)
			throw new Exception("The number of organizations and repositories " +
					"do not match!");
		
		repos = repositoryNames;
		this.orgNames = orgNames;
	}
	
	public boolean getAllIssueData(){
		//Iterate through all our issues.
		for (String user : repos){
			
		}
		
		return false;
	}
	
	private boolean getIssues(String orgName, String repoName){
		//Search for that user.
		if (!Arrays.asList(orgNames).contains(orgName) ||
		    !Arrays.asList(repos).contains(repoName)){
			return false;
		}
		
		//Now we get all the isuses.
		return true;
	}
}
