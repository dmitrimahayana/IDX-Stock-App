from Module.CallAPI import getAPIResults  # Custom Module
from Module.ClassStock import Stock, stockToDict  # Custom Module
from Module.ClassCompany import Company, companyToDict  # Custom Module
from Module.ConfluentKafkaAvroProducer import sendProducer  # Custom Module
from Config import config, srConfig  # Custom Module
from confluent_kafka import Producer
from confluent_kafka.serialization import StringSerializer
from confluent_kafka.schema_registry import SchemaRegistryClient
from confluent_kafka.schema_registry.avro import AvroSerializer
import datetime
import time
import urllib.parse

if __name__ == '__main__':
    topic1 = 'streaming.goapi.idx.stock.json'
    topic2 = 'streaming.goapi.idx.companies.json'
    schema1 = 'IDX-Stock'
    schema2 = 'IDX-Company'
    baseUrl = 'https://api.goapi.id/v1/stock/idx/'

    # Define Kafka Serializer and Schema
    string_serializer = StringSerializer('utf_8')
    schema_registry_client = SchemaRegistryClient(srConfig)
    stockSchema = schema_registry_client.get_latest_version("IDX-Stock")
    stockAvroSerializer = AvroSerializer(schema_registry_client,
                                         stockSchema.schema.schema_str,
                                         stockToDict)
    companySchema = schema_registry_client.get_latest_version("IDX-Company")
    companyAvroSerializer = AvroSerializer(schema_registry_client,
                                           companySchema.schema.schema_str,
                                           companyToDict)

    # Define Kafka Producer
    producer = Producer(config)

    # Query Company Trending
    print("Start extracting trending stock")
    apiUrl = baseUrl + 'trending'
    responseCode, listTrending = getAPIResults(apiUrl)
    print("End extracting trending stock")

    # URL List All Company
    print("Start extracting companies")
    apiUrl2 = baseUrl + 'companies'
    responseCode, listCompany = getAPIResults(apiUrl2)
    print("End extracting companies")

    # Encode the parameter values
    dateTimeFormat = '%Y-%m-%d'
    currentDate = datetime.datetime.now().strftime(dateTimeFormat)
    yesterdayDate = (datetime.datetime.now() - datetime.timedelta(1)).strftime(dateTimeFormat)
    # encodedParam1 = urllib.parse.quote_plus(yesterdayDate)
    encodedParam1 = urllib.parse.quote_plus('2023-08-01')
    encodedParam2 = urllib.parse.quote_plus(currentDate)

    try:
        Counter = 0
        while (True):
            for row in listTrending:
                Counter += 1
                emitent = row['ticker']
                change = row['change']
                percent = row['percent']
                print('Counter: ' + str(Counter) + ' emitent: ' + emitent + ' change: ' + change +
                      ' percent: ' + percent)

                # Query Historical Stock Price
                apiUrl3 = baseUrl + emitent + '/historical'
                apiUrl3 = apiUrl3 + '?from=' + encodedParam1 + '&to=' + encodedParam2
                responseCode, historicalPrices = getAPIResults(apiUrl3)
                print('Counter: ' + str(Counter) + ' API Historical URL: ' + apiUrl3 +
                      ' Response Code: ' + str(responseCode))
                if responseCode == 200:
                    if len(historicalPrices) > 0 and len(listCompany) > 0:
                        for row2 in historicalPrices:
                            ticker = str(row2['ticker'])
                            date = str(row2['date'])
                            id = ticker + '_' + date
                            open = float(row2['open'])
                            high = float(row2['high'])
                            low = float(row2['low'])
                            close = float(row2['close'])
                            volume = int(row2['volume'])
                            print('Counter: ' + str(Counter) + ' ticker: ' + ticker + ' date: ' + date + ' open: ' +
                                  str(open) + ' high: ' + str(high) + ' low: ' + str(low) + ' close: ' + str(close) +
                                  ' volume: ' + str(volume))

                            # Send Avro Producer
                            stock = Stock(id, ticker, date, open, high, low, close, volume)
                            sendProducer(topic1, stock, producer, stockAvroSerializer)

                            filteredCompany = list(filter(lambda x: x['ticker'].lower() == emitent.lower(), listCompany))
                            compTicker = str(filteredCompany[0]['ticker'])
                            compName = str(filteredCompany[0]['name'])
                            compLogo = str(filteredCompany[0]['logo'])
                            print('Counter: ' + str(Counter) + ' name: ' + compName + ' logo: ' + compLogo)

                            # Send Avro Producer
                            company = Company(ticker, ticker, compName, compLogo,
                                              str(datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')))
                            sendProducer(topic2, company, producer, companyAvroSerializer)

                    # Save and flush
                    producer.flush()

            time.sleep(5)  # Wait 5 seconds to see the output
    except KeyboardInterrupt:
        print("Press Ctrl+C to terminate while statement")
        pass
