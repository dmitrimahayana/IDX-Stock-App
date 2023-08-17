from confluent_kafka import Consumer
from confluent_kafka.serialization import StringDeserializer, SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroDeserializer
from Config import config, srConfig

class Stock(object):
    def __init__(self, id, ticker, date, open, high, low, close, volume):
        self.id = id
        self.ticker = ticker
        self.date = date
        self.open = open
        self.high = high
        self.low = low
        self.close = close
        self.volume = volume


class Company(object):
    def __init__(self, id, ticker, name, logo):
        self.id = id
        self.ticker = ticker
        self.name = name
        self.logo = logo

def dictToStock(dict, ctx):
    return dict;
    # return Stock(dict["id"],
    #              dict["ticker"],
    #              dict["date"],
    #              dict["open"],
    #              dict["high"],
    #              dict["low"],
    #              dict["close"],
    #              dict["volume"])


def dictToCompany(Company, ctx):
    return dict;
    # return Company(dict["id"],
    #                dict["ticker"],
    #                dict["name"],
    #                dict["logo"])

def set_consumer_configs():
    config['group.id'] = 'my-py-stock-consumer'
    config['auto.offset.reset'] = 'earliest'

if __name__ == '__main__':
    topic1 = 'streaming.goapi.idx.stock.json'
    topic2 = 'streaming.goapi.idx.companies.json'

    schema_registry_client = SchemaRegistryClient(srConfig)
    mySchema2 = schema_registry_client.get_latest_version("IDX-Stock")
    print("Schema: " + mySchema2.schema.schema_str)
    avro_deserializer = AvroDeserializer(schema_registry_client, mySchema2.schema.schema_str, dictToStock)

    set_consumer_configs()
    consumer = Consumer(config)
    consumer.subscribe([topic1])

    while True:
        try:
            event = consumer.poll(1.0)
            if event is None:
                continue
            stock = avro_deserializer(event.value(), SerializationContext(topic1, MessageField.VALUE))
            if stock is not None:
                print(stock)
            #     print(f'Latest data in {stock.id} is {stock.open} {stock.close}.')

        except KeyboardInterrupt:
            break

    consumer.close()