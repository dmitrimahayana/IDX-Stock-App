import org.apache.spark.ml.recommendation.ALS.Rating
import org.apache.spark.sql.{Encoder, Encoders, Row, SparkSession}
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.recommendation.ALS

//import org.apache.spark.mllib.recommendation.Rating
//import org.apache.spark.mllib.recommendation.ALS
//import org.apache.spark.mllib.recommendation.MatrixFactorizationModel

object Main {
  case class Rating(user: Int, item: Int, rating: Float)

  def main(args: Array[String]): Unit = {
    val sparkMaster = "spark://192.168.1.8:7077"

    val spark = SparkSession.builder()
      .appName("AI Amazon Product")
      .master(sparkMaster)
//      .master("local[*]")
      .config("spark.executor.instances", "2")
      .config("spark.executor.memory", "4g")
      .config("spark.executor.cores", "2")
      .config("spark.driver.memory", "4g")
      .config("spark.driver.cores", "2")
//    spark.yarn.executor.memoryOverhead (Example: 384m for 384 MB)
//    spark.yarn.driver.memoryOverhead (Example: 384m for 384 MB)
//      .config("spark.sql.files.maxPartitionBytes", "12345678")
//      .config("spark.sql.files.openCostInBytes", "12345678")
//      .config("spark.sql.autoBroadcastJoinThreshold", "100485760")
//      .config("spark.sql.broadcastTimeout", "1000")
//      .config("spark.sql.shuffle.partitions", "1000")
      .getOrCreate()

    println("spark: " + spark.version)
    println("spark sc: " + spark.sparkContext.version)
    println("scala: " + util.Properties.versionString)
    println("java: " + System.getProperty("java.version"))

    import spark.implicits._

    val path_file = "D:/00 Project/01 Knowledge Transfer/Big Data/Dataset/Amazon Reviews/"
    //    val filename = "*tsv" //real 25GB data
    val filename = "amazon_reviews_us_Books_v1_00.tsv"
    val df = spark.read
      .format("csv") // Use "csv" regardless of TSV or CSV.
      .option("header", "true") // Does the file have a header line?
      .option("delimiter", "\t") // Set delimiter to tab or comma.
      .load(path_file + filename)
    df.printSchema()
//    df.show(5);

    val clean_df = df.na.drop("any", Seq("product_id", "star_rating")) //delete any row if there is NULL value

    val new_df = clean_df.select("customer_id", "product_id", "star_rating")
      .withColumn("customer_id", $"customer_id" cast "Int")
      .withColumn("product_id", $"product_id" cast "Int")
      .withColumn("star_rating", $"star_rating" cast "Float")

    val new_df2 = new_df.filter("product_id is not null and star_rating is not null")

//    val ratings = new_df2.map(row => (row.getInt(0), row.getInt(1), row.getFloat(2)) match {
//      case (user, item, rating) =>
//        Rating(user.toInt, item.toInt, rating.toFloat)
//    })

    val ratings = new_df2.map { row =>
      val user = row.getInt(0)
      val item = row.getInt(1)
      val rating = row.getFloat(2)
      Rating(user, item, rating)
    }
    ratings.show(5)

    val Array(training, test) = ratings.randomSplit(Array(0.8, 0.2))

    // Build the recommendation model using ALS on the training data
    val als = new ALS()
      .setMaxIter(5)
      .setRegParam(0.01)
      .setUserCol("user")
      .setItemCol("item")
      .setRatingCol("rating")
    val model = als.fit(training)

    // Evaluate the model by computing the RMSE on the test data
    // Note we set cold start strategy to 'drop' to ensure we don't get NaN evaluation metrics
    model.setColdStartStrategy("drop")
    val predictions = model.transform(test)

    val evaluator = new RegressionEvaluator()
      .setMetricName("rmse")
      .setLabelCol("rating")
      .setPredictionCol("prediction")
    val rmse = evaluator.evaluate(predictions)
    println(s"Root-mean-square error = $rmse")

    // Generate top 10 movie recommendations for each user
    val userRecs = model.recommendForAllUsers(10)
    userRecs.show()
    // Generate top 10 user recommendations for each movie
    val movieRecs = model.recommendForAllItems(10)
    movieRecs.show()

    // Generate top 10 movie recommendations for a specified set of users
    val users = ratings.select(als.getUserCol).distinct().limit(3)
    val userSubsetRecs = model.recommendForUserSubset(users, 10)
    users.show()
    userSubsetRecs.show()
    // Generate top 10 user recommendations for a specified set of movies
    val movies = ratings.select(als.getItemCol).distinct().limit(3)
    val movieSubSetRecs = model.recommendForItemSubset(movies, 10)
    movies.show()
    movieSubSetRecs.show()

//    val path_file = "D:/00 Project/01 Knowledge Transfer/Big Data/Dataset/Amazon Reviews/"
//    //    val filename = "*tsv" //real 25GB data
//    val filename = "amazon_reviews_us_Books_v1_00.tsv"
//    val df = spark.read
//      .format("csv") // Use "csv" regardless of TSV or CSV.
//      .option("header", "true") // Does the file have a header line?
//      .option("delimiter", "\t") // Set delimiter to tab or comma.
//      .load(path_file + filename)
//    df.show(5);
//
//    //Filter Data
//    val clean_df = df.na.drop("any", Seq("product_id", "star_rating")) //delete any row if there is NULL value
//
//    val new_df = clean_df.select("customer_id", "product_id", "star_rating")
//      .withColumn("customer_id", $"customer_id" cast "Int")
//      .withColumn("product_id", $"product_id" cast "Int")
//      .withColumn("star_rating", $"star_rating" cast "Double")
//
//    val new_df2 = new_df.filter("product_id is not null and star_rating is not null")
//
//    val ratings_df = new_df2.map(row => (row.getInt(0), row.getInt(1), row.getDouble(2)) match {
//      case (user, item, rate) =>
//        Rating(user.toInt, item.toInt, rate.toDouble)
//    })
//    ratings_df.show(5)
//
//    //Split the data into training and test
//    val Array(training_data, test_data) = ratings_df.randomSplit(Array(0.8, 0.2), seed = 11L)

//    val rank = 10
//    val numIterations = 10
//    val model = ALS.train(training_data.rdd, rank, numIterations, 0.01)
//
//    // Evaluate the model on rating data
//    val usersProducts = test_data.map { case Rating(user, product, rate) =>
//      (user, product)
//    }
//
//    val predictions =
//      model.predict(usersProducts.rdd).map { case Rating(user, product, rate) =>
//        ((user, product), rate)
//      }
//
//    val ratesAndPreds = test_data.map { case Rating(user, product, rate) =>
//      ((user, product), rate)
//    }.rdd.join(predictions)
//
//    val MSE = ratesAndPreds.map { case ((user, product), (r1, r2)) =>
//      val err = (r1 - r2)
//      err * err
//    }.mean()
//    println(s"Mean Squared Error = $MSE")
//    val MSE_df = Seq(("MSE", MSE)).toDF
//    MSE_df.write.format("json").save("D:/00 Project/00 My Project/IdeaProjects/New_Amazon_ETL/")
//
//    // Save and load model
//    val model_path = "D:/00 Project/00 My Project/IdeaProjects/New_Amazon_ETL/"
//    model.save(spark.sparkContext, model_path)
  }
}