package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BuffBlockConfig;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;

/**
 * 특정 블록 위를 걸을 때 버프를 주는 리스너
 */
public class BuffBlockListener implements Listener {

    private final WildCore plugin;

    public BuffBlockListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // 최적화: 같은 블록 내 움직임은 무시
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
                event.getFrom().getBlockY() == event.getTo().getBlockY() &&
                event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        Location to = event.getTo();

        // 월드 확인
        String worldName = to.getWorld().getName();

        // 발 아래 블록 확인 (y-1)
        Block blockUnder = to.getBlock().getRelative(BlockFace.DOWN);

        for (BuffBlockConfig config : plugin.getConfigManager().getBuffBlocks().values()) {
            // 월드 일치 확인
            if (!config.getWorldName().equals(worldName)) {
                continue;
            }

            // 블록 타입 일치 확인
            if (config.getBlockType() == blockUnder.getType()) {
                // 버프 적용
                for (BuffBlockConfig.BuffEffect effect : config.getEffects()) {
                    if (effect.getType() != null) {
                        player.addPotionEffect(new PotionEffect(
                                effect.getType(),
                                effect.getDuration(),
                                effect.getAmplifier(),
                                true, // ambient
                                false, // particles
                                true // icon
                        ));
                    }
                }
            }
        }
    }
}
