import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.{OneHotEncoder, StandardScaler, StringIndexer, VectorAssembler}
import org.apache.spark.ml.regression.{DecisionTreeRegressor, GeneralizedLinearRegression, LinearRegression}
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.types.{DoubleType, LongType, StringType}

object CreateModel {
  def main(args: Array[String]): Unit = {
//    val sparkMaster = "spark://172.20.224.1:7077"
//    val sparkMaster = "spark://localhost:7077"
    val sparkMaster = "local[*]"
    val appName = "Scala IDX Stock Create Model"
    val localHostname = java.net.InetAddress.getLocalHost.getHostName
    var mongoDBURL = ""
    if(localHostname.equalsIgnoreCase("dmitri")){
      mongoDBURL = "mongodb://localhost:27017/";
    } else {
      mongoDBURL = "mongodb://mongodb-server:27017/";
    }
    val mongoDBCollectionInput = mongoDBURL+"kafka.ksql-stock-stream"
    val spark = SparkSession.builder()
      .appName(appName)
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
      .config("spark.mongodb.read.connection.uri", mongoDBCollectionInput) //Docker mongodb
      .getOrCreate()

    println("Hostname: " + localHostname + " MongoDB URL: " + mongoDBURL)
    println("spark: " + spark.version)
    println("scala: " + util.Properties.versionString)
    println("java: " + System.getProperty("java.version"))

    val df = spark.read.format("mongodb").load()
    df.printSchema()

    val newDf = df.select("id", "ticker", "date", "open", "high", "low", "close", "volume")
      .withColumn("id", col("id").cast(StringType))
      .withColumn("ticker", col("ticker").cast(StringType))
      .withColumn("date", col("date").cast(StringType))
      .withColumn("open", col("open").cast(DoubleType))
      .withColumn("high", col("high").cast(DoubleType))
      .withColumn("low", col("low").cast(DoubleType))
      .withColumn("close", col("close").cast(DoubleType))
      .withColumn("volume", col("volume").cast(LongType))
    newDf.printSchema()
    println("Original Total Row: " + newDf.count())

    val filterDf = newDf.filter("date like '%2022%' OR date like '%2023%'")
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
      .setInputCols(Array("tickerOneHot", "dateOneHot", "open", "high", "low", "volume"))
      .setOutputCol("features")

    val scaler = new StandardScaler()
      .setInputCol("features")
      .setOutputCol("scaledFeatures")

    var generalizedLinearRegression = new GeneralizedLinearRegression()
    var linearRegression = new LinearRegression()
    var decisionTreeRegressor = new DecisionTreeRegressor()
    var pipeline = new Pipeline()
    val currentModelName = "modelGaussian"

    if (currentModelName.equalsIgnoreCase("modelGaussian")){
      generalizedLinearRegression = new GeneralizedLinearRegression()
        .setFamily("gaussian")
        .setLink("identity")
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")
        .setMaxIter(100)
        .setRegParam(0.3)

      // Assemble the pipeline
      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, generalizedLinearRegression)
      pipeline = new Pipeline().setStages(stages)
    } else if (currentModelName.equalsIgnoreCase("modelLinear")){
      linearRegression = new LinearRegression()
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")
        .setMaxIter(100)
        .setRegParam(0.3)
        .setElasticNetParam(0.8)

      // Assemble the pipeline
      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, linearRegression)
      pipeline = new Pipeline().setStages(stages)

    } else if (currentModelName.equalsIgnoreCase("modelDT")){
      decisionTreeRegressor = new DecisionTreeRegressor()
        .setFeaturesCol("scaledFeatures")
        .setLabelCol("close")

      // Assemble the pipeline
      val stages = Array(tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, decisionTreeRegressor)
      pipeline = new Pipeline().setStages(stages)
    }

    // Fit the pipeline
    println("Training Data Pipeline...")
    val model1 = pipeline.fit(train)

    // Save Model
    println("save model...")
    model1.write.overwrite().save("/04 Model/Scala-IDX-Stock-Analysis/" + currentModelName)

    // And load it back in during production
    println("load model...")
    val model2 = PipelineModel.load("/04 Model/Scala-IDX-Stock-Analysis/" + currentModelName)
//    val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/" + currentModelName)

    // Make predictions.
    println("Testing Data Pipeline...")
    val predictions = model2.transform(test)

    // Select example rows to display.
    predictions.printSchema()
    predictions.select("prediction", "close", "date", "ticker", "open", "high", "low", "volume").show(20)

    // Select (prediction, true label) and compute test error.
    val evaluator = new RegressionEvaluator()
      .setLabelCol("close")
      .setPredictionCol("prediction")
      .setMetricName("rmse")
    val rmse = evaluator.evaluate(predictions)
    println(s"Root Mean Squared Error (RMSE) on test data = $rmse")

  }
}