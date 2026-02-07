package com.myserver.wildcore.util;

import com.myserver.wildcore.WildCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Gate;
import org.bukkit.entity.Player;

/**
 * 사유지 생성 시 울타리와 문을 설치하는 유틸리티 클래스입니다.
 */
public class StructureBuilder {

    private final WildCore plugin;

    public StructureBuilder(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 사유지 테두리에 울타리를 설치합니다.
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
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cz = center.getBlockZ();
        int centerY = center.getBlockY();

        int minX = cx - radius;
        int maxX = cx + radius;
        int minZ = cz - radius;
        int maxZ = cz + radius;

        int maxHeightDiff = plugin.getConfigManager().getClaimMaxFenceHeightDiff();
        String blockMode = plugin.getConfigManager().getClaimFenceBlockMode();
        boolean replaceBlocks = "REPLACE".equalsIgnoreCase(blockMode);
        boolean autoFloor = plugin.getConfigManager().isClaimAutoFloor() && floorMaterial != null;

        // 북쪽 라인 (Z = minZ)
        for (int x = minX; x <= maxX; x++) {
            int y = setFenceAt(world, x, minZ, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, x, minZ, y - 1, floorMaterial);
            }
        }

        // 남쪽 라인 (Z = maxZ)
        for (int x = minX; x <= maxX; x++) {
            int y = setFenceAt(world, x, maxZ, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, x, maxZ, y - 1, floorMaterial);
            }
        }

        // 서쪽 라인 (X = minX)
        for (int z = minZ + 1; z < maxZ; z++) {
            int y = setFenceAt(world, minX, z, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, minX, z, y - 1, floorMaterial);
            }
        }

        // 동쪽 라인 (X = maxX)
        for (int z = minZ + 1; z < maxZ; z++) {
            int y = setFenceAt(world, maxX, z, centerY, maxHeightDiff, fenceMaterial, replaceBlocks);
            if (autoFloor && y > 0) {
                setFloorAt(world, maxX, z, y - 1, floorMaterial);
            }
        }

        // 문 설치 (플레이어 바라보는 방향 기준)
        if (gateMaterial != null) {
            placeGate(world, center, radius, centerY, maxHeightDiff, gateMaterial, floorMaterial, player, replaceBlocks,
                    autoFloor);
        }
    }

    /**
     * 지정된 위치에 울타리를 설치합니다.
     * 
     * @return 설치된 울타리의 Y좌표, 설치 실패 시 -1
     */
    private int setFenceAt(World world, int x, int z, int centerY, int maxHeightDiff,
            Material material, boolean replaceBlocks) {
        Block highest = world.getHighestBlockAt(x, z);
        int targetY = highest.getY() + 1;

        // 높이 차이가 너무 크면 건너뜀
        if (Math.abs(targetY - centerY) > maxHeightDiff) {
            return -1;
        }

        Block targetBlock = world.getBlockAt(x, targetY, z);

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
     * 지정된 위치에 바닥 블록을 설치합니다 (경계 표시용).
     */
    private void setFloorAt(World world, int x, int z, int y, Material material) {
        Block block = world.getBlockAt(x, y, z);
        // 기존 블록이 고체 블록인 경우에만 교체
        if (block.getType().isSolid()) {
            block.setType(material);
        }
    }

    /**
     * 플레이어 방향에 따라 문을 설치합니다.
     */
    private void placeGate(World world, Location center, int radius, int centerY,
            int maxHeightDiff, Material gateMaterial, Material floorMaterial,
            Player player, boolean replaceBlocks, boolean autoFloor) {
        // 플레이어가 바라보는 방향 계산
        float yaw = player.getLocation().getYaw();
        BlockFace facing = getCardinalDirection(yaw);

        int gateX, gateZ;
        BlockFace gateFacing;

        int cx = center.getBlockX();
        int cz = center.getBlockZ();

        // 방향에 따라 문 위치 결정
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

        if (!gateBlock.getType().isAir() && !replaceBlocks) {
            return;
        }

        gateBlock.setType(gateMaterial);

        // 문 방향 설정
        if (gateBlock.getBlockData() instanceof Gate gate) {
            gate.setFacing(gateFacing);
            gateBlock.setBlockData(gate);
        }

        // 문 아래에도 바닥 블록 설치
        if (autoFloor && floorMaterial != null) {
            setFloorAt(world, gateX, gateZ, targetY - 1, floorMaterial);
        }
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
