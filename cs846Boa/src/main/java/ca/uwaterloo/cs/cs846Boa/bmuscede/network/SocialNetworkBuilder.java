package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class SocialNetworkBuilder {
	private Graph<Actor, Commit> network;
	private final String DB_LOC = "data/boa.db";
	private final int TIMEOUT = 30;
	
	public SocialNetworkBuilder(){
		//Initializes the graph.
		network = new UndirectedSparseGraph<Actor, Commit>();
	}
	
	public boolean buildSocialNetwork(String projectID){
		//Connects to the database.
		Connection conn = null;
		Statement state = null;
	    try {
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
	    	state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
	    } catch (SQLException e){
	    	e.printStackTrace();
	    	return false;
	    }
	    
	    //Reads all project files.
		readFiles(projectID, state);
		
		//Reads all project users.
		readUsers(projectID, state);
		
		//Reads all edges.
		readContributions(projectID, state);
		
		//Closes the database connection.
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	private ArrayList<String[]> readFiles(String projID, Statement state){
		ResultSet rs;
		ArrayList<String[]> results = new ArrayList<String[]>();
		
		//Develops the query.
		try {
			rs = state.executeQuery("SELECT * FROM File WHERE "
							+ "ProjectID = \"" + state + "\";");
			
			//We iterate through the results.
			while (rs.next()){
				results.add(new String[]{rs.getString("FilePath")});
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		//Returns the results.
		if (results.isEmpty()) return null;
		return results;
	}
	
	private ArrayList<String[]> readUsers(String projID, Statement state) {
		ResultSet rs;
		ArrayList<String[]> results = new ArrayList<String[]>();
		
		//Develops the query.
		try {
			rs = state.executeQuery("SELECT * FROM User "
						+ "INNER JOIN BelongsTo ON User.Username = "
						+ "BelongsTo.User WHERE "
						+ "Project = \"" + state + "\";");
			
			//We iterate through the results.
			while (rs.next()){
				results.add(new String[]{rs.getString("Username"),
						rs.getString("Email"),
						rs.getString("Name")});
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		//Returns the results.
		if (results.isEmpty()) return null;
		return results;
	}
	
	private ArrayList<String[]> readContributions(String projID, Statement state){
		ResultSet rs;
		ArrayList<String[]> results = new ArrayList<String[]>();
		
		//Develops the query.
		try {
			rs = state.executeQuery("SELECT * FROM User "
						+ "INNER JOIN BelongsTo ON User.Username = "
						+ "BelongsTo.User WHERE "
						+ "Project = \"" + state + "\";");
			
			//We iterate through the results.
			while (rs.next()){
				results.add(new String[]{rs.getString("Username"),
						rs.getString("FilePath"),
						rs.getString("CommitNum")});
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		//Returns the results.
		if (results.isEmpty()) return null;
		return results;
	}
}
