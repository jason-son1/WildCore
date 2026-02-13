package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.managers.CropGrowthManager;
import com.myserver.wildcore.util.ItemUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 작물 성장 버프 아이템 사용을 처리하는 리스너입니다.
 * crop_growth_buff 기능이 있는 커스텀 아이템을 사유지 안에서 우클릭하면
 * 해당 아이템의 단계(tier)에 맞는 버프가 적용됩니다.
 * 상위 단계 아이템은 하위 단계 버프를 교체할 수 있습니다.
 */
public class CropGrowthBuffListener implements Listener {

    private final WildCore plugin;
    private final ClaimManager claimManager;
    private final CropGrowthManager cropGrowthManager;

    public CropGrowthBuffListener(WildCore plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
        this.cropGrowthManager = plugin.getCropGrowthManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 우클릭만 처리
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
            return;
        if (event.getHand() != EquipmentSlot.HAND)
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // crop_growth_buff 기능이 있는 아이템인지 확인
        if (!ItemUtil.hasFunction(plugin, item, "crop_growth_buff"))
            return;

        event.setCancelled(true);

        // 아이템 ID로 tier 정보 로드
        String itemId = ItemUtil.getCustomItemId(plugin, item);
        if (itemId == null)
            return;

        int tier = plugin.getConfigManager().getItemCropBuffTier(itemId);
        ConfigManager.CropBuffTier tierConfig = plugin.getConfigManager().getCropBuffTier(tier);

        if (tierConfig == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c잘못된 버프 아이템입니다. (티어 " + tier + " 설정 없음)");
            return;
        }

        // 사유지 안에 있는지 확인
        Claim claim = claimManager.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사유지 안에서만 사용할 수 있습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 사유지 주인 또는 관리자 권한 확인
        boolean isOwner = claimManager.isClaimOwner(claim, player.getUniqueId());
        ClaimManager.TrustType trustLevel = claimManager.getPlayerTrustLevel(claim, player.getUniqueId());
        boolean isManager = trustLevel == ClaimManager.TrustType.MANAGER;

        if (!isOwner && !isManager && !player.hasPermission("wildcore.claim.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c이 사유지에서 사용할 권한이 없습니다.");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // 이미 버프가 활성화되어 있는지 확인
        if (cropGrowthManager.hasActiveBuff(claim.getID())) {
            CropGrowthManager.BuffData currentBuff = cropGrowthManager.getBuffData(claim.getID());
            if (currentBuff != null) {
                // 같은 단계이거나 하위 단계면 교체 불가
                if (tier <= currentBuff.getTier()) {
                    long remaining = currentBuff.getRemainingSeconds();
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c이미 " + currentBuff.getTierName() + " §c버프가 활성화되어 있습니다!");
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§7남은 시간: " + formatTime(remaining) + " §7| 배율: §a" + currentBuff.getMultiplier() + "x");
                    if (tier < currentBuff.getTier()) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() +
                                "§7현재 더 높은 단계의 버프가 적용 중입니다.");
                    } else {
                        player.sendMessage(plugin.getConfigManager().getPrefix() +
                                "§7같은 단계의 버프는 중복 사용할 수 없습니다.");
                    }
                    return;
                }
                // 상위 단계면 교체
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§e기존 버프(" + currentBuff.getTierName() + "§e)를 상위 버프로 교체합니다!");
            }
        }

        // 아이템 소모
        item.setAmount(item.getAmount() - 1);

        // 버프 활성화
        cropGrowthManager.activateBuff(claim.getID(), tier, tierConfig.getName(),
                tierConfig.getMultiplier(), tierConfig.getDuration());

        // 성공 메시지
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§a🌾 " + tierConfig.getName() + " §a버프가 활성화되었습니다!");
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7단계: §f" + tier + "단계 §7| 배율: §a" + tierConfig.getMultiplier() + "x §7| 지속시간: §f"
                + formatTime(tierConfig.getDuration()));

        // 효과음 (단계에 따라 피치 변경)
        float pitch = 1.0f + (tier * 0.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, pitch);
    }

    private String formatTime(long seconds) {
        if (seconds <= 0)
            return "0:00";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + ":" + String.format("%02d", secs);
    }

    /**
     * 작물 성장 이벤트 처리 (버프 적용)
     */
    @EventHandler
    public void onBlockGrow(org.bukkit.event.block.BlockGrowEvent event) {
        if (event.isCancelled())
            return;

        // 사유지 확인
        Claim claim = claimManager.getClaimAt(event.getBlock().getLocation());
        if (claim == null)
            return;

        // 버프 확인
        if (!cropGrowthManager.hasActiveBuff(claim.getID()))
            return;

        double multiplier = cropGrowthManager.getBuffMultiplier(claim.getID());
        if (multiplier <= 1.0)
            return;

        // Ageable 작물인지 확인 (밀, 당근, 감자 등)
        if (event.getNewState().getBlockData() instanceof org.bukkit.block.data.Ageable) {
            org.bukkit.block.data.Ageable ageable = (org.bukkit.block.data.Ageable) event.getNewState().getBlockData();
            org.bukkit.block.data.Ageable current = (org.bukkit.block.data.Ageable) event.getBlock().getBlockData();

            int currentAge = current.getAge();
            int nextAge = ageable.getAge();
            int limit = ageable.getMaximumAge();

            // 성장이 일어나는 경우에만
            if (nextAge > currentAge) {
                // 배율 적용 (확률적 추가 성장)
                // 예: 배율 2.0 -> 기본 1 + 추가 1 (100% 확률)
                // 예: 배율 1.5 -> 기본 1 + 추가 1 (50% 확률)
                double bonusGrowthChance = multiplier - 1.0;
                int guaranteedBonus = (int) bonusGrowthChance;
                double randomBonus = bonusGrowthChance - guaranteedBonus;

                int bonus = guaranteedBonus;
                if (Math.random() < randomBonus) {
                    bonus++;
                }

                if (bonus > 0) {
                    int targetAge = Math.min(limit, nextAge + bonus);
                    ageable.setAge(targetAge);
                    event.getNewState().setBlockData(ageable);

                    // 디버그 (필요시)
                    // plugin.debug("Applied growth buff: " + currentAge + " -> " + targetAge + "
                    // (x" + multiplier + ")");
                }
            }
        }
    }
}
