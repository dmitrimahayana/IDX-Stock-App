package org.flink.ml;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.flink.ml.feature.featurehasher.FeatureHasher;
import org.apache.flink.ml.feature.onehotencoder.OneHotEncoder;
import org.apache.flink.ml.feature.onehotencoder.OneHotEncoderModel;
import org.apache.flink.ml.feature.standardscaler.StandardScaler;
import org.apache.flink.ml.feature.standardscaler.StandardScalerModel;
import org.apache.flink.ml.feature.univariatefeatureselector.UnivariateFeatureSelector;
import org.apache.flink.ml.feature.univariatefeatureselector.UnivariateFeatureSelectorModel;
import org.apache.flink.ml.feature.vectorassembler.VectorAssembler;
import org.apache.flink.ml.linalg.DenseVector;
import org.apache.flink.ml.linalg.SparseVector;
import org.apache.flink.ml.linalg.Vector;
import org.apache.flink.ml.regression.linearregression.LinearRegression;
import org.apache.flink.ml.regression.linearregression.LinearRegressionModel;
import org.apache.flink.ml.stats.chisqtest.ChiSqTest;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.flink.ml.connection.RemoteServer;
import org.apache.flink.ml.feature.stringindexer.StringIndexer;
import org.apache.flink.ml.feature.stringindexer.StringIndexerModel;
import org.apache.flink.ml.feature.stringindexer.StringIndexerParams;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Properties;

import static org.apache.flink.table.api.Expressions.$;

