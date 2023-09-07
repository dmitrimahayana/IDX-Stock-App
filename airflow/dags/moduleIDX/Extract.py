import pandas as pd
from pymongo import MongoClient
from moduleIDX.Config import mongodb_config


def getCollection(url, database, collection):
    print("URL: " + url + " Database: " + database + "Collection: " + collection)
    client = MongoClient(url)
    database = client[database]
    collection = database[collection]
    print("Connected to MongoDB...")
    return collection


def extractKSQLStock(query):
    print("Start Extracting...")
    url = f"mongodb://{mongodb_config['hostname']}:{mongodb_config['port']}/"
    database = mongodb_config['database']
    collection = "ksql-stock-stream"
    myCollection = getCollection(url, database, collection)
    # Example: query = {"ticker": "GOTO"}
    item_details = myCollection.find(query)
    df = pd.DataFrame(list(item_details), columns=["id", "ticker", "date", "open", "high", "low", "close", "volume"])
    print("Total Stock Rows: " + str(df.shape[0]))
    print("End Extracting...")
    return df


def extractKSQLCompany(query):
    print("Start Extracting...")
    url = f"mongodb://{mongodb_config['hostname']}:{mongodb_config['port']}/"
    database = mongodb_config['database']
    collection = "ksql-company-stream"
    myCollection = getCollection(url, database, collection)
    # Example: query = {"ticker": "GOTO"}
    item_details = myCollection.find(query)
    df = pd.DataFrame(list(item_details), columns=["id", "ticker", "name", "logo"])
    print("Total Company Rows: " + str(df.shape[0]))
    print("End Extracting...")
    return df