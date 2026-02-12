package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * GUI 자동 새로고침 관리자
 * 플레이어별로 GUI 자동 갱신 태스크를 관리합니다.
 */
public class AutoRefreshGUI {

    private static final Map<UUID, BukkitTask> refreshTasks = new HashMap<>();
    private static final long DEFAULT_REFRESH_INTERVAL = 20L; // 1초 (20틱)

    /**
     * 플레이어의 GUI 자동 새로고침 시작 (기본 1초 간격)
     * 
     * @param plugin        WildCore 플러그인 인스턴스
     * @param player        대상 플레이어
     * @param refreshAction 새로고침 시 실행할 작업
     */
    public static void startAutoRefresh(WildCore plugin, Player player, Runnable refreshAction) {
        startAutoRefresh(plugin, player, refreshAction, DEFAULT_REFRESH_INTERVAL);
    }

    /**
     * 플레이어의 GUI 자동 새로고침 시작 (커스텀 간격)
     * 
     * @param plugin        WildCore 플러그인 인스턴스
     * @param player        대상 플레이어
     * @param refreshAction 새로고침 시 실행할 작업
     * @param intervalTicks 갱신 간격 (틱 단위)
     */
    public static void startAutoRefresh(WildCore plugin, Player player, Runnable refreshAction, long intervalTicks) {
        // 기존 태스크가 있으면 중지
        stopAutoRefresh(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            // 플레이어가 오프라인이면 중지
            if (!player.isOnline()) {
                stopAutoRefresh(player);
                return;
            }

            // GUI가 열려있지 않으면 중지
            Inventory openInventory = player.getOpenInventory().getTopInventory();
            if (openInventory == null || openInventory.getHolder() == null) {
                stopAutoRefresh(player);
                return;
            }

            // 새로고침 작업 실행
            try {
                refreshAction.run();
            } catch (Exception e) {
                plugin.getLogger().warning("GUI 자동 새로고침 중 오류: " + e.getMessage());
                stopAutoRefresh(player);
            }
        }, intervalTicks, intervalTicks);

        refreshTasks.put(player.getUniqueId(), task);
    }

    /**
     * 플레이어의 GUI 자동 새로고침 중지
     * 
     * @param player 대상 플레이어
     */
    public static void stopAutoRefresh(Player player) {
        if (player == null)
            return;
        BukkitTask task = refreshTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 모든 자동 새로고침 중지 (플러그인 종료 시 호출)
     */
    public static void stopAll() {
        for (BukkitTask task : refreshTasks.values()) {
            if (task != null) {
                task.cancel();
            }
        }
        refreshTasks.clear();
    }

    /**
     * 플레이어의 자동 새로고침이 활성화되어 있는지 확인
     * 
     * @param player 대상 플레이어
     * @return 활성화 상태
     */
    public static boolean isAutoRefreshing(Player player) {
        return refreshTasks.containsKey(player.getUniqueId());
    }
}
