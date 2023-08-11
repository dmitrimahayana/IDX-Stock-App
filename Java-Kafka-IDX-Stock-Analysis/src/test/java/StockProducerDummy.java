import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.id.stock.analysis.Module.IdxStock;
import io.id.stock.analysis.Module.IdxCompany;
import io.id.stock.analysis.Module.KafkaStockProducer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public class StockProducerDummy {

    private static final Type REVIEW_TYPE1 = new TypeToken<List<IdxStock>>() {}.getType();

    private static final Type REVIEW_TYPE2 = new TypeToken<List<IdxCompany>>() {}.getType();

    final static Schema createSchemaFromFile(String filepath) {
        String avroSchema = null;
        try {
            avroSchema = new String(Files.readAllBytes(Paths.get(filepath)));
            Schema schema = new Schema.Parser().parse(avroSchema);
            return schema;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        //Create Kafka Producer
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        KafkaStockProducer producerObj = new KafkaStockProducer(true);

        String filename1 = "kafka.stock-stream 2023-07-28.json";
        String filename2 = "kafka.company-stream.json";
        try{
            String SCHEMA_STOCK_PATH = "avro-stock.avsc";
            String SCHEMA_COMPANY_PATH = "avro-company.avsc";
            Schema schemaStock = createSchemaFromFile(SCHEMA_STOCK_PATH);
            Schema schemaCompany = createSchemaFromFile(SCHEMA_COMPANY_PATH);

            //Crete Kafka Connection
            producerObj.createProducerConn();

            //get a reference to the main thread
            final Thread mainThread = Thread.currentThread();
            //adding the shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(){
                public void run(){
                    //Close Producer
                    producerObj.flushAndCloseProducer();
                    //join the main thread to allow the execution of the code in the main thread
                    try {
                        mainThread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });

            Gson gson = new Gson();
            JsonReader reader1 = new JsonReader(new FileReader(filename1));
            List<IdxStock> dataLastStock = gson.fromJson(reader1, REVIEW_TYPE1);

            JsonReader reader2 = new JsonReader(new FileReader(filename2));
            List<IdxCompany> dataCompany = gson.fromJson(reader2, REVIEW_TYPE2);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String dateNow = LocalDate.now().format(formatter);

            Double rangeMin = 500.0;
            Double rangeMax = 5000.0;
            Random r = new Random();
            DecimalFormat f = new DecimalFormat("##.00");

            int Counter = 0;
            while(Counter <= 1){
                //Counter
                for (IdxStock row1 : dataLastStock) {
                    for (IdxCompany row2 : dataCompany) {
                        if (row1.ticker.equals(row2.ticker)) {
                            Counter++;
//                            String date = "2023-08-04";
//                            String date = row1.date;
                            String date = dateNow;
                            String ticker = row1.ticker;
                            String id = ticker + "_" + date;
                            Double open = row1.open;
                            Double high = row1.high;
                            Double low = row1.low;
                            Double close = row1.close;
                            Long volume = Long.parseLong(String.valueOf(row1.volume));

                            double randomValue1 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue2 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue3 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue4 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            open = randomValue1; //For Testing aggregation purpose
                            high = randomValue2; //For Testing aggregation purpose
                            low = randomValue3; //For Testing aggregation purpose
                            close = randomValue4; //For Testing aggregation purpose
                            System.out.println("Counter: " + Counter + " ticker: " + ticker + " date: " + date + " open: " + open + " high: " + high + " low: " + low + " close: " + close + " volume: " + volume);

//                            IdxStock stock = new IdxStock(id, ticker, date, Double.valueOf(open), Double.valueOf(high), Double.valueOf(low), Double.valueOf(close), new BigInteger(volume));
//                            String jsonStock = new Gson().toJson(stock);
//                            //Send Stock Producer
//                            producerObj.startProducer(topic1, id, jsonStock);

                            GenericRecord recordStock = new GenericData.Record(schemaStock);
                            recordStock.put("id", id);
                            recordStock.put("ticker", ticker);
                            recordStock.put("date", date);
                            recordStock.put("open", open);
                            recordStock.put("high", high);
                            recordStock.put("low", low);
                            recordStock.put("close", close);
                            recordStock.put("volume", volume);
                            //Send Avro Stock to Producer
                            producerObj.startProducer(topic1, id, recordStock);

                            String compTicker = row2.ticker;
                            String compName = row2.name;
                            String compLogo = row2.logo;
                            System.out.println("Counter: " + Counter + " name: " + compName + " logo: " + compLogo);

//                            IdxCompany company = new IdxCompany(compTicker, compTicker, compName, compLogo);
//                            String jsonCompany = new Gson().toJson(company);
//                            //Send Company Producer
//                            producerObj.startProducer(topic2, id, jsonCompany);

                            //Avro Company Serializer
                            GenericRecord recordCompany = new GenericData.Record(schemaCompany);
                            recordCompany.put("id", compTicker);
                            recordCompany.put("ticker", compTicker);
                            recordCompany.put("name", compName);
                            recordCompany.put("logo", compLogo);
                            //Send Avro Company to Producer
                            producerObj.startProducer(topic2, id, recordCompany);
                        }
                    }
                }
                Thread.sleep(5000);
            }

//            //Close Producer
//            producerObj.flushAndCloseProducer();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            System.out.println("Error producer: "+e);
        } finally {
            try{
                Thread.sleep(10000);
            } catch (InterruptedException error){
                error.printStackTrace();
            }
        }
    }
}