import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;

public class TestSparkReadJSON {

    public static void main(String[] args) {
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

        String filePath = "KStream-IDXStock.json";
        Dataset<Row> df = spark.read().format("json") // Use "csv" regardless of TSV or CSV.
                .option("header", "true") // Does the file have a header line?
                .load(filePath);

        try {
            df.filter(col("ticker").equalTo("BBCA")).filter(col("date").contains("2022")).show(10);
        } catch (Exception e){
            System.out.println("ERROR SPARK: "+e.getMessage());
        }
    }
}
