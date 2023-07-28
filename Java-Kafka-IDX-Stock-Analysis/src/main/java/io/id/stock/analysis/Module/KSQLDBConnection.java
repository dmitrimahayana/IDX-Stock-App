package io.id.stock.analysis.Module;

import io.confluent.ksql.api.client.*;

import java.util.concurrent.ExecutionException;

public class KSQLDBConnection {

    String KSQLDB_SERVER_HOST;
    private int KSQLDB_SERVER_PORT;
    private Client client;
    private static InsertsPublisher insertsPublisher = new InsertsPublisher();

    public KSQLDBConnection(String host, int port){
        this.KSQLDB_SERVER_HOST = host;
        this.KSQLDB_SERVER_PORT = port;
    }

    public Client createConnection(){
        ClientOptions options = ClientOptions.create()
                .setHost(KSQLDB_SERVER_HOST)
                .setPort(KSQLDB_SERVER_PORT);

        client = Client.create(options);
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
