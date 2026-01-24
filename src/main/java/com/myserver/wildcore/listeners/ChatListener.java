package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.gui.admin.AdminGuiListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * 채팅 입력 리스너 (관리자 GUI 채팅 입력 처리)
 */
public class ChatListener implements Listener {

    private final WildCore plugin;
    private final AdminGuiListener adminGuiListener;

    public ChatListener(WildCore plugin, AdminGuiListener adminGuiListener) {
        this.plugin = plugin;
        this.adminGuiListener = adminGuiListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // 관리자 GUI 채팅 입력 대기 중인지 확인
        if (adminGuiListener.hasPendingInput(player.getUniqueId())) {
            event.setCancelled(true);

            String message = PlainTextComponentSerializer.plainText().serialize(event.message());

            // 메인 스레드에서 처리 (Bukkit API 호출)
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                adminGuiListener.handleChatInput(player, message);
            });
        }
    }
}
