import CustomLibrary.SparkConnection
import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.functions
import org.apache.spark.sql.functions.{col, lit, row_number, when}
import org.apache.spark.sql.types.{DoubleType, IntegerType, StringType}
import spray.json.DefaultJsonProtocol._
import spray.json.RootJsonFormat

import scala.concurrent.{ExecutionContext, Future}
import scala.io.StdIn

object APIJSONPredictData {
  // needed to run the route
  implicit val system: ActorSystem[_] = ActorSystem(Behaviors.empty, "IDX-Stock-API")
  // needed for the future map/flatmap in the end and future in fetchItem and saveOrder
  implicit val executionContext: ExecutionContext = system.executionContext

  var listStockObj: List[Stock] = Nil

  // domain model
  final case class Ticker(id: String, ticker: String)
  final case class Stock(id: String, date: String, ticker: String, open: Double, volume: Long, close: Double)
  final case class LastStock(id: String, date: String, ticker: String, open: Double, volume: Long, close: Double, changeval: String, changepercent: String, status: String, name: String, logo: String)
  final case class StockReqPrediction(date: String, ticker: String, open: Double, volume: Long)
  final case class ListStockReqPrediction(listStock: List[StockReqPrediction])

  // formats for unmarshalling and marshalling
  implicit val itemFormat1: RootJsonFormat[Ticker] = jsonFormat2(Ticker.apply)
  implicit val itemFormat2: RootJsonFormat[Stock] = jsonFormat6(Stock.apply)
  implicit val itemFormat3: RootJsonFormat[LastStock] = jsonFormat11(LastStock.apply)
  implicit val itemFormat4: RootJsonFormat[StockReqPrediction] = jsonFormat4(StockReqPrediction.apply)
  implicit val orderFormat2: RootJsonFormat[ListStockReqPrediction] = jsonFormat1(ListStockReqPrediction.apply)

  //Spark instance and stock dataframes
  val sparkConn = openSpark()
  val MongoStocksDF = sparkConn.MongoDBGetAllStock("mongodb://localhost:27017/kafka.stock-stream");
  val MongoCompanyInfoDF = sparkConn.MongoDBGetCompanyInfo("mongodb://localhost:27017/kafka.stock-stream");

