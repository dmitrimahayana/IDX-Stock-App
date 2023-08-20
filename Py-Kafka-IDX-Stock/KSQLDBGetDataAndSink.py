from ksqldb import KSQLdbClient
from Config import ksqlConfig, mongoConfig
from datetime import datetime
import asyncio
import json
from motor.motor_asyncio import AsyncIOMotorClient
from pymongo.server_api import ServerApi

stockQueryID = ""
companyQueryID = ""
joinStockCompQueryID = ""


async def buildJsonValue(colNames, Rows):
    colNames = colNames
    if len(colNames) == len(Rows):
        global finalJson
        for i in range(len(Rows)):
            value = Rows[i]
            if 'str' in str(type(Rows[i])):
                value = '"' + str(Rows[i]) + '"'
            if colNames[i].lower() != 'windowstart' and colNames[i].lower() != 'windowend':  # Skip this column
                if i == 0:
                    finalJson = '{"' + colNames[i].lower() + '":' + str(value) + ''
                elif i == len(Rows) - 1:
                    finalJson = finalJson + ',"' + colNames[i].lower() + '":' + str(value) + '}'
                else:
                    finalJson = finalJson + ',"' + colNames[i].lower() + '":' + str(value)
    else:
        raise Exception("Total Column " + str(len(colNames)) + "is not match with Total Value " + str(len(Rows)))
    # print(finalJson)
    return finalJson


async def queryAsyncStock():
    global stockQueryID
    query = ksqlClient.query_async("select * from KSQLTABLEGROUPSTOCK emit changes;",
                                   stream_properties={"ksql.streams.auto.offset.reset": ksqlConfig['mode']},
                                   timeout=None)
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
            jsonObject = json.loads(jsonValue)
            collectionName = 'ksql-stock-stream'
            query = {'id': row[0]}
            dateTime = datetime.today().strftime('%Y-%m-%d %H:%M:%S')
            mongoResult = await mongoDB[collectionName].find_one(query)
            # print(mongoResult)
            if mongoResult is not None:
                updateResult = await mongoDB[collectionName].replace_one(query, jsonObject)
                print(dateTime + " Existing collection " + collectionName + " documentID " + str(
                    row[0]) + " modified document count: " + str(updateResult.modified_count))
            else:
                insertResult = await mongoDB[collectionName].insert_one(jsonObject)
                print(dateTime + " Inserted collection " + collectionName + " documentID " + str(
                    row[0]) + " with the following mongoID: " + str(insertResult.inserted_id))


async def queryAsyncCompany():
    global companyQueryID
    query = ksqlClient.query_async("select * from KSQLTABLEGROUPCOMPANY emit changes;",
                                   stream_properties={"ksql.streams.auto.offset.reset": ksqlConfig['mode']},
                                   timeout=None)
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
            jsonObject = json.loads(jsonValue)
            collectionName = 'ksql-company-stream'
            query = {'id': row[0]}
            dateTime = datetime.today().strftime('%Y-%m-%d %H:%M:%S')
            mongoResult = await mongoDB[collectionName].find_one(query)
            # print(mongoResult)
            if mongoResult is not None:
                updateResult = await mongoDB[collectionName].replace_one(query, jsonObject)
                print(dateTime + " Existing collection " + collectionName + " documentID " + str(
                    row[0]) + " modified document count: " + str(updateResult.modified_count))
            else:
                insertResult = await mongoDB[collectionName].insert_one(jsonObject)
                print(dateTime + " Inserted collection " + collectionName + " documentID " + str(
                    row[0]) + " with the following mongoID: " + str(insertResult.inserted_id))


async def queryAsyncJoinStockCompany():
    global joinStockCompQueryID
    query = ksqlClient.query_async("select * from KSQLTABLEJOINSTOCKCOMPANY emit changes;",
                                   stream_properties={"ksql.streams.auto.offset.reset": ksqlConfig['mode']},
                                   timeout=None)
    counter = 0
    global colNamesJoinStockComp
    async for row in query:
        counter = counter + 1
        if counter == 1:
            joinStockCompQueryID = row['queryId']
            colNamesJoinStockComp = row['columnNames']
            print("Company Column Names: ", colNamesJoinStockComp)
            print("Query ID: ", joinStockCompQueryID)
        else:
            jsonValue = await buildJsonValue(colNamesJoinStockComp, row)
            jsonObject = json.loads(jsonValue)
            collectionName = 'ksql-join-stock-company'
            query = {'id': row[0]}
            dateTime = datetime.today().strftime('%Y-%m-%d %H:%M:%S')
            mongoResult = await mongoDB[collectionName].find_one(query)
            # print(mongoResult)
            if mongoResult is not None:
                updateResult = await mongoDB[collectionName].replace_one(query, jsonObject)
                print(dateTime + " Existing collection " + collectionName + " documentID " + str(
                    row[0]) + " modified document count: " + str(updateResult.modified_count))
            else:
                insertResult = await mongoDB[collectionName].insert_one(jsonObject)
                print(dateTime + " Inserted collection " + collectionName + " documentID " + str(
                    row[0]) + " with the following mongoID: " + str(insertResult.inserted_id))


def shutDown():
    print("Shutdown Query Stock ID: ", stockQueryID)
    ksqlClient.close_query(stockQueryID)
    print("Shutdown Query Company ID: ", companyQueryID)
    ksqlClient.close_query(companyQueryID)
    print("Shutdown Query Join Stock Company ID: ", joinStockCompQueryID)
    ksqlClient.close_query(joinStockCompQueryID)


if __name__ == '__main__':
    ksqlClient = KSQLdbClient(ksqlConfig['url'])
    mongoClient = AsyncIOMotorClient(mongoConfig['url'], server_api=ServerApi('1'))
    mongoDB = mongoClient["kafka"]  # Get Database Name
    loop = asyncio.get_event_loop()
    try:
        loop.create_task(queryAsyncStock())
        loop.create_task(queryAsyncCompany())
        loop.create_task(queryAsyncJoinStockCompany())
        loop.run_forever()
    except KeyboardInterrupt:
        print("Shutdown Starting...")
        shutDown()
