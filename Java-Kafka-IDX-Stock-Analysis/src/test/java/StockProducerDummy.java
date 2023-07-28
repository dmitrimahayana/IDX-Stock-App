import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.id.stock.analysis.Module.IdxStock;
import io.id.stock.analysis.Module.IdxCompany;
import io.id.stock.analysis.Module.KafkaStockProducer;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

public class StockProducerDummy {

    private static final Type REVIEW_TYPE1 = new TypeToken<List<IdxStock>>() {
    }.getType();

    private static final Type REVIEW_TYPE2 = new TypeToken<List<IdxCompany>>() {
    }.getType();

    public static void main(String[] args) {
        //Create Kafka Producer
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        KafkaStockProducer producerObj = new KafkaStockProducer(true);

        String filename1 = "kafka.stock-stream 2023-07-28.json";
        String filename2 = "kafka.company-stream.json";
        try{
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

            while(true){
                //Counter
                int Counter = 0;
                for (IdxStock row1 : dataLastStock) {
                    for (IdxCompany row2 : dataCompany) {
                        if (row1.ticker.equals(row2.ticker)) {
                            Counter++;
                            String date = dateNow;
//                            String date = "2023-07-27";
                            String ticker = row1.ticker;
                            String id = ticker + "_" + date;
                            String open = String.valueOf(row1.open);
                            String high = String.valueOf(row1.high);
                            String low = String.valueOf(row1.low);
                            String close = String.valueOf(row1.close);
                            String volume = String.valueOf(row1.volume);

                            double randomValue1 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue2 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue3 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            double randomValue4 = row1.low + (row1.high - row1.low) * r.nextDouble(); //For Testing aggregation purpose
                            open = String.valueOf(f.format(randomValue1)); //For Testing aggregation purpose
                            high = String.valueOf(f.format(randomValue2)); //For Testing aggregation purpose
                            low = String.valueOf(f.format(randomValue3)); //For Testing aggregation purpose
                            close = String.valueOf(f.format(randomValue4)); //For Testing aggregation purpose

                            System.out.println("Counter: " + Counter + " ticker: " + ticker + " date: " + date + " open: " + open + " high: " + high + " low: " + low + " close: " + close + " volume: " + volume);
                            IdxStock stock = new IdxStock(id, ticker, date, Double.valueOf(open), Double.valueOf(high), Double.valueOf(low), Double.valueOf(close), new BigInteger(volume));
                            String jsonStock = new Gson().toJson(stock);
                            //Send Stock Producer
                            producerObj.startProducer(topic1, id, jsonStock);

                            String compTicker = row2.ticker;
                            String compName = row2.name;
                            String compLogo = row2.logo;
                            System.out.println("Counter: " + Counter + " name: " + compName + " logo: " + compLogo);
                            IdxCompany company = new IdxCompany(compTicker, compTicker, compName, compLogo);
                            String jsonCompany = new Gson().toJson(company);
                            //Send Company Producer
                            producerObj.startProducer(topic2, id, jsonCompany);
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