package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.gui.claim.ClaimMainGUI;
import com.myserver.wildcore.managers.ClaimManager;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사유지 관리 명령어
 * /claim - 현재 위치의 사유지 관리 GUI 열기
 * /claim list - 내 사유지 목록
 */
public class ClaimCommand implements CommandExecutor, TabCompleter {

    private final WildCore plugin;
    private final ClaimManager claimManager;

    public ClaimCommand(WildCore plugin) {
        this.plugin = plugin;
        this.claimManager = plugin.getClaimManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        if (!claimManager.isEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사유지 시스템이 비활성화 상태입니다.");
            return true;
        }

        // /claim list
        if (args.length > 0 && args[0].equalsIgnoreCase("list")) {
            showClaimList(player);
            return true;
        }

        // /claim (현재 위치의 사유지 관리)
        Claim claim = claimManager.getClaimAt(player.getLocation());
        if (claim == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c현재 사유지 안에 있지 않습니다.");
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§7내 사유지 목록 보기: /claim list");
            return true;
        }

        // 권한 확인 (주인 또는 관리자)
        boolean isOwner = claimManager.isClaimOwner(claim, player.getUniqueId());
        ClaimManager.TrustType trustLevel = claimManager.getPlayerTrustLevel(claim, player.getUniqueId());
        boolean isManager = trustLevel == ClaimManager.TrustType.MANAGER;

        if (!isOwner && !isManager && !player.hasPermission("wildcore.claim.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c이 사유지를 관리할 권한이 없습니다.");
            return true;
        }

        // GUI 열기
        new ClaimMainGUI(plugin, player, claim).open();
        return true;
    }

    private void showClaimList(Player player) {
        List<Claim> claims = claimManager.getPlayerClaims(player.getUniqueId());

        if (claims.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c보유한 사유지가 없습니다.");
            return;
        }

        player.sendMessage("");
        player.sendMessage("§a§l[ 내 사유지 목록 ]");
        player.sendMessage("");

        int index = 1;
        for (Claim claim : claims) {
            String nickname = plugin.getClaimDataManager().getClaimNickname(claim.getID());
            if (nickname == null || nickname.isEmpty()) {
                nickname = "사유지 #" + claim.getID();
            }

            String size = claimManager.getClaimSize(claim);
            var center = claimManager.getClaimCenter(claim);
            String location = center != null ? center.getBlockX() + ", " + center.getBlockZ() : "알 수 없음";

            player.sendMessage("§e" + index + ". §f" + nickname);
            player.sendMessage("   §7크기: §f" + size + " §7| 위치: §f" + location);
            index++;
        }

        player.sendMessage("");
        player.sendMessage("§7사유지 안에서 §e/claim§7 명령어로 관리 GUI를 열 수 있습니다.");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
        }

        return completions.stream()
                .filter(c -> c.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
