package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

public interface FinishedCallback {
    void onProjectFindFinish(boolean result);
    void onNetworkFinish(boolean result);
    void informCurrentMine(String project);
}
