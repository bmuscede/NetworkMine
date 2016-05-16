package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;

public interface PerformAnalysis {
	public enum StepDescrip {
		LOAD(0, "Loading project."),
		PREPASS(1, "Performing pre-analysis operations."),
		BETWEEN(2, "Computing Freeman betweenness centrality."),
		F_CLOSE(3, "Computing Freeman closeness centrality."),
		R_CLOSE(4, "Computing Reachability closeness centrality."),
		F_DEGREE(5, "Computing degree centrality."),
		BP_DEGREE(6, "Computing Bonacich’s Power."),
		SPEAR(7, "Calculating Spearman correlations."),
		ML(8, "Performing machine learning on project.");
		
		private static final int TOTAL = 8;
		private int stepNum;
		private String step;
		
		StepDescrip(int stepNum, String stepText){
			this.stepNum = stepNum;
			step = stepText;
		}
		
		public int getTotal(){
			return TOTAL;
		}
		
		public int getStepNum(){
			return stepNum;
		}
		
		@Override
		public String toString(){
			return step;
		}
	}
	
	//Informs the user what step the project is on.
    void informCurrentProj(String projID, int currentNum, int finalNum);
    void informCurrentStep(StepDescrip step);
    void informML(MachineLearning mlType, StepDescrip step);
    void informComplete();
}
