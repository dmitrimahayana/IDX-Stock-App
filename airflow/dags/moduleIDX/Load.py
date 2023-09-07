from pymongo import MongoClient
from sqlalchemy import create_engine
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


def add_data_sqlalchemy(df, table_name):
    conn_string = f'postgresql://{postgres_config["username"]}:{postgres_config["password"]}@{postgres_config["hostname"]}:{postgres_config["port"]}/{postgres_config["database"]}'
    engine = create_engine(conn_string)

    try:
        print(f'Start append table {postgres_config["schema"]}.{table_name}')
        df.to_sql(name=table_name, con=engine, schema=postgres_config["schema"], if_exists='replace', index=False)
        print(f'End append table {postgres_config["schema"]}.{table_name}')

        # with engine.connect() as conn:
        #     print(conn.execute(text(f'SELECT * FROM {postgres_config["schema"]}.{table_name}')).fetchall())
    except Exception as e:
        print("Error: ", str(e))