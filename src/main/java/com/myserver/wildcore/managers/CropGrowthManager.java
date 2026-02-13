package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 사유지별 작물 성장 속도 버프를 관리합니다.
 * 아이템 사용으로 활성화되며, 설정된 시간 동안 작물 성장 속도를 증가시킵니다.
 * 단계별(tier) 버프를 지원합니다.
 */
public class CropGrowthManager {

    private final WildCore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // claimId -> BuffData
    private final Map<Long, BuffData> activeBuffs = new HashMap<>();
    private BukkitTask expirationTask;

    public CropGrowthManager(WildCore plugin) {
        this.plugin = plugin;
        loadData();
        startExpirationTask();
    }

    /**
     * 버프 데이터 클래스 (단계 포함)
     */
    public static class BuffData {
        private final int tier;
        private final double multiplier;
        private final long expireTime; // epoch millis
        private final String tierName;

        public BuffData(int tier, String tierName, double multiplier, long expireTime) {
            this.tier = tier;
            this.tierName = tierName;
            this.multiplier = multiplier;
            this.expireTime = expireTime;
        }

        public int getTier() {
            return tier;
        }

        public String getTierName() {
            return tierName;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public long getExpireTime() {
            return expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expireTime;
        }

        public long getRemainingSeconds() {
            long remaining = (expireTime - System.currentTimeMillis()) / 1000;
            return Math.max(0, remaining);
        }
    }

    /**
     * 데이터 파일 로드
     */
    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "crop_buffs.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create crop_buffs.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        activeBuffs.clear();
        ConfigurationSection section = dataConfig.getConfigurationSection("buffs");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                try {
                    long claimId = Long.parseLong(key);
                    int tier = section.getInt(key + ".tier", 1);
                    String tierName = section.getString(key + ".tier_name", "버프 " + tier + "단계");
                    double multiplier = section.getDouble(key + ".multiplier", 2.0);
                    long expireTime = section.getLong(key + ".expire_time", 0);

                    if (System.currentTimeMillis() < expireTime) {
                        activeBuffs.put(claimId, new BuffData(tier, tierName, multiplier, expireTime));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        plugin.getLogger().info("Loaded " + activeBuffs.size() + " active crop growth buffs.");
    }

    /**
     * 데이터 저장
     */
    public void saveData() {
        dataConfig.set("buffs", null);
        for (Map.Entry<Long, BuffData> entry : activeBuffs.entrySet()) {
            if (!entry.getValue().isExpired()) {
                String path = "buffs." + entry.getKey();
                dataConfig.set(path + ".tier", entry.getValue().getTier());
                dataConfig.set(path + ".tier_name", entry.getValue().getTierName());
                dataConfig.set(path + ".multiplier", entry.getValue().getMultiplier());
                dataConfig.set(path + ".expire_time", entry.getValue().getExpireTime());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crop_buffs.yml: " + e.getMessage());
        }
    }

    /**
     * 사유지에 작물 성장 버프를 활성화합니다.
     */
    public void activateBuff(long claimId, int tier, String tierName, double multiplier, long durationSeconds) {
        long expireTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeBuffs.put(claimId, new BuffData(tier, tierName, multiplier, expireTime));
        saveData();
        plugin.debug("Activated crop growth buff for claim " + claimId
                + " (tier " + tier + ", x" + multiplier + ", " + durationSeconds + "s)");
    }

    /**
     * 사유지의 작물 성장 버프를 비활성화합니다.
     */
    public void deactivateBuff(long claimId) {
        activeBuffs.remove(claimId);
        saveData();
    }

    /**
     * 해당 사유지에 활성 버프가 있는지 확인합니다.
     */
    public boolean hasActiveBuff(long claimId) {
        BuffData data = activeBuffs.get(claimId);
        if (data == null)
            return false;
        if (data.isExpired()) {
            activeBuffs.remove(claimId);
            return false;
        }
        return true;
    }

    /**
     * 버프 배율을 가져옵니다.
     */
    public double getBuffMultiplier(long claimId) {
        BuffData data = activeBuffs.get(claimId);
        if (data == null || data.isExpired())
            return 1.0;
        return data.getMultiplier();
    }

    /**
     * 남은 버프 시간 (초)을 가져옵니다.
     */
    public long getRemainingBuffTime(long claimId) {
        BuffData data = activeBuffs.get(claimId);
        if (data == null || data.isExpired())
            return 0;
        return data.getRemainingSeconds();
    }

    /**
     * 버프 데이터를 가져옵니다.
     */
    public BuffData getBuffData(long claimId) {
        BuffData data = activeBuffs.get(claimId);
        if (data != null && data.isExpired()) {
            activeBuffs.remove(claimId);
            return null;
        }
        return data;
    }

    /**
     * 만료된 버프를 주기적으로 정리하는 태스크
     */
    private void startExpirationTask() {
        expirationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            boolean changed = false;
            Iterator<Map.Entry<Long, BuffData>> it = activeBuffs.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Long, BuffData> entry = it.next();
                if (entry.getValue().isExpired()) {
                    it.remove();
                    changed = true;
                    plugin.debug("Crop growth buff expired for claim " + entry.getKey());
                }
            }
            if (changed) {
                saveData();
            }
        }, 600L, 600L); // 30초 간격
    }

    /**
     * 종료 시 정리
     */
    public void shutdown() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        saveData();
    }

    /**
     * 리로드
     */
    public void reload() {
        loadData();
    }
}
