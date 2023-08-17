from kafka import KafkaProducer

bootStrapServer = 'localhost:39092,localhost:39093,localhost:39094'
topic = 'Dummy_Python'

producer = KafkaProducer(bootstrap_servers=bootStrapServer,
                         key_serializer=str.encode,
                         value_serializer=str.encode)


def on_send_success(record_metadata):
    print("Topic:%s Partition:%d Offset:%d Timestamp:%s" %
          (record_metadata.topic, record_metadata.partition, record_metadata.offset, record_metadata.timestamp))


def on_send_error(excp):
    log.error('I am an errback', exc_info=excp)
    # handle exception


# produce asynchronously
for i in range(10):
    producer.send(topic, key='ping'+str(i), value='1234'+str(i)).add_callback(on_send_success).add_errback(on_send_error)

producer.flush()
