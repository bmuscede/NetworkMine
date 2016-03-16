package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class FileActor extends Actor {
	private String fileName;
	
	public FileActor(String filename){
		super (ActorType.FILE);
		setFileName(filename);
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
}
