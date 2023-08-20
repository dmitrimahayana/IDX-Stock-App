from pyspark.ml import Pipeline, PipelineModel
from pyspark.ml.evaluation import RegressionEvaluator
from pyspark.ml.feature import StringIndexer, OneHotEncoder, VectorAssembler, StandardScaler
from pyspark.ml.regression import GeneralizedLinearRegression
from pyspark.sql import SparkSession
from Config import sparkMasterConfig

sparkMaster = sparkMasterConfig['url']
sparkAppName = 'Py-IDX-Stock-CreateModel'
spark = SparkSession \
    .builder \
    .master(sparkMaster) \
    .appName(sparkAppName) \
    .config('spark.mongodb.read.connection.uri', 'mongodb://127.0.0.1/kafka.ksql-stock-stream') \
    .config('spark.mongodb.write.connection.uri', 'mongodb://127.0.0.1/kafka.ksql-predict-stock') \
    .getOrCreate()

# Set Log Level
spark.sparkContext.setLogLevel('INFO')

print('Spark Version: ', spark.version)
df = spark.read.format('mongodb').load()
df.printSchema()

newDf = df.select("id", "ticker", "date", "open", "high", "low", "close", "volume")
newDf.printSchema()
print("Original Total Row: ", newDf.count())

filterDf = newDf.filter("date like '%2022%' OR date like '%2023%'")
print("Filtered Total Row: ", filterDf.count())

train, test = filterDf.randomSplit([0.7, 0.3])
currentModelName = "modelGaussian"
pathModel = "/04 Model/Py-IDX-Stock/" + currentModelName

# Define the feature transformation stages
tickerIndexer = StringIndexer(
    inputCol="ticker",
    outputCol="tickerIndex",
    handleInvalid="keep")
tickerEncoder = OneHotEncoder(
    inputCol="tickerIndex",
    outputCol="tickerOneHot")

dateIndexer = StringIndexer(
    inputCol="date",
    outputCol="dateIndex",
    handleInvalid="keep")
dateEncoder = OneHotEncoder(
    inputCol="dateIndex",
    outputCol="dateOneHot")

assembler = VectorAssembler(
    inputCols=["tickerOneHot", "dateOneHot", "open", "high", "low", "volume"],
    outputCol="features")
scaler = StandardScaler(
    inputCol="features",
    outputCol="scaledFeatures")

# Perform Train Test using Pipeline
generalizedLinearRegression = GeneralizedLinearRegression(
    family="gaussian",
    link="identity",
    featuresCol="scaledFeatures",
    labelCol="close",
    maxIter=100,
    regParam=0.3)

# Assemble the pipeline
stages = [tickerIndexer, tickerEncoder, dateIndexer, dateEncoder, assembler, scaler, generalizedLinearRegression]
pipeline = Pipeline().setStages(stages)

# Fit the pipeline
print("Training Data Pipeline...")
model1 = pipeline.fit(train)

# Save Model
print("save model to ", pathModel)
model1.save(pathModel)

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
