from confluent_kafka import Producer
from Config import config

bootStrapServer = 'localhost:39092,localhost:39093,localhost:39094'
topic = 'Dummy_Python'

def acked(err, msg):
    if err is not None:
        print("Failed to deliver message: %s: %s" % (str(msg), str(err)))
    else:
        print("Topic:%s Partition:%d Timestamp%s" % (msg.topic(), msg.partition(), msg.timestamp()[1]))

producer = Producer(config)

# produce asynchronously
for i in range(10):
    producer.produce(topic, key='ping' + str(i), value='1234' + str(i), callback=acked)
producer.flush()
