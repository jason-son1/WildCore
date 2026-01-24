package com.myserver.wildcore.config;

/**
 * 상점 아이템 설정 데이터 객체
 */
public class ShopItemConfig {

    private final int slot;
    private final String type; // "VANILLA" 또는 "CUSTOM"
    private final String id; // Material 이름 또는 커스텀 아이템 ID
    private final double buyPrice; // 구매가 (-1이면 구매 불가)
    private final double sellPrice; // 판매가 (-1이면 판매 불가)

    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice) {
        this.slot = slot;
        this.type = type;
        this.id = id;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
    }

    public int getSlot() {
        return slot;
    }

    public String getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public double getBuyPrice() {
        return buyPrice;
    }

    public double getSellPrice() {
        return sellPrice;
    }

    /**
     * 구매 가능 여부
     */
    public boolean canBuy() {
        return buyPrice >= 0;
    }

    /**
     * 판매 가능 여부
     */
    public boolean canSell() {
        return sellPrice >= 0;
    }

    /**
     * 바닐라 아이템인지 확인
     */
    public boolean isVanilla() {
        return "VANILLA".equalsIgnoreCase(type);
    }

    /**
     * 커스텀 아이템인지 확인
     */
    public boolean isCustom() {
        return "CUSTOM".equalsIgnoreCase(type);
    }
}
