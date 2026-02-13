package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

import java.util.*;

/**
 * 사유지 내 플레이어에게 작물 성장 버프 정보를 스코어보드로 표시합니다.
 * 플레이어가 버프가 활성화된 사유지에 있을 때만 스코어보드를 표시합니다.
 */
public class ClaimScoreboardManager {

    private final WildCore plugin;
    private final ClaimManager claimManager;
    private final CropGrowthManager cropGrowthManager;
    private BukkitTask updateTask;

    // 현재 스코어보드가 표시 중인 플레이어
    private final Set<UUID> activeScoreboards = new HashSet<>();

    private static final String OBJECTIVE_NAME = "wc_claim_buff";
    private static final String DISPLAY_NAME = "§a§l🌾 사유지 버프";

    public ClaimScoreboardManager(WildCore plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
        this.cropGrowthManager = plugin.getCropGrowthManager();
        startUpdateTask();
    }

    /**
     * 1초 간격으로 스코어보드를 업데이트합니다.
     */
    private void startUpdateTask() {
        updateTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!claimManager.isEnabled())
                return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerScoreboard(player);
            }
        }, 20L, 20L); // 1초 간격
    }

    /**
     * 플레이어의 스코어보드를 업데이트합니다.
     */
    private void updatePlayerScoreboard(Player player) {
        Claim claim = claimManager.getClaimAt(player.getLocation());

        if (claim == null) {
            // 사유지 밖이면 스코어보드 제거
            removeScoreboard(player);
            return;
        }

        // 버프가 활성화되어 있는지 확인
        if (!cropGrowthManager.hasActiveBuff(claim.getID())) {
            removeScoreboard(player);
            return;
        }

        // 스코어보드 생성/업데이트
        CropGrowthManager.BuffData buffData = cropGrowthManager.getBuffData(claim.getID());
        if (buffData == null) {
            removeScoreboard(player);
            return;
        }

        showScoreboard(player, buffData, claim);
    }

    /**
     * 플레이어에게 버프 정보 스코어보드를 표시합니다.
     */
    private void showScoreboard(Player player, CropGrowthManager.BuffData buffData, Claim claim) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null)
            return;

        Scoreboard scoreboard;
        Objective objective;

        // 기존 스코어보드가 있으면 재사용
        if (activeScoreboards.contains(player.getUniqueId())) {
            scoreboard = player.getScoreboard();
            objective = scoreboard.getObjective(OBJECTIVE_NAME);
            if (objective == null) {
                objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, DISPLAY_NAME);
                objective.setDisplaySlot(DisplaySlot.SIDEBAR);
            }
        } else {
            scoreboard = manager.getNewScoreboard();
            objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, Criteria.DUMMY, DISPLAY_NAME);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // 기존 항목 제거
        for (String entry : new HashSet<>(scoreboard.getEntries())) {
            scoreboard.resetScores(entry);
        }

        // 사유지 정보
        String claimName = plugin.getClaimDataManager().getClaimNickname(claim.getID());
        if (claimName == null || claimName.isEmpty()) {
            claimName = "사유지 #" + claim.getID();
        }

        // 남은 시간
        long remaining = buffData.getRemainingSeconds();
        String timeStr = formatTime(remaining);

        // 스코어보드 내용 설정
        objective.getScore("§a§l━━━━━━━━━━━━━━").setScore(7);
        objective.getScore("§f🏡 " + claimName).setScore(6);
        objective.getScore("§a§l━━━━━━━━━━━━━━━").setScore(5);
        objective.getScore(" ").setScore(4);
        objective.getScore("§e🌾 작물 성장 버프").setScore(3);
        objective.getScore("§7배율: §a" + buffData.getMultiplier() + "x").setScore(2);
        objective.getScore("§7남은 시간: §f" + timeStr).setScore(1);
        objective.getScore("§a§l━━━━━━━━━━━━━━━━").setScore(0);

        player.setScoreboard(scoreboard);
        activeScoreboards.add(player.getUniqueId());
    }

    /**
     * 플레이어의 스코어보드를 제거합니다.
     */
    public void removeScoreboard(Player player) {
        if (!activeScoreboards.contains(player.getUniqueId()))
            return;

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }
        activeScoreboards.remove(player.getUniqueId());
    }

    /**
     * 플레이어 로그아웃 시 정리
     */
    public void handlePlayerQuit(UUID playerUUID) {
        activeScoreboards.remove(playerUUID);
    }

    /**
     * 시간 포맷팅 (분:초)
     */
    private String formatTime(long seconds) {
        if (seconds <= 0)
            return "0:00";
        long minutes = seconds / 60;
        long secs = seconds % 60;
        return minutes + ":" + String.format("%02d", secs);
    }

    /**
     * 종료 시 정리
     */
    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }

        // 모든 플레이어의 스코어보드 해제
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            for (UUID uuid : new HashSet<>(activeScoreboards)) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.setScoreboard(manager.getMainScoreboard());
                }
            }
        }
        activeScoreboards.clear();
    }
}
