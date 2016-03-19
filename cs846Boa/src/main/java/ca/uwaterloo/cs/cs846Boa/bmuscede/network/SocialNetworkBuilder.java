package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.spark.mllib.linalg.Vectors;
import org.apache.spark.mllib.regression.LabeledPoint;

import ca.uwaterloo.cs.cs846Boa.bmuscede.common.ModelManager;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.Actor.ActorType;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class SocialNetworkBuilder {
	private Graph<Actor, Commit> network;
	
	//Map to hold centrality.
	Map<Actor, Double[]> scoreMap = null;
	
	//Final variables.
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
		ArrayList<String[]> files = readFiles(projectID, state);
		if (files == null) return false;
		Map<String, FileActor> fileLookup = buildLookupFiles(files);
		
		//Reads all project users.
		ArrayList<String[]> users = readUsers(projectID, state);
		if (users == null) return false;
		Map<String, UserActor> userLookup = buildLookupUsers(users);
		
		//Reads all edges.
		ArrayList<String[]> contrib = readContributions(projectID, state);
		if (contrib == null) return false;
		addEdges(fileLookup, userLookup, contrib);
		
		//Closes the database connection.
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public boolean computeCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		
		//Develops the network scoring systems.
		BetweennessCentrality<Actor, Commit> betweenCompute = 
				new BetweennessCentrality<Actor, Commit>(network);
		ClosenessCentrality<Actor, Commit> closeCompute =
				new ClosenessCentrality<Actor, Commit>(network);
		DegreeScorer<Actor> degreeCompute = 
				new DegreeScorer<Actor>(network);
		
		//Now we create our scoring map.
		scoreMap = new HashMap<Actor, Double[]>();
		
		//Finally, we iterate through the actors in our network.
		for (Actor curr : network.getVertices()){
			Double scores[] = {betweenCompute.getVertexScore(curr),
					closeCompute.getVertexScore(curr),
					(double) degreeCompute.getVertexScore(curr)};
			
			scoreMap.put(curr, scores);
		}
		
		//Success.
		return true;
	}
	
	public void performRegression(){
		//We compute centrality first.
		if (scoreMap == null) computeCentrality();
		
		//First we transform our dataset.
		List<LabeledPoint> metricEntry = new ArrayList<LabeledPoint>();
		for (Actor act : network.getVertices()){
			//We skip users and only look at files.
			if (act.getType() == ActorType.USER) continue;
		
			//Now we get all the centrality metrics and set them as a features.
			//TODO Several things. Add in normalization and set some sort of label.
			Double[] centrality = scoreMap.get(act);
			Double rand = Math.random();
			if (rand < 0.5) rand = 0d; else rand = 1d;
			metricEntry.add(new LabeledPoint
					(Math.random(), 
					Vectors.dense(centrality[0], centrality[1], centrality[2]))); 
		}
		
		//Now that we have our labeled point setup, we pass it to Spark.
		ModelManager manage = new ModelManager(60f, 100, "");
		manage.performRegressionList(metricEntry);
	}
	
	private Map<String, FileActor> 
		buildLookupFiles(ArrayList<String[]> files) {
		Map<String, FileActor> lookup = new HashMap<String, FileActor>();
		
		//Iterates through all the files.
		for (int i = 0; i < files.size(); i++){
			String[] items = files.get(i);
			FileActor file = new FileActor(items[0]);
			
			//Adds the files to the lookup table.
			lookup.put(items[0], file);
		}
		
		return lookup;
	}
	
	private Map<String, UserActor> 
		buildLookupUsers(ArrayList<String[]> users){
		Map<String, UserActor> lookup = new HashMap<String, UserActor>();
		
		//Iterates through all the users.
		for (int i = 0; i < users.size(); i++){
			String[] items = users.get(i);
			UserActor user = new UserActor(items[0], items[1], items[2]);
			
			//Adds the users to the lookup table.
			lookup.put(items[0], user);
		}
		
		return lookup;
	}
	
	private void addEdges(Map<String, FileActor> fLookup,
			Map<String, UserActor> uLookup, 
			ArrayList<String[]> contrib) {
		//Iterates through each of the edges.
		for (int i = 0; i < contrib.size(); i++){
			String[] items = contrib.get(i);
			
			//Develops the edge.
			Commit comm = new Commit(Integer.parseInt(items[2]));
			FileActor file = fLookup.get(items[1]);
			UserActor user = uLookup.get(items[0]);
			
			//Adds the edge based on the lookup table.
			network.addEdge(comm, file, user);
		}
	}
	
	private ArrayList<String[]> readFiles(String projID, Statement state){
		ResultSet rs;
		ArrayList<String[]> results = new ArrayList<String[]>();
		
		//Develops the query.
		try {
			rs = state.executeQuery("SELECT * FROM File WHERE "
							+ "ProjectID = \"" + projID + "\";");
			
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
						+ "Project = \"" + projID + "\";");
			
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
			rs = state.executeQuery("SELECT * FROM CommitData "
						+ "WHERE ProjectID = \"" + projID + "\";");
			
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
