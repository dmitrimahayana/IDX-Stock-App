package io.id.stock.analysis;

import io.id.stock.analysis.Module.MongoDBStock;
import io.id.stock.analysis.Module.kafkaStockConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class SaveCurrentStock {

    private static final Logger log = LoggerFactory.getLogger(SaveCurrentStock.class.getSimpleName());

    public static void main(String[] args) {
//        #Create KSQL Stream
//        CREATE OR REPLACE STREAM StreamIdxStockPrice (id VARCHAR, ticker VARCHAR, date VARCHAR, open DOUBLE, high DOUBLE, low DOUBLE, close DOUBLE, volume BIGINT)
//        WITH (kafka_topic='streaming.goapi.idx.stock.json', value_format='json', partitions=6);
//
//        #Create KSQL Materialized View to remove duplication
//        CREATE TABLE currentIdxStockPrice AS
//        SELECT
//            id,
//            latest_by_offset(ticker) AS ticker,
//            latest_by_offset(date) AS date,
//            latest_by_offset(open) AS open,
//            latest_by_offset(high) AS high,
//            latest_by_offset(low) AS low,
//            latest_by_offset(close) AS close,
//            latest_by_offset(volume) AS volume
//            FROM StreamIdxStockPrice GROUP BY id EMIT CHANGES;

        //Create Kafka Consumer Connection
        Boolean localServer = true;
        String groupId = "consumer-goapi-idx-stock";
        String offset = "earliest"; //use  earliest for testing purposes
        kafkaStockConsumer consumer = new kafkaStockConsumer(localServer, groupId, offset);

        //Create MongoDB Connection
        MongoDBStock mongoDBConn = new MongoDBStock("mongodb://localhost:27017");
        mongoDBConn.createConnection();

        //get a reference to the main thread
        final Thread mainThread = Thread.currentThread();
        //adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
            log.info("Detected a shutdown, let's exit by calling consumer consumer.wakeup()...");
            consumer.wakeUp();
            log.info("Consumer has sent wakeup signal...");
            //join the main thread to allow the execution of the code in the main thread
            try {
                mainThread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            }
        });

        try {
            String topic1 = "group.stock"; //Must check if topic has been created by KSQL or KStream
            String topic2 = "streaming.goapi.idx.companies.json"; //Must check if topic has been created by KSQL or KStream
            //Create consumer
            consumer.createConsumer(Arrays.asList(topic1, topic2));

            //Show data
            while (true){
                //Polling data from topics
                ConsumerRecords<String, String> records = consumer.pollingData();
                int recordCount = records.count();
                log.info("Received "+recordCount+" records");

                for(ConsumerRecord<String, String> record: records) {
                    try {
                        System.out.println("key: " + record.key() + " --- value: " + record.value());
                        //Insert to MongoDB based on the topic
                        if (record.topic().toLowerCase().equals(topic1.toLowerCase())){
                            mongoDBConn.insertOrUpdate("kafka", "stock-stream", record.value().toString());
                        } else if(record.topic().toLowerCase().equals(topic2.toLowerCase())){
                            mongoDBConn.insertOrUpdate("kafka", "company-stream", record.value().toString());
                        }

//                        //Insert to flat file
//                        String filePath = "KStream-IDXStock.json";
//                        try (FileWriter fileWriter = new FileWriter(filePath, true)) {
//                            fileWriter.write(record.value());
//                            fileWriter.write(System.lineSeparator());
//                        } catch (IOException e) {
//                            throw new RuntimeException(e);
//                        }
                    } catch (Exception e) {
                        System.out.println("Error: "+e);
                    }
                }

                try{
                    Thread.sleep(1000);
                } catch (InterruptedException error){
                    error.printStackTrace();
                }

                //commit offset after batch is consumed
                consumer.commit();
                log.info("Offset have been commited");
            }
        }catch (WakeupException e) {
            log.info("Consumer is starting to shut down...");
        }catch (Exception e){
            log.info("Unexpected exception in the consumer", e);
        }finally {
            log.info("starting to close now...");
            consumer.close();
            log.info("Consumer was closed gracefully");
        }

    }
}
