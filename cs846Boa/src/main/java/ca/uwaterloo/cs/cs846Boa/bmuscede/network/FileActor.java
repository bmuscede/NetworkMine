package ca.uwaterloo.cs.cs846Boa.bmuscede.network;

public class FileActor extends Actor {
	private String fileName;
	private int commits;
	private int bugFixes;
	
	public FileActor(String filename, String commits, String bugs){
		super (ActorType.FILE);
		setFileName(filename);
		setCommits(Integer.parseInt(commits));
		setBugFixes(Integer.parseInt(bugs));
	}

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int getCommits() {
		return commits;
	}
	public void setCommits(int commits) {
		this.commits = commits;
	}
	public int getBugFixes() {
		return bugFixes;
	}
	public void setBugFixes(int bugFixes) {
		this.bugFixes = bugFixes;
	}
}
