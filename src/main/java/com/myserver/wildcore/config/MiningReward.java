package com.myserver.wildcore.config;

public class MiningReward {
    private String itemId;
    private double chance;
    private int minAmount;
    private int maxAmount;

    public MiningReward(String itemId, double chance, int minAmount, int maxAmount) {
        this.itemId = itemId;
        this.chance = chance;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
    }

    public String getItemId() {
        return itemId;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = chance;
    }

    public int getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(int minAmount) {
        this.minAmount = minAmount;
    }

    public int getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(int maxAmount) {
        this.maxAmount = maxAmount;
    }
}
