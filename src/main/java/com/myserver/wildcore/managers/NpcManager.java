package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcData;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 통합 NPC 매니저
 * 모든 WildCore NPC(상점, 강화, 주식)를 관리합니다.
 */
public class NpcManager {

    private final WildCore plugin;

    /** 엔티티 UUID -> NPC 데이터 캐시 */
    private final Map<UUID, NpcData> npcCache = new HashMap<>();

    public NpcManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 플러그인 시작 시 기존 태그된 NPC를 모두 제거 (Wipe)
     * 서버 리로드/재시작 시 NPC 중복 방지
     */
    public void removeAllTaggedNpcs() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (NpcTagUtil.isWildCoreNpc(entity)) {
                    entity.remove();
                    removed++;
                }
            }
        }
        npcCache.clear();
        plugin.getLogger().info("기존 WildCore NPC " + removed + "개 제거됨 (Wipe)");
    }

    /**
     * 특정 타입의 태그된 NPC만 제거
     */
    public void removeAllTaggedNpcs(NpcType type) {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (NpcTagUtil.isNpcType(entity, type)) {
                    npcCache.remove(entity.getUniqueId());
                    entity.remove();
                    removed++;
                }
            }
        }
        plugin.debug("WildCore NPC(" + type.getDisplayName() + ") " + removed + "개 제거됨");
    }

    /**
     * NPC 소환
     * 
     * @param type          NPC 타입
     * @param location      소환 위치
     * @param displayName   표시 이름
     * @param targetId      타겟 ID (상점 ID 등, 없으면 null)
     * @param useArmorStand true면 ArmorStand, false면 Villager
     * @return 소환된 엔티티의 UUID
     */
    public UUID spawnNpc(NpcType type, Location location, String displayName, String targetId, boolean useArmorStand) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("NPC 소환 실패: 유효하지 않은 위치");
            return null;
        }

        Entity npc;

        if (useArmorStand) {
            npc = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
                armorStand.setVisible(true);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(true);
                armorStand.setSilent(true);
                armorStand.setCustomNameVisible(true);
                armorStand.customName(ItemUtil.parse(displayName));
                armorStand.setMarker(false);
                armorStand.setBasePlate(false);
                armorStand.setArms(true);
            });
        } else {
            npc = location.getWorld().spawn(location, Villager.class, villager -> {
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.setCollidable(false);
                villager.setCustomNameVisible(true);
                villager.customName(ItemUtil.parse(displayName));
                villager.setProfession(Villager.Profession.NITWIT);
                villager.setVillagerType(Villager.Type.PLAINS);
            });
        }

        // PDC 태그 설정
        NpcTagUtil.setNpcTag(npc, type, targetId);

        // 캐시에 저장
        NpcData data = new NpcData(npc.getUniqueId(), type, targetId, location, displayName);
        npcCache.put(npc.getUniqueId(), data);

        plugin.debug("NPC 소환됨: " + type.getDisplayName() + " (UUID: " + npc.getUniqueId() + ")");
        return npc.getUniqueId();
    }

    /**
     * NPC 제거
     * 
     * @param uuid 엔티티 UUID
     * @return 제거 성공 여부
     */
    public boolean removeNpc(UUID uuid) {
        if (uuid == null)
            return false;

        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) {
            entity.remove();
        }
        npcCache.remove(uuid);
        return true;
    }

    /**
     * UUID로 NPC 데이터 가져오기
     */
    public NpcData getNpcData(UUID uuid) {
        return npcCache.get(uuid);
    }

    /**
     * 엔티티로 NPC 데이터 가져오기
     */
    public NpcData getNpcData(Entity entity) {
        if (entity == null)
            return null;

        NpcData cached = npcCache.get(entity.getUniqueId());
        if (cached != null) {
            return cached;
        }

        // 캐시에 없으면 PDC에서 읽기 시도
        NpcType type = NpcTagUtil.getNpcType(entity);
        if (type != null) {
            String targetId = NpcTagUtil.getTargetId(entity);
            NpcData data = new NpcData(entity.getUniqueId(), type, targetId, entity.getLocation(), null);
            npcCache.put(entity.getUniqueId(), data);
            return data;
        }

        return null;
    }

    /**
     * UUID로 엔티티 찾기
     */
    public Entity getEntity(UUID uuid) {
        return Bukkit.getEntity(uuid);
    }

    /**
     * 캐시에 NPC 등록 (ShopManager에서 사용)
     */
    public void registerNpc(UUID uuid, NpcData data) {
        npcCache.put(uuid, data);
    }

    /**
     * 캐시에서 NPC 해제
     */
    public void unregisterNpc(UUID uuid) {
        npcCache.remove(uuid);
    }

    /**
     * 모든 캐시된 NPC 목록 반환
     */
    public Map<UUID, NpcData> getAllNpcs() {
        return new HashMap<>(npcCache);
    }

    /**
     * 특정 타입의 NPC 목록 반환
     */
    public Map<UUID, NpcData> getNpcsByType(NpcType type) {
        Map<UUID, NpcData> result = new HashMap<>();
        for (Map.Entry<UUID, NpcData> entry : npcCache.entrySet()) {
            if (entry.getValue().getType() == type) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 리로드
     */
    public void reload() {
        npcCache.clear();
    }
}
