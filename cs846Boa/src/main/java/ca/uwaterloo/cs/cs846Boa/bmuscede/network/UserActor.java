package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class UserActor extends Actor {
	private String username;
	private String email;
	private String name;
	
	public UserActor(String username, String email, String name){
		super(ActorType.USER);
		this.setUsername(username);
		this.setEmail(email);
		this.setName(name);
	}

	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
}
