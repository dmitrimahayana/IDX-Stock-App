from pyspark.ml import PipelineModel
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.sql import SparkSession
from pyspark.sql.functions import concat_ws
from pyspark.sql.types import StringType, IntegerType

from Config import sparkMasterConfig, mongoConfig
import datetime

sparkMaster = sparkMasterConfig['url']
sparkAppName = 'Py-IDX-Stock-PredictData'
spark = SparkSession \
    .builder \
    .master(sparkMaster) \
    .appName(sparkAppName) \
    .config('spark.mongodb.read.connection.uri', mongoConfig['url'] + '/kafka.ksql-stock-stream') \
    .config('spark.mongodb.write.connection.uri', mongoConfig['url'] + '/kafka.ksql-predict-stock') \
    .getOrCreate()

# Set Log Level
spark.sparkContext.setLogLevel('INFO')

print('Spark Version: ', spark.version)
df = spark.read.format('mongodb').load()
df.printSchema()

newDf = df.select("id", "ticker", "date", "open", "high", "low", "close", "volume")
newDf.printSchema()
print("Original Total Row: ", newDf.count())

# Filter data by last month
dateTimeFormat = '%Y-%m'
currentDate = datetime.datetime.now()
firstDay = currentDate.replace(day=1)
lastMonth = firstDay - datetime.timedelta(days=1)
query = "date like '%" + lastMonth.strftime(dateTimeFormat) + "%'"
print("Perform query: ", query)
filterDf = newDf.filter(query)
print("Filtered Total Row: ", filterDf.count())

currentModelName = "modelGaussian"
pathModel = "/04 Model/Py-IDX-Stock/" + currentModelName

# And load it back in during production
print("load model...")
model2 = PipelineModel.load(pathModel)

# Make predictions.
print("Testing Data Pipeline...")
predictions = model2.transform(filterDf)

# Formatting column
resultDf = predictions \
            .select("prediction", "date", "ticker", "open", "high", "low", "volume") \
            .withColumn("id", concat_ws("_", predictions["ticker"], predictions["date"]).cast(StringType())) \
            .withColumn("open", predictions["open"].cast(IntegerType())) \
            .withColumn("high", predictions["high"].cast(IntegerType())) \
            .withColumn("low", predictions["low"].cast(IntegerType())) \
            .withColumn("close", predictions["prediction"].cast(IntegerType()))
resultDf = resultDf.select("id", "ticker", "date", "open", "high", "low", "close", "volume")

# Select example rows to display.
resultDf.printSchema()
resultDf.show(20)

# # Write to MongoDB with overwrite/append mode and MUST RUN THIS USING SPARK SUBMIT
print("Updating MongoDB...")
resultDf.write.format("mongodb").mode("overwrite").save()

# Select (prediction, true label) and compute test error.
evaluator = RegressionEvaluator(
    labelCol="close",
    predictionCol="prediction",
    metricName="rmse")
rmse = evaluator.evaluate(predictions)
print(f"Root Mean Squared Error (RMSE) on test data = {rmse}")
