package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.gui.shop.ShopAdminGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * 상점 NPC 상호작용 리스너
 */
public class ShopInteractListener implements Listener {

    private final WildCore plugin;

    public ShopInteractListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // 상점 NPC인지 확인
        ShopConfig shop = plugin.getShopManager().getShopByEntityUUID(entity.getUniqueId());
        if (shop == null) {
            return;
        }

        // 이벤트 취소 (Villager 거래창 방지)
        event.setCancelled(true);

        // 관리자 모드: Shift + 우클릭
        if (player.isSneaking() && player.hasPermission("wildcore.admin.shop")) {
            new ShopAdminGUI(plugin, player, shop).open();
            plugin.debug("상점 관리자 GUI 열림: " + shop.getId() + " (" + player.getName() + ")");
        } else {
            // 일반 모드: 상점 GUI 열기
            new ShopGUI(plugin, player, shop).open();
            plugin.debug("상점 GUI 열림: " + shop.getId() + " (" + player.getName() + ")");
        }
    }
}
