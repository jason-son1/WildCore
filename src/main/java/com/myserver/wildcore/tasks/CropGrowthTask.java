package com.myserver.wildcore.tasks;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.managers.CropGrowthManager;
import com.myserver.wildcore.managers.CropTracker;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 스케줄러 기반 작물 성장 엔진입니다.
 * 바닐라 랜덤 틱과 별개로, 플러그인이 주기적으로 등록된 작물 좌표를
 * 순회하며 확률적으로 추가 성장을 부여합니다.
 *
 * 틱 분산 처리(Tick Amortization): 큐 시스템으로 매 틱마다
 * 최대 maxCropsPerTick개만 처리하여 서버 부하를 분산합니다.
 */
public class CropGrowthTask {

    private final WildCore plugin;
    private final CropGrowthManager cropGrowthManager;
    private final CropTracker cropTracker;
    private BukkitTask task;

    // 처리 대기 큐: (claimId, BlockPos) 쌍
    private final Queue<CropProcessEntry> processQueue = new ConcurrentLinkedQueue<>();

    // 사유지별 마지막 처리 타임스탬프 (밀리초)
    private final Map<Long, Long> lastProcessedTime = new HashMap<>();

    private final Random random = new Random();

    public CropGrowthTask(WildCore plugin, CropGrowthManager cropGrowthManager, CropTracker cropTracker) {
        this.plugin = plugin;
        this.cropGrowthManager = cropGrowthManager;
        this.cropTracker = cropTracker;
    }

    /**
     * 스케줄러 태스크 시작
     */
    public void start() {
        int tickInterval = plugin.getConfigManager().getCropGrowthSchedulerTickInterval();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, tickInterval, tickInterval);
        plugin.debug("CropGrowthTask: 스케줄러 시작 (간격: " + tickInterval + "틱)");
    }

    /**
     * 스케줄러 태스크 중지
     */
    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        processQueue.clear();
        lastProcessedTime.clear();
        plugin.debug("CropGrowthTask: 스케줄러 중지");
    }

    /**
     * 매 틱마다 실행되는 메인 로직
     */
    private void tick() {
        // 1단계: 큐가 비었으면 interval 도달한 사유지의 작물을 큐에 추가
        if (processQueue.isEmpty()) {
            enqueueEligibleClaims();
        }

        // 2단계: 큐에서 maxCropsPerTick개만 꺼내서 처리
        int maxPerTick = plugin.getConfigManager().getCropGrowthMaxCropsPerTick();
        int processed = 0;

        while (!processQueue.isEmpty() && processed < maxPerTick) {
            CropProcessEntry entry = processQueue.poll();
            if (entry != null) {
                processEntry(entry);
                processed++;
            }
        }
    }

    /**
     * interval이 도달한 사유지들의 작물을 라운드 로빈 방식으로 큐에 추가합니다.
     */
    private void enqueueEligibleClaims() {
        long now = System.currentTimeMillis();

        // 활성 버프가 있는 모든 사유지 확인
        Map<Long, CropGrowthManager.BuffData> activeBuffs = cropGrowthManager.getActiveBuffs();

        for (Map.Entry<Long, CropGrowthManager.BuffData> entry : activeBuffs.entrySet()) {
            long claimId = entry.getKey();
            CropGrowthManager.BuffData buffData = entry.getValue();

            if (buffData.isExpired())
                continue;

            // interval 체크
            int intervalSeconds = buffData.getIntervalSeconds();
            long lastTime = lastProcessedTime.getOrDefault(claimId, 0L);
            long elapsedMs = now - lastTime;

            if (elapsedMs < intervalSeconds * 1000L)
                continue;

            // 청크 로드 여부 확인을 위해 claim 조회
            Claim claim = plugin.getClaimManager().getClaimById(claimId);
            if (claim == null)
                continue;

            // 추적 중인 작물 가져오기
            Set<CropTracker.BlockPos> crops = cropTracker.getCrops(claimId);
            if (crops.isEmpty())
                continue;

            // 라운드 로빈: 작물 좌표를 큐에 추가
            double growthChance = buffData.getGrowthChance();
            int growthAmount = buffData.getGrowthAmount();

            for (CropTracker.BlockPos pos : crops) {
                processQueue.add(new CropProcessEntry(claimId, pos, growthChance, growthAmount));
            }

            // 타임스탬프 갱신
            lastProcessedTime.put(claimId, now);
        }
    }

    /**
     * 개별 작물 좌표를 처리합니다.
     */
    private void processEntry(CropProcessEntry entry) {
        Location loc = entry.pos.toLocation();
        if (loc == null)
            return; // 월드 언로드됨

        World world = loc.getWorld();
        if (world == null)
            return;

        // 청크 로드 여부 확인 (언로드된 청크 강제 로드 방지)
        if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
            return;
        }

        Block block = world.getBlockAt(loc);

        // 여전히 작물 블록인지 확인
        if (!CropTracker.isCropBlock(block.getType())) {
            cropTracker.removeCrop(entry.claimId, loc);
            return;
        }

        // Ageable 확인
        if (!(block.getBlockData() instanceof Ageable ageable)) {
            cropTracker.removeCrop(entry.claimId, loc);
            return;
        }

        int currentAge = ageable.getAge();
        int maxAge = ageable.getMaximumAge();

        // 이미 다 자란 작물은 추적 해제
        if (currentAge >= maxAge) {
            cropTracker.removeCrop(entry.claimId, loc);
            return;
        }

        // 확률 체크
        if (random.nextDouble() >= entry.growthChance) {
            return; // 확률 실패
        }

        // 성장 적용
        int newAge = Math.min(maxAge, currentAge + entry.growthAmount);
        ageable.setAge(newAge);
        block.setBlockData(ageable);

        // 다 자란 경우 추적 해제
        if (newAge >= maxAge) {
            cropTracker.removeCrop(entry.claimId, loc);
        }

        // 파티클 효과
        if (plugin.getConfigManager().isCropGrowthShowParticles()) {
            world.spawnParticle(Particle.HAPPY_VILLAGER,
                    loc.getX() + 0.5, loc.getY() + 0.5, loc.getZ() + 0.5,
                    3, 0.3, 0.3, 0.3, 0);
        }

        plugin.debug("CropGrowthTask: 성장 적용 - claim=" + entry.claimId
                + " pos=" + entry.pos + " age=" + currentAge + "->" + newAge);
    }

    /**
     * 처리 큐 엔트리 레코드
     */
    private record CropProcessEntry(long claimId, CropTracker.BlockPos pos,
            double growthChance, int growthAmount) {
    }
}
