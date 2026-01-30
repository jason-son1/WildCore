package com.myserver.wildcore.config;

import java.util.List;

/**
 * 커스텀 아이템 설정 데이터 객체
 */
public class CustomItemConfig {

    private final String id;
    private final String material;
    private final String displayName;
    private final int customModelData;
    private final boolean glow;
    private final List<String> lore;

    private final List<String> functions;

    public CustomItemConfig(String id, String material, String displayName,
            int customModelData, boolean glow, List<String> lore, List<String> functions) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.customModelData = customModelData;
        this.glow = glow;
        this.lore = lore;
        this.functions = functions;
    }

    public String getId() {
        return id;
    }

    public String getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public boolean isGlow() {
        return glow;
    }

    public List<String> getLore() {
        return lore;
    }

    public List<String> getFunctions() {
        return functions;
    }
}
