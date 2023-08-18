from confluent_kafka import Consumer
from confluent_kafka.serialization import SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from Config import config, srConfig
from Module.ClassStock import dictToStock
from Module.ClassCompany import dictToCompany
import time


def set_consumer_configs():
    config['group.id'] = 'my-py-stock-consumer'
    config['auto.offset.reset'] = 'earliest'

if __name__ == '__main__':
    topic1 = 'streaming.goapi.idx.stock.json'
    topic2 = 'streaming.goapi.idx.companies.json'

    schema_registry_client = SchemaRegistryClient(srConfig)
    stockSchema = schema_registry_client.get_latest_version("IDX-Stock")
    stockAvroSerializer = AvroDeserializer(schema_registry_client,
                                           stockSchema.schema.schema_str,
                                           dictToStock)
    companySchema = schema_registry_client.get_latest_version("IDX-Company")
    companyAvroSerializer = AvroDeserializer(schema_registry_client,
                                           companySchema.schema.schema_str,
                                           dictToCompany)

    set_consumer_configs()
    consumer = Consumer(config)
    consumer.subscribe([topic1, topic2])

    while True:
        try:
            event = consumer.poll(1.0)

            if event is None:
                continue
            else:
                if event.topic() == topic1:
                    stock = stockAvroSerializer(event.value(), SerializationContext(topic1, MessageField.VALUE))
                    if stock is not None:
                        print(stock)
                elif event.topic() == topic2:
                    company = companyAvroSerializer(event.value(), SerializationContext(topic2, MessageField.VALUE))
                    if company is not None:
                        print(company)

        except KeyboardInterrupt:
            print("Shutdown Starting...")
            break

    consumer.close()