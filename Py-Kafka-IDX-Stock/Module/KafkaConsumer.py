from kafka import KafkaConsumer

bootStrapServer = 'localhost:39092,localhost:39093,localhost:39094'
topic = 'Dummy_Python'
consumer = KafkaConsumer(topic,
                         bootstrap_servers=bootStrapServer,
                         auto_offset_reset='earliest',
                         group_id='my_favorite_group')

for message in consumer:
    # message value and key are raw bytes -- decode if necessary!
    # e.g., for unicode: `message.value.decode('utf-8')`
    print("Topic:%s Partition:%d Offset:%d key=%s value=%s" % (message.topic, message.partition,
                                         message.offset, message.key.decode('utf-8'),
                                         message.value.decode('utf-8')))
