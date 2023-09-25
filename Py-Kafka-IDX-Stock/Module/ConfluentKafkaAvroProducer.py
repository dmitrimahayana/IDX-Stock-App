from confluent_kafka.serialization import StringSerializer, SerializationContext, MessageField

def deliveryReport(err, event):
    if err is not None:
        print(f"Error ID: {event.key().decode('utf8')}: {err}")
    # else:
    #     print(f"Stored ID: {event.key().decode('utf8')} to Topic: {event.topic()}")

def sendProducer(topic, object, producer, avroSerializer):
    producer.produce(topic=topic,
                     key=StringSerializer('utf_8')(str(object.id)),
                     value=avroSerializer(object, SerializationContext(topic, MessageField.VALUE)),
                     on_delivery=deliveryReport)