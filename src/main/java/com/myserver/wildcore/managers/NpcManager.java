package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcData;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.ItemUtil;
import com.myserver.wildcore.util.NpcTagUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통합 NPC 매니저
 * 모든 WildCore NPC(상점, 강화, 주식, 은행)를 관리합니다.
 */
public class NpcManager {

    private final WildCore plugin;

    /** NPC ID -> NPC 데이터 캐시 */
    private final Map<String, NpcData> npcCache = new HashMap<>();

    /** 엔티티 UUID -> NPC ID 역참조 */
    private final Map<UUID, String> entityToNpcId = new HashMap<>();

    // npcs.yml 파일
    private File npcsFile;
    private FileConfiguration npcsConfig;

    public NpcManager(WildCore plugin) {
        this.plugin = plugin;
        loadNpcsConfig();
    }

    // =====================
    // 설정 파일 관리
    // =====================

    /**
     * npcs.yml 로드
     */
    private void loadNpcsConfig() {
        npcsFile = new File(plugin.getDataFolder(), "npcs.yml");
        if (!npcsFile.exists()) {
            plugin.saveResource("npcs.yml", false);
        }
        npcsConfig = YamlConfiguration.loadConfiguration(npcsFile);
        loadAllNpcs();
    }

    /**
     * npcs.yml 저장
     */
    public void saveNpcsConfig() {
        // NPC 데이터 저장
        npcsConfig.set("npcs", null);

        for (Map.Entry<String, NpcData> entry : npcCache.entrySet()) {
            String id = entry.getKey();
            NpcData data = entry.getValue();

            // 상점 NPC는 shops.yml에서 관리하므로 건너뜀
            if (data.getType() == NpcType.SHOP)
                continue;

            String path = "npcs." + id;
            npcsConfig.set(path + ".type", data.getType().getId());
            npcsConfig.set(path + ".display_name", data.getDisplayName());
            npcsConfig.set(path + ".entity_type", data.getEntityType().name());
            npcsConfig.set(path + ".target_id", data.getTargetId());

            if (data.getLocation() != null && data.getLocation().getWorld() != null) {
                npcsConfig.set(path + ".location.world", data.getLocation().getWorld().getName());
                npcsConfig.set(path + ".location.x", data.getLocation().getX());
                npcsConfig.set(path + ".location.y", data.getLocation().getY());
                npcsConfig.set(path + ".location.z", data.getLocation().getZ());
                npcsConfig.set(path + ".location.yaw", data.getLocation().getYaw());
            }

            npcsConfig.set(path + ".created", data.getCreatedTime());
            if (data.getCreatorUuid() != null) {
                npcsConfig.set(path + ".creator", data.getCreatorUuid().toString());
            }
            if (data.getEntityUuid() != null) {
                npcsConfig.set(path + ".entity_uuid", data.getEntityUuid().toString());
            }
        }

        try {
            npcsConfig.save(npcsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("NPC 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 저장된 NPC 데이터 로드
     */
    private void loadAllNpcs() {
        npcCache.clear();
        entityToNpcId.clear();

        ConfigurationSection npcsSection = npcsConfig.getConfigurationSection("npcs");
        if (npcsSection == null) {
            plugin.getLogger().info("저장된 NPC 데이터 없음");
            return;
        }

        int loaded = 0;
        for (String id : npcsSection.getKeys(false)) {
            String path = "npcs." + id;

            NpcType type = NpcType.fromId(npcsConfig.getString(path + ".type"));
            if (type == null)
                continue;

            String displayName = npcsConfig.getString(path + ".display_name", type.getDefaultNpcName());
            EntityType entityType = EntityType.valueOf(
                    npcsConfig.getString(path + ".entity_type", "VILLAGER"));
            String targetId = npcsConfig.getString(path + ".target_id");

            // 위치 로드
            Location location = null;
            if (npcsConfig.isConfigurationSection(path + ".location")) {
                String worldName = npcsConfig.getString(path + ".location.world");
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    location = new Location(
                            world,
                            npcsConfig.getDouble(path + ".location.x"),
                            npcsConfig.getDouble(path + ".location.y"),
                            npcsConfig.getDouble(path + ".location.z"),
                            (float) npcsConfig.getDouble(path + ".location.yaw"),
                            0);
                }
            }

            long createdTime = npcsConfig.getLong(path + ".created", System.currentTimeMillis());
            UUID creatorUuid = null;
            if (npcsConfig.contains(path + ".creator")) {
                try {
                    creatorUuid = UUID.fromString(npcsConfig.getString(path + ".creator"));
                } catch (Exception ignored) {
                }
            }
            UUID entityUuid = null;
            if (npcsConfig.contains(path + ".entity_uuid")) {
                try {
                    entityUuid = UUID.fromString(npcsConfig.getString(path + ".entity_uuid"));
                } catch (Exception ignored) {
                }
            }

            NpcData data = new NpcData(id, entityUuid, type, targetId, location,
                    displayName, entityType, createdTime, creatorUuid);
            npcCache.put(id, data);

            if (entityUuid != null) {
                entityToNpcId.put(entityUuid, id);
            }

            loaded++;
        }

        plugin.getLogger().info("NPC 데이터 " + loaded + "개 로드됨");
    }

    // =====================
    // NPC 소환/제거
    // =====================

    /**
     * 플러그인 시작 시 저장된 NPC 다시 소환
     */
    public void respawnAllNpcs() {
        int spawned = 0;
        for (NpcData data : new ArrayList<>(npcCache.values())) {
            if (data.getLocation() != null) {
                // 기존 엔티티 제거 (있다면)
                if (data.getEntityUuid() != null) {
                    Entity existing = Bukkit.getEntity(data.getEntityUuid());
                    if (existing != null) {
                        existing.remove();
                    }
                }

                // 새로 소환
                UUID newUuid = spawnNpcEntity(data);
                if (newUuid != null) {
                    data.setEntityUuid(newUuid);
                    entityToNpcId.put(newUuid, data.getId());
                    spawned++;
                }
            }
        }
        plugin.getLogger().info("NPC " + spawned + "개 재소환됨");
        saveNpcsConfig();
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
        plugin.getLogger().info("기존 WildCore NPC 엔티티 " + removed + "개 제거됨 (Wipe)");
    }

    /**
     * 특정 타입의 태그된 NPC만 제거
     */
    public void removeAllTaggedNpcs(NpcType type) {
        int removed = 0;

        // 캐시에서 해당 타입 NPC 제거
        List<String> toRemove = npcCache.entrySet().stream()
                .filter(e -> e.getValue().getType() == type)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String id : toRemove) {
            NpcData data = npcCache.remove(id);
            if (data != null && data.getEntityUuid() != null) {
                entityToNpcId.remove(data.getEntityUuid());
                Entity entity = Bukkit.getEntity(data.getEntityUuid());
                if (entity != null) {
                    entity.remove();
                    removed++;
                }
            }
        }

        // 태그로 추가 검색하여 제거
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (NpcTagUtil.isNpcType(entity, type)) {
                    entity.remove();
                    removed++;
                }
            }
        }

        saveNpcsConfig();
        plugin.debug("WildCore NPC(" + type.getDisplayName() + ") " + removed + "개 제거됨");
    }

