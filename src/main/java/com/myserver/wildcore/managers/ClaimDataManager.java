package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * WildCore 자체 Claim 메타데이터를 관리합니다.
 * GriefPrevention이 저장하지 않는 별명, 홈 위치, 플래그 등을 저장합니다.
 */
public class ClaimDataManager {

    private final WildCore plugin;
    private File claimsFile;
    private FileConfiguration claimsConfig;

    // 메모리 캐시
    private final Map<Long, ClaimMetadata> claimDataCache = new HashMap<>();

    public ClaimDataManager(WildCore plugin) {
        this.plugin = plugin;
        loadClaimsData();
    }

    /**
     * claims.yml 파일 로드
     */
    private void loadClaimsData() {
        claimsFile = new File(plugin.getDataFolder(), "claims.yml");
        if (!claimsFile.exists()) {
            try {
                claimsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create claims.yml: " + e.getMessage());
            }
        }
        claimsConfig = YamlConfiguration.loadConfiguration(claimsFile);

        // 기존 데이터 로드
        claimDataCache.clear();
        ConfigurationSection claimsSection = claimsConfig.getConfigurationSection("claims");
        if (claimsSection != null) {
            for (String claimIdStr : claimsSection.getKeys(false)) {
                try {
                    Long claimId = Long.parseLong(claimIdStr);
                    String path = "claims." + claimIdStr;

                    ClaimMetadata metadata = new ClaimMetadata(claimId);
                    metadata.setNickname(claimsConfig.getString(path + ".nickname", ""));
                    metadata.setCreatedAt(claimsConfig.getLong(path + ".created_at", System.currentTimeMillis()));
                    metadata.setIcon(claimsConfig.getString(path + ".icon", "GRASS_BLOCK"));
                    metadata.setChunkLoaded(claimsConfig.getBoolean(path + ".chunk_loaded", false));

                    // 홈 위치 로드
                    if (claimsConfig.contains(path + ".home")) {
                        String worldName = claimsConfig.getString(path + ".home.world");
                        if (worldName != null) {
                            World world = plugin.getServer().getWorld(worldName);
                            if (world != null) {
                                double x = claimsConfig.getDouble(path + ".home.x");
                                double y = claimsConfig.getDouble(path + ".home.y");
                                double z = claimsConfig.getDouble(path + ".home.z");
                                float yaw = (float) claimsConfig.getDouble(path + ".home.yaw");
                                float pitch = (float) claimsConfig.getDouble(path + ".home.pitch");
                                metadata.setHome(new Location(world, x, y, z, yaw, pitch));
                            }
                        }
                    }

                    // 플래그 로드
                    ConfigurationSection flagsSection = claimsConfig.getConfigurationSection(path + ".flags");
                    if (flagsSection != null) {
                        for (String flag : flagsSection.getKeys(false)) {
                            metadata.setFlag(flag, flagsSection.getBoolean(flag));
                        }
                    }

                    claimDataCache.put(claimId, metadata);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        plugin.getLogger().info("Loaded " + claimDataCache.size() + " claim metadata entries.");
    }

    /**
     * 저장
     */
    public void save() {
        try {
            claimsConfig.save(claimsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save claims.yml: " + e.getMessage());
        }
    }

    /**
     * Claim 메타데이터 가져오기 (없으면 새로 생성)
     */
    public ClaimMetadata getClaimData(Long claimId) {
        if (claimId == null)
            return null;
        return claimDataCache.computeIfAbsent(claimId, id -> {
            ClaimMetadata metadata = new ClaimMetadata(id);
            metadata.setCreatedAt(System.currentTimeMillis());
            return metadata;
        });
    }

    /**
     * Claim 메타데이터 저장
     */
    public void saveClaimData(ClaimMetadata metadata) {
        if (metadata == null || metadata.getClaimId() == null)
            return;

        Long claimId = metadata.getClaimId();
        String path = "claims." + claimId;

        claimsConfig.set(path + ".nickname", metadata.getNickname());
        claimsConfig.set(path + ".created_at", metadata.getCreatedAt());
        claimsConfig.set(path + ".icon", metadata.getIcon());
        claimsConfig.set(path + ".chunk_loaded", metadata.isChunkLoaded());

        // 홈 위치 저장
        Location home = metadata.getHome();
        if (home != null && home.getWorld() != null) {
            claimsConfig.set(path + ".home.world", home.getWorld().getName());
            claimsConfig.set(path + ".home.x", home.getX());
            claimsConfig.set(path + ".home.y", home.getY());
            claimsConfig.set(path + ".home.z", home.getZ());
            claimsConfig.set(path + ".home.yaw", home.getYaw());
            claimsConfig.set(path + ".home.pitch", home.getPitch());
        } else {
            claimsConfig.set(path + ".home", null);
        }

        // 플래그 저장
        claimsConfig.set(path + ".flags", null);
        for (Map.Entry<String, Boolean> entry : metadata.getFlags().entrySet()) {
            claimsConfig.set(path + ".flags." + entry.getKey(), entry.getValue());
        }

        claimDataCache.put(claimId, metadata);
        save();
    }

    /**
     * Claim 메타데이터 삭제
     */
    public void removeClaimData(Long claimId) {
        if (claimId == null)
            return;

        claimDataCache.remove(claimId);
        claimsConfig.set("claims." + claimId, null);
        save();
        plugin.debug("Removed claim metadata for claim " + claimId);
    }

    // =====================
    // 편의 메소드
    // =====================

    /**
     * 별명 설정
     */
    public void setClaimNickname(Long claimId, String nickname) {
        ClaimMetadata metadata = getClaimData(claimId);
        if (metadata != null) {
            metadata.setNickname(nickname);
            saveClaimData(metadata);
        }
    }

    /**
     * 별명 가져오기
     */
    public String getClaimNickname(Long claimId) {
        ClaimMetadata metadata = claimDataCache.get(claimId);
        return metadata != null ? metadata.getNickname() : "";
    }

    /**
     * 홈 위치 설정
     */
    public void setClaimHome(Long claimId, Location home) {
        ClaimMetadata metadata = getClaimData(claimId);
        if (metadata != null) {
            metadata.setHome(home);
            saveClaimData(metadata);
        }
    }

    /**
     * 홈 위치 가져오기
     */
    public Location getClaimHome(Long claimId) {
        ClaimMetadata metadata = claimDataCache.get(claimId);
        return metadata != null ? metadata.getHome() : null;
    }

    /**
     * 플래그 설정
     */
    public void setClaimFlag(Long claimId, String flag, boolean value) {
        ClaimMetadata metadata = getClaimData(claimId);
        if (metadata != null) {
            metadata.setFlag(flag, value);
            saveClaimData(metadata);
        }
    }

    /**
     * 플래그 가져오기
     */
    public boolean getClaimFlag(Long claimId, String flag, boolean defaultValue) {
        ClaimMetadata metadata = claimDataCache.get(claimId);
        if (metadata != null) {
            return metadata.getFlag(flag, defaultValue);
        }
        return defaultValue;
    }

    /**
     * 모든 캐시된 Claim ID 가져오기
     */
    public Set<Long> getAllClaimIds() {
        return new HashSet<>(claimDataCache.keySet());
    }

    /**
     * 리로드
     */
    public void reload() {
        loadClaimsData();
    }

    // =====================
    // 내부 데이터 클래스
    // =====================

    /**
     * Claim 메타데이터 클래스
     */
    public static class ClaimMetadata {
        private final Long claimId;
        private String nickname;
        private Location home;
        private long createdAt;
        private String icon;
        private boolean chunkLoaded;
        private final Map<String, Boolean> flags = new HashMap<>();

        public ClaimMetadata(Long claimId) {
            this.claimId = claimId;
            this.nickname = "";
            this.createdAt = System.currentTimeMillis();
            this.icon = "GRASS_BLOCK";
            this.chunkLoaded = false;
        }

        public Long getClaimId() {
            return claimId;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname != null ? nickname : "";
        }

        public Location getHome() {
            return home;
        }

        public void setHome(Location home) {
            this.home = home;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public String getIcon() {
            return icon;
        }

        public void setIcon(String icon) {
            this.icon = icon != null ? icon : "GRASS_BLOCK";
        }

        public boolean isChunkLoaded() {
            return chunkLoaded;
        }

        public void setChunkLoaded(boolean chunkLoaded) {
            this.chunkLoaded = chunkLoaded;
        }

        public Map<String, Boolean> getFlags() {
            return flags;
        }

        public boolean getFlag(String flag, boolean defaultValue) {
            return flags.getOrDefault(flag, defaultValue);
        }

        public void setFlag(String flag, boolean value) {
            flags.put(flag, value);
        }
    }
}
