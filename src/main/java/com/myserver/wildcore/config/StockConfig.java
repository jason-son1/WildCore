package com.myserver.wildcore.config;

import java.util.List;

/**
 * 주식 설정 데이터 객체
 */
public class StockConfig {

    private final String id;
    private final String displayName;
    private final String material;
    private final int slot;
    private final double basePrice;
    private final double minPrice;
    private final double maxPrice;
    private final double volatility;
    private final List<String> lore;

    public StockConfig(String id, String displayName, String material, int slot,
            double basePrice, double minPrice, double maxPrice,
            double volatility, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.basePrice = basePrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.volatility = volatility;
        this.lore = lore;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public int getSlot() {
        return slot;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getMinPrice() {
        return minPrice;
    }

    public double getMaxPrice() {
        return maxPrice;
    }

    public double getVolatility() {
        return volatility;
    }

    public List<String> getLore() {
        return lore;
    }
}
