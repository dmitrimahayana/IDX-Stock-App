const express = require('express');
const app = express();
const port = 9000;
const { MongoClient } = require('mongodb');
var cors = require("cors");
var CronJob = require('cron').CronJob;
const { spawn } = require('node:child_process');

var PredictStockJarPath = 'D:/00 Project/00 My Project/Run Scala_IDX_Stock_MongoDB.bat';

app.use(express.json());
app.use(cors());

var ScalaSparkPredictionJob = new CronJob(
    '2 * * * * *', //every 2 mins for testing purposes but actually we can set it up every x mins
    function () {
        let dateTimeNow = getCurrentDateTime();
        console.log('You will see this message every 2 mins ' + dateTimeNow);
        callJarScalaSparkPrediction();
    },
);
// ScalaSparkPredictionJob.start()

function callJarScalaSparkPrediction() {
    const bat = spawn('cmd.exe', ['/c', PredictStockJarPath]);
    bat.stdout.on('data', (data) => {
        console.log(data.toString());
    });
    bat.stderr.on('data', (data) => {
        console.error(data.toString());
    });
    bat.on('exit', (code) => {
        console.log(`Child exited with code ${code}`);
    });
}

function getCurrentDateTime() {
    let date_ob = new Date();
    let date = ("0" + date_ob.getDate()).slice(-2);
    let month = ("0" + (date_ob.getMonth() + 1)).slice(-2);
    let year = date_ob.getFullYear();
    let hours = date_ob.getHours();
    let minutes = date_ob.getMinutes();
    let seconds = date_ob.getSeconds();
    let dateTimeNow = year + "-" + month + "-" + date + " " + hours + ":" + minutes + ":" + seconds;
    return dateTimeNow;
}

function getCurrentDate() {
    let date_ob = new Date();
    let date = ("0" + date_ob.getDate()).slice(-2);
    let month = ("0" + (date_ob.getMonth() + 1)).slice(-2);
    let year = date_ob.getFullYear();
    let hours = date_ob.getHours();
    let minutes = date_ob.getMinutes();
    let seconds = date_ob.getSeconds();
    let dateTimeNow = year + "-" + month + "-" + date;
    return dateTimeNow;
}

function getYesterdayDate() {
    let date_ob = new Date();
    date_ob.setDate(date_ob.getDate() - 1); // Yesterday!
    let date = ("0" + date_ob.getDate()).slice(-2);
    let month = ("0" + (date_ob.getMonth() + 1)).slice(-2);
    let year = date_ob.getFullYear();
    let hours = date_ob.getHours();
    let minutes = date_ob.getMinutes();
    let seconds = date_ob.getSeconds();
    let dateTimeNow = year + "-" + month + "-" + date;
    return dateTimeNow;
}

