import org.apache.spark.ml.PipelineModel
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{DoubleType, LongType, StringType}

import java.io.IOException

object PredictData {
  def main(args: Array[String]): Unit = {
    val sparkMaster = "spark://192.168.1.7:7077"

    try {
      val spark = SparkSession.builder()
        .appName("Scala IDX Stock Prediction")
        .master(sparkMaster)
        .config("spark.executor.instances", "2")
        .config("spark.executor.memory", "6g")
        .config("spark.executor.cores", "3")
        .config("spark.driver.memory", "6g")
        .config("spark.driver.cores", "3")
        .config("spark.cores.max", "6")
        .config("spark.sql.files.maxPartitionBytes", "12345678")
        .config("spark.sql.files.openCostInBytes", "12345678")
        .config("spark.sql.broadcastTimeout", "1000")
        .config("spark.sql.autoBroadcastJoinThreshold", "100485760")
        .config("spark.sql.shuffle.partitions", "1000")
        .getOrCreate()

      println("spark: " + spark.version)
      println("scala: " + util.Properties.versionString)
      println("java: " + System.getProperty("java.version"))

      val df = spark
        .read
        .format("mongodb")
        .option("database", "kafka")
        .option("collection", "stock-stream")
        .option("spark.mongodb.input.partitioner", "MongoSinglePartitioner")
        .option("connection.uri", "mongodb://localhost:27017/kafka.stock-stream")
        .load()
      df.printSchema()

      val newDf = df.select("date", "ticker", "open", "volume", "close")
        .withColumn("date",col("date").cast(StringType))
        .withColumn("ticker", col("ticker").cast(StringType))
        .withColumn("open", col("open").cast(DoubleType))
        .withColumn("volume", col("volume").cast(LongType))
        .withColumn("close", col("close").cast(DoubleType))
      newDf.printSchema()
      println("Original Total Row: " + newDf.count())

      val filterDf = newDf.filter("date like '%2023%'")
      println("Filtered Total Row: " + filterDf.count())

      // And load it back in during production
      println("load model...")
      val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/modelGaussian")

      // Make predictions.
      println("Testing Data Pipeline...")
      val predictions = model2.transform(filterDf)

      // Select example rows to display.
      predictions.select("prediction", "close", "date", "ticker", "open", "volume").show(20)

      // Select (prediction, true label) and compute test error based on label and prediction column
      val evaluator = new RegressionEvaluator()
        .setLabelCol("close")
        .setPredictionCol("prediction")
        .setMetricName("rmse")
      val rmse = evaluator.evaluate(predictions)
      println(s"Root Mean Squared Error (RMSE) on test data = $rmse")

      spark.stop()
    } catch {
      case x: IOException => {
        println("Input/output Exception")
      }
    }
  }
}
