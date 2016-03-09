package ca.uwaterloo.cs.cs846Boa.bmuscede.helloworld;

import ca.uwaterloo.cs.cs846Boa.bmuscede.common.ContributionBuilder;
import ca.uwaterloo.cs.cs846Boa.bmuscede.common.FinishedCallback;

/**
 * NOTE THIS CLASS IS USED TO TEST THE NETWORK BUILDER
 * @author Bryan
 *
 */
public class Test implements FinishedCallback {

	public static void main(String[] args) {
		ContributionBuilder build = new ContributionBuilder("amoneus/UsagiProject");
		build.login("<PLACEHOLDER>", "<PLACEHOLDER>");
		Test test = new Test();
		build.buildContributionNetwork(test);
		
		while (true) { }
	}

	@Override
	public void onFinish(boolean result) {
		System.out.println(result);
	}

}