async function connectToDB() {
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

// URL: http://localhost:9000/allCompany
app.get('/allCompany', async (req, res) => {
    console.log("All Company Query")
    let db = await connectToDB();
    let collection = db.collection("company-stream");
    let results = await collection.find({}).toArray();

    res.send(results).status(200);
});

// URL: http://localhost:9000/company
app.get('/company/:ticker', async (req, res) => {
    let ticker = req.params.ticker.toUpperCase()
    console.log("Company Query Ticker: " + ticker)
    let db = await connectToDB();
    let collection = db.collection("company-stream");
    let results = await collection.find({ ticker: ticker }).toArray();

    res.send(results).status(200);
});

// URL: http://localhost:9000/allStock
app.get('/allStock', async (req, res) => {
    let dateNow = getCurrentDate();
    let dateYesterday = getYesterdayDate();
    console.log("All Stock Query Date: " + dateNow)
    let db = await connectToDB();
    let collectionNow = db.collection("stock-stream");
    let resultsNow = await collectionNow.find({ date: dateNow }).toArray();
    let resultsYesterday = await collectionNow.find({ date: dateYesterday }).toArray();

    let collectionComp = db.collection("company-stream");
    let resultsComp = await collectionComp.find({}).toArray();

    console.log(resultsNow.length+" "+resultsYesterday.length+" "+resultsComp.length)
    //Join the data
    resultsNow.forEach(row1 => {
        let change;
        let changeval;
        let changepercent;
        let name;
        let logo;
        resultsYesterday.forEach(row2 => {
            if (row1.ticker == row2.ticker) {
                change = row1.close - row2.close;
                changepercent = (change / row2.close * 100);
                if (change > 0) {
                    changeval = "+" + change.toFixed(2).toString();
                    changepercent = "+" + changepercent.toFixed(2).toString() + "%";
                } else if (change < 0) {
                    changeval = change.toFixed(2).toString();
                    changepercent = changepercent.toFixed(2).toString() + "%";
                } else {
                    changeval = change.toFixed(2).toString();
                    changepercent = changepercent.toString() + "%";
                }

            }
        })
        resultsComp.forEach(row3 => {
            if (row1.ticker == row3.ticker) {
                name = row3.name;
                logo = row3.logo;
            }
        })
        row1.change = change;
        row1.changeval = changeval;
        row1.changepercent = changepercent;
        row1.name = name;
        row1.logo = logo;
        // console.log(row1)
    })

    res.send(resultsNow).status(200);
});

// URL: http://localhost:9000/allStock
app.get('/history/:ticker', async (req, res) => {
    let ticker = req.params.ticker.toUpperCase()
    console.log("History Stock Query Ticker: " + ticker)
    let db = await connectToDB();
    let collection = db.collection("stock-stream");
    let results = await collection.find({ ticker: ticker }).toArray();

    res.send(results).status(200);
});

// URL: http://localhost:9000/stock/{ticker}/{date}
app.get('/stock/:ticker/:date', async (req, res) => {
    let dateNow = req.params.date.toString();
    let ticker = req.params.ticker.toUpperCase()
    console.log("Stock Query Ticker: " + ticker + " Date: " + dateNow)
    let db = await connectToDB();
    let collection = db.collection("stock-stream");
    let results = await collection.find({ date: dateNow, ticker: ticker }).toArray();

    res.send(results).status(200);
});

// URL: http://localhost:9000/predictAllStockNow
app.get('/predictAllStockNow', async (req, res) => {
    let dateNow = getCurrentDate();
    console.log("Prediction All Stock Query Date: " + dateNow)
    let db = await connectToDB();
    let collectionPredictNow = db.collection("predict-stock");
    let resultsPredictNow = await collectionPredictNow.find({ date: dateNow }).toArray();

    let collectionStockNow = db.collection("stock-stream");
    let resultsStockNow = await collectionStockNow.find({ date: dateNow }).toArray();
    if (resultsPredictNow.length == 0 && resultsStockNow.length > 0) {
        const bat = spawn('cmd.exe', ['/c', PredictStockJarPath]);
        bat.stdout.on('data', (data) => {
            console.log(data.toString());
        });
        bat.stderr.on('data', (data) => {
            console.error(data.toString());
        });
        bat.on('exit', (code) => {
            res.redirect("/predictAllStockNow")
        });
    } else if (resultsPredictNow.length > 0) {
        res.send(resultsPredictNow).status(200);
    }
});

// URL: http://localhost:9000/predictStock/{ticker}
app.get('/predictStock/:ticker', async (req, res) => {
    let ticker = req.params.ticker.toUpperCase()
    let dateNow = getCurrentDate();
    console.log("Prediction Stock Stock Query Ticker: " + ticker + " Date: " + dateNow)
    let db = await connectToDB();
    let collectionPredictNow = db.collection("predict-stock");
    let resultsPredictNow = await collectionPredictNow.find({ date: dateNow, ticker: ticker }).toArray();

    let collectionStockNow = db.collection("stock-stream");
    let resultsStockNow = await collectionStockNow.find({ date: dateNow, ticker: ticker }).toArray();
    if (resultsPredictNow.length == 0 && resultsStockNow.length > 0) {
        const bat = spawn('cmd.exe', ['/c', PredictStockJarPath]);
        bat.stdout.on('data', (data) => {
            console.log(data.toString());
        });
        bat.stderr.on('data', (data) => {
            console.error(data.toString());
        });
        bat.on('exit', (code) => {
            res.redirect("/predictStock/" + ticker)
        });
    } else if (resultsPredictNow.length > 0) {
        res.send(resultsPredictNow).status(200);
    }
});

// URL: http://localhost:9000/executePredictStock/{ticker}
app.get('/executePredictStock/:ticker', async (req, res) => {
    let ticker = req.params.ticker.toUpperCase()
    const bat = spawn('cmd.exe', ['/c', PredictStockJarPath]);
    bat.stdout.on('data', (data) => {
        console.log(data.toString());
    });
    bat.stderr.on('data', (data) => {
        console.error(data.toString());
    });
    bat.on('exit', (code) => {
        res.redirect("/predictStock/" + ticker)
    });
});

app.listen(port, () => {
    console.log(`Server is running on http://localhost:${port}`);
});