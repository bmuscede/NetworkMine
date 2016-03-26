package ca.uwaterloo.cs.cs846Boa.bmuscede.common

import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import scala.collection.mutable.Map
import scala.collection.mutable.ListBuffer
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
  
  //Threshold value
  var threshold = 0d
  
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
    threshold = generateThreshold(input)
    for (i <- 1 to iterations){
      regressionRunner(input, threshold,
          genSaveLoc + "/" + MODEL_SAVE + i.toString(),
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
    val threshold = generateThreshold(input)
    regressionRunner(input, threshold, mSave, tSave, 1);
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
  
  def getThreshold() : Double = {
    return threshold  
  }
  
  private def generateThreshold(input: java.util.List[LabeledPoint]) : Double = {
    val points = asScalaBuffer(input).toList
    
    //Loads the labeled points into an RDD.
    val data = sc.parallelize(points)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit)) 
    
    //Now, computes the training and test RDDs.
    val training = data(0).cache()
    val testing = data(1)
    
    //Uses logical regression to compute the model.
    val model = SVMWithSGD.train(training, iter)
      .clearThreshold()
      
    //Uses this model to predict.
    //Tuple in the form of (score, label)
    val scores = testing.map(svmPoint => {
       val rawScore = model.predict(svmPoint.features)
       
       (rawScore, svmPoint.label)
    })
    
    //Finds a classifier.
    val results = new BinaryClassificationMetrics(scores)
    val prec = results.precisionByThreshold().collect()
    val rec = results.recallByThreshold().collect()
    
    //Combines the values.
    val list = for (i <- 0 to (prec.size - 1)) yield {
      //Gets the current precision.  
      val currentPrec = prec(i)
        
      //Checks to see if the recall exists for this.
      var currentRec : (Double, Double) = null;
      for (item <- rec){
        if (item._1 == currentPrec._1)
          currentRec = item
      }
      
      //Emits the result.
      (currentPrec._1, currentPrec._2, currentRec._2)
    }
    
    //We now find the maximum threshold.
    var threshold = 0d
    var maxVal = 0d
    for (item <- list){
      val itemAvg = (item._2 + item._3) / 2
      if (itemAvg > maxVal){
        threshold = item._1
        maxVal = itemAvg
      }
    }

    System.out.println("Current Threshold: " + threshold);
    return threshold
  }
  
  private def regressionRunner(
      input: java.util.List[LabeledPoint], threshold: Double,
      mSave: String = "", tSave: String = "", itNum : Int) = {
    val points = asScalaBuffer(input).toList
    
    //Sets the save locations.
    modelSaveLoc = mSave
    testSaveLoc = tSave
    
    //Loads the labeled points.
    val data = sc.parallelize(points)
      .randomSplit(Array(trainingSplit, 100 - trainingSplit))
    
    //Now, computes the training and test RDDs.
    val training = data(0).cache()
    val testing = data(1)
    
    //Uses logical regression to compute the model.
    val model = SVMWithSGD.train(training, iter)
      .setThreshold(threshold)
    
    //Uses this model to predict.
    //Tuple in the form of (score, label)
    val scores = testing.map(svmPoint => {
       val score = model.predict(svmPoint.features)
       
       (score, svmPoint.label)
    })
      
    //Computes precision and recall.
    val prediction = scores
      .mapPartitions(predictions => {
        var corr = 0
        var fP = 0
        var fN = 0
        
        //Iterate through all the predictions.
        while (predictions.hasNext){
          val next = predictions.next()
          
          //Make a prediction classifier.
          if (next._1 == next._2) corr += 1
          else if (next._1 > next._2) fP += 1
          else fN += 1
        }
        //Iterate through all the predictions.
        val list = Array((corr, fP, fN))
        list.toIterator
      }).collect()
    correct.put(itNum, prediction(0)._1)
    falsePos.put(itNum, prediction(0)._2)
    falseNeg.put(itNum, prediction(0)._3)
    
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