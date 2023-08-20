from pyspark.sql import SparkSession

sparkMaster = 'spark://192.168.1.7:7077'
sparkAppName = 'Py-IDX-Stock-CreateModel'
spark = SparkSession \
    .builder \
    .master(sparkMaster) \
    .appName(sparkAppName) \
    .config('spark.mongodb.read.connection.uri', 'mongodb://127.0.0.1/kafka.ksql-stock-stream') \
    .config('spark.mongodb.write.connection.uri', 'mongodb://127.0.0.1/kafka.ksql-predict-stock') \
    .getOrCreate()

df = spark.read.format('mongodb').load()
df.printSchema()

df.filter(df['date'] == '2023-07-28').show()