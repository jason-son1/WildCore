package com.myserver.wildcore.config;

import org.bukkit.Material;

/**
 * 클레임 생성 아이템의 설정 데이터 클래스
 * items.yml의 claim 섹션 설정을 저장합니다.
 */
public class ClaimItemConfig {

    private final String itemId; // items.yml 키
    private final int radius; // 사유지 반지름
    private final Material fenceMaterial; // 울타리 재질
    private final Material gateMaterial; // 문 재질
    private final Material floorMaterial; // 바닥 블록 재질 (경계 표시용)

    public ClaimItemConfig(String itemId, int radius, Material fenceMaterial, Material gateMaterial,
            Material floorMaterial) {
        this.itemId = itemId;
        this.radius = radius;
        this.fenceMaterial = fenceMaterial;
        this.gateMaterial = gateMaterial;
        this.floorMaterial = floorMaterial;
    }

    public String getItemId() {
        return itemId;
    }

    public int getRadius() {
        return radius;
    }

    /**
     * 사유지 지름 (반지름 * 2 + 1)
     */
    public int getDiameter() {
        return radius * 2 + 1;
    }

    public Material getFenceMaterial() {
        return fenceMaterial;
    }

    public Material getGateMaterial() {
        return gateMaterial;
    }

    public Material getFloorMaterial() {
        return floorMaterial;
    }
}
