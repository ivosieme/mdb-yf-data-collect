package com.example.mdbspringboot.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import com.example.mdbspringboot.model.StockSymbol;
import com.mongodb.client.result.UpdateResult;

@Component
public class CustomSymbolRepositoryImpl implements CustomSymbolRepository {

    @Autowired
    MongoTemplate mongoTemplate;

    public void updateSymbolLastSale(String symbol, float lastSalePrice) {
        Query query = new Query(Criteria.where("symbol").is(symbol));
        Update update = new Update();
        update.set("quantity", lastSalePrice);

        UpdateResult result = mongoTemplate.updateFirst(query, update, StockSymbol.class);

        if(result == null)
            System.out.println("No documents updated");
        else
            System.out.println(result.getModifiedCount() + " document(s) updated..");

    }

    public StockSymbol findOneBySymbol(String symbol) {
        Query query = new Query(Criteria.where("symbol").is(symbol));
        return mongoTemplate.findOne(query, StockSymbol.class);
    }

}
