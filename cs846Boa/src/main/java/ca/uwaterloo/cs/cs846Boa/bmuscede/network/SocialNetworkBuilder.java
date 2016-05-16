package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

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
import java.util.Map;

import org.apache.commons.collections15.Transformer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.inference.TTest;

import ca.uwaterloo.cs.cs846Boa.bmuscede.common.MLAnalysis;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.Actor.ActorType;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.SocialMetrics;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.PerformAnalysis.StepDescrip;
import edu.uci.ics.jung.algorithms.scoring.BetweennessCentrality;
import edu.uci.ics.jung.algorithms.scoring.ClosenessCentrality;
import edu.uci.ics.jung.algorithms.scoring.DegreeScorer;
import edu.uci.ics.jung.algorithms.scoring.EigenvectorCentrality;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;

public class SocialNetworkBuilder {
	private enum ColumnType{
		COMMIT(0, "Commits"),
		FAILURE(1, "Failures"),
		F_BETWEENNESS(2, "Freeman Node Betweenness Centrality"),
		F_CLOSENESS(3, "Freeman Geodesic Closeness Centrality"),
		R_CLOSENESS(4, "Reachability Closeness Centrality"),
		F_DEGREE(5, "Freeman Degree Centrality"),
		BP_DEGREE(6, "Bonacich’s Power Degree Centrality");
		
		private static final int TOTAL = 7;
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
	
	//Holds number of metrics being computed.
	private static int numMetrics = 0;
	private static ArrayList<SocialMetrics> metricList;
	
	//Map to hold centrality.
	Map<Actor, Double> betweenScore = new HashMap<Actor, Double>();
	Map<Actor, Double> fCloseScore = new HashMap<Actor, Double>();
	Map<Actor, Double> rCloseScore = new HashMap<Actor, Double>();
	Map<Actor, Double> fDegreeScore = new HashMap<Actor, Double>();
	Map<Actor, Double> bpDegreeScore = new HashMap<Actor, Double>();
	Transformer<Commit, Integer> edgeWeights;
	
	//Final variables.
	private final static String DB_LOC = "data/boa.db";
	private final static int TIMEOUT = 30;
	private final double THRESHOLD = 0d;
	private final static String OUTPUT = "output.csv";
	
	//Holds values over all data.
	//(ONLY PRINTS WHEN DOING BATCH).
	private static double[][][] corr;
	private static double[][] PR;
	private static int projNum = 0;
	private static double finalThreshold;
	
	public static void performAnalysisOnAll(PerformAnalysis notify,
			String csvOut,
			boolean preContrib, boolean preFiles,
			ArrayList<SocialMetrics> metrics,
			boolean spearman, MachineLearning mlType, int iterations, int training){
		//Creates the thread.
		Thread t = new Thread() {
			public void run() {
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
			    	return;
			    }
			    
				//Clears the arrays that hold average.
			    numMetrics = metrics.size() + 2;
				resetArrays(spearman, results.size(), iterations);
				
			    //Next, we iterate through all the IDs and run our program on it.
			    String csv = "";
			    int i = 0;
			    for (String ID : results){
			    	outputProject(notify, ID, i, results.size() - 1);
			    	try {
						csv += runOnProject(false, notify, ID, csvOut + "//" + 
								ID + "_" + OUTPUT,
								preContrib, preFiles, metrics,
								spearman, mlType, iterations, training);
					} catch (Exception e) {
						System.err.println("Failure performing Machine Learning.");
						System.exit(1);
					}
			    	projNum++;
			    	i++;
			    }
			    
				//Gets the final values.
				csv += computeFinalStats();
				
				//Prints out to a final CSV.
		    	try {
					FileUtils.writeStringToFile(new File(csvOut + "//" + OUTPUT), 
							csv);
				} catch (IOException e) {
					System.err.println("CSV generated but could not be written to the"
							+ " file " + csvOut + "!");
				}
		    	
		    	if (notify != null) notify.informComplete();
			}
		};
		t.start();
	}
	
