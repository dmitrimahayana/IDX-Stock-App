from pymongo import MongoClient
from sqlalchemy import create_engine, text
import psycopg2
import numpy as np
import psycopg2.extras as extras
from moduleIDX.Config import mongodb_config, postgres_config


def getCollection(url, database, collection):
    print("URL: " + url + " Database: " + database + "Collection: " + collection)
    client = MongoClient(url)
    database = client[database]
    collection = database[collection]
    print("Connected to MongoDB...")
    return collection


def insertOrUpdateCollection(newCollectionName, df):
    print("Start Inserting to MongoDB...")
    url = f"mongodb://{mongodb_config['hostname']}:{mongodb_config['port']}/"
    database = mongodb_config['database']
    myCollection = getCollection(url, database, newCollectionName)
    arrayJSON = df.to_dict('records')
    for index, row in df.iterrows():
        query = {'id': row["id"]}
        item_details = myCollection.count_documents(query)
        if (item_details == 0):
            insertResult = myCollection.insert_one(arrayJSON[index])
            print("Insert Result ID: " + str(insertResult.inserted_id) + " id: " + row["id"] + " changeval:" + row[
                "changeval"])
        else:
            updateResult = myCollection.update_one(query, {"$set": arrayJSON[index]})
            print("Matched count: " + str(updateResult.matched_count) + " modified count: " + str(
                updateResult.modified_count) + " id: " + row["id"] + " changeval:" + row["changeval"])


def postgres_add_data_cursor(df, table_name, mode):
    conn = psycopg2.connect(
        user=postgres_config["username"],
        password=postgres_config["password"],
        database=postgres_config["database"],
        host=postgres_config["hostname"],
        port=postgres_config["port"]
    )

    tuples = [tuple(x) for x in df.to_numpy()]

    cols = ','.join(list(df.columns))
    # SQL query to execute
    query = 'INSERT INTO "%s"."%s" (%s) VALUES %%s' % (postgres_config["schema"], table_name, cols)
    cursor = conn.cursor()
    try:
        extras.execute_values(cursor, query, tuples)
        conn.commit()
        cursor.close()
    except (Exception, psycopg2.DatabaseError) as error:
        print("Error: %s" % error)
        conn.rollback()
        cursor.close()
        return 1
    print("the dataframe is inserted to", table_name)


def postgres_add_data_sqlalchemy(df, table_name, mode):
    conn_string = f'postgresql://{postgres_config["username"]}:{postgres_config["password"]}@{postgres_config["hostname"]}:{postgres_config["port"]}/{postgres_config["database"]}'
    engine = create_engine(conn_string)

    try:
        print(f'Start append table {postgres_config["schema"]}.{table_name}')
        df.to_sql(name=table_name, con=engine, schema=postgres_config["schema"], if_exists=mode, index=False)
        print(f'End append table {postgres_config["schema"]}.{table_name}')

        if (table_name == "ksql-stock-stream"):
            with engine.connect() as conn:
                where_condition = "date = '2023-09-14'"
                print(conn.execute(text(
                    f'SELECT DISTINCT id, ticker, date FROM "{postgres_config["schema"]}"."{table_name}" WHERE {where_condition}')).fetchall())
    except Exception as e:
        print("Error: ", str(e))
