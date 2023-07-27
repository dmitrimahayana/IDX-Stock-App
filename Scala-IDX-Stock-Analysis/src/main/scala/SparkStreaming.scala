import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object SparkStreaming {

  def main(args: Array[String]): Unit = {
    val AppName = "POC Spark Stream"
    val ConnString = "spark://172.20.224.1:7077"
    val spark = SparkSession
      .builder()
      .appName(AppName)
      .master(ConnString)
      .config("spark.executor.instances", "2")
      .config("spark.executor.memory", "2g")
      .config("spark.executor.cores", "3")
      .config("spark.driver.memory", "2g")
      .config("spark.driver.cores", "3")
      .config("spark.cores.max", "6")
      .config("spark.sql.files.maxPartitionBytes", "12345678")
      .config("spark.sql.files.openCostInBytes", "12345678")
      .config("spark.sql.broadcastTimeout", "1000")
      .config("spark.sql.autoBroadcastJoinThreshold", "100485760")
      .config("spark.sql.shuffle.partitions", "1000")
      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .getOrCreate()

    println(spark.version)

    import spark.implicits._

    // Define the input data source (e.g., a socket or Kafka topic)
    val bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094"
    val topic1 = "group.stock"
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootStrapServer1)
      .option("subscribe", topic1)
      .option("startingOffsets", "earliest")
      .load()

    val stockSchema = StructType(Seq(
      StructField("id", StringType),
      StructField("ticker", StringType),
      StructField("date", StringType),
      StructField("open", DoubleType),
      StructField("high", DoubleType),
      StructField("low", DoubleType),
      StructField("close", DoubleType),
      StructField("volume", LongType)
    ))

    val ds = df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .as[(String, String)]

    // Apply transformations on the streaming DataFrame (ds) directly
    var transformedDF = ds
      .select(from_json($"value", stockSchema).alias("stock"))
      .select("stock.*")

//    //Create new column of rank
//    transformedDF = transformedDF.withColumn("rank", dense_rank().over(Window.partitionBy("ticker").orderBy(desc("date"))))
//    val allTickerRank1 = transformedDF.filter("rank == 1")
//    var allTickerRank2 = transformedDF.filter("rank == 2")
//    //Add ticker2, rank2, yesterdayclose as new column in allTickerRank1 dataframe
//    allTickerRank2 = allTickerRank2
//      .withColumn("ticker2", col("ticker").cast(StringType))
//      .withColumn("rank2", col("rank").cast(IntegerType))
//      .withColumn("yesterdayclose", col("close").cast(DoubleType))
//    allTickerRank2 = allTickerRank2.select("ticker2", "rank2", "yesterdayclose")
//    //Inner join between allTickerRank1 and allTickerRank2 dataframe
//    var joinTable1 = allTickerRank1.join(allTickerRank2, allTickerRank1("ticker") === allTickerRank2("ticker2"), "inner")
//
////    //FOR TESTING PURPOSES
////    joinTable1 = joinTable1
////      .withColumn("close", functions.rand().multiply(30))
////      .withColumn("yesterdayclose", functions.rand().multiply(30))
//
//    //Create new columnn of status
//    joinTable1 = joinTable1
//      .withColumn("status",
//        when(col("close") > col("yesterdayclose"), "Up")
//          .when(col("close") < col("yesterdayclose"), "Down")
//          .otherwise("Stay"))
//    //Create new columnn of change
//    joinTable1 = joinTable1
//      .withColumn("change", functions.round(col("close") - col("yesterdayclose")))
//    //Create new columnn of changeval and changepercent
//    joinTable1 = joinTable1
//      .withColumn("changeval",
//        when(col("change") > lit(0), functions.concat(lit("+"), col("change").cast(IntegerType)))
//          .when(col("change") < lit(0), col("change").cast(IntegerType))
//          .otherwise("0"))
//      .withColumn("changepercent",
//        when(col("change") > lit(0), functions.concat(lit("+"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
//          .when(col("change") < lit(0), functions.concat(lit("-"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
//          .otherwise("0%"))

    // Define the output sink for the transformed data (e.g., console, file, or another Kafka topic)
    val outputQuery = transformedDF.writeStream
      .outputMode("append") // You can change this to your desired output mode
      .format("console") // Change this to your desired output sink
      .start()

    outputQuery.awaitTermination()
  }
}
