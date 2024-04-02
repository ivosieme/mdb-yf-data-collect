package com.example.mdbspringboot.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("StockSymbol")
public class StockSymbol {

    @Id
    private String id;

    private String symbol;
    private String name;
    private float lastSale;
    private float highSale = 0;
    private float lowSale = 0;
    private float volatilityIndex = 0;

    @LastModifiedDate
    private String lastModifiedDate;

    // Default no-argument constructor
    public StockSymbol() {
        super();
    }

    // All-arguments constructor
    public StockSymbol(String id, String symbol, String name, float lastSale) {
        super();
        this.id = id;
        this.name = name;
        this.symbol = symbol;
        this.lastSale = lastSale;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public float getLastSale() {
        return lastSale;
    }

    public void setLastSale(float lastSale) {
        this.lastSale = lastSale;
    }

    public float getHighSale() {
        return highSale;
    }

    public void setHighSale(float highSale) {
        this.highSale = highSale;
    }

    public float getLowSale() {
        return lowSale;
    }

    public void setLowSale(float lowSale) {
        this.lowSale = lowSale;
    }

    public float getVolatilityIndex() {
        return volatilityIndex;
    }

    public void setVolatilityIndex(float volatilityIndex) {
        this.volatilityIndex = volatilityIndex;
    }

    public String getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(String lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }
}
