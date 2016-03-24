package ca.uwaterloo.cs.cs846Boa.bmuscede.common

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import scala.collection.mutable.Map
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import scala.collection.JavaConversions.asScalaBuffer

class ModelManager(trainingSplit: Float = 60f, iterations: Integer = 100) {
  val split = trainingSplit
  val iter = iterations
  
  //File names for output.
  var modelSaveLoc = ""
  var testSaveLoc = ""
  
  //Predictors for correct, false positive, false negative.
  var correct : Map[Int,Int] = Map()
  var falsePos : Map[Int,Int] = Map()
  var falseNeg : Map[Int,Int] = Map()
  
  //Output for iterations system.
  var MODEL_SAVE = "model_"
  var TEST_SAVE = "test_"
  
  //Create the Spark context and conf
  val conf = new SparkConf()
    .setAppName("Logical Regression: Model")
    .setMaster("local")
  val sc = new SparkContext(conf)
  
  def runIterations(input : java.util.List[LabeledPoint],
    genSaveLoc: String, iterations: Int) = {
    //Resets values for precision and recall.
    correct.clear()
    falsePos.clear()
    falseNeg.clear()
    
    //We simply run the regression program a certain number of times.
    for (i <- 1 to iterations){
      regressionRunner(input, genSaveLoc + "/" + MODEL_SAVE + i.toString(),
          genSaveLoc + "/" + TEST_SAVE + i.toString(), i);
    }
  }
  
  def performRegression(sc: SparkContext, input: java.util.List[LabeledPoint],
      mSave: String = "", tSave: String = "") = {
    //Resets values for precision and recall.
    correct.clear()
    falsePos.clear()
    falseNeg.clear()
    
    //Runs the regression
    regressionRunner(input, mSave, tSave, 1);
  }
  
  def getPrecision() : Array[Double] = {
    if (correct == 0 && falsePos == 0) return null
    
    //Computes the precision for each computed instance.
    var precision = new Array[Double](correct.size)
    for (i <- 1 to correct.size){
      precision(i - 1) = correct(i).toDouble / (correct(i) + falsePos(i))
    }
    
    return precision
  }
  
  def getRecall() : Array[Double] = {
    if (correct(1) == 0 && falseNeg(1) == 0) return null;
    
    //Computes the recall for each instance.
    var recall = new Array[Double](correct.size)
    for (i <- 1 to correct.size){
      recall(i - 1) = correct(i).toDouble / (correct(i) + falseNeg(i))  
    }
    
    return recall
  }
  
  private def regressionRunner(
      input: java.util.List[LabeledPoint],
      mSave: String = "", tSave: String = "", itNum : Int) = {
    val points = asScalaBuffer(input).toList
    
    //Sets the save locations.
    modelSaveLoc = mSave
    testSaveLoc = tSave
    
    //Loads the labeled points.
    val data = sc.parallelize(points)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit), 11L)
    
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
       
       (if (score > 0) 1d else 0d, svmPoint.label)
    }).persist()
    
    //Computes precision and recall.
    val precision = scores
      .mapPartitions(predictions => {
        var correct = 0
        var falsePos = 0
        var falseNeg = 0
        
        //Iterate through all the predictions.
        while (predictions.hasNext){
          val next = predictions.next()
          
          //Make a prediction classifier.
          if (next._1 == next._2) correct += 1
          else if (next._1 > next._2) falsePos += 1
          else falseNeg += 1
        }
        //Iterate through all the predictions.
        val list = Array((correct, falsePos, falseNeg))
        list.toIterator
      }).collect()
    correct.put(itNum, precision(0)._1)
    falsePos.put(itNum, precision(0)._2)
    falseNeg.put(itNum, precision(0)._3)
    
    //Saves the testing data.
    if (testSaveLoc != "")
      scores.saveAsTextFile(testSaveLoc)
    
    //Saves the model.
    if (modelSaveLoc != "")
      model.save(sc, modelSaveLoc)
      
    //Clears out the test and model loc.
    modelSaveLoc = ""
    testSaveLoc = ""
  }
}