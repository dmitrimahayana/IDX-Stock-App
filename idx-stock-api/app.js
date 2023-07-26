const express = require('express');
const app = express();
const port = 9000;
const { MongoClient } = require('mongodb');
var cors = require("cors");
// var { connectToCluster } = require('./conn.js')

async function connectToCluster() {
    const connectionString = "mongodb://localhost:27017";
  
    const client = new MongoClient(connectionString);
  
    let conn;
    try {
        conn = await client.connect();
    } catch (e) {
        console.error(e);
    }
  
    let db = conn.db("kafka");
    return await db;
  }

app.use(express.json());
app.use(cors());

// Route: http://localhost:9000/allStock
app.get('/allStock', async (req, res) => {
    let db = await connectToCluster();
    let collection = db.collection("join-stock-company-stream");
    let results = await collection.find({date: "2023-07-26"})
        // .limit(5)
        .toArray();

    res.send(results).status(200);
});

// Route: http://localhost:9000/yesterdayStock
app.get('/yesterdayStock', async (req, res) => {
    let db = await connectToCluster();
    let collection = db.collection("join-stock-company-stream");
    let results = await collection.find({date: "2023-07-24"})
        // .limit(5)
        .toArray();

    res.send(results).status(200);
});

app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
});