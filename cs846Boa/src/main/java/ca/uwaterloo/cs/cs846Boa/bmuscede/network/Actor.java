package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class Actor {
	public enum ActorType{
		USER,
		FILE
	}
	private ActorType type;
	
	protected Actor(ActorType type){
		this.type = type;
	}
	
	public ActorType getType(){
		return type;
	}
	public void setType(ActorType type){
		this.type = type;
	}
}
