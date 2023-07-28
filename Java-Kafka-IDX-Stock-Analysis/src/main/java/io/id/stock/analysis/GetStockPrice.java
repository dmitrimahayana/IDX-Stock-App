package io.id.stock.analysis;

import com.google.gson.*;
import io.id.stock.analysis.Module.IdxStock;
import io.id.stock.analysis.Module.IdxCompany;
import io.id.stock.analysis.Module.KafkaStockProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class GetStockPrice {

    private static final Logger log = LoggerFactory.getLogger(GetStockPrice.class.getSimpleName());

    private static StringBuilder getAPIResponse(HttpURLConnection connection) throws IOException {
        // Read the response from the API
        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response;
    }

//    private static String CountCompany(String value) {
//        return JsonParser.parseString(value)
//            .getAsJsonObject()
//            .get("data")
//            .getAsJsonObject()
//            .get("count")
//            .getAsString();
//    }

    private static JsonArray getAPIResults(String apiUrl){
        try {
            // Create a URL object from the API URL
            URL url = new URL(apiUrl);

            // Open a connection to the API URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Set the HTTP method to GET
            connection.setRequestMethod("GET");

            // Set the Accept header to request JSON content
            connection.setRequestProperty("Accept", "*/*");

            // Set the X-API-KEY header
            connection.setRequestProperty("X-API-KEY", "pKg7UnAUzqKj8GMKQWu2R83e2N7Jno");

            // Set User Agent
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");

            // Get the response code
            int responseCode = connection.getResponseCode();

            if (responseCode == 200){
                //Get Response message
                String responseMessage = getAPIResponse(connection).toString();

                //Read Array Json
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(responseMessage, JsonObject.class);
                JsonArray resultsArray = jsonObject.getAsJsonObject("data").getAsJsonArray("results");

                // Close the connection
                connection.disconnect();

                return resultsArray;
            } else {
                log.info("ERROR with Response Code: " + responseCode+" URL: "+url);
                JsonArray emptyArray = new JsonArray();
                return emptyArray;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        //Create Kafka Producer
        String topic1 = "streaming.goapi.idx.stock.json";
        String topic2 = "streaming.goapi.idx.companies.json";
        KafkaStockProducer producer = new KafkaStockProducer(true);

        //Base API URL
        String baseUrl = "https://api.goapi.id/v1/stock/idx/";

        //Query Company Trending
        String apiUrl = baseUrl + "trending";
        JsonArray companyTrendResults = getAPIResults(apiUrl);

        //URL List All Company
        String apiUrl2 = baseUrl + "companies";
        JsonArray listCompany = getAPIResults(apiUrl2);

        try {
            //Crete Kafka Connection
            producer.createProducerConn();

//            //Only to save all company name and logo
//            for (JsonElement listCompanyElement : listCompany) {
//                //Get JSON Company
//                JsonObject listCompanyObject = listCompanyElement.getAsJsonObject();
//                String compTicker = listCompanyObject.get("ticker").getAsString();
//                String compName = listCompanyObject.get("name").getAsString();
//                String compLogo = listCompanyObject.get("logo").getAsString();
//                System.out.println("name: " + compName);
//                System.out.println("logo: " + compLogo);
//                System.out.println(" ");
//                IdxCompany company = new IdxCompany(compTicker, compTicker, compName, compLogo);
//                String jsonCompany = new Gson().toJson(company);
//                //Send Company Producer
//                producer.startProducer(topic2, compTicker, jsonCompany);
//            }

            //Counter
            int Counter = 0;

            // Encode the parameter values
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); //Date format
            String dateNow = LocalDate.now().format(formatter); //Date Now
            String dateYesterday = LocalDate.now().minusDays(1).format(formatter);  //yesterday
//            String encodedParam1 = URLEncoder.encode("2020-01-02", StandardCharsets.UTF_8.toString());
            String encodedParam1 = URLEncoder.encode(dateYesterday, StandardCharsets.UTF_8.toString());
//            String encodedParam2 = URLEncoder.encode("2020-01-02", StandardCharsets.UTF_8.toString());
            String encodedParam2 = URLEncoder.encode(dateNow, StandardCharsets.UTF_8.toString());

            for (JsonElement companyTrendElement : companyTrendResults) {
                Counter++;
                JsonObject companyTrendObject = companyTrendElement.getAsJsonObject();
                String emitent = companyTrendObject.get("ticker").getAsString();
                String change = companyTrendObject.get("change").getAsString();
                String percent = companyTrendObject.get("percent").getAsString();
                System.out.println("Counter: " + Counter + " emitent: " + emitent + " change: " + change + " percent: " + percent);

                //Query Historical Stock Price
                String apiUrl3 = baseUrl + emitent + "/historical";
                apiUrl3 = apiUrl3 + "?from=" + encodedParam1 + "&to=" + encodedParam2;
                log.info("API Historical URL: "+apiUrl3);
                JsonArray historicalPrices = getAPIResults(apiUrl3);
                if (historicalPrices.isJsonNull() != true && listCompany.isJsonNull() != true){
                    if (historicalPrices.size() > 0 && listCompany.size() > 0){
                        for (JsonElement historicalPriceElement : historicalPrices) {
                            //Get JSON Stock
                            JsonObject historicalPriceObject = historicalPriceElement.getAsJsonObject();
                            String ticker = historicalPriceObject.get("ticker").getAsString();
                            String date = historicalPriceObject.get("date").getAsString();
                            String open = historicalPriceObject.get("open").getAsString();
                            String high = historicalPriceObject.get("high").getAsString();
                            String low = historicalPriceObject.get("low").getAsString();
                            String close = historicalPriceObject.get("close").getAsString();
                            String volume = historicalPriceObject.get("volume").getAsString();
                            String id = ticker + "_" + date;
                            System.out.println("Counter: " + Counter + " ticker: " + ticker + " date: " + date + " open: " + open + " high: " + high + " low: " + low + " close: " + close + " volume: " + volume);

                            IdxStock stock = new IdxStock(id, ticker, date, Double.valueOf(open),Double.valueOf(high),Double.valueOf(low),Double.valueOf(close),new BigInteger(volume));
                            String jsonStock = new Gson().toJson(stock);
                            //Send Stock Producer
                            producer.startProducer(topic1, id, jsonStock);

                            for (JsonElement listCompanyElement : listCompany) {
                                //Get JSON Company
                                JsonObject listCompanyObject = listCompanyElement.getAsJsonObject();
                                String compTicker = listCompanyObject.get("ticker").getAsString();
                                String compName = listCompanyObject.get("name").getAsString();
                                String compLogo = listCompanyObject.get("logo").getAsString();
                                if(compTicker.equalsIgnoreCase(emitent)){
                                    System.out.println("Counter: " + Counter + " name: " + compName + " logo: " + compLogo);
                                    IdxCompany company = new IdxCompany(compTicker, compTicker, compName, compLogo);
                                    String jsonCompany = new Gson().toJson(company);
                                    //Send Company Producer
                                    producer.startProducer(topic2, id, jsonCompany);
                                }
                            }
                        }
                    } else {
                        System.out.println("Empty array");
                    }
                }

//                //Only call the first 1 of Company
//                if (Counter >= 1){
//                    break;
//                }
            }

            //Close Producer
            producer.flushAndCloseProducer();
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