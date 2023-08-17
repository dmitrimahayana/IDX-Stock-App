from confluent_kafka import Producer
from confluent_kafka.serialization import StringSerializer, SerializationContext, MessageField
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
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


def stockToDict(Stock, ctx):
    return {"id": Stock.id,
            "ticker": Stock.ticker,
            "date": Stock.date,
            "open": Stock.open,
            "high": Stock.high,
            "low": Stock.low,
            "close": Stock.close,
            "volume": Stock.volume}


def companyToDict(Company, ctx):
    return {"id": Company.id,
            "ticker": Company.ticker,
            "name": Company.name,
            "logo": Company.logo}


def deliveryReport(err, event):
    if err is not None:
        print(f'Delivery failed on reading for {event.key().decode("utf8")}: {err}')
    else:
        print(f'Reading for {event.key().decode("utf8")} produced to {event.topic()}')


if __name__ == '__main__':
    topic1 = 'streaming.goapi.idx.stock.json'
    topic2 = 'streaming.goapi.idx.companies.json'

    string_serializer = StringSerializer('utf_8')
    schema_registry_client = SchemaRegistryClient(srConfig)
    mySchema2 = schema_registry_client.get_latest_version("IDX-Stock")
    print("Schema: " + mySchema2.schema.schema_str)

    avro_serializer = AvroSerializer(schema_registry_client,
                                     mySchema2.schema.schema_str,
                                     stockToDict)

    stockData = [Stock('GOTO_2023-08-10', 'GOTO', '2023-08-10', 111, 116, 114, 114.5, 4581933000),
                 Stock('GOTO_2023-08-09', 'GOTO', '2023-08-09', 111, 115, 113, 113.4, 4581933000),
                 Stock('GOTO_2023-08-08', 'GOTO', '2023-08-08', 111, 114, 112, 112.3, 4581933000),
                 Stock('GOTO_2023-08-07', 'GOTO', '2023-08-07', 111, 113, 111, 111.2, 4581933000),
                 Stock('GOTO_2023-08-06', 'GOTO', '2023-08-06', 111, 112, 110, 110.1, 4581933000)]

    producer = Producer(config)
    for stock in stockData:
        producer.produce(topic=topic1,
                         key=string_serializer(str(stock.id)),
                         value=avro_serializer(stock, SerializationContext(topic1, MessageField.VALUE)),
                         on_delivery=deliveryReport)

    producer.flush()
