package com.example.mdbspringboot.tasks;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.example.mdbspringboot.model.StockSymbol;
import com.example.mdbspringboot.repository.CustomItemRepositoryImpl;
import com.example.mdbspringboot.repository.CustomSymbolRepository;
import com.example.mdbspringboot.repository.StockSymbolRepository;
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

    @Autowired
    CustomSymbolRepository customSymbolRepository;

    @Autowired
    CustomItemRepositoryImpl customItemRepositoryImpl;

    @Autowired
    StockSymbolRepository stockSymbolRepository;

    @Scheduled(fixedRate = 10000)
    public void reportCurrentTime() {
        log.info("The time is now {}", dateFormat.format(new Date()));
    }

    @Scheduled(fixedRate = 3600000)
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
                        System.out.println("Updating: " + symbolStr);
                        restTemplate.put(MDB_API_URL + "/api/stock/" + symbolStr, symbol);
                    }
                } catch (HttpClientErrorException.NotFound e) {
                    // Symbol not found, create a new one
                    System.out.println("Creating new: " + symbolStr);
                    restTemplate.postForEntity(MDB_API_URL + "/api/stock", symbol, StockSymbol.class);
                } catch (Exception e) {
                    // Handle other exceptions
                    log.error("Error while checking/updating the symbol: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error fetching data from API: {}", e.getMessage());
        }
    }
    /*
    public void fetchDataFromApi() {
        Date date = new Date();
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = API_URL + "?type=STOCKS&page=1";

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-RapidAPI-Key", API_KEY);
            headers.set("X-RapidAPI-Host", API_HOST);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make the HTTP GET request
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();

            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map<String, Object> map = springParser.parseMap(responseBody);
            List<Map<String, Object>> bodyList = (List<Map<String, Object>>) map.get("body");

            List<String> symbols = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<Float> lastSales = new ArrayList<>();

            for (Map<String, Object> item : bodyList) {
                symbols.add((String) item.get("symbol"));
                names.add((String) item.get("name"));
                String lastSaleString = (String) item.get("lastsale");
                float lastSale = Float.parseFloat(lastSaleString.substring(1)); // Remove the '$' character
                lastSales.add(lastSale);
            }

            System.out.println("Items found: " + symbols.size());
            for (int i = 0; i < symbols.size(); i++) {
                //System.out.println("Symbol: " + symbols.get(i) + ", Name: " + names.get(i) + ", Last Sale: " + lastSales.get(i));
                StockSymbol symbol = stockSymbolRepository.findItemBySymbol(symbols.get(i));
                if (symbol != null) {
                    System.out.println("Check for: " + symbols.get(i));
                    symbol.setLastSale(lastSales.get(i));
                    //Update High sale if the price is higher
                    if (lastSales.get(i) > symbol.getHighSale() || symbol.getHighSale() <= 0) {
                        symbol.setHighSale(lastSales.get(i));
                        System.out.println("High Sale update on" + symbols.get(i));
                    }
                    //Update low sale if the price went lower
                    if (lastSales.get(i) <= symbol.getLowSale() || symbol.getLowSale() <= 0) {
                        symbol.setLowSale(lastSales.get(i));
                        System.out.println("Low Sale update on" + symbols.get(i));
                    }
                    //Save Persist the data to MongoDB
                    stockSymbolRepository.save(symbol);
                    //send to RabbitMQ
                    rabbitTemplate.convertAndSend(topicExchangeName, routingKey, "Data updated for " + symbols.get(i));
                } else {
                    System.out.println("Insert new row: " + symbols.get(i));
                    //groceryItemRepo.save(new GroceryItem("Whole Wheat Biscuit", "Whole Wheat Biscuit", 5, "snacks"));
                    stockSymbolRepository.save(new StockSymbol(symbols.get(i), symbols.get(i), names.get(i), lastSales.get(i) ));
                    rabbitTemplate.convertAndSend(topicExchangeName, routingKey, "New symbol inserted " + symbols.get(i));
                }
            }
            // Print the returned JSON in the console
            //log.info("Response from API: {}", responseBody);
        } catch (Exception e) {
            log.error("Error fetching data from API: {}", e.getMessage());
        }
    }

     */
}
