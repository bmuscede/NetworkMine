package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class Commit {
	private int weight;
	
	public Commit(int commits){
		weight = commits;
	}
	
	public void setWeight(int newWeight){
		weight = newWeight;
	}
	
	public int getWeight(){
		return weight;
	}
}
