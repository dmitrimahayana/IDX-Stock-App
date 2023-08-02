from pymongo import MongoClient

def getCollection(url, database, collection):
    print("URL: "+url+" Database: "+database+"Collection: "+collection)
    client = MongoClient(url)
    database = client[database]
    collection = database[collection]
    print("Connected to MongoDB...")
    return collection

def insertOrUpdateCollection(newCollectionName, df):
    print("Start Inserting to MongoDB...")
    # url = "mongodb://host.docker.internal:27017/"
    url = "mongodb://mongodb-server:27017/" #Docker mongodb
    database = "kafka"
    myCollection = getCollection(url, database, newCollectionName)
    arrayJSON = df.to_dict('records')
    for index, row in df.iterrows():
        query = {'id': row["id"]}
        item_details = myCollection.count_documents(query)
        if(item_details == 0):
            insertResult = myCollection.insert_one(arrayJSON[index])
            print("Insert Result ID: "+str(insertResult.inserted_id)+" id: "+row["id"]+" changeval:"+row["changeval"])
        else:
            updateResult = myCollection.update_one(query, {"$set": arrayJSON[index]})
            print("Matched count: "+str(updateResult.matched_count)+" modified count: "+str(updateResult.modified_count)+" id: "+row["id"]+" changeval:"+row["changeval"])