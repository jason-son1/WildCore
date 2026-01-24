package com.myserver.wildcore.tasks;

import com.myserver.wildcore.WildCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * ActionBar에 플레이어의 잔액을 표시하는 태스크
 * Vault/EssentialsX의 경제 시스템과 연동
 */
public class ActionBarMoneyTask extends BukkitRunnable {

    private final WildCore plugin;

    public ActionBarMoneyTask(WildCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 권한이 없는 플레이어는 스킵
            if (!player.hasPermission("wildcore.actionbar.money")) {
                continue;
            }

            double balance = plugin.getEconomy().getBalance(player);
            String formattedBalance = String.format("%,.0f", balance);

            Component message = Component.text(" ", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, false)
                    .append(Component.text(formattedBalance + "원", NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true));

            player.sendActionBar(message);
        }
    }

    /**
     * 태스크 시작 (20틱 = 1초마다 갱신)
     */
    public void start() {
        // 20틱(1초) 후 시작, 20틱(1초)마다 반복
        this.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * 태스크 중지
     */
    public void stop() {
        try {
            this.cancel();
        } catch (IllegalStateException ignored) {
            // 이미 취소된 경우 무시
        }
    }
}
