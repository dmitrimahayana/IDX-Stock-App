import com.google.gson.Gson;
import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONObject;
import org.json.JSONArray;

public class TestMongoDB {

    public static void main(String[] args) {
        // Set up connection parameters
        String connectionString = "mongodb://localhost:27017"; // MongoDB server URI
        String databaseName = "kafka"; // Name of the database
        String collectionName = "stock-stream"; // Name of the collection

        // Connect to MongoDB server
        ConnectionString connString = new ConnectionString(connectionString);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();

        try (com.mongodb.client.MongoClient mongoClient = MongoClients.create(settings)) {

            // Access the database
            MongoDatabase database = mongoClient.getDatabase(databaseName);

            // Access the collection
            MongoCollection<Document> collection = database.getCollection(collectionName);

            String strJson = "{\"id\":\"ASII_2020-01-02\",\"ticker\":\"ASII\",\"date\":\"2020-01-02\",\"open\":121.0,\"high\":222.0,\"low\":333.0,\"close\":444.0,\"volume\":15008600}";
            Document newDoc = Document.parse(strJson); //Convert to Doc format

            // Query the collection
            // Define the filter conditions
            Document query = new Document();
//            query.append("date", new Document("$regex", ".*2023.*"));
//            query.append("date", dateStock);
//            query.append("ticker", tickerStock);
            query.append("id", newDoc.getString("id"));
            FindIterable<Document> documents = collection.find(query);

            // Iterate over the documents
            for (Document document : documents) {
                // Access the fields of each document
                String id = (String) document.get("id");
                String ticker = (String) document.get("ticker");
                String date = (String) document.get("date");
                Double open = (Double) document.get("open");
                Double high = (Double) document.get("high");
                Double low = (Double) document.get("low");
                Double close = (Double) document.get("close");
                Long volume = Long.parseLong(document.get("volume").toString());

                // Do something with the data
                System.out.println("id: " + id);
                System.out.println("ticker: " + ticker);
                System.out.println("date: " + date);
                System.out.println("open: " + open);
                System.out.println("high: " + high);
                System.out.println("low: " + low);
                System.out.println("close: " + close);
                System.out.println("volume: " + volume);
                System.out.println("-------------------------");
            }

            Document existingDocument = collection.find(query).first();
            if (existingDocument == null) {
                // Insert the document if no duplicates found
                InsertOneResult result = collection.insertOne(newDoc);
                System.out.println("Inserted a document with the following id: " + result.getInsertedId().asObjectId().getValue().toString());
            } else {
                Bson filter = (Filters.eq("id", newDoc.getString("id")));
                UpdateResult updateResult = collection.replaceOne(filter, newDoc);
                System.out.println("Existing document " + newDoc.getString("id") + " modified document count: " + updateResult.getModifiedCount());
            }

//            JSONArray jArray = new JSONArray();
//
//            for (Document document : documents) {
//                JSONObject jObject = new JSONObject(document.toJson());
//                jArray.put(jObject);
//            }
//
//            for (Object jObject : jArray){
//                System.out.println(jObject);
//            }



        }
    }
}
