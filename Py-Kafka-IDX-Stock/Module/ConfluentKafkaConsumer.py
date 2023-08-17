from confluent_kafka import Consumer, KafkaError, KafkaException

bootStrapServer = 'localhost:39092,localhost:39093,localhost:39094'
groupID = 'my-python-kafka-group-consumer'
topic = 'Dummy_Python'
MIN_COMMIT_COUNT = 5


def commit_completed(err, partitions):
    if err:
        print(str(err))
    else:
        print("Committed partition offsets: " + str(partitions))


conf = {'bootstrap.servers': bootStrapServer,
        'group.id': groupID,
        'enable.auto.commit': False,
        'auto.offset.reset': 'earliest'}


def msg_handler(msg):
    if msg.error():
        print(f"Error: {msg.error()}")
    else:
        try:
            message_value = msg.value().decode('utf-8')  # Deserialize the message value
            print(
                f"Received message: {message_value} Partition: {msg.partition()}, Offset: {msg.offset()}, Topic: {msg.topic()}")
        except Exception as e:
            print(f"Error decoding message value: {e}")


def consume_loop(consumer, topics):
    try:
        consumer.subscribe(topics)

        msg_count = 0
        while True:
            msg = consumer.poll(timeout=1.0)
            if msg is None:
                continue
            if msg.error():
                if msg.error().code() == KafkaError._PARTITION_EOF:
                    # End of partition event
                    print('%% %s [%d] reached end at offset %d\n' %
                          (msg.topic(), msg.partition(), msg.offset()))
                elif msg.error():
                    print("Error: ", msg.error())
            else:
                msg_handler(msg)
                msg_count += 1
                if msg_count % MIN_COMMIT_COUNT == 0:
                    print("Committed in " + str(msg_count) + " message")
                    consumer.commit(asynchronous=True)
    finally:
        # Close down consumer to commit final offsets.
        consumer.close()


# Create a Kafka consumer instance
consumer = Consumer(conf)
consume_loop(consumer, [topic])