public class PredictKSQLStock {
    public static void main(String[] args) throws Exception {
        String topic1 = "KSQLTABLEGROUPSTOCK"; //KSQLDB Table
        String group = "flink-group-idx-stock-consumer";
        String jarsPath = "D:/00 Project/00 My Project/Jars/Java-Flink-IDX-Stock-Analysis/";

        Boolean runLocal = Boolean.FALSE; //TRUE if you want to run locally
        RemoteServer remoteServer = new RemoteServer(runLocal, jarsPath);
        remoteServer.EstablishConnection();
        StreamTableEnvironment tableEnv = remoteServer.tableEnv;

        //SQL TABLE MUST USE UPPERCASE COLUMN NAME
        tableEnv.executeSql("CREATE TABLE flink_ksql_groupstock (" +
                "  `TICKER` STRING, " +
                "  `DATE` STRING, " +
                "  `OPEN` DOUBLE, " +
                "  `HIGH` DOUBLE, " +
                "  `LOW` DOUBLE, " +
                "  `CLOSE` DOUBLE, " +
                "  `VOLUME` BIGINT " +
                ") WITH (" +
                "  'connector' = 'kafka', " +
                "  'topic' = '" + topic1 + "', " +
                "  'properties.bootstrap.servers' = '" + remoteServer.bootStrapServer + "', " +
                "  'properties.group.id' = '" + group + "', " +
                "  'scan.startup.mode' = 'earliest-offset', " +
                "  'value.format' = 'avro-confluent', " +
                "  'value.avro-confluent.url' = '" + remoteServer.schemaHost + "' " +
                ");");
//        // Define a query using the Kafka source
//        Table inputTable = tableEnv.sqlQuery("SELECT * FROM flink_ksql_groupstock WHERE `DATE` LIKE '%2023-08%'")
//                .select(
//                        $("TICKER").as("ticker"),
//                        $("DATE").as("date"),
//                        $("OPEN").as("open"),
//                        $("HIGH").as("high"),
//                        $("LOW").as("low"),
//                        $("CLOSE").as("close"),
//                        $("VOLUME").as("volume")
//                );

        tableEnv.executeSql("CREATE TABLE flink_mongodb_stock (" +
                "  `id` STRING, " +
                "  `ticker` STRING, " +
                "  `date` STRING, " +
                "  `open` DOUBLE, " +
                "  `high` DOUBLE, " +
                "  `low` DOUBLE, " +
                "  `close` DOUBLE " +
                ") WITH (" +
                "   'connector' = 'mongodb'," +
                "   'uri' = '" + remoteServer.mongodbHost + "'," +
                "   'database' = 'kafka'," +
                "   'collection' = 'ksql-stock-stream'" +
                ");");
        Table inputTable = tableEnv.sqlQuery("SELECT * FROM flink_mongodb_stock WHERE `date` LIKE '%2023-08%'");

//        inputTable.printSchema();
//        for (CloseableIterator<Row> it = inputTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//            System.out.println("Ticker: " + row.getField("ticker") + " --- Date: " + row.getField("date") + " --- Open: " + row.getField("open") + " --- Close: " + row.getField("close"));
//        }

        // Creates a StringIndexer object and initializes its parameters.
        StringIndexer stringIndexer =
                new StringIndexer()
                        .setStringOrderType(StringIndexerParams.ALPHABET_ASC_ORDER)
                        .setInputCols("ticker", "date")
                        .setOutputCols("tickerIndex", "dateIndex");

        // Trains the StringIndexer Model.
        StringIndexerModel indexerModel = stringIndexer.fit(inputTable);

        // Uses the StringIndexer Model for predictions.
        Table indexerTable = indexerModel.transform(inputTable)[0];
        indexerTable.printSchema();

//        // Extracts and displays the results.
//        for (CloseableIterator<Row> it = indexerTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//
//            Object[] inputValues = new Object[stringIndexer.getInputCols().length];
//            double[] outputValues = new double[stringIndexer.getInputCols().length];
//            for (int i = 0; i < inputValues.length; i++) {
//                inputValues[i] = row.getField(stringIndexer.getInputCols()[i]);
//                outputValues[i] = (double) row.getField(stringIndexer.getOutputCols()[i]);
//            }
//
//            System.out.printf(
//                    "Input Values: %s \tOutput Values: %s\n",
//                    Arrays.toString(inputValues), Arrays.toString(outputValues));
//        }

        // Creates a OneHotEncoder object and initializes its parameters.
        OneHotEncoder oneHotModel =
                new OneHotEncoder()
                        .setInputCols("tickerIndex", "dateIndex")
                        .setOutputCols("tickerOneHot", "dateOneHot");

        // Trains the OneHotEncoder Model.
        OneHotEncoderModel oneHotEncoderModel = oneHotModel.fit(indexerTable);

        // Uses the OneHotEncoder Model for predictions.
        Table oneHotTable = oneHotEncoderModel.transform(indexerTable)[0];
        oneHotTable.printSchema();

//        // Extracts and displays the results.
//        for (CloseableIterator<Row> it = oneHotTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//            double[] inputValues = new double[oneHotEncoderModel.getInputCols().length];
//            for (int i = 0; i < inputValues.length; i++) {
//                inputValues[i] = (double) row.getField(oneHotEncoderModel.getInputCols()[i]);
//            }
//            SparseVector outputValue = (SparseVector) row.getField(oneHotModel.getOutputCols()[0]);
//            System.out.printf("Input Value: %s\tOutput Value: %s\n", Arrays.toString(inputValues), outputValue);
//        }

        //Get Ticker and Date Vector Size
        int tickerSize = 0, dateSize = 0;
        CloseableIterator<Row> iterator = oneHotTable.execute().collect();
        if (iterator.hasNext()) {
            Row row = iterator.next();
            SparseVector tickerVec = (SparseVector) row.getField("tickerOneHot");
            SparseVector dateVec = (SparseVector) row.getField("dateOneHot");
            tickerSize = tickerVec.size();
            dateSize = dateVec.size();
//            System.out.printf("TickerOneHote Vec Size: %s\tDateOneHot Vec Size: %s\n", tickerSize, dateSize);
        }

        // Creates a VectorAssembler object and initializes its parameters.
        VectorAssembler vectorAssembler =
                new VectorAssembler()
                        .setInputCols("tickerOneHot", "dateOneHot", "open", "high", "low")
//                        .setInputCols("tickerOneHot", "dateOneHot", "open")
                        .setInputSizes(tickerSize, dateSize, 1, 1, 1)
//                        .setInputSizes(tickerSize, dateSize, 1)
                        .setOutputCol("vectorAssembly");

        // Uses the VectorAssembler object for feature transformations.
        Table vectorTable = vectorAssembler.transform(oneHotTable)[0];
        vectorTable.printSchema();

//        // Extracts and displays the results.
//        for (CloseableIterator<Row> it = vectorTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//
//            Object[] inputValues = new Object[vectorAssembler.getInputCols().length];
//            for (int i = 0; i < inputValues.length; i++) {
//                inputValues[i] = row.getField(vectorAssembler.getInputCols()[i]);
//            }
//
//            Vector outputValue = (Vector) row.getField(vectorAssembler.getOutputCol());
//
//            System.out.printf(
//                    "Input Values: %s \tOutput Value: %s\n",
//                    Arrays.toString(inputValues), outputValue);
//        }

        // Creates a StandardScaler object and initializes its parameters.
        StandardScaler scaleModel = new StandardScaler()
                .setInputCol("vectorAssembly")
                .setOutputCol("scaledFeatures");

        // Trains the StandardScaler Model.
        StandardScalerModel scalerModel = scaleModel.fit(vectorTable);

        // Uses the StandardScaler Model for predictions.
        Table scaleTable = scalerModel.transform(vectorTable)[0];
        scaleTable.printSchema();

//        // Extracts and displays the results.
//        for (CloseableIterator<Row> it = scaleTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//            Vector inputValue = (Vector) row.getField(scaleModel.getInputCol());
//            Vector outputValue = (Vector) row.getField(scaleModel.getOutputCol());
//            System.out.printf("Input Value: %s\tOutput Value: %s\n", inputValue, outputValue);
//        }

//        Table outputTable = scaleTable.select($("scaledFeatures").as("features"), $("close").as("label"));

        // Creates a UnivariateFeatureSelector object and initializes its parameters.
        UnivariateFeatureSelector univariateFeatureSelector =
                new UnivariateFeatureSelector()
                        .setFeaturesCol("scaledFeatures")
                        .setLabelCol("close")
                        .setOutputCol("newFeatures")
                        .setFeatureType("continuous")
                        .setLabelType("continuous")
                        .setSelectionMode("numTopFeatures")
                        .setSelectionThreshold(1);

        // Trains the UnivariateFeatureSelector model.
        UnivariateFeatureSelectorModel featureSelectorModel = univariateFeatureSelector.fit(scaleTable);

        // Uses the UnivariateFeatureSelector model for predictions.
        Table featureSelectorTable = featureSelectorModel.transform(scaleTable)[0];
        featureSelectorTable.printSchema();

//        // Extracts and displays the results.
//        for (CloseableIterator<Row> it = featureSelectorTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//            Vector inputValue =
//                    (Vector) row.getField(univariateFeatureSelector.getFeaturesCol());
//            Vector outputValue =
//                    (Vector) row.getField(univariateFeatureSelector.getOutputCol());
//            System.out.printf("Input Value: %s\tOutput Value: %s\n", inputValue, outputValue);
//        }

//        // Creates a RandomSplitter object and initializes its parameters.
//        RandomSplitter splitter = new RandomSplitter().setWeights(4.0, 6.0);
//
//        // Uses the RandomSplitter to split inputData.
//        Table[] outputTables = splitter.transform(inputTable);

//        //Show Features and Label
//        for (CloseableIterator<Row> it = outputTable.execute().collect(); it.hasNext(); ) {
//            Row row = it.next();
//            Vector features = (Vector) row.getField("features");
//            Double label = (Double) row.getField("label");
//            System.out.printf("Scaled Features Value: %s\tLabel Value: %s\n", features, label);
//        }

        // Creates a LinearRegression object and initializes its parameters.
        LinearRegression lr = new LinearRegression()
                .setFeaturesCol("newFeatures")
                .setLabelCol("close")
                .setReg(0.3)
                .setElasticNet(0.8);

        // Trains the LinearRegression Model.
        LinearRegressionModel lrModel = lr.fit(featureSelectorTable);

        // Uses the LinearRegression Model for predictions.
        Table predictTable = lrModel.transform(featureSelectorTable)[0];
        predictTable.printSchema();

        //Print FeatureHasher Result
        for (CloseableIterator<Row> it = predictTable.execute().collect(); it.hasNext(); ) {
            Row row = it.next();
            Vector features = (Vector) row.getField(lr.getFeaturesCol());
            String id = (String) row.getField("ticker") + "_" + (String) row.getField("date");
            double expectedResult = (Double) row.getField(lr.getLabelCol());
            double predictionResult = (Double) row.getField(lr.getPredictionCol());
            System.out.printf(
                    "ID: %s Expected Result: %s \tPrediction Result: %s\n",
                    id, expectedResult, predictionResult);
        }

        // Execute the Flink job
        if (!runLocal) {
            remoteServer.env.execute("Flink Kafka SQL Predict Stock");
        }
    }
}
