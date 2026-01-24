package com.myserver.wildcore.config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 상점 설정 데이터 객체
 */
public class ShopConfig {

    private final String id;
    private String displayName;
    private String npcType; // VILLAGER 또는 ARMOR_STAND
    private Location location;
    private UUID entityUuid;
    private final Map<Integer, ShopItemConfig> items;

    public ShopConfig(String id, String displayName, String npcType, Location location,
            UUID entityUuid, Map<Integer, ShopItemConfig> items) {
        this.id = id;
        this.displayName = displayName;
        this.npcType = npcType;
        this.location = location;
        this.entityUuid = entityUuid;
        this.items = items != null ? items : new HashMap<>();
    }

    // === Getters ===

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getNpcType() {
        return npcType;
    }

    public Location getLocation() {
        return location;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public Map<Integer, ShopItemConfig> getItems() {
        return items;
    }

    // === Setters ===

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setNpcType(String npcType) {
        this.npcType = npcType;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    // === Helper Methods ===

    /**
     * Villager NPC인지 확인
     */
    public boolean isVillager() {
        return "VILLAGER".equalsIgnoreCase(npcType);
    }

    /**
     * ArmorStand NPC인지 확인
     */
    public boolean isArmorStand() {
        return "ARMOR_STAND".equalsIgnoreCase(npcType);
    }

    /**
     * 특정 슬롯의 아이템 가져오기
     */
    public ShopItemConfig getItem(int slot) {
        return items.get(slot);
    }

    /**
     * 아이템 추가/수정
     */
    public void setItem(int slot, ShopItemConfig item) {
        items.put(slot, item);
    }

    /**
     * 아이템 제거
     */
    public void removeItem(int slot) {
        items.remove(slot);
    }

    /**
     * 아이템 개수
     */
    public int getItemCount() {
        return items.size();
    }

    /**
     * Location 문자열로 변환 (디버깅/로깅용)
     */
    public String getLocationString() {
        if (location == null)
            return "없음";
        return String.format("%s, %.1f, %.1f, %.1f",
                location.getWorld() != null ? location.getWorld().getName() : "?",
                location.getX(), location.getY(), location.getZ());
    }

    /**
     * Location 객체 생성 헬퍼 메서드
     */
    public static Location createLocation(String worldName, double x, double y, double z, float yaw) {
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        Location loc = new Location(world, x, y, z);
        loc.setYaw(yaw);
        loc.setPitch(0);
        return loc;
    }
}
