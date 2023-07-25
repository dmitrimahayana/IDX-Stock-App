import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{OneHotEncoder, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.{DecisionTreeRegressor, GeneralizedLinearRegression, LinearRegression}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{DoubleType, LongType, StringType}

import java.io.IOException

object CreateModel {
  def main(args: Array[String]): Unit = {
    val sparkMaster = "spark://192.168.1.7:7077"

    try {
      val spark = SparkSession.builder()
        .appName("Scala IDX Stock Create Model")
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
        .withColumn("date", col("date").cast(StringType))
        .withColumn("ticker", col("ticker").cast(StringType))
        .withColumn("open", col("open").cast(DoubleType))
        .withColumn("volume", col("volume").cast(LongType))
        .withColumn("close", col("close").cast(DoubleType))
      newDf.printSchema()
      println("Original Total Row: " + newDf.count())

      val filterDf = newDf.filter("date like '%2022%' OR date like '%2023%'")
//      val filterDf = newDf.filter("date like '%2022%'")
      println("Filtered Total Row: " + filterDf.count())

      val Array(train, test) = filterDf.randomSplit(Array(0.7, 0.3))

      // Define the feature transformation stages
      val tickerIndexer = new StringIndexer()
        .setInputCol("ticker")
        .setOutputCol("tickerIndex")
        .setHandleInvalid("keep")

      val tickerEncoder = new OneHotEncoder()
        .setInputCol("tickerIndex")
        .setOutputCol("tickerOneHot")

      val dateIndexer = new StringIndexer()
        .setInputCol("date")
        .setOutputCol("dateIndex")
        .setHandleInvalid("keep")

      val dateEncoder = new OneHotEncoder()
        .setInputCol("dateIndex")
        .setOutputCol("dateOneHot")

      val assembler = new VectorAssembler()
        .setInputCols(Array("tickerOneHot", "dateOneHot", "open", "volume"))
        .setOutputCol("features")

      val scaler = new StandardScaler()
        .setInputCol("features")
        .setOutputCol("scaledFeatures")

      val linearRegression = new LinearRegression()
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")
        .setMaxIter(100)
        .setRegParam(0.3)
        .setElasticNetParam(0.8)

      val generalizedLinearRegression = new GeneralizedLinearRegression()
        .setFamily("gaussian")
        .setLink("identity")
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")
        .setMaxIter(50)
        .setRegParam(0.3)

      val decisionTreeRegressor = new DecisionTreeRegressor()
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")

//      // Assemble the pipeline
//      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, linearRegression)
//      val pipeline = new Pipeline().setStages(stages)

      // Assemble the pipeline
      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, generalizedLinearRegression)
      val pipeline = new Pipeline().setStages(stages)

//      // Assemble the pipeline
//      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, decisionTreeRegressor)
//      val pipeline = new Pipeline().setStages(stages)

      // Fit the pipeline
      println("Training Data Pipeline...")
      val model1 = pipeline.fit(train)

      // Save Model
      println("save model...")
//      model1.write.overwrite().save("modelLinearReg")
      model1.write.overwrite().save("modelGaussian")
//      model1.write.overwrite().save("modelDecisionTreeReg")

      // And load it back in during production
      println("load model...")
//      val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/modelLinearReg")
      val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/modelGaussian")
//      val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/modelDecisionTreeReg")

      // Make predictions.
      println("Testing Data Pipeline...")
      val predictions = model2.transform(test)

      // Select example rows to display.
      predictions.printSchema()
      predictions.select("prediction", "close", "date", "ticker", "open", "volume").show(20)

      // Select (prediction, true label) and compute test error.
      val evaluator = new RegressionEvaluator()
        .setLabelCol("close")
        .setPredictionCol("prediction")
        .setMetricName("rmse")
      val rmse = evaluator.evaluate(predictions)
      println(s"Root Mean Squared Error (RMSE) on test data = $rmse")

      spark.close()
    } catch {
      case x: IOException => {
        println("Input/output Exception")
      }
    }
  }
}