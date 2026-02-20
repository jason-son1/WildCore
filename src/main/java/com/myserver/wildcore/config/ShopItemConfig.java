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

    // 포션 메타데이터 (포션/투척 포션/잔류 포션/화살 등)
    private final String potionType; // 포션 타입 (예: HEALING, REGENERATION 등, null이면 포션 아님)
    private final boolean potionExtended; // 연장 여부
    private final boolean potionUpgraded; // 강화 여부

    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice, String displayName,
            java.util.List<String> lore, String potionType, boolean potionExtended, boolean potionUpgraded) {
        this.slot = slot;
        this.type = type;
        this.id = id;
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.displayName = displayName;
        this.lore = lore;
        this.potionType = potionType;
        this.potionExtended = potionExtended;
        this.potionUpgraded = potionUpgraded;
    }

    // 포션 데이터 없는 생성자 (기존 호환)
    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice, String displayName,
            java.util.List<String> lore) {
        this(slot, type, id, buyPrice, sellPrice, displayName, lore, null, false, false);
    }

    // 레거시 호환용 생성자
    public ShopItemConfig(int slot, String type, String id, double buyPrice, double sellPrice) {
        this(slot, type, id, buyPrice, sellPrice, null, null, null, false, false);
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

    public String getPotionType() {
        return potionType;
    }

    public boolean isPotionExtended() {
        return potionExtended;
    }

    public boolean isPotionUpgraded() {
        return potionUpgraded;
    }

    /**
     * 포션 메타데이터가 있는지 확인
     */
    public boolean hasPotionData() {
        return potionType != null && !potionType.isEmpty();
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
