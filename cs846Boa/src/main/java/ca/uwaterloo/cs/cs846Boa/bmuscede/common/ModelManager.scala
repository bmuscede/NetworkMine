package ca.uwaterloo.cs.cs846Boa.bmuscede.common

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import scala.collection.JavaConversions.asScalaBuffer

class ModelManager(trainingSplit: Float = 60f, iterations: Integer = 100, save: String = "") {
  val split = trainingSplit
  val iter = iterations
  val saveLoc = save
  
  def performRegressionSVM(graphLoc : String) = {
    //Create the Spark context and conf
    val conf = new SparkConf().setAppName("Logical Regression: Model")
    val sc = new SparkContext(conf)
    
    //Loads in LibSVM file, parses it and generates the training data.
    val svmFile = MLUtils
      .loadLibSVMFile(sc, graphLoc)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit), seed = 11L)
    
    //Now runs the regression program.
    regressionRunner(sc, svmFile)
  }
  
  def performRegressionList(input : java.util.List[LabeledPoint]) = {
    val points = asScalaBuffer(input).toList
    
    //Create the Spark context and conf
    val conf = new SparkConf().setAppName("Logical Regression: Model")
    val sc = new SparkContext(conf)
    
    //Loads the labeled points.
    val data = sc.parallelize(points)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit), 11L)
    
    //Now runs the regression program.
    regressionRunner(sc, data);
  }
  
  private def regressionRunner(sc: SparkContext, data : Array[RDD[LabeledPoint]]) = {
    //Now, computes the training and test RDDs.
    val training = data(0).cache()
    val testing = data(1)
    
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