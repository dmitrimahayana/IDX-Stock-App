import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import static org.apache.spark.sql.functions.col;

public class TestSparkFilter {

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

        Dataset<Row> df = spark.read().format("csv") // Use "csv" regardless of TSV or CSV.
                .option("header", "true") // Does the file have a header line?
                .load("D:/00 Project/00 My Project/Dataset/Digimon Dataset/DigiDB_digimonlist.csv");

        try {
            df.filter(col("Stage").equalTo("Champion")).filter(col("Digimon").equalTo("Garurumon")).show(10);
        } catch (Exception e){
            System.out.println("ERROR SPARK: "+e.getMessage());
        }
    }
}
