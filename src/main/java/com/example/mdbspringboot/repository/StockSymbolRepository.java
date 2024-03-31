package com.example.mdbspringboot.repository;

import java.util.List;

import com.example.mdbspringboot.model.GroceryItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.mdbspringboot.model.StockSymbol;
public interface StockSymbolRepository extends MongoRepository<StockSymbol, String> {
    @Query("{symbol:'?0'}")
    StockSymbol findItemBySymbol(String symbol);

    @Query(value="{category:'?0'}", fields="{'name' : 1, 'quantity' : 1}")
    List<StockSymbol> findAll(String category);

    public long count();
}