	public static void performAnalysisOnSome(PerformAnalysis notify,
			String[] networks, String csvOut,
			boolean preContrib, boolean preFiles,
			ArrayList<SocialMetrics> metrics,
			boolean spearman, MachineLearning mlType, int iterations, int training) {
		//Creates the thread.
		Thread t = new Thread() {
			public void run() {
				//Clears the arrays that hold average.
				numMetrics = metrics.size() + 2;
				resetArrays(spearman, networks.length, iterations);
				
				//Simply loop through all the networks.
				String csv = "";
			    int i = 0;
				for (String ID : networks){
			    	outputProject(notify, ID, i, networks.length - 1);
			    	try {
						csv += runOnProject(false, notify, ID, csvOut + "//" + 
								ID + "_" + OUTPUT, 
								preContrib, preFiles, metrics,
								spearman, mlType, iterations, training);
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("Failure running program.");
						System.exit(1);
					}
			    	projNum++;
			    	i++;
				}
				
				//Prints out the final values.
				csv += computeFinalStats();
				
		    	try {
					FileUtils.writeStringToFile(new File(csvOut + "//" + OUTPUT), 
							csv);
				} catch (IOException e) {
					System.err.println("CSV generated but could not be written to the"
							+ " file " + csvOut + "!");
				}
		    	
		    	if (notify != null) notify.informComplete();
			}
		};
		t.start();
	}
	
	public static void performAnalysis(PerformAnalysis notify,
			String ID, String csvOut, 
			boolean preContrib, boolean preFiles,
			ArrayList<SocialMetrics> metrics,
			boolean spearman, MachineLearning mlType, int iterations, int training){
		//Creates the thread.
		Thread t = new Thread() {
			public void run() {
				try {
					String CSV = runOnProject(true, notify, ID, csvOut,
						preContrib, preFiles,
						metrics, spearman, mlType, iterations, training);
		    	
					FileUtils.writeStringToFile(new File(csvOut), 
							CSV);
				} catch (IOException e) {
					System.err.println("CSV generated but could not be written to the"
							+ " file " + csvOut + "!");
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Failure performing Machine Learning.");
					System.exit(1);
				}
		    	
				if (notify != null) notify.informComplete();
			}
		};
		
		//Starts the thread.
		t.start();
	}
	
	private static String runOnProject(boolean single, PerformAnalysis notify,
			String ID, String csvOut,
			boolean preContrib, boolean preFiles,
			ArrayList<SocialMetrics> metrics,
			boolean spearman, MachineLearning mlType, int iterations, int training) 
			throws Exception{
		//Sets the number of metrics.
		numMetrics = metrics.size() + 2;
		metricList = metrics;
		
		//Checks if we need to notify.
		if (single) outputProject(notify, ID, 0, 0);
		
		//Creates an instance of the social network builder.
		if (snb == null)
			snb = new SocialNetworkBuilder();
		else
			snb.refresh();
		
		//Builds the social network.
		outputStatus(notify, StepDescrip.LOAD, null);
		snb.buildSocialNetwork(ID);
    	
		//Check for prepass options.
		//Filters unnecessary files and contributors.
		if (preContrib) snb.filterContributors();
		if (preFiles) snb.filterFiles();
		
		//Compute the centrality metrics.
		for (SocialMetrics current : metrics){
			switch (current){
				case F_BETWEENNESS:
					//Compute betweenness centrality.
					outputStatus(notify, StepDescrip.BETWEEN, null);
					snb.computeBetweennessCentrality();
					break;
				case F_CLOSENESS:
					//Compute first type of closeness centrality.
					outputStatus(notify, StepDescrip.F_CLOSE, null);
					snb.computeFreemanClosenessCentrality();
					break;
				case R_CLOSENESS:
					//Compute second type of closeness centrality.
					outputStatus(notify, StepDescrip.R_CLOSE, null);
					snb.computeReachabilityClosenessCentrality();
					break;
				case F_DEGREE:
					//Compute the first type of degree centrality.
					outputStatus(notify, StepDescrip.F_DEGREE, null);
					snb.computeFreemanDegreeCentrality();
					break;
				case BP_DEGREE:
					//Compute Boniach's Power
					outputStatus(notify, StepDescrip.BP_DEGREE, null);
					snb.computeBonacichPower();
					break;
			}
		}

    	//Computes the Spearman and PR values.
		double[][] spearmanValue = null;
		if (spearman) {
			outputStatus(notify, StepDescrip.SPEAR, null);
	    	spearmanValue = snb.performSpearmanCorrelation();
		}
		
		//Performs machine learning.
		outputStatus(notify, StepDescrip.ML, mlType);
		double[][] pR = snb.performML(metrics, iterations, training, 
				mlType);
    	
    	//Returns the final result.
    	return generateCSV(ID, csvOut, projNum, iterations, spearmanValue, pR);
	}

