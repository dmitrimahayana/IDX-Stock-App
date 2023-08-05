package io.id.stock.analysis.Module;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

public class kafkaStockConsumer {

    Boolean localServer;
    String offset;
    String groupId;
    KafkaConsumer<String, GenericRecord> consumer;

    private static final Logger log = LoggerFactory.getLogger(kafkaStockConsumer.class.getSimpleName());

    public kafkaStockConsumer(Boolean localServer, String groupId, String offset) {
        this.localServer = localServer;
        this.groupId = groupId;
        this.offset = offset;
    }

    private KafkaConsumer<String, GenericRecord> createConnection() {
        String bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094";
        String bootStrapServer2 = "cluster.playground.cdkt.io:9092"; //kafka server from Conductor
        String schemaHost = "http://localhost:8282";

        //Create producer properties
        Properties properties = new Properties();
        if(localServer){
            log.info("Call Kafka Local Server...");
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        } else {
            log.info("Call Kafka Conductor Server...");
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer2);
            properties.setProperty("security.protocol", "SASL_SSL");
            properties.setProperty("sasl.jaas.config", "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"6W1Ja4rpKmarggy5YCr7In\" password=\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJodHRwczovL2F1dGguY29uZHVrdG9yLmlvIiwic291cmNlQXBwbGljYXRpb24iOiJhZG1pbiIsInVzZXJNYWlsIjpudWxsLCJwYXlsb2FkIjp7InZhbGlkRm9yVXNlcm5hbWUiOiI2VzFKYTRycEttYXJnZ3k1WUNyN0luIiwib3JnYW5pemF0aW9uSWQiOjczMzM5LCJ1c2VySWQiOjg1MjY1LCJmb3JFeHBpcmF0aW9uQ2hlY2siOiIxNWRmZjljZC1hYTgwLTRlMmItYTAzYi1iMjUxYWMyNDA5YzMifX0.Dg8zCO6dJq9hDbVsuvzmlba2RWv6g0WBw0hkhpawX-w\";");
        }
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
//        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); //String Deserializer
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName()); //Avro Deserializer
        properties.setProperty("schema.registry.url", schemaHost);
        properties.setProperty("sasl.mechanism", "PLAIN");

        //create consumer config
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offset); //chose none/earliest/latest
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); //enable or disable auto commit offset

        //create a consumer
        return new KafkaConsumer<>(properties);
    }

    public void createConsumer(List<String> topics) {
        consumer = createConnection();
        consumer.subscribe(topics);
    }

    public ConsumerRecords<String, GenericRecord> pollingData(){
        return consumer.poll(Duration.ofMillis(3000)); //call every 3 sec when there is no data
    }

    public void commit(){
        consumer.commitSync();
    }

    public void wakeUp(){
        consumer.wakeup();
    }

    public void close(){
        consumer.close();
    }

}
