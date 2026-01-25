package com.myserver.wildcore.npc;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.UUID;

/**
 * WildCore NPC 데이터 클래스
 * NPC의 모든 정보를 담는 DTO
 */
public class NpcData {

    private final String id; // 고유 NPC ID (자동 생성 또는 지정)
    private UUID entityUuid; // 엔티티 UUID (소환 후 설정)
    private final NpcType type;
    private final String targetId; // SHOP의 경우 상점 ID, WARP의 경우 월드 이름
    private Location location;
    private String displayName;
    private EntityType entityType; // 엔티티 타입 (VILLAGER, ARMOR_STAND 등)
    private final long createdTime; // 생성 시간
    private final UUID creatorUuid; // 생성자 UUID

    /**
     * 신규 NPC 생성용 생성자
     */
    public NpcData(String id, NpcType type, String targetId, Location location,
            String displayName, EntityType entityType, UUID creatorUuid) {
        this.id = id;
        this.entityUuid = null;
        this.type = type;
        this.targetId = targetId;
        this.location = location;
        this.displayName = displayName;
        this.entityType = entityType;
        this.createdTime = System.currentTimeMillis();
        this.creatorUuid = creatorUuid;
    }

    /**
     * 저장된 데이터 로드용 생성자
     */
    public NpcData(String id, UUID entityUuid, NpcType type, String targetId,
            Location location, String displayName, EntityType entityType,
            long createdTime, UUID creatorUuid) {
        this.id = id;
        this.entityUuid = entityUuid;
        this.type = type;
        this.targetId = targetId;
        this.location = location;
        this.displayName = displayName;
        this.entityType = entityType;
        this.createdTime = createdTime;
        this.creatorUuid = creatorUuid;
    }

    /**
     * 레거시 호환용 생성자 (entityUuid 기반)
     */
    public NpcData(UUID entityUuid, NpcType type, String targetId, Location location, String displayName) {
        this.id = generateId();
        this.entityUuid = entityUuid;
        this.type = type;
        this.targetId = targetId;
        this.location = location;
        this.displayName = displayName;
        this.entityType = EntityType.VILLAGER;
        this.createdTime = System.currentTimeMillis();
        this.creatorUuid = null;
    }

    // =====================
    // Getters
    // =====================

    public String getId() {
        return id;
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public NpcType getType() {
        return type;
    }

    public String getTargetId() {
        return targetId;
    }

    public Location getLocation() {
        return location;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public UUID getCreatorUuid() {
        return creatorUuid;
    }

    // =====================
    // Setters
    // =====================

    public void setEntityUuid(UUID entityUuid) {
        this.entityUuid = entityUuid;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    // =====================
    // 유틸리티 메서드
    // =====================

    /**
     * 짧은 ID 생성 (6자리)
     */
    private static String generateId() {
        return UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 새 ID 생성 (static)
     */
    public static String createNewId() {
        return generateId();
    }

    /**
     * 위치를 보기 좋은 문자열로 반환
     */
    public String getLocationString() {
        if (location == null || location.getWorld() == null) {
            return "알 수 없음";
        }
        return String.format("%s: %d, %d, %d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    /**
     * 엔티티가 유효한지 확인
     */
    public boolean hasValidEntity() {
        return entityUuid != null;
    }

    @Override
    public String toString() {
        return "NpcData{" +
                "id='" + id + '\'' +
                ", entityUuid=" + entityUuid +
                ", type=" + type +
                ", targetId='" + targetId + '\'' +
                ", location=" + getLocationString() +
                ", displayName='" + displayName + '\'' +
                ", entityType=" + entityType +
                '}';
    }
}
