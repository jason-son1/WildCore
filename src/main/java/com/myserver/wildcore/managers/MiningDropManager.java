package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.MiningDropData;
import com.myserver.wildcore.config.MiningReward;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MiningDropManager {

    private final WildCore plugin;

    public MiningDropManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 블록 파괴 시 드랍 처리
     * 
     * @param player    블록을 캔 플레이어
     * @param blockType 블록 타입
     * @param location  드랍 위치
     */
    public void processBlockBreak(Player player, Material blockType, Location location) {
        MiningDropData dropData = plugin.getConfigManager().getMiningDropData(blockType);

        if (dropData == null || !dropData.isEnabled()) {
            return;
        }

        List<ItemStack> drops = calculateRewards(dropData);
        for (ItemStack item : drops) {
            if (item != null) {
                location.getWorld().dropItemNaturally(location, item);
            }
        }
    }

    /**
     * 보상 계산
     */
    public List<ItemStack> calculateRewards(MiningDropData data) {
        List<ItemStack> rewards = new ArrayList<>();

        for (MiningReward reward : data.getRewards()) {
            double chance = reward.getChance();
            if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
                String itemId = reward.getItemId();
                ItemStack item = ItemUtil.createCustomItem(plugin, itemId, 1);

                if (item != null) {
                    int amount = reward.getMinAmount();
                    if (reward.getMaxAmount() > reward.getMinAmount()) {
                        amount += ThreadLocalRandom.current()
                                .nextInt(reward.getMaxAmount() - reward.getMinAmount() + 1);
                    }
                    item.setAmount(amount);
                    rewards.add(item);
                } else {
                    plugin.getLogger().warning("Mining reward item not found: " + itemId);
                }
            }
        }

        return rewards;
    }

    /**
     * 블록 추가 (ConfigManager 위임)
     */
    public void addBlock(Material material) {
        if (plugin.getConfigManager().getMiningDropData(material) != null) {
            return;
        }
        MiningDropData newData = new MiningDropData(material, true);
        plugin.getConfigManager().setMiningDropData(material, newData);
        plugin.getConfigManager().saveMiningDropsConfig();
    }

    /**
     * 블록 삭제 (ConfigManager 위임)
     */
    public void removeBlock(Material material) {
        plugin.getConfigManager().removeMiningDropData(material);
        plugin.getConfigManager().saveMiningDropsConfig();
    }

    /**
     * 보상 추가
     */
    public void addReward(Material material, MiningReward reward) {
        MiningDropData data = plugin.getConfigManager().getMiningDropData(material);
        if (data != null) {
            data.addReward(reward);
            plugin.getConfigManager().saveMiningDropsConfig();
        }
    }

    /**
     * 보상 삭제
     */
    public void removeReward(Material material, MiningReward reward) {
        MiningDropData data = plugin.getConfigManager().getMiningDropData(material);
        if (data != null) {
            data.removeReward(reward);
            plugin.getConfigManager().saveMiningDropsConfig();
        }
    }

    /**
     * 보상 가져오기 (ItemID로 검색)
     */
    public MiningReward getReward(Material material, String itemId) {
        MiningDropData data = plugin.getConfigManager().getMiningDropData(material);
        if (data != null) {
            for (MiningReward reward : data.getRewards()) {
                if (reward.getItemId().equals(itemId)) {
                    return reward;
                }
            }
        }
        return null;
    }

    public void saveConfig() {
        plugin.getConfigManager().saveMiningDropsConfig();
    }
}
