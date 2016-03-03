package ca.uwaterloo.cs.cs846Boa.bmuscede.common

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics

class ModelManager(filename: String, trainingSplit: Float = 60f, 
    isSVM: Boolean = false, iterations: Integer = 100, save: String = "") {
  val graphLoc = filename
  val svm = isSVM
  val split = trainingSplit
  val iter = iterations
  val saveLoc = save
  
  def performRegression() = {
    //Create the Spark context and conf
    val conf = new SparkConf().setAppName("Logical Regression: Model")
    val sc = new SparkContext(conf)
    
    //Loads in LibSVM file, parses it and generates the training data.
    val svmFile = MLUtils
      .loadLibSVMFile(sc, graphLoc)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit), seed = 11L)
      
    //Now, computes the training and test RDDs.
    val training = svmFile(0).cache()
    val testing = svmFile(1)
    
    //Uses logical regression to compute the model.
    val model = SVMWithSGD.train(training, iter)
      .clearThreshold()
    
    //Uses this model to predict.
    //Tuple in the form of (score, label)
    val scores = testing.map(svmPoint => {
       val score = model.predict(svmPoint.features)
       
       (score, svmPoint.label)
    })
    
    //Saves the model.
    if (saveLoc != "")
      model.save(sc, saveLoc)
  }
}