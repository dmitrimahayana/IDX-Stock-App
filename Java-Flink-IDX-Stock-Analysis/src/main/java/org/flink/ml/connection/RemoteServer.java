package org.flink.ml.connection;

import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.ExecutionCheckpointingOptions;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;

public class RemoteServer {
    public StreamExecutionEnvironment env;
    public StreamTableEnvironment tableEnv;
    public String bootStrapServer;
    public String schemaHost;
    public String mongodbHost;
    Boolean runLocal;
    String jarsPath;

    public RemoteServer(Boolean runLocal, String jarsPath) {
        this.runLocal = runLocal;
        this.jarsPath = jarsPath;
    }

    public void EstablishConnection() {
        if (runLocal) {
            System.out.println("Connected to Mini Local Flink");
            bootStrapServer = "localhost:39092,localhost:39093,localhost:39094";
            schemaHost = "http://localhost:8282";
            env = StreamExecutionEnvironment.getExecutionEnvironment();
            mongodbHost = "mongodb://localhost:27017";
        } else {
            System.out.println("Connected to Docker Flink");
            bootStrapServer = ",kafka1:19092,kafka2:19093,kafka3:19094"; //Docker Kafka URL
            schemaHost = "http://schema-registry:8282"; //Docker Schema Registry URL
            mongodbHost = "mongodb://mongodb-server:27017";
            env = StreamExecutionEnvironment.createRemoteEnvironment(
                    "localhost",
                    8383,
                    jarsPath + "flink-connector-kafka-1.17.1.jar",
                    jarsPath + "kafka-clients-3.2.3.jar",
                    jarsPath + "flink-avro-1.17.1.jar",
                    jarsPath + "avro-1.11.0.jar",
                    jarsPath + "kafka-schema-registry-client-7.4.0.jar",
                    jarsPath + "flink-avro-confluent-registry-1.17.1.jar",
                    jarsPath + "jackson-core-2.12.5.jar",
                    jarsPath + "jackson-databind-2.12.5.jar",
                    jarsPath + "jackson-annotations-2.12.5.jar",
                    jarsPath + "guava-30.1.1-jre.jar",
                    jarsPath + "flink-table-runtime-1.17.1.jar",
                    jarsPath + "flink-connector-files-1.17.1.jar",
                    jarsPath + "flink-ml-uber-1.17-2.3.0.jar",
                    jarsPath + "statefun-flink-core-3.2.0.jar",
                    jarsPath + "flink-connector-mongodb-1.0.1-1.17.jar",
                    jarsPath + "bson-4.7.2.jar",
                    jarsPath + "mongodb-driver-sync-4.7.2.jar",
                    jarsPath + "mongodb-driver-core-4.7.2.jar"
            );

            // start a checkpoint every x ms
            int checkpoint = 1;
            env.enableCheckpointing(checkpoint * 1000);
            // advanced options:
            // set mode to exactly-once (this is the default)
            env.getCheckpointConfig().setCheckpointingMode(CheckpointingMode.EXACTLY_ONCE);
            // make sure 500 ms of progress happen between checkpoints
            env.getCheckpointConfig().setMinPauseBetweenCheckpoints(500);
            // checkpoints have to complete within one minute, or are discarded
            env.getCheckpointConfig().setCheckpointTimeout(60000);
            // only two consecutive checkpoint failures are tolerated
            env.getCheckpointConfig().setTolerableCheckpointFailureNumber(2);
            // allow only one checkpoint to be in progress at the same time
            env.getCheckpointConfig().setMaxConcurrentCheckpoints(1);
            // enable externalized checkpoints which are retained
            // after job cancellation
            env.getCheckpointConfig().setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
            // enables the unaligned checkpoints
            env.getCheckpointConfig().enableUnalignedCheckpoints();
            // sets the checkpoint storage where checkpoint snapshots will be written
            env.getCheckpointConfig().setCheckpointStorage("file:///opt/flink/checkpoints");
            // enable checkpointing with finished tasks
            Configuration config = new Configuration();
            config.set(ExecutionCheckpointingOptions.ENABLE_CHECKPOINTS_AFTER_TASKS_FINISH, true);
            env.configure(config);
        }

        EnvironmentSettings settings = EnvironmentSettings
                .newInstance()
                .inStreamingMode()
//                .inBatchMode()
                .build();

        tableEnv = StreamTableEnvironment.create(env, settings);
    }
}
