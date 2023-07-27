import org.apache.spark.sql.SparkSession

object MongoDBSaveData {
  def main(args: Array[String]): Unit = {
    val sparkMaster = "spark://172.20.224.1:7077"
    val appName = "Scala IDX Stock Prediction"
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
//      .config("spark.mongodb.read.connection.uri", "mongodb://localhost:27017/kafka.stock-stream")
//      .config("spark.mongodb.write.connection.uri", "mongodb://localhost:27017/kafka.predict-stock")
//      .config("com.mongodb.spark.sql.connector.MongoTableProvider", "D:/03 Data Tools/spark-3.4.1-bin-hadoop3/jars/mongo-spark-connector_2.12-10.2.0.jar")
      .getOrCreate()

//    val df = spark.read.format("mongodb").load()
//    val resultDF = df.select("close", "date", "ticker", "open", "volume").limit(5)
    val resultDF = spark.read.option("multiline","true").json("./inputSample.json").limit(5)
    resultDF.printSchema()
    resultDF.show()
//    resultDF.write.format("mongodb").mode("append").save()

//    val newDf = df.select("date", "ticker", "open", "volume", "close")
//      .withColumn("date", col("date").cast(StringType))
//      .withColumn("ticker", col("ticker").cast(StringType))
//      .withColumn("open", col("open").cast(DoubleType))
//      .withColumn("volume", col("volume").cast(LongType))
//      .withColumn("close", col("close").cast(DoubleType))
//    newDf.printSchema()
//    println("Original Total Row: " + newDf.count())
//
//    val filterDf = newDf.filter("date like '%2023-07-25%'").limit(5)
//    println("Filtered Total Row: " + filterDf.count())
//
//    // Write to MongoDB
//    val resultDF = filterDf.select("close", "date", "ticker", "open", "volume")
//    resultDF.write.format("mongodb").mode("overwrite").save()
//    resultDF.write
//      .format("mongodb")
//      .option("connection.uri", "mongodb://localhost:27017")
//      .option("database", "kafka")
//      .option("collection", "predict-stock")
//      .mode("overwrite")
//      .save()
//    resultDF.write.json("PredictionResult")

    println("Done Writing DF to MongoDB ...")

  }

}
