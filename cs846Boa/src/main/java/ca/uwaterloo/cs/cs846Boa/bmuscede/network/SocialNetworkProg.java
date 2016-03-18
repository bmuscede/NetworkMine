package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import org.kohsuke.args4j.*;

public class SocialNetworkProg {

    public static class Args {
    	//The project ID.
        @Option(name = "-projectID", metaVar = "[pID]", 
        		required = true, usage = "The project ID")
        public String projectID;
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
	}
}
