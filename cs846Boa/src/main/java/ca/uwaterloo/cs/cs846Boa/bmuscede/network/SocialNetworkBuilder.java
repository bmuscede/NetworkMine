package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.*;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.inference.TTest;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
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
	
	private static SocialNetworkBuilder snb = null;
	
	//Social network.
	private Graph<Actor, Commit> network;
	
	//Map to hold centrality.
	Map<Actor, Double> betweenScore = new HashMap<Actor, Double>();
	Map<Actor, Double> closeScore = new HashMap<Actor, Double>();
	Map<Actor, Double> degreeScore = new HashMap<Actor, Double>();
	Transformer<Commit, Integer> edgeWeights;
	
	//Regression Manager
	private ModelManager manager;
	
	//Final variables.
	private final static String DB_LOC = "data/boa.db";
	private final static int TIMEOUT = 30;
	private final static int MAX_CORR = 5;
	private final int NUM_REGRESS_ITER = 100;
	
	//Holds values over all data.
	//(ONLY PRINTS WHEN DOING BATCH).
	private static double[][][] corr;
	private static double[][] PR;
	private static int projNum = 0;
	private static double finalThreshold;
	
	public static String performFunctionsOnAll(String output, String csvOut, 
			int iterations) {		
		long startTime = System.currentTimeMillis();
		
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
	    
		//Clears the arrays that hold average.
		resetArrays(results.size(), iterations);
		
	    //Next, we iterate through all the IDs and run our program on it.
	    String csv = "";
	    for (String ID : results){
	    	performFunctions(ID, csvOut, output, iterations);
	    	projNum++;
	    }
	    
	    System.out.println("All " + results.size() + " projects " +
				"analyzed in " + 
    			(System.currentTimeMillis() - startTime) / 1000 + " seconds.");
	    return csv;
	}
	
	public static String performFunctionsOnSome(String[] networks, 
			String csvOut, String output, int iterations) {		
		long startTime = System.currentTimeMillis();
		
		//Clears the arrays that hold average.
		resetArrays(networks.length, iterations);
		
		//Simply loop through all the networks.
		String csv = "";
		for (String ID : networks){
			csv += performFunctions(ID, csvOut, output, iterations);
			projNum++;
		}
		
		//Prints out the final values.
		csv += computeFinalStats();
		
		System.out.println("All " + networks.length + " projects " +
				"analyzed in " + 
    			(System.currentTimeMillis() - startTime) / 1000 + " seconds.");
		return csv;
	}
	
	public static String performFunctions(String ID, String csvOut,
			String output, int iterations){
		long startTime = System.currentTimeMillis();
		
		//Creates an instance of the social network builder.
		if (snb == null)
			snb = new SocialNetworkBuilder();
		else
			snb.refresh();
		
		//Builds the social network.
		System.out.println("Developing social network for project #" + ID + "...");
		snb.buildSocialNetwork(ID);
    	
		//Computes the centrality.
		System.out.println("Computing betweenness centrality for project #" + ID + "...");
    	snb.computeBetweennessCentrality();
    	System.out.println("Computing closeness centrality for project #" + ID + "...");
    	snb.computeClosenessCentrality();
    	System.out.println("Computing degree centrality for project #" + ID + "...");
    	snb.computeDegreeCentrality();
    	
    	//Computes the Spearman and PR values.
    	System.out.println("Calculating Spearman Correlation for all metrics " +
    			"for project #" + ID + "...");
    	double[][] spearman = snb.performSpearmanCorrelation();
    	System.out.println("Performing logical regression for project #" + ID + "...");
    	double[][] pR = snb.performRegression(output + "_" + ID, iterations);
    	
    	//Returns the final result.
    	System.out.println("Generating CSV text...");
    	String CSV = generateCSV(ID, csvOut, projNum, iterations, spearman, pR);
    	
    	System.out.println("Project #" + ID + " analyzed in " + 
    			(System.currentTimeMillis() - startTime) / 1000 + " seconds.");
    	
    	return CSV;
	}
	
	public SocialNetworkBuilder(){
		//Refreshes the logistic manager.
		refresh();
		
		//Creates the logicstic regression parameter.
		manager = new ModelManager(60f, NUM_REGRESS_ITER);
		finalThreshold = 0;
	}
	
	private void refresh(){
		betweenScore = new HashMap<Actor, Double>();
		closeScore = new HashMap<Actor, Double>();
		degreeScore = new HashMap<Actor, Double>();
		
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

	public boolean computeBetweennessCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		if (edgeWeights == null) developTransformer();
		
		//Creates the betweenness centrality object.
		BetweennessCentrality<Actor, Commit> betweenCompute = 
				new BetweennessCentrality<Actor, Commit>(network, edgeWeights);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			betweenScore.put(curr, betweenCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public boolean computeClosenessCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		if (edgeWeights == null) developTransformer();
		
		//Creates the closeness centrality object.
		ClosenessCentrality<Actor, Commit> closeCompute =
				new ClosenessCentrality<Actor, Commit>(network, edgeWeights);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			closeScore.put(curr, closeCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public boolean computeDegreeCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		
		//Creates the degree centrality object.
		DegreeScorer<Actor> degreeCompute = 
				new DegreeScorer<Actor>(network);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			degreeScore.put(curr, (double) degreeCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public double[][] performRegression(String output, int iterations){
		//We compute centrality first.
		if (betweenScore.size() == 0){
			computeBetweennessCentrality();
		}
		if (closeScore.size() == 0){
			computeClosenessCentrality();
		}
		if (degreeScore.size() == 0){
			computeDegreeCentrality();
		}
		
		//Develop our bug threshold.
		ArrayList<Double> bugs = new ArrayList<Double>();
		for(Actor act : network.getVertices()){
			//Converts to file actor.
			if (act.getType() == ActorType.USER) continue;
			FileActor file = (FileActor) act;
			
			//Gets the current bug threshold.
			bugs.add((double) file.getBugFixes() / file.getCommits());
		}
		double[] bugSorted = ArrayUtils.toPrimitive(bugs.toArray(new Double[bugs.size()]));
		Arrays.sort(bugSorted);
		double threshold = (new Median()).evaluate(bugSorted);
		
		//Next, we normalize the other variables.
		Map<Actor, Double> betweenNorm = normalize(betweenScore);
		Map<Actor, Double> closeNorm = normalize(closeScore);
		Map<Actor, Double> degreeNorm = normalize(degreeScore);
		
		//First we transform our dataset.
		List<LabeledPoint> metricEntry = new ArrayList<LabeledPoint>();
		for (Actor act : network.getVertices()){
			//We skip users and only look at files.
			if (act.getType() == ActorType.USER) continue;
		
			//Now we get all the centrality metrics and set them as a features.
			Double betweenCentrality = betweenNorm.get(act);
			Double closeCentrality = closeNorm.get(act);
			Double degreeCentrality = degreeNorm.get(act);
			
			//Get the label.
			FileActor file = (FileActor) act;
			double bugProportion = (double) file.getBugFixes() / file.getCommits();
			int label = (bugProportion > threshold) ? 1 : 0;
			
			//Adds the feature.
			metricEntry.add(new LabeledPoint
					(label, 
					Vectors.dense(betweenCentrality, 
							closeCentrality, degreeCentrality))); 
		}
		
		//Now that we have our labeled point setup, we pass it to Spark.
		manager.runIterations(metricEntry, output,  iterations);
		
		//Gets the precision and recall for each.
		double[][] prResults = new double[2][iterations];
		prResults[0] = manager.getPrecision();
		prResults[1] = manager.getRecall();
		finalThreshold = manager.getThreshold();
		
		return prResults;
	}
	
	private Map<Actor, Double> normalize(Map<Actor, Double> score) {
		//Build a new map.
		Map<Actor, Double> normMap = new HashMap<Actor, Double>();
		double[] values = new double[score.size()];
		
		//Iterate through our old map.
		int i = 0;
		for (Map.Entry<Actor, Double> entry : score.entrySet()){
			values[i++] = entry.getValue();
		}
		
		//Get the min and max.
		double min = StatUtils.min(values);
		double max = StatUtils.max(values);
		
		//Adds to the new map.
		for (Map.Entry<Actor, Double> entry : score.entrySet()){
			double norm = ((entry.getValue() - min) / (min + max));
			normMap.put(entry.getKey(), norm);
		}
		
		return normMap;
	}

	public double[][] performSpearmanCorrelation(){
		double[][] xCol, yCol;
		double[][] correlation = new double[MAX_CORR*2][MAX_CORR*2];
		
		//We compute centrality first.
		if (betweenScore.size() == 0){
			computeBetweennessCentrality();
		}
		if (closeScore.size() == 0){
			computeClosenessCentrality();
		}
		if (degreeScore.size() == 0){
			computeDegreeCentrality();
		}
		
		//Calculate number of files.
		int numFiles = 0;
		for (Actor ac : network.getVertices())
			if (ac instanceof FileActor) numFiles++;
		
		//Creates a generic SpearmansCorrelation object.
		SpearmansCorrelation corr = new SpearmansCorrelation();
		TTest corrTest = new TTest();
		
		//Populates all columns.
		xCol = populateAll(numFiles);
		yCol = populateAll(numFiles);
		
		//Performs correlation analysis.
		for (int i = 0; i < MAX_CORR; i++){
			for (int j = 0; j < MAX_CORR; j++){
				//Performs Spearmans correlation.
				correlation[i][j] = corr.correlation(xCol[i], yCol[j]);
				correlation[i + MAX_CORR][j + MAX_CORR] = 
						corrTest.pairedTTest(xCol[i], yCol[j], 0.01) ? 1d :
							0d;
			}
		}
		
		return correlation;
	}
	
	private void developTransformer(){
		//Develops the transformer.
		edgeWeights = new Transformer<Commit, Integer>() {
			public Integer transform(Commit curCommit){
				return curCommit.getWeight();
			}
		};
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
					result[i] = betweenScore.get(file);
					break;
				case CLOSE:
					result[i] = closeScore.get(file);
					break;
				case DEGREE:
					result[i] = degreeScore.get(file);
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
			
			//Adds system to ensure user is not null.
			//Note: Files are not handled like this
			//since we expect there to be a record of such.
			if (user == null){
				System.err.println("Unknown Actor: " + items[0] + "\n" +
						"Actor added in manually.");
				//Default actor for this.
				user = new UserActor(items[0], items[0], items[0]);
				uLookup.put(items[0], user);
			}
			
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
			rs = state.executeQuery("SELECT * FROM User INNER JOIN BelongsTo "
					+ "ON User.Username = BelongsTo.User"
					+ " WHERE Project = \"" + projID + "\";");
			
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
		generateCSV(String ID, String csvOut, int num, int iterations, 
				double[][] spearman, double[][] PR){
		String output = "Project #" + ID + ":\n";
		
		//Prints the Spearman correlations.
		output += "Spearman - \n";
		output += printSpearman(num, spearman);
		
		//Prints the Precision and Recall values.
		output += "Precision & Recall - \n";
		output += "Threshold," + finalThreshold + "\n";
		DescriptiveStatistics calcPre = new DescriptiveStatistics();
		DescriptiveStatistics calcRec = new DescriptiveStatistics();
		Median med = new Median();
		for (int i = 0; i < PR[0].length; i++){
			output += i + "," + PR[0][i] + "," + PR[1][i] + "\n";
			calcPre.addValue(PR[0][i]);
			calcRec.addValue(PR[1][i]);
			if (SocialNetworkBuilder.PR != null){
				SocialNetworkBuilder.PR[0][(num * iterations) + i] = PR[0][i];
				SocialNetworkBuilder.PR[1][(num * iterations) + i] = PR[1][i];
			}
		}
		
		//Outputs average.
		Arrays.sort(PR[0]);
		Arrays.sort(PR[1]);
		output += "Mean" + "," + calcPre.getMean() + ","
			+ calcRec.getMean() + "\n";
		output += "Median" + "," + med.evaluate(PR[0]) + ","
				+ med.evaluate(PR[1]) + "\n";
		output += "Standard Dev." + "," + calcPre.getStandardDeviation() + ","
				+ calcRec.getStandardDeviation() + "\n";
		output += "-,-,-,-\n";
		
		//Checks if we generate at each step.
		if (!csvOut.equals("")){
	    	//Outputs the CSV.
	    	try {
	    		csvOut = csvOut.replace("<PLACEHOLDER>", ID);
				FileUtils.writeStringToFile(new File(csvOut), 
						output);
			} catch (IOException e) {
				System.err.println("CSV generated but could not be written to the"
						+ " file " + csvOut + "!");
			}
		}
		//Returns the output.
		return output;
	}
	
	private static String printSpearman(int projNum, double[][] spearman){
		String output = "";
		
		//First, print out the correlations.
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
					if (corr != null)
						corr[i][j][projNum] = spearman[i][j];
				}
				
				//Adds in the ,
				if (j < MAX_CORR - 1)
					line += ",";
			}
			
			//Flushes output.
			output += line + "\n";
		}
		
		//Next, print out the p-values.
		for (int i = (MAX_CORR - 1); i < MAX_CORR * 2; i++){
			String line = "";
			for (int j = (MAX_CORR - 1); j < MAX_CORR * 2; j++){
				//Checks if we're printing the labels.
				if (i == (MAX_CORR - 1) && j == (MAX_CORR) - 1){
					line += ",";
					continue;
				} else if (i == (MAX_CORR - 1)){
					line += ColumnType.labelFor(j - MAX_CORR);
				} else if (j == (MAX_CORR - 1)){
					line += ColumnType.labelFor(i - MAX_CORR);
				} else {
					//Print the value.
					line += spearman[i][j];
				}
				
				//Adds in the ,
				if (j < (MAX_CORR * 2) - 1)
					line += ",";
			}
			
			//Flushes output.
			output += line + "\n";
		}
		return output;
	}
	
	private static void resetArrays(int numProj, int iterations){
		//Resets stats values.
		corr = new double[MAX_CORR][MAX_CORR][numProj];
		
		//Fills them.
		for (int i = 0; i < MAX_CORR; i++){
			for (int j = 0; j < MAX_CORR; j++){
				Arrays.fill(corr[i][j], 0d);
			}
		}
		
		//Resets PR values.
		PR = new double[2][iterations * numProj];
		
		//Fills them.
		for (int i = 0; i < 2; i++){
			Arrays.fill(PR[i], 0d);
		}
	}
	
	private static String computeFinalStats(){
		String csv = "Final Spearman Statistics (Mean, Median, Std Dev) - \n";
		DescriptiveStatistics dev = new DescriptiveStatistics();
		Median med = new Median();
		
		//We want to compute the final stats
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
					//Adds the number in for standard deviation.
					for (double num : corr[i][j])
						dev.addValue(num);
					
					//Print the value.
					line += StatUtils.mean(corr[i][j]) + " " +
							med.evaluate(corr[i][j]) + " " +
							dev.getStandardDeviation();
					dev.clear();
				}
				
				//Adds in the ,
				if (j < MAX_CORR - 1)
					line += ",";
			}
					
			//Flushes output.
			csv += line + "\n";
		}
		
		//Next, computes the precision and recall values.
		Arrays.sort(PR[0]);
		Arrays.sort(PR[1]);
		csv += "Final Precision & Recall - \n";
		csv += "Mean" + "," + StatUtils.mean(PR[0]) + "," +
				StatUtils.mean(PR[1]) + "\n";
		csv += "Median" + "," + med.evaluate(PR[0]) + "," +
				med.evaluate(PR[1]) + "\n";
		for (int i = 0; i < PR[0].length; i++){
			dev.addValue(PR[0][i]);
		}
		csv += "Std Dev" + "," + dev.getStandardDeviation();
		dev.clear();
		for (int i = 0; i < PR[1].length; i++){
			dev.addValue(PR[1][i]);
		}
		csv += "," + dev.getStandardDeviation() + "\n";

		//Resets the project number.
		projNum = 0;
		
		return csv;
	}
}
