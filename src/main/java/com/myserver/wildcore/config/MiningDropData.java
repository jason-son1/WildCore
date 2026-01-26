package com.myserver.wildcore.config;

import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

public class MiningDropData {
    private Material targetBlock;
    private boolean enabled;
    private boolean vanillaDrops = true; // [NEW] 바닐라 드랍 여부
    private List<MiningReward> rewards;

    public MiningDropData(Material targetBlock, boolean enabled) {
        this.targetBlock = targetBlock;
        this.enabled = enabled;
        this.rewards = new ArrayList<>();
    }

    public MiningDropData(Material targetBlock, boolean enabled, List<MiningReward> rewards) {
        this.targetBlock = targetBlock;
        this.enabled = enabled;
        this.rewards = rewards;
    }

    public Material getTargetBlock() {
        return targetBlock;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<MiningReward> getRewards() {
        return rewards;
    }

    public void setRewards(List<MiningReward> rewards) {
        this.rewards = rewards;
    }

    public boolean isVanillaDrops() {
        return vanillaDrops;
    }

    public void setVanillaDrops(boolean vanillaDrops) {
        this.vanillaDrops = vanillaDrops;
    }

    public void addReward(MiningReward reward) {
        this.rewards.add(reward);
    }

    public void removeReward(MiningReward reward) {
        this.rewards.remove(reward);
    }
}
