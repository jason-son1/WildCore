package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.*;

/**
 * 사유지 영역의 청크를 강제 로드(Force Load)하는 매니저입니다.
 * 사유지별로 개별 토글이 가능하며 서버 시작/종료 시 자동으로 처리됩니다.
 */
public class ClaimChunkLoader {

    private final WildCore plugin;
    private final ClaimManager claimManager;
    private final ClaimDataManager claimDataManager;

    // 현재 force-loaded된 청크 추적 (claimId -> chunk set)
    private final Map<Long, Set<long[]>> loadedChunks = new HashMap<>();

    public ClaimChunkLoader(WildCore plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
        this.claimDataManager = plugin.getClaimDataManager();
    }

    /**
     * 서버 시작 시 저장된 설정에 따라 청크를 로드합니다.
     */
    public void loadAllChunks() {
        if (!claimManager.isEnabled())
            return;

        int loadedCount = 0;
        for (Long claimId : claimDataManager.getAllClaimIds()) {
            ClaimDataManager.ClaimMetadata metadata = claimDataManager.getClaimData(claimId);
            if (metadata != null && metadata.isChunkLoaded()) {
                Claim claim = claimManager.getClaimById(claimId);
                if (claim != null) {
                    forceLoadClaimChunks(claimId, claim);
                    loadedCount++;
                }
            }
        }

        if (loadedCount > 0) {
            plugin.getLogger().info("Force-loaded chunks for " + loadedCount + " claims.");
        }
    }

    /**
     * 사유지의 청크 로드를 토글합니다.
     */
    public void toggleChunkLoading(long claimId, boolean enabled) {
        Claim claim = claimManager.getClaimById(claimId);
        if (claim == null)
            return;

        ClaimDataManager.ClaimMetadata metadata = claimDataManager.getClaimData(claimId);
        if (metadata == null)
            return;

        metadata.setChunkLoaded(enabled);
        claimDataManager.saveClaimData(metadata);

        if (enabled) {
            forceLoadClaimChunks(claimId, claim);
        } else {
            unloadClaimChunks(claimId, claim);
        }

        plugin.debug("Chunk loading " + (enabled ? "enabled" : "disabled") + " for claim " + claimId);
    }

    /**
     * 사유지의 청크 로드 상태를 확인합니다.
     */
    public boolean isChunkLoadingEnabled(long claimId) {
        ClaimDataManager.ClaimMetadata metadata = claimDataManager.getClaimData(claimId);
        return metadata != null && metadata.isChunkLoaded();
    }

    /**
     * 사유지 영역에 해당하는 모든 청크를 force-load합니다.
     */
    private void forceLoadClaimChunks(long claimId, Claim claim) {
        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();
        if (lesser == null || greater == null)
            return;

        World world = lesser.getWorld();
        if (world == null)
            return;

        int minChunkX = lesser.getBlockX() >> 4;
        int maxChunkX = greater.getBlockX() >> 4;
        int minChunkZ = lesser.getBlockZ() >> 4;
        int maxChunkZ = greater.getBlockZ() >> 4;

        Set<long[]> chunks = new HashSet<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                chunk.setForceLoaded(true);
                chunks.add(new long[] { cx, cz });
            }
        }

        loadedChunks.put(claimId, chunks);
        plugin.debug("Force-loaded " + chunks.size() + " chunks for claim " + claimId);
    }

    /**
     * 사유지 영역의 force-load를 해제합니다.
     */
    private void unloadClaimChunks(long claimId, Claim claim) {
        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();
        if (lesser == null || greater == null)
            return;

        World world = lesser.getWorld();
        if (world == null)
            return;

        int minChunkX = lesser.getBlockX() >> 4;
        int maxChunkX = greater.getBlockX() >> 4;
        int minChunkZ = lesser.getBlockZ() >> 4;
        int maxChunkZ = greater.getBlockZ() >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                chunk.setForceLoaded(false);
            }
        }

        loadedChunks.remove(claimId);
        plugin.debug("Unloaded chunks for claim " + claimId);
    }

    /**
     * 모든 force-loaded 청크를 해제합니다 (서버 종료 시).
     */
    public void unloadAllChunks() {
        if (!claimManager.isEnabled())
            return;

        for (Long claimId : new ArrayList<>(loadedChunks.keySet())) {
            Claim claim = claimManager.getClaimById(claimId);
            if (claim != null) {
                unloadClaimChunks(claimId, claim);
            }
        }
        loadedChunks.clear();
    }
}
