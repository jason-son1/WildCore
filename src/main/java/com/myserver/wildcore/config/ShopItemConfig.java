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
    private final String displayName; // 표시 이름 (null이면 기본값)
    private final java.util.List<String> lore; // 설명 (null이면 기본값)

    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice, String displayName,
            java.util.List<String> lore) {
        this.slot = slot;
        this.type = type;
        this.id = id;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.displayName = displayName;
        this.lore = lore;
    }

    // 레거시 호환용 생성자
    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice) {
        this(slot, type, id, buyPrice, sellPrice, null, null);
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

    public String getDisplayName() {
        return displayName;
    }

    public java.util.List<String> getLore() {
        return lore;
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
