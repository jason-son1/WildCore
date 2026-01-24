package com.myserver.wildcore.util;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * NPC PersistentDataContainer(PDC) 태그 유틸리티
 * NPC 엔티티에 WildCore 태그를 설정하고 읽습니다.
 */
public class NpcTagUtil {

    private static final String NAMESPACE = "wildcore";
    private static final String KEY_NPC_TYPE = "npc_type";
    private static final String KEY_TARGET_ID = "target_id";

    /**
     * NPC에 WildCore 태그 설정
     * 
     * @param entity   NPC 엔티티
     * @param type     NPC 타입
     * @param targetId 타겟 ID (상점의 경우 상점 ID, 그 외 null)
     */
    public static void setNpcTag(Entity entity, NpcType type, String targetId) {
        if (entity == null || type == null)
            return;

        WildCore plugin = WildCore.getInstance();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_NPC_TYPE);
        pdc.set(typeKey, PersistentDataType.STRING, type.getId());

        if (targetId != null && !targetId.isEmpty()) {
            NamespacedKey targetKey = new NamespacedKey(plugin, KEY_TARGET_ID);
            pdc.set(targetKey, PersistentDataType.STRING, targetId);
        }
    }

    /**
     * NPC 타입 읽기
     * 
     * @param entity NPC 엔티티
     * @return NPC 타입, WildCore NPC가 아니면 null
     */
    public static NpcType getNpcType(Entity entity) {
        if (entity == null)
            return null;

        WildCore plugin = WildCore.getInstance();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_NPC_TYPE);
        String typeId = pdc.get(typeKey, PersistentDataType.STRING);

        return NpcType.fromId(typeId);
    }

    /**
     * NPC 타겟 ID 읽기 (상점 ID 등)
     * 
     * @param entity NPC 엔티티
     * @return 타겟 ID, 없으면 null
     */
    public static String getTargetId(Entity entity) {
        if (entity == null)
            return null;

        WildCore plugin = WildCore.getInstance();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        NamespacedKey targetKey = new NamespacedKey(plugin, KEY_TARGET_ID);
        return pdc.get(targetKey, PersistentDataType.STRING);
    }

    /**
     * WildCore NPC인지 확인
     * 
     * @param entity 엔티티
     * @return WildCore NPC이면 true
     */
    public static boolean isWildCoreNpc(Entity entity) {
        return getNpcType(entity) != null;
    }

    /**
     * 특정 타입의 WildCore NPC인지 확인
     * 
     * @param entity 엔티티
     * @param type   확인할 타입
     * @return 해당 타입이면 true
     */
    public static boolean isNpcType(Entity entity, NpcType type) {
        NpcType entityType = getNpcType(entity);
        return entityType != null && entityType == type;
    }

    /**
     * NPC 태그 제거
     * 
     * @param entity NPC 엔티티
     */
    public static void removeNpcTag(Entity entity) {
        if (entity == null)
            return;

        WildCore plugin = WildCore.getInstance();
        PersistentDataContainer pdc = entity.getPersistentDataContainer();

        NamespacedKey typeKey = new NamespacedKey(plugin, KEY_NPC_TYPE);
        NamespacedKey targetKey = new NamespacedKey(plugin, KEY_TARGET_ID);

        pdc.remove(typeKey);
        pdc.remove(targetKey);
    }
}
