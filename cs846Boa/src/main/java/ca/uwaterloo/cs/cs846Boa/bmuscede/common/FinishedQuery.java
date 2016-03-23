package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

public interface FinishedQuery {
	void finishedQuery(String ID, String results);
	void fail(String ID);
}
