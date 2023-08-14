/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flink.ml;

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;

/**
 * Skeleton for a Flink DataStream Job.
 *
 * <p>For a tutorial how to write a Flink application, check the
 * tutorials and examples on the <a href="https://flink.apache.org">Flink Website</a>.
 *
 * <p>To package your application into a JAR file for execution, run
 * 'mvn clean package' on the command line.
 *
 * <p>If you change the name of the main class (with the public static void main(String[] args))
 * method, change the respective entry in the POM.xml file (simply search for 'mainClass').
 */
public class DataStreamJob {

	public static void main(String[] args) throws Exception {
		// Sets up the execution environment, which is the main entry point
		// to building Flink applications.
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);

		// create a DataStream
		DataStream<Row> dataStream = env.fromElements(
				Row.of("Alice", 12),
				Row.of("Bob", 10),
				Row.of("Alice", 100));

		// interpret the insert-only DataStream as a Table
		Table inputTable = tableEnv.fromDataStream(dataStream).as("name", "score");

		// register the Table object as a view and query it
		// the query contains an aggregation that produces updates
		tableEnv.createTemporaryView("InputTable", inputTable);
		Table resultTable = tableEnv.sqlQuery(
				"SELECT name, SUM(score) FROM InputTable GROUP BY name");

		// interpret the updating Table as a changelog DataStream
		DataStream<Row> resultStream = tableEnv.toChangelogStream(resultTable);

		// add a printing sink and execute in DataStream API
		resultStream.print();

		// Execute program, beginning computation.
		env.execute("Flink Java API Skeleton");
	}
}
