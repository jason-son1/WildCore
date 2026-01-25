package com.myserver.wildcore.npc;

import org.bukkit.Material;

/**
 * WildCore NPC 타입
 * 각 NPC 타입은 클릭 시 서로 다른 GUI를 엽니다.
 */
public enum NpcType {

    /** 상점 NPC - 클릭 시 ShopGUI 오픈 */
    SHOP("shop", "상점", Material.CHEST, "§a", "§7상점 거래를 담당합니다."),

    /** 강화 NPC - 클릭 시 EnchantGUI 오픈 */
    ENCHANT("enchant", "강화소", Material.ENCHANTING_TABLE, "§d", "§7아이템 강화를 담당합니다."),

    /** 주식 NPC - 클릭 시 StockGUI 오픈 */
    STOCK("stock", "주식거래소", Material.EMERALD, "§2", "§7주식 거래를 담당합니다."),

    /** 은행 NPC - 클릭 시 BankMainGUI 오픈 */
    BANK("bank", "은행", Material.GOLD_INGOT, "§6", "§7예금/적금 서비스를 담당합니다."),

    /** 이동 NPC - 클릭 시 지정된 월드로 이동 */
    WARP("warp", "이동 도우미", Material.COMPASS, "§b", "§7다른 월드로 이동시켜줍니다.");

    private final String id;
    private final String displayName;
    private final Material icon;
    private final String colorCode;
    private final String description;

    NpcType(String id, String displayName, Material icon, String colorCode, String description) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
        this.colorCode = colorCode;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 색상 코드가 적용된 표시 이름
     */
    public String getColoredName() {
        return colorCode + displayName;
    }

    /**
     * GUI 아이콘 Material
     */
    public Material getIcon() {
        return icon;
    }

    /**
     * 색상 코드
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * 설명 문구
     */
    public String getDescription() {
        return description;
    }

    /**
     * 기본 NPC 이름 생성
     */
    public String getDefaultNpcName() {
        return colorCode + "[ " + displayName + " ]";
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

    /**
     * targetId가 필요한 타입인지 확인
     * SHOP, WARP는 targetId 필요 (상점ID, 월드이름)
     */
    public boolean requiresTargetId() {
        return this == SHOP || this == WARP;
    }
}
