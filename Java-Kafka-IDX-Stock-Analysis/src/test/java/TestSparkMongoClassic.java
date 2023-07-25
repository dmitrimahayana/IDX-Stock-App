import com.mongodb.*;
import com.mongodb.client.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.bson.Document;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Collections;

public class TestSparkMongoClassic {

    public static void main(String[] args) {
        // Set up MongoDB connection parameters
        String connectionString = "mongodb://localhost:27017"; // MongoDB server URI
        String databaseName = "kafka"; // Name of the database
        String collectionName = "stock-stream"; // Name of the collection

        // Connect to MongoDB server
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();

        // Spark Host
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

        try (com.mongodb.client.MongoClient mongoClient = MongoClients.create(settings)) {

            // Access the database
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Access the collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            // Query the collection
            // Define the filter conditions
            Document query = new Document();
            query.append("date", new Document("$regex", ".*2023.*"));
//            query.append("ticker", "BBCA");
            FindIterable<Document> documents = collection.find(query);

            JSONArray jArray = new JSONArray();

            for (Document document : documents) {
                JSONObject jObject = new JSONObject(document.toJson());
                jArray.put(jObject);
            }

            Dataset<Row> df = spark.read().json(spark.createDataset(Collections.singletonList(jArray.toString()), Encoders.STRING()));
            df = df.select("ticker", "date", "open", "volume", "close");
            df.printSchema();
            df.show(5);
            System.out.println("Total Data: "+df.count());
        }
    }
}
