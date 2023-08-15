package org.flink.ml;

import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;
import org.flink.ml.connection.RemoteServer;

public class DataStreamMongoDBJob {
    public static void main(String[] args) {
        String topic1 = "KSQLGROUPSTOCK"; //KSQLDB Table
        String group = "flink-group-idx-stock-consumer";
        String jarsPath = "D:/00 Project/00 My Project/Jars/Java-Flink-IDX-Stock-Analysis/";

        Boolean runLocal = Boolean.TRUE;
        RemoteServer remoteServer = new RemoteServer(runLocal, jarsPath);
        remoteServer.EstablishConnection();
        StreamTableEnvironment tableEnv = remoteServer.tableEnv;

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
                "   'uri' = 'mongodb://localhost:27017'," +
                "   'database' = 'kafka'," +
                "   'collection' = 'ksql-stock-stream'" +
                ");");
        Table inputTable = tableEnv.sqlQuery("SELECT * FROM flink_mongodb_stock LIMIT 5");
        inputTable.printSchema();
        for (CloseableIterator<Row> it = inputTable.execute().collect(); it.hasNext(); ) {
            Row row = it.next();
            System.out.println("Ticker: " + row.getField("ticker") + " --- Date: " + row.getField("date") + " --- Open: " + row.getField("open") + " --- Close: " + row.getField("close"));
        }
    }
}
