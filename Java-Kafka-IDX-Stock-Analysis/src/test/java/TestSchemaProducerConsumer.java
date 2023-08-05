import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Properties;
import java.util.Random;

public class TestSchemaProducerConsumer {
    private static final String TOPIC = "streaming.goapi.idx.stock.json";
    private static final String SCHEMA_FILE_PATH = "avro-stock.avsc";

    public static void main(String[] args) {
        produceAvroRecord();
        consumeAvroRecord();
    }

    public static void produceAvroRecord() {
        String bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094";
        String schemaHost = "http://localhost:8282";
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put("schema.registry.url", schemaHost);

        KafkaProducer<String, GenericRecord> producer = new KafkaProducer<>(properties);

        try {
            String avroSchema = new String(Files.readAllBytes(Paths.get(SCHEMA_FILE_PATH)));
            Schema schema = new Schema.Parser().parse(avroSchema);

            Random r = new Random();
            DecimalFormat f = new DecimalFormat("##.00");
            for (int i = 1; i <= 10; i++) {
                GenericRecord record = new GenericData.Record(schema);
                String ticker = "BBCA" + i;
                String date = "2023-08-0" + i;
                String id = ticker+"_"+date;

                Double rangeMin = 500.0;
                Double rangeMax = 5000.0;
                double randomValue1 = rangeMin + (rangeMax - rangeMin) * r.nextDouble(); //For Testing aggregation purpose
                double randomValue2 = rangeMin + (rangeMax - rangeMin) * r.nextDouble(); //For Testing aggregation purpose
                double randomValue3 = rangeMin + (rangeMax - rangeMin) * r.nextDouble(); //For Testing aggregation purpose
                double randomValue4 = rangeMin + (rangeMax - rangeMin) * r.nextDouble(); //For Testing aggregation purpose
                record.put("id", id);
                record.put("ticker", ticker);
                record.put("date", date);
                record.put("open", randomValue1);
                record.put("high", randomValue2);
                record.put("low", randomValue3);
                record.put("close", randomValue4);
                record.put("volume", 10000000 + i);

                ProducerRecord<String, GenericRecord> producerRecord = new ProducerRecord<>(TOPIC, record);

                producer.send(producerRecord, (recordMetadata, e) -> {
                    if (e == null) {
                        System.out.println("Produced record to topic: " + recordMetadata.topic() +
                                ", partition: " + recordMetadata.partition() +
                                ", offset: " + recordMetadata.offset());
                    } else {
                        e.printStackTrace();
                    }
                });
            };

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            producer.close();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(TestSchemaProducerConsumer.class.getSimpleName());
    public static void consumeAvroRecord() {
        String bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094";
        String schemaHost = "http://localhost:8282";
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "avro-consumer-group");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); //chose none/earliest/latest
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        properties.put("schema.registry.url", schemaHost);

        KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(properties);

        consumer.subscribe(java.util.Collections.singletonList(TOPIC));

        //get a reference to the main thread
        final Thread mainThread = Thread.currentThread();
        //adding the shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(){
            public void run(){
                log.info("Detected a shutdown, let's exit by calling consumer consumer.wakeup()...");
                consumer.wakeup();
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
            while (true) {
                ConsumerRecords<String, GenericRecord> records = consumer.poll(java.time.Duration.ofMillis(100));
                for (ConsumerRecord<String, GenericRecord> record : records) {
                    GenericRecord avroRecord = record.value();
                    System.out.println("Consumed Avro record: " + avroRecord.get("id") + " --- " + avroRecord.get("open") + " --- " + avroRecord.get("close") + " --- " + avroRecord.get("volume"));
                }
            }
        }catch (WakeupException e) {
            log.info("Consumer is starting to shut down...");
            consumer.close();
        }catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }
}
