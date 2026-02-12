package com.myserver.wildcore.util;

import com.myserver.wildcore.WildCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.Player;

import java.util.Set;

/**
 * 사유지 생성 시 울타리와 문을 설치하는 유틸리티 클래스입니다.
 * SKIP, REPLACE, FORCE 3가지 모드를 지원합니다.
 */
public class StructureBuilder {

    private final WildCore plugin;

    // 블랙리스트 캐시 (성능 최적화)
    private Set<Material> blacklistCache;
    private Set<Material> clearableCache;

    public StructureBuilder(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 사유지 테두리에 울타리를 설치합니다.
     * 설정에 따라 SKIP, REPLACE, FORCE 모드로 동작합니다.
     *
     * @param center        중심 좌표
     * @param radius        반지름
     * @param fenceMaterial 울타리 재질
     * @param gateMaterial  문 재질
     * @param floorMaterial 바닥 블록 재질 (경계 표시용)
     * @param player        플레이어 (문 방향 설정용)
     */
    public void buildFences(Location center, int radius, Material fenceMaterial,
            Material gateMaterial, Material floorMaterial, Player player) {

        String blockMode = plugin.getConfigManager().getClaimFenceBlockMode();

        if ("FORCE".equalsIgnoreCase(blockMode)) {
            buildForcedFences(center, radius, fenceMaterial, gateMaterial, floorMaterial, player);
        } else {
            buildNormalFences(center, radius, fenceMaterial, gateMaterial, floorMaterial, player, blockMode);
        }

        // 내부 청소 (옵션)
        if (plugin.getConfigManager().isClaimClearInside()) {
            clearInsideArea(center, radius);
        }
    }

    /**
     * FORCE 모드: 고정 Y좌표에 강제로 울타리를 설치합니다.
     */
    private void buildForcedFences(Location center, int radius, Material fenceMaterial,
            Material gateMaterial, Material floorMaterial, Player player) {

        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int fixedY = center.getBlockY(); // 고정 Y좌표

        int minX = cx - radius;
        int maxX = cx + radius;
        int minZ = cz - radius;
        int maxZ = cz + radius;

        boolean autoFloor = plugin.getConfigManager().isClaimAutoFloor() && floorMaterial != null;
        boolean fillBottom = plugin.getConfigManager().isClaimFillBottom();
        Material fillMaterial = plugin.getConfigManager().getClaimFillMaterial();

        // 블랙리스트 로드
        loadBlacklist();

        // 북쪽 라인 (Z = minZ)
        for (int x = minX; x <= maxX; x++) {
            setFenceForced(world, x, minZ, fixedY, fenceMaterial, autoFloor, floorMaterial, fillBottom, fillMaterial);
        }

        // 남쪽 라인 (Z = maxZ)
        for (int x = minX; x <= maxX; x++) {
            setFenceForced(world, x, maxZ, fixedY, fenceMaterial, autoFloor, floorMaterial, fillBottom, fillMaterial);
        }

        // 서쪽 라인 (X = minX)
        for (int z = minZ + 1; z < maxZ; z++) {
            setFenceForced(world, minX, z, fixedY, fenceMaterial, autoFloor, floorMaterial, fillBottom, fillMaterial);
        }

        // 동쪽 라인 (X = maxX)
        for (int z = minZ + 1; z < maxZ; z++) {
            setFenceForced(world, maxX, z, fixedY, fenceMaterial, autoFloor, floorMaterial, fillBottom, fillMaterial);
        }

        // 문 설치
        if (gateMaterial != null) {
            placeGateForced(world, center, radius, fixedY, gateMaterial, floorMaterial, player, autoFloor, fillBottom,
                    fillMaterial);
        }
    }

    /**
     * 강제 모드에서 울타리 설치
     */
    private void setFenceForced(World world, int x, int z, int y, Material fenceMaterial,
            boolean autoFloor, Material floorMaterial, boolean fillBottom, Material fillMaterial) {

        Block targetBlock = world.getBlockAt(x, y, z);

        // 블랙리스트 체크
        if (blacklistCache != null && blacklistCache.contains(targetBlock.getType())) {
            plugin.debug("Skipped blacklisted block at " + x + "," + y + "," + z + ": " + targetBlock.getType());
            return;
        }

        // 울타리 설치
        targetBlock.setType(fenceMaterial);

        // 바닥 블록 설치 (경계 표시용)
        if (autoFloor) {
            Block floorBlock = world.getBlockAt(x, y - 1, z);
            if (!blacklistCache.contains(floorBlock.getType())) {
                floorBlock.setType(floorMaterial);
            }
        }

        // 바닥 채우기 (Fill Down)
        if (fillBottom) {
            fillDownColumn(world, x, z, y - 1, fillMaterial);
        }
    }

    /**
     * 울타리 아래가 비어있을 경우 바닥까지 기둥을 채웁니다.
     */
    private void fillDownColumn(World world, int x, int z, int startY, Material fillMaterial) {
        for (int y = startY; y >= world.getMinHeight(); y--) {
            Block block = world.getBlockAt(x, y, z);

            // 이미 고체 블록이면 중단
            if (block.getType().isSolid()) {
                break;
            }

            // 블랙리스트 체크
            if (blacklistCache != null && blacklistCache.contains(block.getType())) {
                break;
            }

            // 빈 공간이면 채움
            block.setType(fillMaterial);
        }
    }

    /**
     * FORCE 모드에서 문 설치
     */
    private void placeGateForced(World world, Location center, int radius, int y,
            Material gateMaterial, Material floorMaterial, Player player,
            boolean autoFloor, boolean fillBottom, Material fillMaterial) {

        float yaw = player.getLocation().getYaw();
        BlockFace facing = getCardinalDirection(yaw);

        int gateX, gateZ;
        BlockFace gateFacing;

        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        switch (facing) {
            case NORTH:
                gateX = cx;
                gateZ = cz - radius;
                gateFacing = BlockFace.SOUTH;
                break;
            case SOUTH:
                gateX = cx;
                gateZ = cz + radius;
                gateFacing = BlockFace.NORTH;
                break;
            case EAST:
                gateX = cx + radius;
                gateZ = cz;
                gateFacing = BlockFace.WEST;
                break;
            case WEST:
                gateX = cx - radius;
                gateZ = cz;
                gateFacing = BlockFace.EAST;
                break;
            default:
                gateX = cx;
                gateZ = cz + radius;
                gateFacing = BlockFace.NORTH;
        }

        Block gateBlock = world.getBlockAt(gateX, y, gateZ);

        // 블랙리스트 체크
        if (blacklistCache != null && blacklistCache.contains(gateBlock.getType())) {
            return;
        }

        gateBlock.setType(gateMaterial);

        // 문 방향 설정
        if (gateBlock.getBlockData() instanceof Gate gate) {
            gate.setFacing(gateFacing);
            gateBlock.setBlockData(gate);
        }

        // 바닥 블록 설치
        if (autoFloor && floorMaterial != null) {
            Block floorBlock = world.getBlockAt(gateX, y - 1, gateZ);
            if (!blacklistCache.contains(floorBlock.getType())) {
                floorBlock.setType(floorMaterial);
            }
        }

        // 바닥 채우기
        if (fillBottom) {
            fillDownColumn(world, gateX, gateZ, y - 1, fillMaterial);
        }
    }

    /**
     * SKIP/REPLACE 모드: 기존 로직 (지형을 따라감)
     */
    private void buildNormalFences(Location center, int radius, Material fenceMaterial,
            Material gateMaterial, Material floorMaterial, Player player, String blockMode) {

        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int centerY = center.getBlockY();

        int minX = cx - radius;
        int maxX = cx + radius;
        int minZ = cz - radius;
        int maxZ = cz + radius;

        int maxHeightDiff = plugin.getConfigManager().getClaimMaxFenceHeightDiff();
        boolean replaceBlocks = "REPLACE".equalsIgnoreCase(blockMode);
        boolean autoFloor = plugin.getConfigManager().isClaimAutoFloor() && floorMaterial != null;

        // 블랙리스트 로드
        loadBlacklist();

        // 북쪽 라인 (Z = minZ)
        for (int x = minX; x <= maxX; x++) {
            int y = setFenceNormal(world, x, minZ, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, x, minZ, y - 1, floorMaterial);
            }
        }

        // 남쪽 라인 (Z = maxZ)
        for (int x = minX; x <= maxX; x++) {
            int y = setFenceNormal(world, x, maxZ, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, x, maxZ, y - 1, floorMaterial);
            }
        }

