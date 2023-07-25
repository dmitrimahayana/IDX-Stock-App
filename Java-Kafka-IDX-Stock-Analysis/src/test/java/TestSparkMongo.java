import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import java.util.concurrent.TimeoutException;

public class TestSparkMongo {
    public static void main(String[] args) throws TimeoutException, InterruptedException {
        String sparkMaster = "spark://192.168.1.4:7077";

        // Create SparkSession
        SparkSession spark = SparkSession.builder()
                .appName("StocksPredictionAppJAVA")
                .master(sparkMaster)
                .config("spark.driver.memory", "2g")
                .config("spark.driver.cores", "4")
                .config("spark.executor.memory", "2g")
                .config("spark.executor.cores", "4")
                .config("spark.cores.max", "12")
                .getOrCreate();

        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(spark::close));

        Dataset<Row> df = spark
                .read()
                .format("mongodb")
                .option("database", "kafka")
                .option("collection", "stock-stream")
                .load();
        df.printSchema();

        try {
            df.show(5);
        } catch (Exception e){
            System.out.println("ERROR SPARK DF: "+e.getMessage());
        }
        spark.close();
    }
}