	private static void outputProject(PerformAnalysis notify, 
			String ID, int cur, int last){
		if (notify == null)
			//Prints to the console if not defined.
			System.out.println("Mining project #" + ID);
		else
			//Otherwise, calls appropriate method.
			notify.informCurrentProj(ID, cur, last);
	}
	
	private static void outputStatus(PerformAnalysis notify, StepDescrip descrip,
			MachineLearning type) {
		if (notify == null)
			//Prints to the console if not defined.
			System.out.println("\t" + descrip.toString());
		else
			if (descrip == StepDescrip.ML){
				notify.informML(type, descrip);
			} else {
				//Otherwise, calls appropriate method.
				notify.informCurrentStep(descrip);
			}
	}
	
	public SocialNetworkBuilder(){
		//Refreshes the logistic manager.
		refresh();
		
		//Creates the logicstic regression parameter.
		finalThreshold = 0;
	}

	private void refresh(){
		betweenScore = new HashMap<Actor, Double>();
		fCloseScore = new HashMap<Actor, Double>();
		rCloseScore = new HashMap<Actor, Double>();
		fDegreeScore = new HashMap<Actor, Double>();
		bpDegreeScore = new HashMap<Actor, Double>();
		
		//Initializes the graph.
		network = new UndirectedSparseGraph<Actor, Commit>();
	}
	
	public Graph<Actor, Commit> buildSocialNetwork(String projectID){
		//Connects to the database.
		Connection conn = null;
		Statement state = null;
	    try {
	    	conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
	    	state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
	    } catch (SQLException e){
	    	e.printStackTrace();
	    	return null;
	    }
	    
	    //Reads all project files.
		ArrayList<String[]> files = readFiles(projectID, state);
		if (files == null) return null;
		Map<String, FileActor> fileLookup = buildLookupFiles(files);
		
		//Reads all project users.
		ArrayList<String[]> users = readUsers(projectID, state);
		if (users == null) return null;
		Map<String, UserActor> userLookup = buildLookupUsers(users);
		
		//Reads all edges.
		ArrayList<String[]> contrib = readContributions(projectID, state);
		if (contrib == null) return null;
		addEdges(fileLookup, userLookup, contrib);
		
		//Closes the database connection.
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		
		return network;
	}

	public void filterContributors(){
		//TODO: Implement
	}
	
