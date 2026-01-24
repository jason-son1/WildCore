package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.util.NpcTagUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class NpcDamageListener implements Listener {

    private final WildCore plugin;

    public NpcDamageListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // WildCore NPC인지 확인
        if (!NpcTagUtil.isWildCoreNpc(event.getEntity())) {
            return;
        }

        // 무적 설정 확인 후 취소
        if (plugin.getConfigManager().isShopNpcInvulnerable()) {
            event.setCancelled(true);

            // 추가 보호: 화염/용암 등으로 인한 지속 데미지 방지
            event.getEntity().setFireTicks(0);

            plugin.debug("NPC 데미지 방지됨: " + event.getCause());
        }
    }
}
