package org.flink.ml;

import org.apache.flink.ml.feature.vectorassembler.VectorAssembler;
import org.apache.flink.ml.linalg.Vector;
import org.apache.flink.ml.linalg.Vectors;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;

import java.util.Arrays;

/** Simple program that creates a VectorAssembler instance and uses it for feature engineering. */
public class VectorAssemblerExample {
    public static void main(String[] args) {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env);

        // Generates input data.
        DataStream<Row> inputStream =
                env.fromElements(
                        Row.of(
                                Vectors.dense(2.1, 3.1),
                                1.11, 2.11,
                                Vectors.sparse(
                                        9,
                                        new int[] {8, 2, 3, 1, 5, 7},
                                        new double[] {4.0, 2.0, 3.0, 1.0, 3.2, 1.5})),
                        Row.of(
                                Vectors.dense(2.1, 3.1),
                                2.11, 3.11,
                                Vectors.sparse(
                                        9,
                                        new int[] {8, 2, 3, 1, 5, 7},
                                        new double[] {4.0, 2.0, 3.0, 1.0, 3.2, 1.5})));
        Table inputTable = tEnv.fromDataStream(inputStream).as("vec", "num1", "num2", "sparseVec");

        // Creates a VectorAssembler object and initializes its parameters.
        VectorAssembler vectorAssembler =
                new VectorAssembler()
                        .setInputCols("vec", "num1", "num2", "sparseVec")
                        .setOutputCol("assembledVec")
                        .setInputSizes(2, 1, 1, 9);
//                        .setInputSizes(2, 1, 5);

        // Uses the VectorAssembler object for feature transformations.
        Table outputTable = vectorAssembler.transform(inputTable)[0];

        // Extracts and displays the results.
        for (CloseableIterator<Row> it = outputTable.execute().collect(); it.hasNext(); ) {
            Row row = it.next();

            Object[] inputValues = new Object[vectorAssembler.getInputCols().length];
            for (int i = 0; i < inputValues.length; i++) {
                inputValues[i] = row.getField(vectorAssembler.getInputCols()[i]);
            }

            Vector outputValue = (Vector) row.getField(vectorAssembler.getOutputCol());

            System.out.printf(
                    "Input Values: %s \tOutput Value: %s\n",
                    Arrays.toString(inputValues), outputValue);
        }
    }
}

