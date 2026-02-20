package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 사유지별 작물 좌표를 메모리에 캐싱하여 추적하는 클래스입니다.
 * 전체 블록 스캔(Brute-Force) 대신, 작물이 심어지거나 파괴될 때만
 * 좌표를 등록/해제하여 성능을 최적화합니다.
 */
public class CropTracker {

    private final WildCore plugin;

    // claimId -> 해당 사유지 내 작물 좌표 집합
    private final Map<Long, Set<BlockPos>> trackedCrops = new ConcurrentHashMap<>();

    // 작물 블록으로 인식되는 Material 목록
    private static final Set<Material> CROP_MATERIALS = EnumSet.of(
            Material.WHEAT,
            Material.CARROTS,
            Material.POTATOES,
            Material.BEETROOTS,
            Material.NETHER_WART,
            Material.MELON_STEM,
            Material.PUMPKIN_STEM,
            Material.COCOA,
            Material.SWEET_BERRY_BUSH);

    public CropTracker(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 경량 좌표 클래스 (World UUID + x,y,z)
     */
    public static class BlockPos {
        private final UUID worldId;
        private final int x, y, z;

        public BlockPos(UUID worldId, int x, int y, int z) {
            this.worldId = worldId;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public BlockPos(Location loc) {
            this(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }

        public UUID getWorldId() {
            return worldId;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        /**
         * Bukkit Location으로 변환합니다.
         * 월드가 로드되지 않은 경우 null을 반환합니다.
         */
        public Location toLocation() {
            World world = org.bukkit.Bukkit.getWorld(worldId);
            if (world == null)
                return null;
            return new Location(world, x, y, z);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof BlockPos other))
                return false;
            return x == other.x && y == other.y && z == other.z
                    && Objects.equals(worldId, other.worldId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldId, x, y, z);
        }

        @Override
        public String toString() {
            return "BlockPos{" + worldId + ", " + x + ", " + y + ", " + z + "}";
        }
    }

    /**
     * 해당 블록이 추적 대상 작물인지 판별합니다.
     */
    public static boolean isCropBlock(Material material) {
        return CROP_MATERIALS.contains(material);
    }

    /**
     * 작물 좌표를 추적 목록에 추가합니다.
     */
    public void addCrop(long claimId, Location location) {
        trackedCrops.computeIfAbsent(claimId, k -> ConcurrentHashMap.newKeySet())
                .add(new BlockPos(location));
    }

    /**
     * 작물 좌표를 추적 목록에서 제거합니다.
     */
    public void removeCrop(long claimId, Location location) {
        Set<BlockPos> crops = trackedCrops.get(claimId);
        if (crops != null) {
            crops.remove(new BlockPos(location));
            if (crops.isEmpty()) {
                trackedCrops.remove(claimId);
            }
        }
    }

    /**
     * 특정 좌표를 모든 사유지에서 제거합니다.
     * (사유지 ID를 모를 때 사용)
     */
    public void removeCropFromAll(Location location) {
        BlockPos pos = new BlockPos(location);
        for (Set<BlockPos> crops : trackedCrops.values()) {
            crops.remove(pos);
        }
    }

    /**
     * 해당 사유지의 모든 추적 작물 좌표를 반환합니다.
     */
    public Set<BlockPos> getCrops(long claimId) {
        return trackedCrops.getOrDefault(claimId, Collections.emptySet());
    }

    /**
     * 해당 사유지의 추적중인 작물 수를 반환합니다.
     */
    public int getCropCount(long claimId) {
        Set<BlockPos> crops = trackedCrops.get(claimId);
        return crops != null ? crops.size() : 0;
    }

    /**
     * 해당 사유지의 모든 추적 데이터를 제거합니다.
     */
    public void clearClaim(long claimId) {
        trackedCrops.remove(claimId);
    }

    /**
     * 모든 추적 데이터를 제거합니다.
     */
    public void clearAll() {
        trackedCrops.clear();
    }

    /**
     * 현재 추적 중인 사유지 ID 목록을 반환합니다.
     */
    public Set<Long> getTrackedClaimIds() {
        return trackedCrops.keySet();
    }

    /**
     * 버프 활성화 시 사유지 범위 내 기존 작물을 일괄 등록합니다.
     * GP(GriefPrevention) claim의 경계(lesser/greater boundary)를 사용하여
     * 스캔 범위를 최소화합니다.
     *
     * @param claim   GriefPrevention Claim 객체
     * @param claimId 사유지 ID
     * @return 등록된 작물 수
     */
    public int scanAndRegister(Claim claim, long claimId) {
        // 기존 데이터 정리
        clearClaim(claimId);

        Location lesser = claim.getLesserBoundaryCorner();
        Location greater = claim.getGreaterBoundaryCorner();

        if (lesser == null || greater == null || lesser.getWorld() == null) {
            return 0;
        }

        World world = lesser.getWorld();
        int minX = lesser.getBlockX();
        int maxX = greater.getBlockX();
        int minZ = lesser.getBlockZ();
        int maxZ = greater.getBlockZ();

        // Y축 범위: 세계의 최소~최대 높이 범위 내에서 합리적 범위로 제한
        // 농작물은 보통 지표면에 있으므로 [-64, 320] 전체 대신 claim 중심 ±20으로 제한
        int centerY = (lesser.getBlockY() + greater.getBlockY()) / 2;
        int minY = Math.max(world.getMinHeight(), centerY - 20);
        int maxY = Math.min(world.getMaxHeight() - 1, centerY + 20);

        Set<BlockPos> crops = ConcurrentHashMap.newKeySet();
        int count = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (isCropBlock(block.getType())) {
                        // Ageable인 경우에만 추적 (이미 다 자란 것은 제외)
                        if (block.getBlockData() instanceof Ageable ageable) {
                            if (ageable.getAge() < ageable.getMaximumAge()) {
                                crops.add(new BlockPos(world.getUID(), x, y, z));
                                count++;
                            }
                        }
                    }
                }
            }
        }

        if (!crops.isEmpty()) {
            trackedCrops.put(claimId, crops);
        }

        plugin.debug("CropTracker: 사유지 " + claimId + " 스캔 완료 - " + count + "개 작물 등록 (범위: "
                + minX + "," + minY + "," + minZ + " ~ " + maxX + "," + maxY + "," + maxZ + ")");

        return count;
    }
}
