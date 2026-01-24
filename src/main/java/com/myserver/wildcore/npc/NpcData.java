package com.myserver.wildcore.npc;

import org.bukkit.Location;
import java.util.UUID;

/**
 * WildCore NPC 데이터 클래스
 * NPC의 모든 정보를 담는 DTO
 */
public class NpcData {

    private final UUID entityUuid;
    private final NpcType type;
    private final String targetId; // SHOP의 경우 상점 ID, 나머지는 null
    private final Location location;
    private final String displayName;

    public NpcData(UUID entityUuid, NpcType type, String targetId, Location location, String displayName) {
        this.entityUuid = entityUuid;
        this.type = type;
        this.targetId = targetId;
        this.location = location;
        this.displayName = displayName;
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

    @Override
    public String toString() {
        return "NpcData{" +
                "entityUuid=" + entityUuid +
                ", type=" + type +
                ", targetId='" + targetId + '\'' +
                ", location="
                + (location != null
                        ? location.getWorld().getName() + ":" + location.getBlockX() + "," + location.getBlockY() + ","
                                + location.getBlockZ()
                        : "null")
                +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
