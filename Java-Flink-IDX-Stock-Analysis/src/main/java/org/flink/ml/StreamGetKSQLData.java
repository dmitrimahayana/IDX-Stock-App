package org.flink.ml;

import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.flink.ml.connection.RemoteServer;

public class StreamGetKSQLData {
    public static void main(String[] args) throws Exception {
        String topic1 = "KSQLTABLEGROUPSTOCK"; //KSQLDB Table
        String topic2 = "KSQLTABLEGROUPCOMPANY"; //KSQLDB Table
        String group = "flink-group-idx-stock-consumer";
        String jarsPath = "D:/00 Project/00 My Project/Jars/Java-Flink-IDX-Stock-Analysis/";

        RemoteServer remoteServer = new RemoteServer(Boolean.FALSE, jarsPath);
        remoteServer.EstablishConnection();
        StreamTableEnvironment tableEnv = remoteServer.tableEnv;

        //SQL TABLE MUST USE UPPERCASE COLUMN NAME
        tableEnv.executeSql("CREATE TABLE flink_ksql_groupstock (" +
                "  `EVENT_TIME` TIMESTAMP(3) METADATA FROM 'timestamp', " +
                "  `STOCKID` STRING, " +
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
                ")");

        //SQL TABLE MUST USE UPPERCASE COLUMN NAME
        tableEnv.executeSql("CREATE TABLE flink_ksql_groupcompany (" +
                "  `EVENT_TIME` TIMESTAMP(3) METADATA FROM 'timestamp', " +
                "  `COMPANYID` STRING, " +
                "  `TICKER` STRING, " +
                "  `NAME` STRING, " +
                "  `LOGO` STRING " +
                ") WITH (" +
                "  'connector' = 'kafka', " +
                "  'topic' = '" + topic2 + "', " +
                "  'properties.bootstrap.servers' = '" + remoteServer.bootStrapServer + "', " +
                "  'properties.group.id' = '" + group + "', " +
                "  'scan.startup.mode' = 'earliest-offset', " +
                "  'value.format' = 'avro-confluent', " +
                "  'value.avro-confluent.url' = '" + remoteServer.schemaHost + "' " +
                ")");

        // Define a query using the Kafka source
        Table resultTable = tableEnv.sqlQuery("SELECT " +
                "  table1.`EVENT_TIME`," +
                "  `STOCKID`," +
                "  table1.`TICKER`," +
                "  `DATE`," +
                "  `OPEN`," +
                "  `HIGH`," +
                "  `LOW`," +
                "  `CLOSE`," +
                "  `VOLUME`, " +
                "  `NAME`, " +
                "  `LOGO` " +
                "  FROM flink_ksql_groupstock table1" +
                "  INNER JOIN flink_ksql_groupcompany table2" +
                "  ON table1.TICKER = table2.TICKER");

        for (CloseableIterator<Row> it = resultTable.execute().collect(); it.hasNext(); ) {
            Row row = it.next();
            System.out.println("Flink KSQL Value: " + row.getField("EVENT_TIME") + " --- " + row.getField("STOCKID") + " --- " + row.getField("CLOSE") + " --- " + row.getField("NAME"));
        }

        // Execute the Flink job
        remoteServer.env.execute("Flink Kafka SQL Consumer");
    }
}
