import logging

from pyspark.ml import PipelineModel
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.sql import SparkSession
from Config import sparkMasterConfig, mongoConfig

sparkMaster = sparkMasterConfig['url']
sparkAppName = 'Py-IDX-Stock-PredictData'
spark = SparkSession \
    .builder \
    .master(sparkMaster) \
    .appName(sparkAppName) \
    .config('spark.mongodb.read.connection.uri', mongoConfig['url']+'/kafka.ksql-stock-stream') \
    .config('spark.mongodb.write.connection.uri', mongoConfig['url']+'/kafka.ksql-predict-stock') \
    .getOrCreate()

# Set Log Level
spark.sparkContext.setLogLevel('INFO')

print('Spark Version: ', spark.version)
df = spark.read.format('mongodb').load()
df.printSchema()

newDf = df.select("id", "ticker", "date", "open", "high", "low", "close", "volume")
newDf.printSchema()
print("Original Total Row: ", newDf.count())

filterDf = newDf.filter("date like '%2023-07%'")
print("Filtered Total Row: ", filterDf.count())

train, test = filterDf.randomSplit([0.7, 0.3])
currentModelName = "modelGaussian"
pathModel = "/04 Model/Py-IDX-Stock/" + currentModelName

# And load it back in during production
print("load model...")
model2 = PipelineModel.load(pathModel)

# Make predictions.
print("Testing Data Pipeline...")
predictions = model2.transform(test)

# Select example rows to display.
predictions.printSchema()
predictions.select("prediction", "close", "date", "ticker", "open", "high", "low", "volume").show(20)

# Select (prediction, true label) and compute test error.
evaluator = RegressionEvaluator(
    labelCol="close",
    predictionCol="prediction",
    metricName="rmse")
rmse = evaluator.evaluate(predictions)
print(f"Root Mean Squared Error (RMSE) on test data = {rmse}")
