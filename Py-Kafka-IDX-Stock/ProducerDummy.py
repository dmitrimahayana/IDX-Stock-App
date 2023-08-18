from confluent_kafka import Producer
from confluent_kafka.serialization import StringSerializer, SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
from Config import config, srConfig, rootDirectory
from Module.ClassStock import Stock, stockToDict
from Module.ClassCompany import Company, companyToDict
from datetime import datetime
import os, json, random, time

def deliveryReport(err, event):
    if err is not None:
        print(f"Error ID: {event.key().decode('utf8')}: {err}")
    else:
        print(f"Stored ID: {event.key().decode('utf8')} to Topic: {event.topic()}")

def sendProducer(topic, object, producer, avroSerializer):
    producer.produce(topic=topic,
                     key=StringSerializer('utf_8')(str(object.id)),
                     value=avroSerializer(object, SerializationContext(topic, MessageField.VALUE)),
                     on_delivery=deliveryReport)

if __name__ == '__main__':
    topic1 = 'streaming.goapi.idx.stock.json'
    topic2 = 'streaming.goapi.idx.companies.json'
    schema1 = 'IDX-Stock'
    schema2 = 'IDX-Company'
    jsonFile = 'kafka.ksql-join-stock-company 2023-07-28.json'

    string_serializer = StringSerializer('utf_8')
    schema_registry_client = SchemaRegistryClient(srConfig)

    stockSchema = schema_registry_client.get_latest_version('IDX-Stock')
    stockAvroSerializer = AvroSerializer(schema_registry_client,
                                         stockSchema.schema.schema_str,
                                         stockToDict)
    companySchema = schema_registry_client.get_latest_version('IDX-Company')
    companyAvroSerializer = AvroSerializer(schema_registry_client,
                                           companySchema.schema.schema_str,
                                           companyToDict)
    # Read Dummy Input
    jsonPath = os.path.join(rootDirectory, 'Resource', jsonFile)

    # Opening JSON file
    file = open(jsonPath)

    # returns JSON object as a dictionary
    jsonData = json.load(file)

    producer = Producer(config)
    while True:
        for row in jsonData:
            close = random.uniform(row['low'], row['high'])
            stock = Stock(row['id'], row['ticker'], row['date'], row['open'], row['high'], row['low'], close, row['volume'])
            company = Company(row['ticker'], row['ticker'], row['name'], row['logo'], str(datetime.today().strftime('%Y-%m-%d %H:%M:%S')))
            print(f"id:{row['id']} high:{row['high']} open:{row['open']} low:{row['low']} close:{close} name:{row['name']} logo:{row['logo']}")
            sendProducer(topic1, stock, producer, stockAvroSerializer)
            sendProducer(topic2, company, producer, companyAvroSerializer)
        time.sleep(5)

    producer.flush()
