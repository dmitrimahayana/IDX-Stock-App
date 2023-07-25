import CustomLibrary.SparkConnection
import org.apache.spark.sql.functions
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType}

object MongoDBGetData {
  def main(args: Array[String]): Unit = {
    // Create Spark Session
    val sparkMaster = "spark://172.20.224.1:7077"
    val sparkAppName = "Scala REST API IDX Stock Prediction"
    val sparkConn = new SparkConnection(sparkMaster, sparkAppName)
    sparkConn.CreateSparkSession()
    val spark = sparkConn.spark.getOrCreate()
    println("Spark Version: " + spark.version)

    val kafkaStocks = sparkConn.MongoDBGetAllStock("mongodb://localhost:27017/kafka.stock-stream");
//    var allTicker = kafkaStocks.select("ticker").groupBy("ticker").count().select("ticker")
//    val windowSpec  = Window.orderBy("ticker")
//    allTicker = allTicker.withColumn("id",row_number.over(windowSpec))
//    allTicker.show()

    var allTicker = kafkaStocks
    //Add id as new column in allTicker dataframe
    allTicker = allTicker.withColumn("id", org.apache.spark.sql.functions.concat(col("ticker"), lit("-"), col("date")))
    val allTickerRank1 = allTicker.filter("rank == 1")
    var allTickerRank2 = allTicker.filter("rank == 2")
    //Add ticker2, rank2, yesterdayclose as new column in allTickerRank1 dataframe
    allTickerRank2 = allTickerRank2
      .withColumn("ticker2", col("ticker").cast(StringType))
      .withColumn("rank2", col("rank").cast(IntegerType))
      .withColumn("yesterdayclose", col("close").cast(DoubleType))
    allTickerRank2 = allTickerRank2.select("ticker2", "rank2", "yesterdayclose")
    //Inner join between allTickerRank1 and allTickerRank2 dataframe
    var joinTable1 = allTickerRank1.join(allTickerRank2, allTickerRank1("ticker") === allTickerRank2("ticker2"), "inner")

    //FOR TESTING PURPOSES
    joinTable1 = joinTable1
      .withColumn("close", functions.rand().multiply(30))
      .withColumn("yesterdayclose", functions.rand().multiply(30))

    //Create new columnn of status
    joinTable1 = joinTable1
      .withColumn("status",
        when(col("close") > col("yesterdayclose"), "Up")
          .when(col("close") < col("yesterdayclose"), "Down")
          .otherwise("Stay"))
    //Create new columnn of change
    joinTable1 = joinTable1
      .withColumn("change", functions.round(col("close") - col("yesterdayclose")))
    //Create new columnn of changeval and changepercent
    joinTable1 = joinTable1
      .withColumn("changeval",
        when(col("change") > lit(0), org.apache.spark.sql.functions.concat(lit("+"), col("change").cast(IntegerType)))
          .when(col("change") < lit(0), col("change").cast(IntegerType))
          .otherwise("0"))
      .withColumn("changepercent",
        when(col("change") > lit(0), org.apache.spark.sql.functions.concat(lit("+"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
          .when(col("change") < lit(0), org.apache.spark.sql.functions.concat(lit("-"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
          .otherwise("0%"))
    joinTable1.select("id","date", "ticker", "open", "volume", "close", "yesterdayclose", "change", "changeval", "changepercent", "status").show()
    joinTable1.printSchema()
  }

}
