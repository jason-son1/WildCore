package com.myserver.wildcore.config;

import java.util.List;

/**
 * 인챈트 설정 데이터 객체
 */
public class EnchantConfig {

    private final String id;
    private final String displayName;
    private final String material;
    private final int slot;
    private final List<String> targetWhitelist;
    private final String resultEnchantment;
    private final int resultLevel;
    private final double costMoney;
    private final List<String> costItems;
    private final double successRate;
    private final double failRate;
    private final double destroyRate;
    private final List<String> lore;

    public EnchantConfig(String id, String displayName, String material, int slot,
            List<String> targetWhitelist, String resultEnchantment, int resultLevel,
            double costMoney, List<String> costItems,
            double successRate, double failRate, double destroyRate,
            List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.slot = slot;
        this.targetWhitelist = targetWhitelist;
        this.resultEnchantment = resultEnchantment;
        this.resultLevel = resultLevel;
        this.costMoney = costMoney;
        this.costItems = costItems;
        this.successRate = successRate;
        this.failRate = failRate;
        this.destroyRate = destroyRate;
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

    public List<String> getTargetWhitelist() {
        return targetWhitelist;
    }

    public String getResultEnchantment() {
        return resultEnchantment;
    }

    public int getResultLevel() {
        return resultLevel;
    }

    public double getCostMoney() {
        return costMoney;
    }

    public List<String> getCostItems() {
        return costItems;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public double getFailRate() {
        return failRate;
    }

    public double getDestroyRate() {
        return destroyRate;
    }

    public List<String> getLore() {
        return lore;
    }
}
