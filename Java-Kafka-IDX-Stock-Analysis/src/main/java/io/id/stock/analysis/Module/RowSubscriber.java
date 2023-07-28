package io.id.stock.analysis.Module;

import io.confluent.ksql.api.client.Row;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RowSubscriber implements Subscriber<Row> {

    private static final Logger log = LoggerFactory.getLogger(RowSubscriber.class.getSimpleName());
    private Subscription subscription;

    public RowSubscriber() {
    }

    MongoDBStock mongoDBConn = new MongoDBStock("mongodb://localhost:27017");

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        log.info("Subscriber is subscribed.");
        this.subscription = subscription;

        //Create MongoDB Connection
        mongoDBConn.createConnection();

        // Request the first row
        subscription.request(1);
    }

    public String createJsonString(Row row){
        String jsonCol = row.columnNames().toString().replace("[", "").replace("]", "").replace(" ", "").toLowerCase();
        String jsonVal = row.values().toString().replace("[", "").replace("]", "").replace(", ", " ");
        String[] jsonColSplit = jsonCol.split(",");
        String[] jsonValSplit = jsonVal.split(",");
        String finalJson= "";
        if (jsonColSplit.length == jsonValSplit.length){
            for(int i = 0; i < jsonColSplit.length; i++){
                if(i == 0){
                    finalJson = "{" + jsonColSplit[i] + ":" + jsonValSplit[i];
                } else if (i == jsonColSplit.length-1) {
                    finalJson = finalJson + "," + jsonColSplit[i] + ":" + jsonValSplit[i] + "}";
                } else {
                    finalJson = finalJson + "," + jsonColSplit[i] + ":" + jsonValSplit[i];
                }
            }
        } else {
            log.info("Total Column " + jsonColSplit.length + "is not match with Total Value " + jsonValSplit.length);
        }
        return finalJson;
    }

    @Override
    public synchronized void onNext(Row row) {
//        log.info("Received a row!");

//        log.info("KSQL Column: " + jsonCol+" - Value: " + jsonVal);
        String finalJson = createJsonString(row);
        log.info("Async Query Result "+ finalJson);
        mongoDBConn.insertOrUpdate("kafka", "ksql-stock-stream", finalJson);

        // Request the next row
        subscription.request(1);
    }

    @Override
    public synchronized void onError(Throwable t) {
        System.out.println("Received an error: " + t);
    }

    @Override
    public synchronized void onComplete() {
        System.out.println("Query has ended.");
    }
}

