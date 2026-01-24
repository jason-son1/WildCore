package com.myserver.wildcore.npc;

/**
 * WildCore NPC 타입
 * 각 NPC 타입은 클릭 시 서로 다른 GUI를 엽니다.
 */
public enum NpcType {

    /** 상점 NPC - 클릭 시 ShopGUI 오픈 */
    SHOP("shop", "상점"),

    /** 강화 NPC - 클릭 시 EnchantGUI 오픈 */
    ENCHANT("enchant", "강화소"),

    /** 주식 NPC - 클릭 시 StockGUI 오픈 */
    STOCK("stock", "주식거래소");

    private final String id;
    private final String displayName;

    NpcType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * ID로 NpcType 찾기
     */
    public static NpcType fromId(String id) {
        if (id == null)
            return null;
        for (NpcType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }
}
