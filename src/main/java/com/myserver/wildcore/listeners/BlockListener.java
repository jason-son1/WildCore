package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.gui.EnchantGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
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
                    plugin.getConfigManager().getMessage("no_permission"));
            event.setCancelled(true);
            return;
        }

        // 바닐라 인챈트 GUI 차단
        event.setCancelled(true);

        // 커스텀 인챈트 GUI 열기
        new EnchantGUI(plugin, player).open();
    }
}
