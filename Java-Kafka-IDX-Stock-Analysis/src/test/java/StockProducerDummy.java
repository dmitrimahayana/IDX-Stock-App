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
import java.util.List;

public class StockProducerDummy {

    private static final Type REVIEW_TYPE1 = new TypeToken<List<IdxStock>>() {
    }.getType();

    private static final Type REVIEW_TYPE2 = new TypeToken<List<IdxCompany>>() {
    }.getType();

    public static void main(String[] args) {
        //Create Kafka Producer
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        KafkaStockProducer producer = new KafkaStockProducer(true);

        String filename1 = "kafka.stock-stream 2023-07-26.json";
        String filename2 = "kafka.company-stream.json";
        try{
            //Crete Kafka Connection
            producer.createProducerConn();

            Gson gson = new Gson();
            JsonReader reader1 = new JsonReader(new FileReader(filename1));
            List<IdxStock> dataLastStock = gson.fromJson(reader1, REVIEW_TYPE1);

            JsonReader reader2 = new JsonReader(new FileReader(filename2));
            List<IdxCompany> dataCompany = gson.fromJson(reader2, REVIEW_TYPE2);

            //Counter
            int Counter = 0;
            for (IdxStock row1 : dataLastStock) {
                for (IdxCompany row2 : dataCompany) {
                    if (row1.ticker.equals(row2.ticker)) {
                        Counter++;
                        String id = row1.id;
                        String ticker = row1.ticker;
                        String date = row1.date;
                        String open = String.valueOf(row1.open);
                        String high = String.valueOf(row1.high);
                        String low = String.valueOf(row1.low);
                        String close = String.valueOf(row1.close);
                        String volume = String.valueOf(row1.volume);
//                        open = "1111"; //For Testing aggregation purpose
//                        high = "2222"; //For Testing aggregation purpose
//                        low = "3333"; //For Testing aggregation purpose
//                        close = "4444"; //For Testing aggregation purpose
//                        volume = "5555"; //For Testing aggregation purpose

                        System.out.println("Counter: " + Counter);
                        System.out.println("ticker: " + ticker);
                        System.out.println("date: " + date);
                        System.out.println("open: " + open);
                        System.out.println("high: " + high);
                        System.out.println("low: " + low);
                        System.out.println("close: " + close);
                        System.out.println("volume: " + volume);

                        IdxStock stock = new IdxStock(id, ticker, date, Double.valueOf(open), Double.valueOf(high), Double.valueOf(low), Double.valueOf(close), new BigInteger(volume));
                        String jsonStock = new Gson().toJson(stock);
                        //Send Stock Producer
                        producer.startProducer(topic1, id, jsonStock);

                        String compTicker = row2.ticker;
                        String compName = row2.name;
                        String compLogo = row2.logo;
                        System.out.println("name: " + compName);
                        System.out.println("logo: " + compLogo);
                        System.out.println(" ");
                        System.out.println("-----------------------------");
                        IdxCompany company = new IdxCompany(compTicker, compTicker, compName, compLogo);
                        String jsonCompany = new Gson().toJson(company);
                        //Send Company Producer
                        producer.startProducer(topic2, id, jsonCompany);
                    }
                }
            }

            //Close Producer
            producer.flushAndCloseProducer();
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