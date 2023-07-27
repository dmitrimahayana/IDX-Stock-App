package CustomLibrary

import org.apache.spark.sql
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions.{col, dense_rank, desc}
import org.apache.spark.sql.types.{DoubleType, LongType, StringType}

class SparkConnection(var ConnString: String, var AppName: String){
  var spark = SparkSession.builder()

  def CreateSparkSession(): Unit = {
    val localSpark = SparkSession
      .builder()
      .appName(AppName)
      .master(ConnString)
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
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    this.spark = localSpark
  }

  def MongoDBGetAllStock(ConnString: String): sql.DataFrame = {
    val df = spark.getOrCreate()
      .read
      .format("mongodb")
      .option("database", "kafka")
      .option("collection", "stock-stream")
      .option("spark.mongodb.input.partitioner", "MongoSinglePartitioner")
      .option("connection.uri", ConnString)
      .load()
    println("Old Stock Schema:")
    df.printSchema()

//    var newDf = df.select("date", "ticker", "open", "volume", "close")
    var newDf = df
      .withColumn("date", col("date").cast(StringType))
      .withColumn("ticker", col("ticker").cast(StringType))
      .withColumn("open", col("open").cast(DoubleType))
      .withColumn("volume", col("volume").cast(LongType))
      .withColumn("close", col("close").cast(DoubleType))

    newDf = newDf.withColumn("rank", dense_rank().over(Window.partitionBy("ticker").orderBy(desc("date"))))
    println("New Stock Schema:")
    newDf.printSchema()
    println("Original Total Row: " + newDf.count())

    newDf
  }

  def MongoDBGetCompanyInfo(ConnString: String): sql.DataFrame = {
    val df = spark.getOrCreate()
      .read
      .format("mongodb")
      .option("database", "kafka")
      .option("collection", "company-stream")
      .option("spark.mongodb.input.partitioner", "MongoSinglePartitioner")
      .option("connection.uri", ConnString)
      .load()
    println("Company Schema:")
    df.printSchema()

    df
  }

  def closeConnection(): Unit = {
    this.spark.getOrCreate().close()
  }

}
