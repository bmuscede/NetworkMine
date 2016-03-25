package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
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
        
        @Option(name = "-csvOut", metaVar = "[name]",
        		required = true, usage = "The output file for the CSV generated.")
        public String csvOut;
        
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
    	String CSV = "";
    	if (arguments.projectID.equals("all")){
    		//Runs the analysis on ALL networks.
    		CSV += SocialNetworkBuilder.performFunctionsOnAll(arguments.output, 
    				arguments.iterations);
    	} else if (arguments.projectID.contains(",")) {
    		//Runs on a collection of networks.
    		String[] networks = arguments.projectID.split(",");
    		CSV += SocialNetworkBuilder.performFunctionsOnSome(networks,
    				arguments.output, arguments.iterations);
    	} else {
    		//Runs the analysis on some specified network.
    		CSV += SocialNetworkBuilder.performFunctions(arguments.projectID, 
    				arguments.output, arguments.iterations);
    	}
    	
    	//Checks for failures.
    	if (CSV.equals("")){
    		System.err.println("Failure performing analysis on social network "
    				+ "for project #" + arguments.projectID + "!");
    		return;
    	}
    	
    	//Outputs the CSV.
    	try {
			FileUtils.writeStringToFile(new File(arguments.csvOut), 
					CSV);
		} catch (IOException e) {
			System.err.println("CSV generated but could not be written to the"
					+ " file " + arguments.csvOut + "!");
		}
	}
}
