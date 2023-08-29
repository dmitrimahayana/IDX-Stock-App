package io.id.stock.analysis;


import io.confluent.kafka.serializers.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;

public class KStreamAggregateAvroStock {

    private static final Logger log = LoggerFactory.getLogger(KStreamAggregateAvroStock.class.getSimpleName());

    private static Properties properties;

    private static Properties createProperties(String bootStrapServer1, String schemaHost){
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-idx-stock-application");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
//        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.ByteArray().getClass().getName());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class);
        properties.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaHost);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return properties;
    }

    public static void groupStream(KStream<String, GenericRecord> inputStream, String topicOutput){
        inputStream
                .groupByKey()
                .aggregate(
                        () -> null,
                        (key, value, aggregate) -> {
                            aggregate = value;
                            return aggregate;
                        },
                        Materialized.with(stringSerde, genericAvroSerde)
                )
                .toStream()
                .peek((key, value) -> System.out.println("KStream "+topicOutput+" key:"+key+" value:"+value))
                .to(topicOutput, Produced.with(stringSerde, genericAvroSerde));
    }

    public static void joinKTableStock(String stockTopic, String companyTopic, StreamsBuilder builder, String outputTopic, Schema schema){
        KTable<String, GenericRecord> groupStockTable = builder
                .table(stockTopic,  Materialized.with(stringSerde, genericAvroSerde));
        KTable<String, GenericRecord> CompanyTable = builder
                .table(companyTopic,  Materialized.with(stringSerde, genericAvroSerde));

        KTable<String, GenericRecord> joinTable = groupStockTable.join(CompanyTable,
                (stockValue, companyValue) -> {
                    try {
                        GenericRecord record = new GenericData.Record(schema);
                        record.put("id", stockValue.get("id"));
                        record.put("ticker", stockValue.get("ticker"));
                        record.put("date", stockValue.get("date"));
                        record.put("open", stockValue.get("open"));
                        record.put("high", stockValue.get("high"));
                        record.put("low", stockValue.get("low"));
                        record.put("close", stockValue.get("close"));
                        record.put("volume", stockValue.get("volume"));
                        record.put("name", companyValue.get("name"));
                        record.put("logo", companyValue.get("logo"));
                        return (record);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        //Save KTable in new topic
        joinTable
                .toStream()
                .peek((key, value) -> System.out.println("KTable "+outputTopic+" key:"+key+" value:"+value))
                .to(outputTopic, Produced.with(stringSerde, genericAvroSerde));
    }

    final static Serde<String> stringSerde = Serdes.String();
    final static Serde<GenericRecord> genericAvroSerde = new GenericAvroSerde();

    final static Schema createSchemaFromFile(String filepath) {
        String avroSchema = null;
        try {
            avroSchema = new String(Files.readAllBytes(Paths.get(filepath)));
            Schema schema = new Schema.Parser().parse(avroSchema);
            return schema;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        String topic3 = "kstream.group.stock";
        String topic4 = "kstream.group.company";
        String topic5 = "kstream.join.stock.company";
        String bootStrapServer = "localhost:39092,localhost:39093,localhost:39094";
        String schemaHost = "http://localhost:8282";

        genericAvroSerde.configure(Collections.singletonMap(AbstractKafkaSchemaSerDeConfig .SCHEMA_REGISTRY_URL_CONFIG, schemaHost), false);
        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, GenericRecord> inputStream1 = builder.stream(topic1);
        KStream<String, GenericRecord> inputStream2 = builder.stream(topic2);
        groupStream(inputStream1, topic3);
        groupStream(inputStream2, topic4);

        Schema schema = createSchemaFromFile("avro-joinstock.avsc");
        joinKTableStock(topic3, topic4, builder, topic5, schema);

        final Topology appTopology = builder.build();
        log.info("Topology: {}", appTopology.describe());
        KafkaStreams streams = new KafkaStreams(appTopology, createProperties(bootStrapServer, schemaHost));

        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        //Start stream
        streams.start();

    }
}