        // 서쪽 라인 (X = minX)
        for (int z = minZ + 1; z < maxZ; z++) {
            int y = setFenceNormal(world, minX, z, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, minX, z, y - 1, floorMaterial);
            }
        }

        // 동쪽 라인 (X = maxX)
        for (int z = minZ + 1; z < maxZ; z++) {
            int y = setFenceNormal(world, maxX, z, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, maxX, z, y - 1, floorMaterial);
            }
        }

        // 문 설치
        if (gateMaterial != null) {
            placeGateNormal(world, center, radius, centerY, maxHeightDiff, gateMaterial, floorMaterial, player,
                    replaceBlocks, autoFloor);
        }
    }

    /**
     * SKIP/REPLACE 모드에서 울타리 설치
     */
    private int setFenceNormal(World world, int x, int z, int centerY, int maxHeightDiff,
            Material material, boolean replaceBlocks) {

        Block highest = world.getHighestBlockAt(x, z);
        int targetY = highest.getY() + 1;

        // 높이 차이가 너무 크면 건너뜀
        if (Math.abs(targetY - centerY) > maxHeightDiff) {
            return -1;
        }

        Block targetBlock = world.getBlockAt(x, targetY, z);

        // 블랙리스트 체크
        if (blacklistCache != null && blacklistCache.contains(targetBlock.getType())) {
            return -1;
        }

        // 기존 블록이 공기가 아니면 설정에 따라 처리
        if (!targetBlock.getType().isAir()) {
            if (!replaceBlocks) {
                return -1; // SKIP 모드: 건너뜀
            }
        }

        targetBlock.setType(material);
        return targetY;
    }

    /**
     * 바닥 블록 설치 (경계 표시용)
     */
    private void setFloorAt(World world, int x, int z, int y, Material material) {
        Block block = world.getBlockAt(x, y, z);

        // 블랙리스트 체크
        if (blacklistCache != null && blacklistCache.contains(block.getType())) {
            return;
        }

        // 기존 블록이 고체 블록인 경우에만 교체
        if (block.getType().isSolid()) {
            block.setType(material);
        }
    }

    /**
     * SKIP/REPLACE 모드에서 문 설치
     */
    private void placeGateNormal(World world, Location center, int radius, int centerY,
            int maxHeightDiff, Material gateMaterial, Material floorMaterial,
            Player player, boolean replaceBlocks, boolean autoFloor) {

        float yaw = player.getLocation().getYaw();
        BlockFace facing = getCardinalDirection(yaw);

        int gateX, gateZ;
        BlockFace gateFacing;

        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        switch (facing) {
            case NORTH:
                gateX = cx;
                gateZ = cz - radius;
                gateFacing = BlockFace.SOUTH;
                break;
            case SOUTH:
                gateX = cx;
                gateZ = cz + radius;
                gateFacing = BlockFace.NORTH;
                break;
            case EAST:
                gateX = cx + radius;
                gateZ = cz;
                gateFacing = BlockFace.WEST;
                break;
            case WEST:
                gateX = cx - radius;
                gateZ = cz;
                gateFacing = BlockFace.EAST;
                break;
            default:
                gateX = cx;
                gateZ = cz + radius;
                gateFacing = BlockFace.NORTH;
        }

        Block highest = world.getHighestBlockAt(gateX, gateZ);
        int targetY = highest.getY() + 1;

        if (Math.abs(targetY - centerY) > maxHeightDiff) {
            return;
        }

        Block gateBlock = world.getBlockAt(gateX, targetY, gateZ);

        // 블랙리스트 체크
        if (blacklistCache != null && blacklistCache.contains(gateBlock.getType())) {
            return;
        }

        if (!gateBlock.getType().isAir() && !replaceBlocks) {
            return;
        }

        gateBlock.setType(gateMaterial);

        // 문 방향 설정
        if (gateBlock.getBlockData() instanceof Gate gate) {
            gate.setFacing(gateFacing);
            gateBlock.setBlockData(gate);
        }

        // 바닥 블록 설치
        if (autoFloor && floorMaterial != null) {
            setFloorAt(world, gateX, gateZ, targetY - 1, floorMaterial);
        }
    }

    /**
     * 사유지 내부의 잡초, 꽃 등을 제거합니다.
     */
    public void clearInsideArea(Location center, int radius) {
        World world = center.getWorld();
        if (world == null)
            return;

        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int baseY = center.getBlockY();

        int minX = cx - radius + 1;
        int maxX = cx + radius - 1;
        int minZ = cz - radius + 1;
        int maxZ = cz + radius - 1;

        // 제거할 블록 목록 로드
        loadClearableBlocks();

        if (clearableCache == null || clearableCache.isEmpty()) {
            return;
        }

        // 내부 영역 순회 (Y축은 기준점 주변 ±3 범위)
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = baseY - 3; y <= baseY + 5; y++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (clearableCache.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }

        plugin.debug("Cleared inside area of claim at " + center);
    }

    /**
     * 블랙리스트 로드
     */
    private void loadBlacklist() {
        if (blacklistCache == null) {
            blacklistCache = plugin.getConfigManager().getClaimBlockBlacklist();
        }
    }

    /**
     * 제거 가능 블록 목록 로드
     */
    private void loadClearableBlocks() {
        if (clearableCache == null) {
            clearableCache = plugin.getConfigManager().getClaimClearableBlocks();
        }
    }

    /**
     * 캐시 초기화 (리로드 시 호출)
     */
    public void clearCache() {
        blacklistCache = null;
        clearableCache = null;
    }

    /**
     * yaw 값을 기본 방향(동서남북)으로 변환합니다.
     */
    private BlockFace getCardinalDirection(float yaw) {
        yaw = (yaw % 360 + 360) % 360;

        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }
}
