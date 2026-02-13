package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.gui.admin.AdminGuiListener;
import com.myserver.wildcore.gui.claim.ClaimMainGUI;
import com.myserver.wildcore.managers.ClaimManager;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 입력 리스너 (관리자 GUI + 사유지 입력 처리)
 */
public class ChatListener implements Listener {

    private final WildCore plugin;
    private final AdminGuiListener adminGuiListener;

    // 사유지 별명 변경 대기 (UUID -> Claim ID)
    private final Map<UUID, Long> pendingNicknameInput = new ConcurrentHashMap<>();
    // 사유지 멤버 추가 대기 (UUID -> Claim ID)
    private final Map<UUID, Long> pendingMemberInput = new ConcurrentHashMap<>();

    public ChatListener(WildCore plugin, AdminGuiListener adminGuiListener) {
        this.plugin = plugin;
        this.adminGuiListener = adminGuiListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // 관리자 GUI 채팅 입력 대기 중인지 확인
        if (adminGuiListener.hasPendingInput(uuid)) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                adminGuiListener.handleChatInput(player, message);
            });
            return;
        }

        // 사유지 별명 변경 처리
        if (pendingNicknameInput.containsKey(uuid)) {
            event.setCancelled(true);
            Long claimId = pendingNicknameInput.remove(uuid);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§e별명 변경이 취소되었습니다.");
                    return;
                }

                // 별명 길이 제한 (16자)
                if (message.length() > 16) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c별명은 16자 이내여야 합니다.");
                    return;
                }

                plugin.getClaimDataManager().setClaimNickname(claimId, message);
                player.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§a사유지 별명이 '§f" + message + "§a'으로 변경되었습니다!");

                // GUI 다시 열기
                Claim claim = plugin.getClaimManager().getClaimById(claimId);
                if (claim != null) {
                    new ClaimMainGUI(plugin, player, claim).open();
                }
            });
            return;
        }

        // 사유지 멤버 추가 처리
        if (pendingMemberInput.containsKey(uuid)) {
            event.setCancelled(true);
            Long claimId = pendingMemberInput.remove(uuid);

            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (message.equalsIgnoreCase("cancel")) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§e멤버 추가가 취소되었습니다.");
                    return;
                }

                // 플레이어 이름으로 UUID 찾기
                OfflinePlayer target = Bukkit.getOfflinePlayer(message);
                if (target == null || !target.hasPlayedBefore()) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c해당 플레이어를 찾을 수 없습니다: " + message);
                    return;
                }

                Claim claim = plugin.getClaimManager().getClaimById(claimId);
                if (claim == null) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c사유지를 찾을 수 없습니다.");
                    return;
                }

                // Trust 추가 (기본: BUILDER)
                boolean success = plugin.getClaimManager().addTrust(
                        claim, target.getUniqueId(), ClaimManager.TrustType.BUILD);

                if (success) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§a'" + target.getName() + "§a'님이 사유지 멤버로 추가되었습니다!");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() +
                            "§c멤버 추가에 실패했습니다.");
                }

                // 멤버 GUI 다시 열기
                if (claim != null) {
                    new com.myserver.wildcore.gui.claim.ClaimMemberGUI(plugin, player, claim).open();
                }
            });
            return;
        }
    }

    // === 사유지 관련 대기 상태 관리 ===

    public void setPendingNicknameInput(UUID playerUuid, Long claimId) {
        pendingNicknameInput.put(playerUuid, claimId);
    }

    public void setPendingMemberInput(UUID playerUuid, Long claimId) {
        pendingMemberInput.put(playerUuid, claimId);
    }

    public boolean hasPendingClaimInput(UUID playerUuid) {
        return pendingNicknameInput.containsKey(playerUuid) || pendingMemberInput.containsKey(playerUuid);
    }
}
