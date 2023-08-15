package org.flink.ml;

import org.apache.avro.Schema;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.flink.formats.avro.registry.confluent.ConfluentRegistryAvroDeserializationSchema;
import org.apache.avro.generic.GenericRecord;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class DataStreamAvroConsumeJob {
    public static void main(String[] args) throws Exception {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        String bootStrapServer = "localhost:39092,localhost:39093,localhost:39094";
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        String topic3 = "KSQLGROUPSTOCK"; //KSQLDB Table
        String schemaHost = "http://localhost:8282";
        String group = "my-flink-group";

        Properties kafkaConsumerConfig = new Properties();
        kafkaConsumerConfig.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServer);
        kafkaConsumerConfig.setProperty(ConsumerConfig.GROUP_ID_CONFIG, group);
        kafkaConsumerConfig.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        String SCHEMA_STOCK_PATH = "src/main/avro/avro-ksqlstock.avsc";
        String avroStockSchema = new String(Files.readAllBytes(Paths.get(SCHEMA_STOCK_PATH)));
        Schema schemaKsqlStock = new Schema.Parser().parse(avroStockSchema);
        DataStreamSource<GenericRecord> kafkaStream1 =
                env.addSource(
                        new FlinkKafkaConsumer<>(
                                topic3,
                                ConfluentRegistryAvroDeserializationSchema.forGeneric(schemaKsqlStock, schemaHost),
                                kafkaConsumerConfig)
                                .setStartFromEarliest());
        kafkaStream1.print();

        env.execute("Flink Kafka Topic Consumer");
    }
}
