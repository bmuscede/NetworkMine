package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import org.kohsuke.args4j.*;

public class SocialNetworkProg {

    public static class Args {
    	//The project ID.
        @Option(name = "-projectID", metaVar = "[pID]", 
        		required = true, usage = "The project ID")
        public String projectID;
        
        //The output location of the regression system.
        @Option(name = "-regOut", metaVar = "[path]", 
        		required = true, usage = "The directory for the regression output")
        public String output;
        
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
    	
    	//Checks the project ID flag.
    	if (arguments.projectID.equals("all")){
    		SocialNetworkBuilder.runRegressionOnAll(arguments.output, arguments.iterations);
    	}
    	
    	//Creates the social network program.
    	SocialNetworkBuilder snb = new SocialNetworkBuilder();
    	boolean succ = snb.buildSocialNetwork(arguments.projectID);
    	
    	if (!succ){
    		System.out.println("Failure");
    		return;
		} else {
			System.out.println("Social network for " + arguments.projectID +
					" created.");
		}
    	
    	//Now we compute the centrality.
    	succ = snb.computeCentrality();
    	if (!succ){
    		System.out.println("Failure");
    		return;
    	}
    	
		System.out.println("Centralities for social network #" +
				arguments.projectID + " computed.");
		
		snb.performRegression(arguments.output, arguments.iterations);
		System.out.println("Regression analysis complete.");
	}
}
