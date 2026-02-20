package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.tasks.CropGrowthTask;
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
 * 아이템 사용으로 활성화되며, 설정된 시간 동안 스케줄러 기반으로
 * 작물의 추가 성장을 부여합니다.
 * CropTracker(좌표 추적)와 CropGrowthTask(성장 엔진)를 통합 관리합니다.
 */
public class CropGrowthManager {

    private final WildCore plugin;
    private File dataFile;
    private FileConfiguration dataConfig;

    // claimId -> BuffData
    private final Map<Long, BuffData> activeBuffs = new HashMap<>();
    private BukkitTask expirationTask;

    // 작물 좌표 추적기
    private final CropTracker cropTracker;

    // 스케줄러 기반 성장 엔진
    private CropGrowthTask cropGrowthTask;

    public CropGrowthManager(WildCore plugin) {
        this.plugin = plugin;
        this.cropTracker = new CropTracker(plugin);
        loadData();
    }

    /**
     * 매니저 초기화 - 모든 매니저가 준비된 후 호출해야 합니다.
     * 태스크 시작 및 지연 재스캔을 수행합니다.
     */
    public void init() {
        startExpirationTask();
        startGrowthTask();

        // GriefPrevention 데이터 로드 완료를 보장하기 위해 1초 후 재스캔
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            rescanAllActiveClaims();
            plugin.getLogger().info("CropGrowthManager: 지연 재스캔 완료 - 활성 버프 " + activeBuffs.size() + "개");
        }, 20L);
    }

    /**
     * 버프 데이터 클래스 (스케줄러 기반)
     */
    public static class BuffData {
        private final int tier;
        private final String tierName;
        private final long expireTime; // epoch millis
        private final int intervalSeconds;
        private final double growthChance;
        private final int growthAmount;

        public BuffData(int tier, String tierName, long expireTime,
                int intervalSeconds, double growthChance, int growthAmount) {
            this.tier = tier;
            this.tierName = tierName;
            this.expireTime = expireTime;
            this.intervalSeconds = intervalSeconds;
            this.growthChance = growthChance;
            this.growthAmount = growthAmount;
        }

        public int getTier() {
            return tier;
        }

        public String getTierName() {
            return tierName;
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

        public int getIntervalSeconds() {
            return intervalSeconds;
        }

        public double getGrowthChance() {
            return growthChance;
        }

        public int getGrowthAmount() {
            return growthAmount;
        }
    }

    /**
     * 데이터 파일 로드
     */
    private void loadData() {
        // 데이터 폴더가 없으면 생성
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

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
                    long expireTime = section.getLong(key + ".expire_time", 0);
                    int intervalSeconds = section.getInt(key + ".interval_seconds", 20);
                    double growthChance = section.getDouble(key + ".growth_chance", 0.10);
                    int growthAmount = section.getInt(key + ".growth_amount", 1);

                    if (System.currentTimeMillis() < expireTime) {
                        activeBuffs.put(claimId, new BuffData(tier, tierName, expireTime,
                                intervalSeconds, growthChance, growthAmount));
                        plugin.debug("CropGrowthManager: 버프 로드 - claim=" + claimId
                                + " tier=" + tier + " 남은시간=" + ((expireTime - System.currentTimeMillis()) / 1000)
                                + "초");
                    } else {
                        plugin.debug("CropGrowthManager: 만료된 버프 건너뜀 - claim=" + claimId);
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
        // 데이터 폴더가 없으면 생성
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        dataConfig.set("buffs", null);
        int savedCount = 0;
        for (Map.Entry<Long, BuffData> entry : activeBuffs.entrySet()) {
            if (!entry.getValue().isExpired()) {
                String path = "buffs." + entry.getKey();
                dataConfig.set(path + ".tier", entry.getValue().getTier());
                dataConfig.set(path + ".tier_name", entry.getValue().getTierName());
                dataConfig.set(path + ".expire_time", entry.getValue().getExpireTime());
                dataConfig.set(path + ".interval_seconds", entry.getValue().getIntervalSeconds());
                dataConfig.set(path + ".growth_chance", entry.getValue().getGrowthChance());
                dataConfig.set(path + ".growth_amount", entry.getValue().getGrowthAmount());
                savedCount++;
            }
        }

        try {
            dataConfig.save(dataFile);
            plugin.debug("CropGrowthManager: 버프 데이터 저장 완료 - " + savedCount + "개 활성 버프");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save crop_buffs.yml: " + e.getMessage());
        }
    }

    /**
     * 사유지에 작물 성장 버프를 활성화합니다.
     */
    public void activateBuff(long claimId, int tier, String tierName,
            long durationSeconds, int intervalSeconds,
            double growthChance, int growthAmount) {
        long expireTime = System.currentTimeMillis() + (durationSeconds * 1000L);
        activeBuffs.put(claimId, new BuffData(tier, tierName, expireTime,
                intervalSeconds, growthChance, growthAmount));
        saveData();

        // 사유지 내 기존 작물 스캔 및 등록
        me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimById(claimId);
        if (claim != null) {
            int count = cropTracker.scanAndRegister(claim, claimId);
            plugin.debug("Activated crop growth buff for claim " + claimId
                    + " (tier " + tier + ", interval=" + intervalSeconds + "s, chance="
                    + growthChance + ", amount=" + growthAmount + ", duration=" + durationSeconds
                    + "s, crops=" + count + ")");
        }
    }

    /**
     * 사유지의 작물 성장 버프를 비활성화합니다.
     */
    public void deactivateBuff(long claimId) {
        activeBuffs.remove(claimId);
        cropTracker.clearClaim(claimId);
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
            cropTracker.clearClaim(claimId);
            return false;
        }
        return true;
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
            cropTracker.clearClaim(claimId);
            return null;
        }
        return data;
    }

    /**
     * 활성 버프 맵을 반환합니다. (CropGrowthTask에서 사용)
     */
    public Map<Long, BuffData> getActiveBuffs() {
        return activeBuffs;
    }

    /**
     * 작물 추적기를 반환합니다.
     */
    public CropTracker getCropTracker() {
        return cropTracker;
    }

    /**
     * 성장 엔진 태스크 시작
     */
    private void startGrowthTask() {
        cropGrowthTask = new CropGrowthTask(plugin, this, cropTracker);
        cropGrowthTask.start();
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
                    cropTracker.clearClaim(entry.getKey());
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
     * 모든 활성 사유지의 작물을 재스캔합니다.
     * 서버 재시작이나 리로드 시 호출됩니다.
     */
    private void rescanAllActiveClaims() {
        if (plugin.getClaimManager() == null || !plugin.getClaimManager().isEnabled())
            return;

        int totalCrops = 0;
        for (Map.Entry<Long, BuffData> entry : activeBuffs.entrySet()) {
            long claimId = entry.getKey();
            me.ryanhamshire.GriefPrevention.Claim claim = plugin.getClaimManager().getClaimById(claimId);
            if (claim != null) {
                totalCrops += cropTracker.scanAndRegister(claim, claimId);
            }
        }
        if (!activeBuffs.isEmpty()) {
            plugin.getLogger()
                    .info("Rescanned " + activeBuffs.size() + " active claims, " + totalCrops + " crops registered.");
        }
    }

    /**
     * 종료 시 정리
     */
    public void shutdown() {
        if (cropGrowthTask != null) {
            cropGrowthTask.stop();
            cropGrowthTask = null;
        }
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        cropTracker.clearAll();
        saveData();
    }

    /**
     * 리로드
     */
    public void reload() {
        // 기존 태스크 정지
        if (cropGrowthTask != null) {
            cropGrowthTask.stop();
        }
        if (expirationTask != null) {
            expirationTask.cancel();
        }
        cropTracker.clearAll();

        // 데이터 다시 로드
        loadData();
        startExpirationTask();
        startGrowthTask();

        // 리로드 시에도 지연 재스캔
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            rescanAllActiveClaims();
            plugin.getLogger().info("CropGrowthManager: 리로드 재스캔 완료 - 활성 버프 " + activeBuffs.size() + "개");
        }, 20L);
    }
}
