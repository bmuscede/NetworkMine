package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import org.apache.commons.math3.stat.correlation.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections15.Transformer;
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
	private enum ColumnType{
		COMMIT(0, "Commits"),
		FAILURE(1, "Failures"),
		BETWEEN(2, "Betweenness Centrality"),
		CLOSE(3, "Closeness Centrality"),
		DEGREE(4, "Degree Centrality");
		
		private int val;
		private String lab;
		
		ColumnType(int num, String label){
			val = num;
			lab = label;
		}
		
		public String getLabel(){
			return lab;
		}
		
		public static ColumnType valueOf(int value) {
		    for (ColumnType type : values()) {
		        if (type.val == value) {
		            return type;
		        }
		    }    
		    throw new IllegalArgumentException(String.valueOf(value));
		}
		
		public static String labelFor(int value) {
		    for (ColumnType type : values()) {
		        if (type.val == value) {
		            return type.getLabel();
		        }
		    }    
		    throw new IllegalArgumentException(String.valueOf(value));
		}
	}
	
	private Graph<Actor, Commit> network;
	
	//Map to hold centrality.
	Map<Actor, Double[]> scoreMap = null;
	
	//Final variables.
	private final static String DB_LOC = "data/boa.db";
	private final static int TIMEOUT = 30;
	private final static int MAX_CORR = 5;
	private final int NUM_REGRESS_ITER = 100;
	

	public static String performFunctionsOnAll(String output, int iterations) {
		//Creates an instance of the social network builder.
		SocialNetworkBuilder snb = new SocialNetworkBuilder();
		
		//Pulls a list of all projects from the database.
		String sql = "SELECT ProjectID FROM Project;";
		
		Connection conn = null;
		Statement state = null;
		ArrayList<String> results = new ArrayList<String>();
	    try {
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
	    	state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
			
			//Runs the query to get all project IDs
			ResultSet rs = state.executeQuery(sql);
			while (rs.next()){
				results.add(rs.getString("ProjectID"));
			}
		    conn.close();
	    } catch (SQLException e){
	    	e.printStackTrace();
	    	return "";
	    }
	    
	    //Next, we iterate through all the IDs and run our program on it.
	    String csv = "";
	    for (String ID : results){
	    	snb.buildSocialNetwork(ID);
	    	
	    	snb.computeCentrality();
	    	
	    	double[][] spearman = snb.performSpearmanCorrelation();
	    	
	    	double[][] pR = snb.performRegression(output + "_" + ID, iterations);
	    	csv += generateCSV(ID, spearman, pR);
	    }
	    
	    return csv;
	}
	
	
	public static String performFunctions(String ID, String output, int iterations){
		//Creates an instance of the social network builder.
		SocialNetworkBuilder snb = new SocialNetworkBuilder();
		
		//Builds the social network.
		snb.buildSocialNetwork(ID);
    	
		//Computes the centrality.
    	snb.computeCentrality();
    	
    	//Computes the Spearman and PR values.
    	double[][] spearman = snb.performSpearmanCorrelation();
    	double[][] pR = snb.performRegression(output + "_" + ID, iterations);
    	
    	//Returns the final result.
    	return generateCSV(ID, spearman, pR);
	}
	
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
		
		//Builds a transformer to get edge weights.
		Transformer<Commit, Integer> edgeWeights = 
				new Transformer<Commit, Integer>() {
			public Integer transform(Commit curCommit){
				return curCommit.getWeight();
			}
		};

		//Develops the network scoring systems.
		BetweennessCentrality<Actor, Commit> betweenCompute = 
				new BetweennessCentrality<Actor, Commit>(network, edgeWeights);
		ClosenessCentrality<Actor, Commit> closeCompute =
				new ClosenessCentrality<Actor, Commit>(network, edgeWeights);
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
	
	public double[][] performRegression(String output, int iterations){
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
			
			//Get the label.
			FileActor file = (FileActor) act;
			double bugProportion = (double) file.getBugFixes() / file.getCommits();
			int label = (bugProportion > 0.2) ? 1 : 0;
			
			//Adds the feature.
			metricEntry.add(new LabeledPoint
					(label, 
					Vectors.dense(centrality[0], centrality[1], centrality[2]))); 
		}
		
		//Now that we have our labeled point setup, we pass it to Spark.
		ModelManager manage = new ModelManager(60f, NUM_REGRESS_ITER);
		manage.runIterations(metricEntry, output,  iterations);
		
		//Gets the precision and recall for each.
		double[][] prResults = new double[2][iterations];
		prResults[0] = manage.getPrecision();
		prResults[1] = manage.getRecall();
		
		return prResults;
	}
	
	public double[][] performSpearmanCorrelation(){
		double[][] xCol, yCol;
		double[][] correlation = new double[MAX_CORR][MAX_CORR];
		
		//We compute centrality first.
		if (scoreMap == null) computeCentrality();
		
		//Calculate number of files.
		int numFiles = 0;
		for (Actor ac : network.getVertices())
			if (ac instanceof FileActor) numFiles++;
		
		//Creates a generic SpearmansCorrelation object.
		SpearmansCorrelation corr = new SpearmansCorrelation();
		
		//Populates all columns.
		xCol = populateAll(numFiles);
		yCol = populateAll(numFiles);
		
		//Performs correlation analysis.
		for (int i = 0; i < MAX_CORR; i++){
			for (int j = 0; j < MAX_CORR; j++){
				//Performs Spearmans correlation.
				correlation[i][j] = corr.correlation(xCol[i], yCol[j]);
			}
		}
		
		return correlation;
	}
	
	private double[][] populateAll(int size){
		double[][] columnVals = new double[MAX_CORR][size];
		
		//Populate the columns for all metrics.
		for (int i = 0; i < MAX_CORR; i++){
			columnVals[i] = populateColumn(size, ColumnType.valueOf(i));
		}
		
		return columnVals;
	}
	
	private double[] populateColumn(int size, ColumnType type){
		double[] result = new double[size];
		
		//Goes through each of the files.
		int i = 0;
		for (Actor vertex : network.getVertices()){
			if (vertex instanceof UserActor) continue;
			FileActor file = (FileActor) vertex;
			
			//Sees how we want to fill up the column.
			switch (type){
				case COMMIT:
					result[i] = file.getCommits();
					break;
				case FAILURE:
					result[i] = file.getBugFixes();
					break;
				case BETWEEN:
					result[i] = scoreMap.get(file)[0];
					break;
				case CLOSE:
					result[i] = scoreMap.get(file)[1];
					break;
				case DEGREE:
					result[i] = scoreMap.get(file)[2];
					break;
			}
			
			//Increments the counter
			i++;
		}
		
		return result;
	}
	
	private Map<String, FileActor> 
		buildLookupFiles(ArrayList<String[]> files) {
		Map<String, FileActor> lookup = new HashMap<String, FileActor>();
		
		//Iterates through all the files.
		for (int i = 0; i < files.size(); i++){
			String[] items = files.get(i);
			FileActor file = new FileActor(items[0], items[1], items[2]);
			
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
				results.add(new String[]{rs.getString("FilePath"),
						rs.getString("Commits"),
						rs.getString("BugFixes")});
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
	
	private static String 
		generateCSV(String ID, double[][] spearman, double[][] PR){
		String output = "Project #" + ID + ":\n";
		
		//Prints the Spearman correlations.
		output += "Spearman - \n";
		for (int i = -1; i < MAX_CORR; i++){
			String line = "";
			for (int j = -1; j < MAX_CORR; j++){
				//Checks if we're printing the labels.
				if (i == -1 && j == -1){
					line += ",";
					continue;
				} else if (i == -1){
					line += ColumnType.labelFor(j);
				} else if (j == -1){
					line += ColumnType.labelFor(i);
				} else {
					//Print the value.
					line += spearman[i][j];
				}
				
				//Adds in the ,
				if (j < MAX_CORR - 1)
					line += ",";
			}
			
			//Flushes output.
			output += line + "\n";
		}
		
		//Prints the Precision and Recall values.
		output += "Precision & Recall - \n";
		for (int i = 0; i < PR[0].length; i++){
			output += i + "," + PR[0][i] + "," + PR[1][i] + "\n";
		}
		output += "-,-,-,-";
		
		//Returns the output.
		return output;
	}
}
