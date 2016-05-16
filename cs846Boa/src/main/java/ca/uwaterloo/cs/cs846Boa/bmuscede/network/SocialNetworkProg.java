package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import java.util.ArrayList;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.ParserProperties;

import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.SocialMetrics;

public class SocialNetworkProg {
	private static final int DEFAULT_TRAIN = 60;
	
    public static class Args {
    	//The project ID.
        @Option(name = "-projectID", metaVar = "[pID]", 
        		required = true, usage = "The project ID")
        public String projectID;
        
        //The output location of the regression system.
        @Option(name = "-regOut", metaVar = "[path]", 
        		required = true, usage = "The directory for the regression output")
        public String output;
        
        //The output of the final filename.
        @Option(name = "-csvOut", metaVar = "[name]",
        		required = true, usage = "The output file for the CSV generated.")
        public String csvOut;
        
        //Whether we perform prepass.
        @Option(name = "-prepass", 
        		required = false, usage = "Whether prepass analysis is performed.")
        public boolean prepass;
        
        //The number of regression iterations.
        @Option(name = "-iterations", metaVar = "[num]", 
        		required = true, usage = "The number of regression iterations")
        public int iterations;
    }
    
	public static void main(String[] args) {
		//First, we parse the arguments written.
    	Args arguments = new Args();
    	CmdLineParser parser = new CmdLineParser(arguments, ParserProperties.defaults().withUsageWidth(100));
    	
    	//Now, we obtain the arguments.
    	try{
    		parser.parseArgument(args);
    	} catch (CmdLineException e) {
    		//Prints usage message to std err.
    		System.err.println(e.getMessage());
    		parser.printUsage(System.err);
    		return;
    	}
    	
    	String csvOut = arguments.csvOut;

    	//Creates a default list of metrics.
    	ArrayList<SocialMetrics> metrics = new ArrayList<SocialMetrics>();
    	metrics.add(SocialMetrics.F_BETWEENNESS);
    	metrics.add(SocialMetrics.F_CLOSENESS);
    	metrics.add(SocialMetrics.F_DEGREE);
    	
    	//Checks the project ID flag.
    	if (arguments.projectID.equals("all")){
    		//Runs the analysis on ALL networks.
    		SocialNetworkBuilder.performAnalysisOnAll(null,
    				csvOut, arguments.prepass, arguments.prepass, 
    				metrics, true, MachineLearning.SVM, 
    				arguments.iterations, DEFAULT_TRAIN);
    	} else if (arguments.projectID.contains(",")) {
    		//Runs on a collection of networks.
    		String[] networks = arguments.projectID.split(",");
    		SocialNetworkBuilder.performAnalysisOnSome(null, networks,
    				csvOut, arguments.prepass, arguments.prepass, 
    				metrics, true, MachineLearning.SVM, 
    				arguments.iterations, DEFAULT_TRAIN);
    	} else {
    		//Runs the analysis on some specified network.
    		SocialNetworkBuilder.performAnalysis(null, arguments.projectID, 
    				csvOut, arguments.prepass, arguments.prepass, 
    				metrics, true, MachineLearning.SVM, 
    				arguments.iterations, DEFAULT_TRAIN);
    	}
	}
}
