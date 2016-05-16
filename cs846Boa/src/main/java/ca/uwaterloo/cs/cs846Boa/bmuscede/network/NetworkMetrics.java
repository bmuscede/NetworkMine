package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class NetworkMetrics {
	public enum SocialMetrics {
		F_BETWEENNESS("Freeman Node Betweenness Centrality", "BET"),
		F_CLOSENESS("Freeman Geodesic Closeness Centrality", "F_CLOSE"),
		R_CLOSENESS("Reachability Closeness Centrality", "R_CLOSE"),
		F_DEGREE("Freeman Degree Centrality", "F_DEGREE"),
		BP_DEGREE("Bonacich's Power Degree Centrality", "BP_DEGREE");
		
		private String stringRep;
		private String shortRep;
		
		SocialMetrics(String type, String shortType){
			stringRep = type;
			shortRep = shortType;
		}
		
		@Override
		public String toString(){
			return stringRep;
		}
		
		public String getShort(){
			return shortRep;
		}
	}
	
	public enum MachineLearning {
		LOG_REG("Logistic Regression"),
		DEC_TREE("Decision Tree"),
		RAND_FOR("Random Forest"),
		NEURAL("Neural Networks"),
		SVM("Support Vector Machines (SVM)");
		
		private String mlType;
		
		MachineLearning(String type){
			mlType = type;
		}
		
		@Override
		public String toString(){
			return mlType;
		}
	}
}