  // Return into JSON format
  def GetStockJSON(dataframe: sql.DataFrame): List[Stock] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    implicit val formats = DefaultFormats
    var listObj: List[Stock] = Nil
    val jsonStringArray = dataframe.toJSON.collect()
    for (row <- jsonStringArray) {
      val result = parse(row).extract[Stock]
//      println(result)
      listObj = listObj :+ result
    }
    listObj
  }

  // Return into JSON format
  def GetLastStockJSON(dataframe: sql.DataFrame): List[LastStock] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    implicit val formats = DefaultFormats
    var listObj: List[LastStock] = Nil
    val jsonStringArray = dataframe.toJSON.collect()
    for (row <- jsonStringArray) {
      val result = parse(row).extract[LastStock]
//      println(result)
      listObj = listObj :+ result
    }
    listObj
  }

  def GetTickerJSON(dataframe: sql.DataFrame): List[Ticker] = {
    import org.json4s._
    import org.json4s.native.JsonMethods._
    implicit val formats = DefaultFormats
    var listObj: List[Ticker] = Nil
    val jsonStringArray = dataframe.toJSON.collect()
    for (row <- jsonStringArray) {
      val result = parse(row).extract[Ticker]
//      println(result)
      listObj = listObj :+ result
    }
    listObj
  }

  def openSpark(): SparkConnection = {
    // Create Spark Session
    val sparkMaster = "spark://172.20.224.1:7077"
    val sparkAppName = "Scala REST API IDX Stock Prediction"
    val sparkConn = new SparkConnection(sparkMaster, sparkAppName)
    sparkConn.CreateSparkSession()
    val spark = sparkConn.spark.getOrCreate()
    println("Spark Version: " + spark.version)

    sparkConn
  }

  // (fake) async database query api
  def findTickerHistory(ticker: String): Future[List[Stock]] = Future {
    var histTicker = MongoStocksDF.filter("ticker == '" + ticker + "'")
    histTicker = histTicker.withColumn("id", functions.concat(col("ticker"), lit("-"), col("date")))
    histTicker = histTicker.select("id","date", "ticker", "open", "volume", "close")

    GetStockJSON(histTicker)
  }

  def AllTickerLastStock(): Future[List[LastStock]] = Future {
    var allTicker = MongoStocksDF
    //Add id as new column in allTicker dataframe
//    allTicker = allTicker.withColumn("id", functions.concat(col("ticker"), lit("-"), col("date")))
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
        when(col("change") > lit(0), functions.concat(lit("+"), col("change").cast(IntegerType)))
          .when(col("change") < lit(0), col("change").cast(IntegerType))
          .otherwise("0"))
      .withColumn("changepercent",
        when(col("change") > lit(0), functions.concat(lit("+"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
          .when(col("change") < lit(0), functions.concat(lit("-"), functions.abs((col("change") / col("yesterdayclose")) * 100).cast(IntegerType), lit("%")))
          .otherwise("0%"))

    var companyInfo = MongoCompanyInfoDF
    companyInfo = companyInfo
      .withColumn("ticker3", col("ticker").cast(StringType))
    companyInfo = companyInfo.select("ticker3", "name", "logo")
    //Inner join between joinTable1 and companyInfo dataframe
    var joinTable2 = joinTable1.join(companyInfo, joinTable1("ticker") === companyInfo("ticker3"), "inner")
    joinTable2 = joinTable2.select("id", "date", "ticker", "open", "volume", "close", "change", "changeval", "changepercent", "status", "name", "logo" )

    GetLastStockJSON(joinTable2)

  }

  def findAllEmitent(): Future[List[Ticker]] = Future {
    var allTicker = MongoStocksDF.select("ticker").groupBy("ticker").count().select("ticker")
    val windowSpec = Window.orderBy("ticker")
    allTicker = allTicker.withColumn("id", row_number.over(windowSpec))

    GetTickerJSON(allTicker)
  }

  def predictStock(order: ListStockReqPrediction): Future[Done] = {
    val spark = sparkConn.spark.getOrCreate()
    var df = spark.createDataFrame(order.listStock)
    df = df.select("date", "ticker", "open", "volume")
    println(df.printSchema)

    // And load it back in during production
    println("load model...")
    val model2 = PipelineModel.load("D:/00 Project/00 My Project/IdeaProjects/Scala-IDX-Stock-Analysis/modelGaussian")

    // Make predictions.
    println("Testing Data Pipeline...")
    var predictions = model2.transform(df)

    // Select example rows to display.
    predictions = predictions.withColumn("close", col("prediction"))
    predictions = predictions.withColumn("id", functions.concat(col("ticker"), lit("-"), col("date")))
    predictions.select("id","date", "ticker", "open", "volume", "prediction", "close").show(20)

    // Return JSON
    val listObj = GetStockJSON(predictions.select("id","date", "ticker", "open", "volume", "close"))

    listStockObj = listObj
    Future {Done}
  }

  def findAllPrediction(): Future[List[Stock]] = Future {
    listStockObj
  }

  def main(args: Array[String]): Unit = {
    val route: Route = cors() {
      concat(
        get {
          pathPrefix("ticker-list-last-stock") {
            val maybeItem: Future[List[LastStock]] = AllTickerLastStock()
            onSuccess(maybeItem) {
              case item if item.nonEmpty => complete(item)
              case _ => complete(StatusCodes.NotFound)
            }
          }
        },
        get {
          pathPrefix("ticker" / Segment) { productName =>
            val maybeItem: Future[List[Stock]] = findTickerHistory(productName)
            onSuccess(maybeItem) {
              case item if item.nonEmpty => complete(item)
              case _ => complete(StatusCodes.NotFound)
            }
          }
        },
        get {
          pathPrefix("emitent-list") {
            val maybeItem: Future[List[Ticker]] = findAllEmitent()
            onSuccess(maybeItem) {
              case item if item.nonEmpty => complete(item)
              case _ => complete(StatusCodes.NotFound)
            }
          }
        },
        get {
          pathPrefix("predict-list") {
            val maybeItem: Future[List[Stock]] = findAllPrediction()
            onSuccess(maybeItem) {
              case item if item.nonEmpty => complete(item)
              case _ => complete(StatusCodes.NotFound)
            }
          }
        },
        post {
          path("predict-stock") {
            entity(as[ListStockReqPrediction]) { dummy =>
              val saved: Future[Done] = predictStock(dummy)
              onSuccess(saved) { _ => // we are not interested in the result value `Done` but only in the fact that it was successful
                val maybeItem: Future[List[Stock]] = findAllPrediction()
                onSuccess(maybeItem) {
                  case item if item.nonEmpty => complete(item)
                  case _ => complete(StatusCodes.NotFound)
                }
              }
            }
          }
        }
      )
    }

    // Start Akka API Server
    val bindingFuture = Http().newServerAt("localhost", 9090).bind(route)
    println(s"Server online at http://localhost:9090/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done

  }

}
