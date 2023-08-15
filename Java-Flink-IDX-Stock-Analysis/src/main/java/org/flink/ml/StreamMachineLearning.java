package org.flink.ml;


import org.apache.flink.ml.feature.featurehasher.FeatureHasher;
import org.apache.flink.ml.linalg.DenseVector;
import org.apache.flink.ml.linalg.SparseVector;
import org.apache.flink.ml.linalg.Vector;
import org.apache.flink.ml.linalg.Vectors;
import org.apache.flink.ml.regression.linearregression.LinearRegression;
import org.apache.flink.ml.regression.linearregression.LinearRegressionModel;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.apache.flink.ml.Functions.arrayToVector;
import static org.apache.flink.table.api.Expressions.$;

public class StreamMachineLearning {
    private static final Logger log = LoggerFactory.getLogger(StreamMachineLearning.class.getSimpleName());

    public static void main(String[] args) throws Exception {
        // Sets up the execution environment, which is the main entry point
        // to building Flink applications.
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // Generates input data.
        DataStream<Row> inputStream =
                env.fromElements(
                        Row.of(Vectors.dense(2, 1), 10.0),
                        Row.of(Vectors.dense(1, 0), 6.0),
                        Row.of(Vectors.dense(2, 3), 10.0),
                        Row.of(Vectors.dense(3, 5), 5.0),
                        Row.of(Vectors.dense(4, 3), 11.0));

        Table inputData = tEnv.fromDataStream(inputStream).as("features", "label");
        Table trainTable = inputData;

        // Creates a LinearRegression object and initializes its parameters.
        LinearRegression lr = new LinearRegression();

        // Trains the LinearRegression Model.
        LinearRegressionModel lrModel = lr.fit(trainTable);

        // Uses the LinearRegression Model for predictions.
        Table predictTable = lrModel.transform(trainTable)[0];
        predictTable.printSchema();

        //Print FeatureHasher Result
        for (CloseableIterator<Row> it = predictTable.execute().collect(); it.hasNext(); ) {
            Row row = it.next();
//            SparseVector features = (SparseVector) row.getField(lr.getFeaturesCol());
            DenseVector features = (DenseVector) row.getField(lr.getFeaturesCol());
            double expectedResult = (Double) row.getField(lr.getLabelCol());
            double predictionResult = (Double) row.getField(lr.getPredictionCol());
            System.out.printf(
                    "Features: %s \tExpected Result: %s \tPrediction Result: %s\n",
                    features, expectedResult, predictionResult);
        }

        // Execute program, beginning computation.
//        env.execute("Flink ML Sample");
    }

}
