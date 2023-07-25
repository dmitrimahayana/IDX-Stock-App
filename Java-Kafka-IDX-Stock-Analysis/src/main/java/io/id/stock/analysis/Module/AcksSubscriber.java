package io.id.stock.analysis.Module;

import io.confluent.ksql.api.client.InsertAck;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AcksSubscriber implements Subscriber<InsertAck> {

    private static final Logger log = LoggerFactory.getLogger(AcksSubscriber.class.getSimpleName());
    private Subscription subscription;

    public AcksSubscriber() {
    }

    @Override
    public synchronized void onSubscribe(Subscription subscription) {
        log.info("Subscriber is subscribed.");
        this.subscription = subscription;

        // Request the first ack
        subscription.request(1);
    }

    @Override
    public synchronized void onNext(InsertAck ack) {
        log.info("Received an ack for insert number: " + ack.seqNum());

        // Request the next ack
        subscription.request(1);
    }

    @Override
    public synchronized void onError(Throwable t) {
        log.info("Received an error: " + t);
    }

    @Override
    public synchronized void onComplete() {
        log.info("Inserts stream has been closed.");
    }
}
