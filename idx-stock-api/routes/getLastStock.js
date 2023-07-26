
// import db from "./conn.js";
// import * from "mongoose";

var express = require('express');
var router = express.Router();

/* testAPI segment. */
router.get('/', function (req, res, next) {
    async function connectDB() {
        const connectionString = "mongodb://localhost:27017";
        const client = new MongoClient(connectionString);
        let conn;
        try {
            conn = await client.connect();
            console.error(conn);
        } catch (e) {
            console.error(e);
        }
    
        let db = conn.db("kafka");
    }
    connectDB();
    res.send('Express API is working properly...');
    // async function main() {
    //     // we'll add code here soon
    //     const mongoString = "mongodb://localhost:27017";
    //     const client = new MongoClient(uri);
    //     try {
    //         await client.connect();
    //         await listDatabases(client);
    //     } catch (e) {
    //         console.error(e);
    //     } finally {
    //         await client.close();
    //     }
    // }

    // main().catch(console.error);

    // mongoose.connect(mongoString);
    // const database = mongoose.connection;
    // database.on('error', (error) => {
    //     console.log(error)
    // });

    // database.once('connected', () => {
    //     console.log('Database Connected');
    // });
});

module.exports = router;