package com.myserver.wildcore.config;

import java.util.List;
import java.util.Set;

/**
 * 수리 옵션 설정 클래스
 */
public class RepairConfig {

    private final String id;
    private final String displayName;
    private final String material;
    private final double repairPercentage;
    private final double costMoney;
    private final List<String> costItems;
    private final List<String> targetGroups;
    private final Set<String> targetWhitelist;
    private final List<String> lore;

    public RepairConfig(String id, String displayName, String material,
            double repairPercentage, double costMoney,
            List<String> costItems, List<String> targetGroups,
            Set<String> targetWhitelist, List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.repairPercentage = repairPercentage;
        this.costMoney = costMoney;
        this.costItems = costItems;
        this.targetGroups = targetGroups;
        this.targetWhitelist = targetWhitelist;
        this.lore = lore;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    /**
     * 수리 비율 (0.0~1.0)
     * 예: 0.25 = 25% 수리, 1.0 = 100% 수리
     */
    public double getRepairPercentage() {
        return repairPercentage;
    }

    public double getCostMoney() {
        return costMoney;
    }

    public List<String> getCostItems() {
        return costItems;
    }

    public List<String> getTargetGroups() {
        return targetGroups;
    }

    public Set<String> getTargetWhitelist() {
        return targetWhitelist;
    }

    public List<String> getLore() {
        return lore;
    }

    /**
     * 화이트리스트가 설정되어 있는지 확인
     */
    public boolean hasWhitelist() {
        return targetWhitelist != null && !targetWhitelist.isEmpty();
    }

    /**
     * 그룹이 설정되어 있는지 확인
     */
    public boolean hasTargetGroups() {
        return targetGroups != null && !targetGroups.isEmpty();
    }
}
