package io.id.stock.analysis.Module;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaStockProducer {

    Boolean localServer;
    KafkaProducer<String, String> producer;

    private static final Logger log = LoggerFactory.getLogger(KafkaStockProducer.class.getSimpleName());

    public KafkaStockProducer(Boolean localServer) {
        this.localServer = localServer;
    }

    private KafkaProducer<String, String> createConnection() {
        String bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094";
        String bootStrapServer2 = "cluster.playground.cdkt.io:9092"; //kafka server from Conductor

        //Create producer properties
        Properties properties = new Properties();
        if(localServer){
            log.info("Call Kafka Local Server...");
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        } else {
            log.info("Call Kafka Conductor Server...");
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer2);
            properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"6W1Ja4rpKmarggy5YCr7In\" password=\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI2VzFKYTRycEttYXJnZ3k1WUNyN0luIiwib3JnYW5pemF0aW9uSWQiOjczMzM5LCJ1c2VySWQiOjg1MjY1LCJmb3JFeHBpcmF0aW9uQ2hlY2siOiIxNWRmZjljZC1hYTgwLTRlMmItYTAzYi1iMjUxYWMyNDA5YzMifX0.Dg8zCO6dJq9hDbVsuvzmlba2RWv6g0WBw0hkhpawX-w\";");
        }
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty("sasl.mechanism", "PLAIN");

        properties.setProperty(ProducerConfig.PARTITIONER_CLASS_CONFIG, RoundRobinPartitioner.class.getName()); //For testing purposes use round robbin partition

        //Important Config for Safe Producer especially for old version (Kafka <= 2.8)
        properties.setProperty(ProducerConfig.ACKS_CONFIG, "all"); //Ensure data is properly replicated before ack is received
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true"); //Broker will not allow any duplicate data from producer
        properties.setProperty(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE)); //Retry until it meet delivery.timeout.ms
        properties.setProperty(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "120000"); //Fail after retrying for 2 minutes
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5"); //Maximum 5 batches that run concurrently in single broker
        //min.insync.replica=2 //Ensure 2 brokers (1 leader and 1 in-sync replica) are available [THIS IS BROKER SETTING!!!]

        //Set high throughput producer config
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20"); //maximum wait is 20 ms before creating single batch
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); //Batch size is 32KB
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); //Compression for message text based i.e. Log Lines or JSON

        //Create the producer
        return new KafkaProducer<>(properties);
    }

    public void createProducerConn() {
        this.producer = createConnection();
    }

    public void startProducer(String topic, String key, String value){
        //Create a producer record
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, value);

        //Send data
        producer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                //executed everytime a record successfully sent or an exception is thrown
                if (exception == null) {
                    log.info("key: " + key + " | " + "Partition: " + metadata.partition());
                } else {
                    log.info("Error while producing ", exception);
                }
            }
        });
    }

    public void flushAndCloseProducer(){
        producer.flush();
        producer.close();
    }

}
