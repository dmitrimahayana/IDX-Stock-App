from ksqldb import KSQLdbClient
from Config import config, srConfig, rootDirectory, ksqlConfig
import asyncio, signal, logging

stockQueryID = ""
companyQueryID = ""


async def buildJsonValue(colNames, Rows):
    if len(colNames) == len(Rows):
        global finalJson
        for i in range(len(Rows)):
            if i == 0:
                finalJson = "{" + colNames[i] + ":" + str(Rows[i])
            elif i == len(Rows) - 1:
                finalJson = finalJson + "," + colNames[i] + ":" + str(Rows[i]) + "}"
            else:
                finalJson = finalJson + "," + colNames[i] + ":" + str(Rows[i])
    else:
        raise Exception("Total Column " + str(len(colNames)) + "is not match with Total Value " + str(len(Rows)))
    return finalJson


async def queryAsyncStock():
    global stockQueryID
    query = client.query_async("select * from ksqlgroupstock emit changes;",
                               stream_properties={"ksql.streams.auto.offset.reset": "earliest"}, timeout=None)
    counter = 0
    global colNamesStock
    async for row in query:
        counter = counter + 1
        if counter == 1:
            stockQueryID = row['queryId']
            colNamesStock = row['columnNames']
            print("Stock Column Names: ", colNamesStock)
            print("Query ID: ", stockQueryID)
        else:
            jsonValue = await buildJsonValue(colNamesStock, row)
            print(jsonValue)
            # print(f"Row ID:{row[0]} StockID: {row[1]} Ticker: {row[2]} Date: {row[3]} "
            #       f"Open: {row[4]} High: {row[5]} Low: {row[6]} Close: {row[7]} Volume: {row[8]}")


async def queryAsyncCompany():
    global companyQueryID
    query = client.query_async("select * from ksqlgroupcompany emit changes;",
                               stream_properties={"ksql.streams.auto.offset.reset": "earliest"}, timeout=None)
    counter = 0
    global colNamesCompany
    async for row in query:
        counter = counter + 1
        if counter == 1:
            companyQueryID = row['queryId']
            colNamesCompany = row['columnNames']
            print("Company Column Names: ", colNamesCompany)
            print("Query ID: ", companyQueryID)
        else:
            jsonValue = await buildJsonValue(colNamesCompany, row)
            print(jsonValue)
            # print(f"Row ID:{row[0]} CompanyID: {row[1]} Ticker: {row[2]} Name: {row[3]} Logo: {row[4]}")


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
