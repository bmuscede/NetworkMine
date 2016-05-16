package ca.uwaterloo.cs.cs846Boa.bmuscede.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.SocialMetrics;
import weka.core.Instances;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LibSVM;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.Logistic;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;

public class MLAnalysis {
	//Names for relations.
	private static final String TRAIN_RELATION_NAME = "train";
	private static final String TEST_RELATION_NAME = "test";
	public static final String CLASS_ATTRIBUTE = "class_attr";
	public static final String BUG_PRONE = "bug_prone";
	public static final String N_BUG_PRONE = "non_bug_prone";
	private static final int CLASS_INDEX = 0;
	
	private static Instances training;
	private static Instances testing;
	
	public static double[][] runMachineLearning(
			MachineLearning type, ArrayList<SocialMetrics> metrics,
			Map<String, ArrayList<Double>> data, float trainingSplit,
			int iterations) throws Exception{
		FastVector attributes = generateAttributes(metrics);
		double[][] precRec = new double[2][iterations];
		
		//Iterates for the number of specified iterations.
		for (int i = 0; i < iterations; i++){
			//Generates the testing and training sets.
			training = new Instances(TRAIN_RELATION_NAME, attributes, iterations);
			training.setClassIndex(CLASS_INDEX);
			testing = new Instances(TEST_RELATION_NAME, attributes, iterations);
			testing.setClassIndex(CLASS_INDEX);
			
			//Splits the input.
			splitGraph(data, attributes, trainingSplit);
			
			//Builds classifier.
			Classifier classifier = generateClassifier(type);
			classifier.buildClassifier(training);
			
			//Evaluates classifier.
			Evaluation eval = new Evaluation(training);
			eval.evaluateModel(classifier, testing);
			
			//Now, gets the precision and recall.
			precRec[0][i] = eval.precision(CLASS_INDEX);
			precRec[1][i] = eval.recall(CLASS_INDEX);
		}
		
		return precRec;
	}
	
	private static Classifier generateClassifier(MachineLearning type) {
		//Check which type of classifier we want.
		Classifier classifier = null;
		switch(type){
			case LOG_REG:
				classifier = new Logistic();
				//TODO: Optimize.
				break;
			case DEC_TREE:
				classifier = new J48();
				//TODO: Optimize.
				break;
			case RAND_FOR:
				classifier = new RandomForest();
				//TODO: Optimize.
				break;
			case SVM:
				classifier = new LibSVM();
				//TODO: Optimize.
				break;
			case NEURAL:
				classifier = new MultilayerPerceptron();
				//TODO: Optimize.
		default:
			break;
				
		}
		
		return classifier;
	}
	
	private static void splitGraph(Map<String, ArrayList<Double>> original, 
			FastVector attributes, float trainingSplit) {
		//Copies the original graph.
		Map<String, ArrayList<Double>> data = 
				new HashMap<String, ArrayList<Double>>();
		for (Map.Entry<String, ArrayList<Double>> curr : original.entrySet()){
			ArrayList<Double> list = new ArrayList<Double>();
			
			//Iterates through all the data.
			for (Double entry : curr.getValue())
				list.add(entry);
			
			//Adds in the new entry.
			data.put(curr.getKey(), list);
		}
	
		//Generates the split number.
		int numTraining = (int) (data.get(CLASS_ATTRIBUTE).size() * (trainingSplit / 100));
		
		//Iterates through and removes the training elements randomly.
		while(numTraining > 0) {
			//Get the data position.
			int index = ThreadLocalRandom.current()
					.nextInt(0, data.get(CLASS_ATTRIBUTE).size());
			
			//First, creates the instance.
			Instance current = new Instance(attributes.size());
			
			//Iterate through all attributes.
			Attribute currentAttr;
			for (int i = 0; i < attributes.size(); i++){
				//Gets the data and places it in the instance.
				currentAttr = (Attribute) attributes.elementAt(i);
				
				//Check if we need to convert.
				if (currentAttr.name() == CLASS_ATTRIBUTE){
					current.setValue(currentAttr,
							(data.get(currentAttr.name()).get(index) == 1) ? 
									BUG_PRONE : N_BUG_PRONE);
				} else {
					current.setValue(currentAttr,
							data.get(currentAttr.name()).get(index));
				}

				//Remove the value.
				data.get(currentAttr.name()).remove(index);
			}
			
			//Adds the instance in.
			training.add(current);
			
			//Iterates downward.
			numTraining--;
		}
		
		//Now, generates testing elements.
		while(data.get(CLASS_ATTRIBUTE).size() > 0) {
			//First, creates the instance.
			Instance current = new Instance(attributes.size());
			
			//Iterate through all attributes.
			Attribute currentAttr;
			for (int i = 0; i < attributes.size(); i++){
				//Gets the data and places it in the instance.
				currentAttr = (Attribute) attributes.elementAt(i);
				
				//Check if we need to convert.
				if (currentAttr.name() == CLASS_ATTRIBUTE){
					current.setValue(currentAttr,
							(data.get(currentAttr.name()).get(0) == 1) ? 
									BUG_PRONE : N_BUG_PRONE);
				} else {
					current.setValue(currentAttr,
							data.get(currentAttr.name()).get(0));
				}
				
				//Remove the value.
				data.get(currentAttr.name()).remove(0);
			}
			
			//Adds the instance in.
			testing.add(current);
		}
	}

	private static FastVector generateAttributes(ArrayList<SocialMetrics> metrics) {
		//Creates a new attributes list.
		FastVector attributes = new FastVector(metrics.size() + 1);
		
		//Generates the class attribute.
		FastVector classAttr = new FastVector(2);
		classAttr.addElement(BUG_PRONE);
		classAttr.addElement(N_BUG_PRONE);
		attributes.addElement(new Attribute(CLASS_ATTRIBUTE, classAttr));
		 
		//Iterate through and set the metrics for the instance list.
		Attribute current;
		for (SocialMetrics metric : metrics){
			current = new Attribute(metric.getShort());
			attributes.addElement(current);
		}
		
		return attributes;
	}
}
