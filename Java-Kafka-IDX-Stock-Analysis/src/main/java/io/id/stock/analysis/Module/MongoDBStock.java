package io.id.stock.analysis.Module;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDBStock {

    String uri;
    ServerApi serverApi;
    MongoClientSettings settings;
    MongoClient mongoClient;
    MongoDatabase mongodb;
    MongoCollection<Document> collection;

    private static final Logger log = LoggerFactory.getLogger(MongoDBStock.class.getSimpleName());

    public MongoDBStock(String connectionString){
        this.uri = connectionString;
    }
    public void createConnection(){
        // Construct a ServerApi instance using the ServerApi.builder() method
        serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .serverApi(serverApi)
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            this.mongoClient = MongoClients.create(settings);
            log.info("Connection MongoDB Success");
        } catch (Exception e){
            log.info("Connection MongoDB Failed "+e.getMessage());
        }
    }

    public void insertOneDoc(String databaseName, String collectionName, String json){
        try {
            mongodb = mongoClient.getDatabase(databaseName);
            collection = mongodb.getCollection(collectionName);

            InsertOneResult result = collection.insertOne(Document.parse(json));
            log.info("Inserted a document with the following id: " + result.getInsertedId().asObjectId().getValue().toString());
        } catch (MongoException me) {
            log.info(me.getMessage());
        }
    }

    public void insertOrUpdate(String databaseName, String collectionName, String strJson){
        try {
            mongodb = mongoClient.getDatabase(databaseName);
            collection = mongodb.getCollection(collectionName);
            Document newDoc = Document.parse(strJson); //Convert to Doc format

            // Check if we need to update or insert
            Document query = new Document();
            query.append("id", newDoc.getString("id"));
            Document existingDocument = collection.find(query).first();
            if (existingDocument == null) {
                // Insert the document if no doc found
                InsertOneResult result = collection.insertOne(newDoc);
//                System.out.println("Inserted collection "+collectionName+" documentID " + newDoc.getString("id") + " with the following mongoID: " + result.getInsertedId().asObjectId().getValue().toString());
            } else {
                // Update the document if doc found with existing id
                Bson filter = (Filters.eq("id", newDoc.getString("id")));
                UpdateResult updateResult = collection.replaceOne(filter, newDoc);
//                System.out.println("Existing collection "+collectionName+" documentID " + newDoc.getString("id") + " modified document count: " + updateResult.getModifiedCount());
            }
        } catch (MongoException me) {
            log.info(me.getMessage());
        }
    }
}
