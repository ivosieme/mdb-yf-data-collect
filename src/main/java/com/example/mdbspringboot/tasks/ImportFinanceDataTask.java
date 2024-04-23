package com.example.mdbspringboot.tasks;

import java.text.SimpleDateFormat;
import java.util.*;

import com.example.mdbspringboot.model.StockSymbol;
/*
import com.example.mdbspringboot.repository.CustomItemRepositoryImpl;
import com.example.mdbspringboot.repository.CustomSymbolRepository;
import com.example.mdbspringboot.repository.StockSymbolRepository;
 */
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;

@Component
public class ImportFinanceDataTask {

    @Value("${app.mdb.api.url}")
    private String MDB_API_URL;

    @Value("${app.yahoo.api.url}")
    private String API_URL;

    @Value("${app.yahoo.api.key}")
    private String API_KEY;

    @Value("${app.yahoo.api.host}")
    private String API_HOST;

    private static final Logger log = LoggerFactory.getLogger(ImportFinanceDataTask.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Value("${app.rabbit.topicExchangeName}")
    private String topicExchangeName;

    @Value("${app.rabbit.queue.name}")
    private String queueName;

    @Value("${app.rabbit.routing.key}")
    private String routingKey;

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Scheduled(fixedRate = 30000)
    public void reportCurrentTime() {
        log.info("The time is now {}", dateFormat.format(new Date()));
    }

    @Scheduled(fixedRate = 3000)
    public void fetchDataFromApi() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        Date date = new Date();
        try {
            String fetchUrl = API_URL + "?type=STOCKS&page=1";
            // Set headers for fetching data
            HttpHeaders fetchHeaders = new HttpHeaders();
            fetchHeaders.set("X-RapidAPI-Key", API_KEY);
            fetchHeaders.set("X-RapidAPI-Host", API_HOST);
            HttpEntity<String> fetchEntity = new HttpEntity<>(fetchHeaders);

            // Fetch data from the external API
            ResponseEntity<String> response = restTemplate.exchange(fetchUrl, HttpMethod.GET, fetchEntity, String.class);
            String responseBody = response.getBody();

            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = springParser.parseMap(responseBody);
            List<Map<String, Object>> bodyList = (List<Map<String, Object>>) map.get("body");

            for (Map<String, Object> item : bodyList) {
                String symbolStr = (String) item.get("symbol");
                String name = (String) item.get("name");
                String lastSaleString = (String) item.get("lastsale");
                float lastSale = Float.parseFloat(lastSaleString.substring(1)); // Remove the '$' character

                // Construct StockSymbol object
                StockSymbol symbol = new StockSymbol(symbolStr, symbolStr, name, lastSale);

                try {
                    // Check if the symbol exists
                    ResponseEntity<StockSymbol> existingSymbolResponse = restTemplate.getForEntity(MDB_API_URL + "/api/stock/" + symbolStr, StockSymbol.class);

                    // If the symbol exists, update it
                    if (existingSymbolResponse.getStatusCode().is2xxSuccessful() && existingSymbolResponse.getBody() != null) {
                        StockSymbol existingSymbol = existingSymbolResponse.getBody();
                        System.out.println("Updating: " + symbolStr);

                        //set the old values
                        symbol.setHighSale(existingSymbol.getHighSale());
                        symbol.setLowSale(existingSymbol.getLowSale());

                        // Compare and set highSale price
                        if (lastSale > existingSymbol.getHighSale()) {
                            symbol.setHighSale(lastSale);
                            System.out.println("- high: " + lastSale);
                        }

                        // Compare and set lowSale price
                        // Ensure that lowSale is not zero (uninitialized) and the lastSale is lower than current lowSale
                        if (existingSymbol.getLowSale() == 0 || lastSale < existingSymbol.getLowSale()) {
                            symbol.setLowSale(lastSale);
                            System.out.println("- low: " + lastSale);
                        }

                        restTemplate.put(MDB_API_URL + "/api/stock/" + symbolStr, symbol);
                        //persist to RabbitMQ
                        rabbitTemplate.convertAndSend(topicExchangeName, routingKey, "UPDATE:" + symbolStr);
                    }
                } catch (HttpClientErrorException.NotFound e) {
                    // Symbol not found, create a new one
                    System.out.println("Creating new: " + symbolStr);
                    restTemplate.postForEntity(MDB_API_URL + "/api/stock", symbol, StockSymbol.class);
                    System.out.println(symbol);
                    //persist to RabbitMQ
                    rabbitTemplate.convertAndSend(topicExchangeName, routingKey, "CREATE:" + symbolStr);
                } catch (Exception e) {
                    // Handle other exceptions
                    log.error("Error while checking/updating the symbol: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching data from API: {}", e.getMessage());
        }
    }

    /**
     * DEMO ONLY
     *
     * because my Yahoo Finance API account is limited to 3000 calls/month,
     * I implemented this method to generate a bit more traffic for my App
     *
     */
    @Scheduled(fixedRate = 3600000) // Adjust the rate as needed
    public void adjustRandomStockPrice() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Fetch all stock symbols from the API
            ResponseEntity<String> response = restTemplate.getForEntity(MDB_API_URL + "/api/stocks", String.class);
            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = springParser.parseMap(response.getBody());
            List<Map<String, Object>> stocks = (List<Map<String, Object>>) map.get("stocks");

            if (stocks != null && !stocks.isEmpty()) {
                // Pick a random stock
                Random random = new Random();
                Map<String, Object> randomStock = stocks.get(random.nextInt(stocks.size()));

                String symbol = (String) randomStock.get("symbol");
                float lastSale = Float.parseFloat((String) randomStock.get("lastSale"));
                boolean increase = random.nextBoolean(); // Randomly decide to increase or decrease

                // Calculate the new price with a ±0.5% variation
                float changeFactor = 1 + (increase ? 0.005f : -0.005f);
                lastSale *= changeFactor;

                // Update the stock object
                StockSymbol stock = new StockSymbol(symbol, symbol, (String) randomStock.get("name"), lastSale);
                restTemplate.put(MDB_API_URL + "/api/stock/" + symbol, stock);
                System.out.println("Adjusted stock " + symbol + " to new last sale price: " + lastSale);

                // Persist to RabbitMQ
                rabbitTemplate.convertAndSend(topicExchangeName, routingKey, "UPDATE:" + symbol);
            }
        } catch (Exception e) {
            log.error("Error adjusting stock price: {}", e.getMessage());
        }
    }
}
