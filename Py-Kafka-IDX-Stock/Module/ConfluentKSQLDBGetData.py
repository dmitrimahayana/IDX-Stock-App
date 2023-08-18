from ksqldb import KSQLdbClient
from Config import config, srConfig, rootDirectory, ksqlConfig
import asyncio, signal, logging

stockQueryID = ""
companyQueryID = ""


async def queryAsyncStock():
    global stockQueryID
    query = client.query_async("select * from ksqlgroupstock emit changes;",
                               stream_properties={"ksql.streams.auto.offset.reset": "earliest"}, timeout=None)
    counter = 0
    async for row in query:
        counter = counter + 1
        if counter == 1:
            stockQueryID = row['queryId']
            # print("Query ID: ", stockQueryID)
        else:
            print(
                f"Row ID:{row[0]} StockID: {row[1]} Ticker: {row[2]} Date: {row[3]} Open: {row[4]} High: {row[5]} Low: {row[6]} Close: {row[7]} Volume: {row[8]}")


async def queryAsyncCompany():
    global companyQueryID
    query = client.query_async("select * from ksqlgroupcompany emit changes;",
                               stream_properties={"ksql.streams.auto.offset.reset": "earliest"}, timeout=None)
    counter = 0
    async for row in query:
        counter = counter + 1
        if counter == 1:
            companyQueryID = row['queryId']
            # print("Query ID: ", companyQueryID)
        else:
            print(f"Row ID:{row[0]} CompanyID: {row[1]} Ticker: {row[2]} Name: {row[3]} Logo: {row[4]}")


def shutDown():
    print("Shutdown Query Stock ID: ", stockQueryID)
    print("Shutdown Company ID: ", companyQueryID)
    client.close_query(stockQueryID)
    client.close_query(companyQueryID)


if __name__ == '__main__':
    client = KSQLdbClient(ksqlConfig['url'])
    loop = asyncio.get_event_loop()
    try:
        loop.create_task(queryAsyncStock())
        loop.create_task(queryAsyncCompany())
        loop.run_forever()
    except KeyboardInterrupt:
        print("Shutdown Starting...")
        shutDown()
