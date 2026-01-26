package com.myserver.wildcore.tasks;

import com.myserver.wildcore.WildCore;
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
        if (!plugin.getConfigManager().isActionBarEnabled()) {
            return;
        }

        String format = plugin.getConfigManager().getActionBarFormat();

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 권한이 없는 플레이어는 스킵
            if (!player.hasPermission("wildcore.actionbar.money")) {
                continue;
            }

            double balance = plugin.getEconomy().getBalance(player);
            String formattedBalance = String.format("%,.1f", balance);

            String finalMessage = format.replace("%money%", formattedBalance);
            player.sendActionBar(com.myserver.wildcore.util.ItemUtil.parse(finalMessage));
        }
    }

    /**
     * 태스크 시작
     */
    public void start() {
        long interval = plugin.getConfigManager().getActionBarUpdateInterval();
        // interval틱 후 시작, interval틱마다 반복
        this.runTaskTimer(plugin, interval, interval);
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
