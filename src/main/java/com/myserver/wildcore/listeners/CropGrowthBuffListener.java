package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ConfigManager;
import com.myserver.wildcore.managers.ClaimManager;
import com.myserver.wildcore.managers.CropGrowthManager;
import com.myserver.wildcore.managers.CropTracker;
import com.myserver.wildcore.util.ItemUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * 작물 성장 버프 시스템 리스너입니다.
 *
 * 1) crop_growth_buff 기능이 있는 커스텀 아이템을 사유지 안에서 우클릭하면
 * 해당 아이템의 단계(tier)에 맞는 스케줄러 기반 버프가 활성화됩니다.
 * 2) 작물 심기/파괴 이벤트를 감지하여 CropTracker의 좌표 캐시를 동기화합니다.
 *
 * 기존 BlockGrowEvent 기반의 수동적 성장 가속은 제거되었으며,
 * CropGrowthTask가 능동적으로 작물 성장을 관리합니다.
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

    /**
     * 버프 아이템 사용 처리
     */
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
                            "§7남은 시간: " + formatTime(remaining)
                            + " §7| 주기: §a" + currentBuff.getIntervalSeconds() + "초"
                            + " §7| 확률: §a" + (int) (currentBuff.getGrowthChance() * 100) + "%");
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

        // 버프 활성화 (새 스케줄러 기반 파라미터 전달)
        cropGrowthManager.activateBuff(claim.getID(), tier, tierConfig.getName(),
                tierConfig.getDuration(), tierConfig.getIntervalSeconds(),
                tierConfig.getGrowthChance(), tierConfig.getGrowthAmount());

        // 추적 중인 작물 수 표시
        int cropCount = cropGrowthManager.getCropTracker().getCropCount(claim.getID());

        // 성공 메시지
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§a🌾 " + tierConfig.getName() + " §a버프가 활성화되었습니다!");
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7단계: §f" + tier + "단계 §7| 주기: §a" + tierConfig.getIntervalSeconds() + "초"
                + " §7| 확률: §a" + (int) (tierConfig.getGrowthChance() * 100) + "%"
                + " §7| 성장량: §a" + tierConfig.getGrowthAmount() + "단계");
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7지속시간: §f" + formatTime(tierConfig.getDuration())
                + " §7| 등록된 작물: §a" + cropCount + "개");

        // 효과음 (단계에 따라 피치 변경)
        float pitch = 1.0f + (tier * 0.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, pitch);
    }

    /**
     * 작물 심기 감지 -> CropTracker에 좌표 추가
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();

        // 작물 블록인지 확인
        if (!CropTracker.isCropBlock(block.getType()))
            return;

        // 사유지 확인
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null)
            return;

        // 해당 사유지에 활성 버프가 있는 경우에만 추적
        if (!cropGrowthManager.hasActiveBuff(claim.getID()))
            return;

        cropGrowthManager.getCropTracker().addCrop(claim.getID(), block.getLocation());
        plugin.debug("CropTracker: 작물 심기 감지 - claim=" + claim.getID()
                + " pos=" + block.getX() + "," + block.getY() + "," + block.getZ());
    }

    /**
     * 작물 파괴 감지 -> CropTracker에서 좌표 제거
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        // 작물 블록인지 확인
        if (!CropTracker.isCropBlock(block.getType()))
            return;

        // 사유지 확인
        Claim claim = claimManager.getClaimAt(block.getLocation());
        if (claim == null) {
            // 사유지 밖이지만 혹시 추적 중일 수 있으므로 전체에서 제거
            cropGrowthManager.getCropTracker().removeCropFromAll(block.getLocation());
            return;
        }

        cropGrowthManager.getCropTracker().removeCrop(claim.getID(), block.getLocation());
        plugin.debug("CropTracker: 작물 파괴 감지 - claim=" + claim.getID()
                + " pos=" + block.getX() + "," + block.getY() + "," + block.getZ());
    }

    private String formatTime(long seconds) {
        if (seconds <= 0)
            return "0:00";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + ":" + String.format("%02d", secs);
    }
}
