from confluent_kafka import Producer
import socket

bootStrapServer = 'localhost:39092,localhost:39093,localhost:39094'
topic = 'Dummy_Python'
conf = {'bootstrap.servers': bootStrapServer,
        'client.id': socket.gethostname()}


def acked(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Topic:%s Partition:%d Timestamp%s" % (msg.topic(), msg.partition(), msg.timestamp()[1]))


producer = Producer(conf)

# produce asynchronously
for i in range(10):
    producer.produce(topic, key='ping' + str(i), value='1234' + str(i), callback=acked)
producer.flush()