    /**
     * NPC 생성 (새 API)
     * 
     * @param type        NPC 타입
     * @param location    소환 위치
     * @param displayName 표시 이름 (null이면 기본값)
     * @param targetId    타겟 ID (상점 ID 등, 없으면 null)
     * @param entityType  엔티티 타입
     * @param creator     생성자 (null 가능)
     * @return 생성된 NPC 데이터
     */
    public NpcData createNpc(NpcType type, Location location, String displayName,
            String targetId, EntityType entityType, Player creator) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("NPC 소환 실패: 유효하지 않은 위치");
            return null;
        }

        String id = NpcData.createNewId();
        if (displayName == null || displayName.isEmpty()) {
            displayName = type.getDefaultNpcName();
        }
        if (entityType == null) {
            entityType = EntityType.VILLAGER;
        }

        NpcData data = new NpcData(id, type, targetId, location, displayName, entityType,
                creator != null ? creator.getUniqueId() : null);

        // 엔티티 소환
        UUID entityUuid = spawnNpcEntity(data);
        if (entityUuid == null) {
            return null;
        }

        data.setEntityUuid(entityUuid);
        npcCache.put(id, data);
        entityToNpcId.put(entityUuid, id);

        saveNpcsConfig();
        plugin.debug("NPC 생성됨: " + type.getDisplayName() + " (ID: " + id + ")");

        return data;
    }

    /**
     * NPC 소환 (레거시 API 호환)
     */
    public UUID spawnNpc(NpcType type, Location location, String displayName,
            String targetId, boolean useArmorStand) {
        EntityType entityType = useArmorStand ? EntityType.ARMOR_STAND : EntityType.VILLAGER;
        NpcData data = createNpc(type, location, displayName, targetId, entityType, null);
        return data != null ? data.getEntityUuid() : null;
    }

    /**
     * 실제 엔티티 소환
     */
    private UUID spawnNpcEntity(NpcData data) {
        Location location = data.getLocation();
        if (location == null || location.getWorld() == null) {
            return null;
        }

        Entity npc;
        EntityType entityType = data.getEntityType();
        if (entityType == null)
            entityType = EntityType.VILLAGER;

        if (entityType == EntityType.ARMOR_STAND) {
            npc = location.getWorld().spawn(location, ArmorStand.class, armorStand -> {
                armorStand.setVisible(true);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(true);
                armorStand.setSilent(true);
                armorStand.setCustomNameVisible(true);
                armorStand.customName(ItemUtil.parse(data.getDisplayName()));
                armorStand.setMarker(false);
                armorStand.setBasePlate(false);
                armorStand.setArms(true);
            });
        } else {
            // 기본: Villager
            npc = location.getWorld().spawn(location, Villager.class, villager -> {
                villager.setAI(false);
                villager.setInvulnerable(true);
                villager.setSilent(true);
                villager.setCollidable(false);
                villager.setCustomNameVisible(true);
                villager.customName(ItemUtil.parse(data.getDisplayName()));
                villager.setProfession(getProfessionForType(data.getType()));
                villager.setVillagerType(Villager.Type.PLAINS);
            });
        }

        // PDC 태그 설정
        NpcTagUtil.setNpcTag(npc, data.getType(), data.getTargetId());

        return npc.getUniqueId();
    }

    /**
     * NPC 타입별 Villager 직업
     */
    private Villager.Profession getProfessionForType(NpcType type) {
        return switch (type) {
            case SHOP -> Villager.Profession.NITWIT;
            case ENCHANT -> Villager.Profession.LIBRARIAN;
            case STOCK -> Villager.Profession.CLERIC;
            case BANK -> Villager.Profession.CARTOGRAPHER;
            case WARP -> Villager.Profession.NITWIT;
        };
    }

    /**
     * NPC 제거
     * 
     * @param id NPC ID
     * @return 제거 성공 여부
     */
    public boolean removeNpc(String id) {
        NpcData data = npcCache.remove(id);
        if (data == null) {
            return false;
        }

        if (data.getEntityUuid() != null) {
            entityToNpcId.remove(data.getEntityUuid());
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null) {
                entity.remove();
            }
        }

        saveNpcsConfig();
        return true;
    }

    /**
     * UUID로 NPC 제거 (레거시 호환)
     */
    public boolean removeNpc(UUID uuid) {
        if (uuid == null)
            return false;

        String id = entityToNpcId.get(uuid);
        if (id != null) {
            return removeNpc(id);
        }

        // 캐시에 없으면 엔티티만 제거
        Entity entity = Bukkit.getEntity(uuid);
        if (entity != null) {
            entity.remove();
            return true;
        }
        return false;
    }

    // =====================
    // 조회 메서드
    // =====================

    /**
     * ID로 NPC 데이터 가져오기
     */
    public NpcData getNpcById(String id) {
        return npcCache.get(id);
    }

    /**
     * UUID로 NPC 데이터 가져오기
     */
    public NpcData getNpcData(UUID uuid) {
        String id = entityToNpcId.get(uuid);
        return id != null ? npcCache.get(id) : null;
    }

    /**
     * 엔티티로 NPC 데이터 가져오기
     */
    public NpcData getNpcData(Entity entity) {
        if (entity == null)
            return null;

        NpcData cached = getNpcData(entity.getUniqueId());
        if (cached != null) {
            return cached;
        }

        // 캐시에 없으면 PDC에서 읽기 시도
        NpcType type = NpcTagUtil.getNpcType(entity);
        if (type != null) {
            String targetId = NpcTagUtil.getTargetId(entity);
            NpcData data = new NpcData(entity.getUniqueId(), type, targetId,
                    entity.getLocation(), null);
            return data;
        }

        return null;
    }

    /**
     * 모든 캐시된 NPC 목록 반환
     */
    public Map<String, NpcData> getAllNpcs() {
        return new HashMap<>(npcCache);
    }

    /**
     * 모든 NPC를 리스트로 반환 (정렬됨)
     */
    public List<NpcData> getAllNpcsList() {
        return npcCache.values().stream()
                .sorted(Comparator.comparing(d -> d.getType().ordinal()))
                .collect(Collectors.toList());
    }

    /**
     * 특정 타입의 NPC 목록 반환
     */
    public List<NpcData> getNpcsByType(NpcType type) {
        return npcCache.values().stream()
                .filter(d -> d.getType() == type)
                .collect(Collectors.toList());
    }

    /**
     * NPC 이름 변경
     */
    public boolean renameNpc(String id, String newName) {
        NpcData data = npcCache.get(id);
        if (data == null)
            return false;

        data.setDisplayName(newName);

        // 엔티티 이름도 변경
        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null) {
                entity.customName(ItemUtil.parse(newName));
            }
        }

        saveNpcsConfig();
        return true;
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
        npcCache.put(data.getId(), data);
        entityToNpcId.put(uuid, data.getId());
    }

    /**
     * 캐시에서 NPC 해제
     */
    public void unregisterNpc(UUID uuid) {
        String id = entityToNpcId.remove(uuid);
        if (id != null) {
            npcCache.remove(id);
        }
    }

    /**
     * 총 NPC 수
     */
    public int getNpcCount() {
        return npcCache.size();
    }

    /**
     * 타입별 NPC 수
     */
    public int getNpcCount(NpcType type) {
        return (int) npcCache.values().stream()
                .filter(d -> d.getType() == type)
                .count();
    }

    /**
     * 리로드
     */
    public void reload() {
        removeAllTaggedNpcs();
        loadNpcsConfig();
        respawnAllNpcs();
    }
}
