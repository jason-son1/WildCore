package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.MiningDropData; // [NEW]
import com.myserver.wildcore.gui.EnchantGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * 블록 관련 이벤트 리스너
 * - 바닐라 인챈트 테이블 차단
 */
public class BlockListener implements Listener {

    private final WildCore plugin;

    public BlockListener(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 인챈트 테이블 우클릭 차단 및 커스텀 GUI 열기
     */
    @EventHandler
    public void onEnchantTableClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getClickedBlock() == null)
            return;
        if (event.getClickedBlock().getType() != Material.ENCHANTING_TABLE)
            return;

        // 바닐라 인챈트 테이블 차단 설정 확인
        if (!plugin.getConfigManager().isVanillaEnchantBlocked())
            return;

        Player player = event.getPlayer();

        // 권한 확인
        if (!player.hasPermission("wildcore.enchant")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("general.no_permission"));
            event.setCancelled(true);
            return;
        }

        // 바닐라 인챈트 GUI 차단
        event.setCancelled(true);

        // 커스텀 인챈트 GUI 열기
        new EnchantGUI(plugin, player).open();
    }

    /**
     * 블록 파괴 시 커스텀 드랍 처리
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled())
            return;

        Player player = event.getPlayer();

        // 크리에이티브 모드나 실크터치는 제외하는지 여부는 기획에 따라 다름.
        // 여기서는 일단 모두 동작하게 하거나, 필요시 필터링.
        // 기획서에는 별도 언급 없으나 보통 크리에이티브는 제외
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }

        Material blockType = event.getBlock().getType();
        MiningDropData dropData = plugin.getConfigManager().getMiningDropData(blockType);

        if (dropData != null && dropData.isEnabled()) {
            // 바닐라 드랍 제어
            if (!dropData.isVanillaDrops()) {
                event.setDropItems(false);
            }
        }

        plugin.getMiningDropManager().processBlockBreak(player, blockType,
                event.getBlock().getLocation());
    }
}
