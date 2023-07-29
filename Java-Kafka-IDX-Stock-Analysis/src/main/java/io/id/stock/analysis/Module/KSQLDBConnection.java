package io.id.stock.analysis.Module;

import io.confluent.ksql.api.client.*;
import io.id.stock.analysis.GetStockPrice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

public class KSQLDBConnection {

    String KSQLDB_SERVER_HOST;
    private int KSQLDB_SERVER_PORT;
    private Client client;
    private static InsertsPublisher insertsPublisher = new InsertsPublisher();

    private static final Logger log = LoggerFactory.getLogger(GetStockPrice.class.getSimpleName());

    public KSQLDBConnection(String host, int port){
        this.KSQLDB_SERVER_HOST = host;
        this.KSQLDB_SERVER_PORT = port;
    }

    public Client createConnection(){
        ClientOptions options = ClientOptions.create()
                .setHost(KSQLDB_SERVER_HOST)
                .setPort(KSQLDB_SERVER_PORT);

        client = Client.create(options);
        log.info("Connected to KSQLDB: "+KSQLDB_SERVER_HOST+" "+KSQLDB_SERVER_PORT);
        return client;
    }

    public AcksPublisher acksPublisher (String StreamTopic) throws ExecutionException, InterruptedException {
        return client.streamInserts(StreamTopic, insertsPublisher).get();
    }

    public void acceptPublisher(KsqlObject row){
        insertsPublisher.accept(row);
    }

    public void insertPublisher(){
        insertsPublisher.complete();
    }

    public void closeConnection(){
        client.close();
    }
}
