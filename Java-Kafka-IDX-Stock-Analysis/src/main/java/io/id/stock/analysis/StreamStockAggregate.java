package io.id.stock.analysis;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class StreamStockAggregate {

    private static final Logger log = LoggerFactory.getLogger(StreamStockAggregate.class.getSimpleName());

    private static Properties properties;

    private static Properties createProperties(String bootStrapServer1){
        properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "stream-idx-stock-application");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer1);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); //chose none/earliest/latest

        return properties;
    }

    public static void groupTicker(KStream<String, String> inputStream){
        String DUMMY_MATERIALIZED = "stream-group-stock";
        String TOPIC_OUTPUT = "group.stock";

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
                .peek((key, value) -> System.out.println("key:"+key+" value:"+value))
                .to(TOPIC_OUTPUT, Produced.with(
                        Serdes.String(),
                        Serdes.String()));
    }

    public static void main(String[] args) {
        String topic = "streaming.goapi.idx.stock.json";
        String bootStrapServer1 = "localhost:39092,localhost:39093,localhost:39094";

        StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> inputStream = builder.stream(topic);
        groupTicker(inputStream);

        final Topology appTopology = builder.build();
        log.info("Topology: {}", appTopology.describe());
        KafkaStreams streams = new KafkaStreams(appTopology, createProperties(bootStrapServer1));

        //Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        //Start stream
        streams.start();

    }
}