	public void filterFiles(){
		//TODO: Check correctness.
		//Pulls a list of all invalid files from the database.
		String sql = "SELECT Name FROM InvalidFile;";
		
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
				results.add(rs.getString("Name"));
			}
		    conn.close();
	    } catch (SQLException e){
	    	e.printStackTrace();
	    	return;
	    }
	    
	    //Next, we iterate through the results.
	    FileActor file;
	    for (String curr : results){
	    	//We loop through the graph for each string.
	    	for (Actor act : network.getVertices()){
	    		if (act instanceof UserActor) continue;
	    		
	    		//Now checks if the filename matches.
	    		file = (FileActor) act;
	    		if (file.getFileName().matches(curr)){
	    			network.removeVertex(file);
	    		}
	    	}
	    	
	    }
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
	
	public boolean computeFreemanClosenessCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		if (edgeWeights == null) developTransformer();
		
		//Creates the closeness centrality object.
		ClosenessCentrality<Actor, Commit> closeCompute =
				new ClosenessCentrality<Actor, Commit>(network, edgeWeights);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			fCloseScore.put(curr, closeCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public boolean computeReachabilityClosenessCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		
		//Creates the reachability centrality object.
		ReachabilityCentrality<Actor, Commit> closeCompute =
				new ReachabilityCentrality<Actor, Commit>(network);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			rCloseScore.put(curr, closeCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public boolean computeFreemanDegreeCentrality(){
		//First, checks if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		
		//Creates the degree centrality object.
		DegreeScorer<Actor> degreeCompute = 
				new DegreeScorer<Actor>(network);
		
		//Iterates through all the vertices.
		for (Actor curr : network.getVertices()){
			fDegreeScore.put(curr, (double) degreeCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public boolean computeBonacichPower(){
		//Check if we have a full graph.
		if (network.getVertexCount() == 0) return false;
		if (edgeWeights == null) developTransformer();
		
		//Create the Bonacich Power scorer.
		EigenvectorCentrality<Actor,Commit> bpCompute =
				new EigenvectorCentrality<Actor, Commit>(network, edgeWeights);
		bpCompute.acceptDisconnectedGraph(true);
		bpCompute.evaluate();
		
		//Iterates through the vertices.
		for (Actor curr : network.getVertices()){
			bpDegreeScore.put(curr, (double) bpCompute.getVertexScore(curr));
		}
		
		return true;
	}
	
	public double[][] performML(ArrayList<SocialMetrics> metrics, 
			int iterations, int split, 
			MachineLearning type) throws Exception{
		//First, we get the bug threshold.
		double threshold = getBugThreshold();
		
		//We then normalize our centrality metrics.
		for (SocialMetrics current : metrics){
			switch(current){
				case F_BETWEENNESS:
					betweenScore = normalize(betweenScore);
					break;
				case F_CLOSENESS:
					fCloseScore = normalize(fCloseScore);
					break;
				case R_CLOSENESS:
					rCloseScore = normalize(rCloseScore);
					break;
				case F_DEGREE:
					fDegreeScore = normalize(fDegreeScore);
					break;
				case BP_DEGREE:
					bpDegreeScore = normalize(bpDegreeScore);
					break;
			}
		}
		
		//Creates array to hold metrics.
		Map<String, ArrayList<Double>> metricEntry = 
				new HashMap<String, ArrayList<Double>>();
		for(int i = 0; i < metrics.size() + 1; i++){
			if (i == metrics.size()){
				metricEntry.put(MLAnalysis.CLASS_ATTRIBUTE, new ArrayList<Double>());
			} else {
				metricEntry.put(metrics.get(i).getShort(), new ArrayList<Double>());
			}
		}
		
		//Iterate through and generate a list of all metrics.
		for (Actor act : network.getVertices()){
			//We skip users and only look at files.
			if (act.getType() == ActorType.USER) continue;
			
			//Adds in the feature types.
			for (SocialMetrics current : metrics){
				switch(current){
					case F_BETWEENNESS:
						metricEntry.get(current.getShort()).add(
							betweenScore.get(act));
						break;
					case F_CLOSENESS:
						metricEntry.get(current.getShort()).add(
							fCloseScore.get(act));
						break;
					case R_CLOSENESS:
						metricEntry.get(current.getShort()).add(
							rCloseScore.get(act));
						break;
					case F_DEGREE:
						metricEntry.get(current.getShort()).add(
							fDegreeScore.get(act));
						break;
					case BP_DEGREE:
						metricEntry.get(current.getShort()).add(
							bpDegreeScore.get(act));
						break;
				}
			}
			
			//Get the label.
			FileActor file = (FileActor) act;
			double bugProportion = (double) file.getBugFixes() / file.getCommits();
			int label = (bugProportion > threshold) ? 1 : 0;
			metricEntry.get(MLAnalysis.CLASS_ATTRIBUTE).add((double)label);
		}
		
		//Next, runs the actual machine learning algorithm.
		double prResults[][] = MLAnalysis.runMachineLearning(type, metrics, metricEntry,
				(float) split, iterations);

		//Returns the precision and recall.
		finalThreshold = THRESHOLD;	
		return prResults;
	}

	private double getBugThreshold(){
		//Develop our bug threshold.
		ArrayList<Double> bugs = new ArrayList<Double>();
		for(Actor act : network.getVertices()){
			//Converts to file actor.
			if (act.getType() == ActorType.USER) continue;
			FileActor file = (FileActor) act;
			
			//Gets the current bug threshold.
			bugs.add((double) file.getBugFixes() / file.getCommits());
		}
		double[] bugSorted = ArrayUtils.toPrimitive
				(bugs.toArray(new Double[bugs.size()]));
		Arrays.sort(bugSorted);
		return (new Median()).evaluate(bugSorted);	
	}
	
	private Map<Actor, Double> normalize(Map<Actor, Double> score) {
		//Build a new map.
		Map<Actor, Double> normMap = new HashMap<Actor, Double>();
		Actor[] actors = new Actor[score.size()];
		double[] values = new double[score.size()];
		
		int i = 0;
		for (Map.Entry<Actor, Double> entry : score.entrySet()){
			actors[i] = entry.getKey();
			values[i] = entry.getValue();
			i++;
		}
		
		values = StatUtils.normalize(values);
		
		for (int j = 0; j < values.length; j++)
			normMap.put(actors[j], values[j]);
		
		return normMap;
	}

	public double[][] performSpearmanCorrelation(){		
		double[][] xCol, yCol;
		double[][] correlation = new double[numMetrics * 2][numMetrics * 2];
		
		//Calculate number of files.
		int numFiles = 0;
		for (Actor ac : network.getVertices())
			if (ac instanceof FileActor) numFiles++;
		
		//Creates a generic SpearmansCorrelation object.
		SpearmansCorrelation corr = new SpearmansCorrelation();
		TTest corrTest = new TTest();
		
		//Populates all columns.
		xCol = populateAll(numMetrics, numFiles);
		yCol = populateAll(numMetrics, numFiles);
		
		//Performs correlation analysis.
		for (int i = 0; i < numMetrics; i++){
			for (int j = 0; j < numMetrics; j++){
				//Performs Spearmans correlation.
				correlation[i][j] = corr.correlation(xCol[i], yCol[j]);
				correlation[i + numMetrics][j + numMetrics] = 
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
	
	private double[][] populateAll(int numMetrics, int numFiles){
		double[][] columnVals = new double[numMetrics][numFiles];
		
		//Populate the columns for all metrics.
		int pos = 0;
		boolean insert = false;
		for (int i = 0; i < ColumnType.TOTAL; i++){
			switch(ColumnType.valueOf(i)){
				case COMMIT:
				case FAILURE:
					insert = true;
					break;
				case F_BETWEENNESS:
					if (betweenScore.size() > 0){
						insert = true;
					}
					break;
				case F_CLOSENESS:
					if (fCloseScore.size() > 0){
						insert = true;
					}
					break;
				case R_CLOSENESS:
					if (rCloseScore.size() > 0){
						insert = true;
					}
					break;
				case F_DEGREE:
					if (fDegreeScore.size() > 0){
						insert = true;
					}
					break;
				case BP_DEGREE:
					if (bpDegreeScore.size() > 0){
						insert = true;
					}
					break;
			}
			
			//Check if we're inserting.
			if (insert){
				insert = false;
				columnVals[pos] = populateColumn(numFiles, ColumnType.valueOf(i));
				pos++;
			}
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
				case F_BETWEENNESS:
					result[i] = betweenScore.get(file);
					break;
				case F_CLOSENESS:
					result[i] = fCloseScore.get(file);
					break;
				case R_CLOSENESS:
					result[i] = rCloseScore.get(file);
					break;
				case F_DEGREE:
					result[i] = fDegreeScore.get(file);
					break;
				case BP_DEGREE:
					result[i] = bpDegreeScore.get(file);
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
		if (spearman != null){
			output += "Spearman - \n";
			output += printSpearman(num, spearman);
		}
		
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
		for (int i = -1; i < numMetrics; i++){
			String line = "";
			for (int j = -1; j < numMetrics; j++){
				//Checks if we're printing the labels.
				if (i == -1 && j == -1){
					line += ",";
					continue;
				} else if (i == -1){
					if (j == 0 || j == 1) line += ColumnType.labelFor(j);
					else line += metricList.get(j - 2).toString();
				} else if (j == -1){
					if (i == 0 || i == 1) line += ColumnType.labelFor(i);
					else line += metricList.get(i - 2).toString();
				} else {
					//Print the value.
					line += spearman[i][j];
					if (corr != null)
						corr[i][j][projNum] = spearman[i][j];
				}
				
				//Adds in the ,
				if (j < numMetrics - 1)
					line += ",";
			}
			
			//Flushes output.
			output += line + "\n";
		}
		
		//Next, print out the p-values.
		for (int i = (numMetrics - 1); i < numMetrics * 2; i++){
			String line = "";
			for (int j = (numMetrics - 1); j < numMetrics * 2; j++){
				//Checks if we're printing the labels.
				if (i == (numMetrics - 1) && j == (numMetrics) - 1){
					line += ",";
					continue;
				} else if (i == (numMetrics - 1)){
					if (j == numMetrics || j == (numMetrics + 1)) line += ColumnType.labelFor(j - numMetrics);
					else line += metricList.get(j - numMetrics - 2).toString();
				} else if (j == (numMetrics - 1)){
					if  (i == numMetrics || i == (numMetrics + 1)) line += ColumnType.labelFor(i - numMetrics);
					else line += metricList.get(i - numMetrics - 2).toString();
				} else {
					//Print the value.
					line += spearman[i][j];
				}
				
				//Adds in the ,
				if (j < (numMetrics * 2) - 1)
					line += ",";
			}
			
			//Flushes output.
			output += line + "\n";
		}
		return output;
	}
	
	private static void resetArrays(boolean spearman, int numProj, int iterations){
		//Resets stats values.
		if (spearman){
			corr = new double[numMetrics][numMetrics][numProj];
		
			//Fills them.
			for (int i = 0; i < numMetrics; i++){
				for (int j = 0; j < numMetrics; j++){
					Arrays.fill(corr[i][j], 0d);
				}
			}
		} else {
			corr = null;
		}
		
		//Resets PR values.
		PR = new double[2][iterations * numProj];
		
		//Fills them.
		for (int i = 0; i < 2; i++){
			Arrays.fill(PR[i], 0d);
		}
	}
	
	private static String computeFinalStats(){
		DescriptiveStatistics dev = new DescriptiveStatistics();
		Median med = new Median();
		
		String csv = "";
		if (corr != null){
			csv = "Final Spearman Statistics (Mean, Median, Std Dev) - \n";
			
			//We want to compute the final stats
			for (int i = -1; i < numMetrics; i++){
				String line = "";
				for (int j = -1; j < numMetrics; j++){
					//Checks if we're printing the labels.
					if (i == -1 && j == -1){
						line += ",";
						continue;
					} else if (i == -1){
						if (j == 0 || j == 1) line += ColumnType.labelFor(j);
						else line += metricList.get(j - 2).toString();
					} else if (j == -1){
						if (i == 0 || i == 1) line += ColumnType.labelFor(i);
						else line += metricList.get(i - 2).toString();
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
					if (j < numMetrics - 1)
						line += ",";
				}
						
				//Flushes output.
				csv += line + "\n";
			}
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
