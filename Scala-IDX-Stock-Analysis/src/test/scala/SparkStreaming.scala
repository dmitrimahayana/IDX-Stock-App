import org.apache.spark._
import org.apache.spark.sql.SparkSession
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming._ // not necessary since Spark 1.3

// Create a local StreamingContext with two working thread and batch interval of 1 second.
// The master requires 2 cores to prevent a starvation scenario.

object SparkStreaming {
  def main(args: Array[String]): Unit = {

    val sparkMaster = "spark://172.20.224.1:7077"
//    val localSpark = SparkSession
//      .builder()
//      .appName("Test")
//      .master(sparkMaster)
//      .config("spark.executor.instances", "2")
//      .config("spark.executor.memory", "2g")
//      .config("spark.executor.cores", "3")
//      .config("spark.driver.memory", "2g")
//      .config("spark.driver.cores", "3")
//      .config("spark.cores.max", "6")
//      .config("spark.sql.files.maxPartitionBytes", "12345678")
//      .config("spark.sql.files.openCostInBytes", "12345678")
//      .config("spark.sql.broadcastTimeout", "1000")
//      .config("spark.sql.autoBroadcastJoinThreshold", "100485760")
//      .config("spark.sql.shuffle.partitions", "1000")
//      .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val spark = SparkSession.builder()
      .appName("SparkStreamingExample")
      .master(sparkMaster) // Change this to your Spark cluster URL for distributed processing
      .getOrCreate()
    println(spark.version)

    import spark.implicits._

    // Define the input data source (e.g., a socket or Kafka topic)
    val bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094"
    val topic1 = "streaming.goapi.idx.stock.json"
    // Subscribe to 1 topic
    val df = spark
      .readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", bootStrapServer1)
      .option("subscribe", topic1)
      .option("includeHeaders", "true")
      .load()
    df.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)", "headers")
      .as[(String, String, Array[(String, Array[Byte])])].show()

//    source.load().selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
//      .as[(String, String)]
//
//    // Read data from the source and split it into words
//    val words = source.load().as[String].flatMap(_.split(" "))
//
//    // Count occurrences of each word
//    val wordCounts = words.groupBy("value").count()
//
//    // Start the streaming query to display the word counts in the console
//    val query = wordCounts.writeStream
//      .outputMode(OutputMode.Complete())
//      .format("console")
//      .start()
//
//    // Wait for the termination of the query
//    query.awaitTermination()
  }
}
