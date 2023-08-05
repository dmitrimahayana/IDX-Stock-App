package io.id.stock.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class KStreamStringStockAggregate {

    private static final Logger log = LoggerFactory.getLogger(KStreamStringStockAggregate.class.getSimpleName());

    private static Properties properties;

    private static Properties createProperties(String bootStrapServer1){
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-idx-stock-application");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        return properties;
    }

    public static void groupStock(KStream<String, String> inputStream, String topicOutput){
        String DUMMY_MATERIALIZED = "stream-group-stock";

        inputStream
                .groupByKey()
                .aggregate(
                        () -> "",
                        (key, value, aggregate) -> {
                            aggregate = value;
                            return aggregate;
                        },
                        Materialized.as(DUMMY_MATERIALIZED)
                )
                .toStream()
                .peek((key, value) -> System.out.println("KStream "+topicOutput+" key:"+key+" value:"+value))
                .to(topicOutput, Produced.with(
                        Serdes.String(),
                        Serdes.String()));
    }

    public static void groupCompany(KStream<String, String> inputStream, String topicOutput){
        String DUMMY_MATERIALIZED = "stream-group-company";

        inputStream
                .groupByKey()
                .aggregate(
                        () -> "",
                        (key, value, aggregate) -> {
                            aggregate = value;
                            return aggregate;
                        },
                        Materialized.as(DUMMY_MATERIALIZED)
                )
                .toStream()
                .peek((key, value) -> System.out.println("KStream "+topicOutput+" key:"+key+" value:"+value))
                .to(topicOutput, Produced.with(
                        Serdes.String(),
                        Serdes.String()));
    }

    public static void joinKTableStock(String stockTopic, String companyTopic, StreamsBuilder builder, String outputTopic){
        KTable<String, String> groupStockTable = builder
                .table(stockTopic,  Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("ktable-group-stock")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.String()));
        KTable<String, String> CompanyTable = builder
                .table(companyTopic,  Materialized.<String, String, KeyValueStore<Bytes, byte[]>>as("ktable-group-company")
                        .withKeySerde(Serdes.String())
                        .withValueSerde(Serdes.String()));

        ObjectMapper OBJECT_MAPPER = new ObjectMapper();
        KTable<String, String> joinTable = groupStockTable.join(CompanyTable,
                (stockValue, companyValue) -> {
                    try {
                        final JsonNode jsonStock = OBJECT_MAPPER.readTree(stockValue);
                        final JsonNode jsonCompany = OBJECT_MAPPER.readTree(companyValue);
                        String result = "{" +
                                "\"id\":\""+jsonStock.get("id").asText()+"\""+
                                ",\"ticker\":\""+jsonStock.get("ticker").asText()+"\""+
                                ",\"date\":\""+jsonStock.get("date").asText()+"\""+
                                ",\"open\":"+jsonStock.get("open").asDouble()+
                                ",\"high\":"+jsonStock.get("high").asDouble()+
                                ",\"low\":"+jsonStock.get("low").asDouble()+
                                ",\"close\":"+jsonStock.get("close").asDouble()+
                                ",\"volume\":"+jsonStock.get("volume").asLong()+
                                ",\"name\":\""+jsonCompany.get("name").asText()+"\""+
                                ",\"logo\":\""+jsonCompany.get("logo").asText()+"\""+
                                "}";
                        return (result);
                    } catch (IOException e) {
                        return "parse-error " + e.getMessage();
                    }
        });

        //Save KTable in new topic
        joinTable
                .toStream()
                .peek((key, value) -> System.out.println("KTable "+outputTopic+" key:"+key+" value:"+value))
                .to(outputTopic, Produced.with(
                        Serdes.String(),
                        Serdes.String()));
    }

    public static void main(String[] args) {
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        String topic3 = "group.stock";
        String topic4 = "group.company";
        String topic5 = "join.stock.company";
        String bootStrapServer = "localhost:39092,localhost:39093,localhost:39094";

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> inputStream1 = builder.stream(topic1);
        KStream<String, String> inputStream2 = builder.stream(topic2);
        groupStock(inputStream1, topic3);
        groupCompany(inputStream2, topic4);
        joinKTableStock(topic3, topic4, builder, topic5);

        final Topology appTopology = builder.build();
        log.info("Topology: {}", appTopology.describe());
        KafkaStreams streams = new KafkaStreams(appTopology, createProperties(bootStrapServer));

        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        //Start stream
        streams.start();

    }
}
