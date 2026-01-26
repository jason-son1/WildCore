package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.BankMainGUI;
import com.myserver.wildcore.gui.shop.ShopAdminGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.NpcTagUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * 통합 NPC 상호작용 리스너
 * 상점/강화/주식 NPC를 모두 처리합니다.
 */
public class NpcInteractListener implements Listener {

    private final WildCore plugin;

    public NpcInteractListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // PDC 태그 확인
        NpcType type = NpcTagUtil.getNpcType(entity);
        if (type == null) {
            return; // WildCore NPC가 아님
        }

        // 이벤트 취소 (Villager 거래창 등 방지)
        event.setCancelled(true);

        switch (type) {
            case SHOP -> handleShopNpc(player, entity);
            case ENCHANT -> handleEnchantNpc(player);
            case STOCK -> handleStockNpc(player);
            case BANK -> handleBankNpc(player);
            case WARP -> handleWarpNpc(player, entity);
        }
    }

    /**
     * 상점 NPC 처리
     */
    private void handleShopNpc(Player player, Entity entity) {
        String shopId = NpcTagUtil.getTargetId(entity);
        if (shopId == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c상점 정보를 찾을 수 없습니다.");
            return;
        }

        ShopConfig shop = plugin.getConfigManager().getShop(shopId);
        if (shop == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c상점을 찾을 수 없습니다: " + shopId);
            return;
        }

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

    /**
     * 강화 NPC 처리
     */
    private void handleEnchantNpc(Player player) {
        // 권한 확인
        if (!player.hasPermission("wildcore.enchant.use")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c강화소를 사용할 권한이 없습니다.");
            return;
        }

        new EnchantGUI(plugin, player).open();
        plugin.debug("강화 GUI 열림 (NPC 클릭): " + player.getName());
    }

    /**
     * 주식 NPC 처리
     */
    private void handleStockNpc(Player player) {
        // 권한 확인
        if (!player.hasPermission("wildcore.stock.use")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c주식거래소를 사용할 권한이 없습니다.");
            return;
        }

        new StockGUI(plugin, player).open();
        plugin.debug("주식 GUI 열림 (NPC 클릭): " + player.getName());
    }

    /**
     * 이동 NPC 처리
     */
    /**
     * 이동 NPC 처리
     */
    private void handleWarpNpc(Player player, Entity entity) {
        String targetId = NpcTagUtil.getTargetId(entity);
        if (targetId == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c이동 정보를 찾을 수 없습니다.");
            return;
        }

        // 1. locations.yml에 설정된 워프 위치 확인
        Location warpLoc = plugin.getConfigManager().getWarpLocation(targetId);
        if (warpLoc != null) {
            player.teleport(warpLoc);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a" + targetId + " (으)로 이동했습니다.");
            plugin.debug("워프 이동 (NPC 클릭): " + player.getName() + " -> " + targetId + " (Location: " + warpLoc + ")");
            return;
        }

        // 2. 월드 이름으로 확인 (기존 방식 호환)
        org.bukkit.World world = org.bukkit.Bukkit.getWorld(targetId);
        if (world != null) {
            player.teleport(world.getSpawnLocation());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a" + targetId + " 월드로 이동했습니다.");
            plugin.debug("월드 이동 (NPC 클릭): " + player.getName() + " -> " + targetId);
            return;
        }

        // 3. 둘 다 없음
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c워프 지점 또는 월드를 찾을 수 없습니다: " + targetId);
    }

    /**
     * 은행 NPC 처리
     */
    private void handleBankNpc(Player player) {
        // 권한 확인
        if (!player.hasPermission("wildcore.bank.use")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c은행을 사용할 권한이 없습니다.");
            return;
        }

        new BankMainGUI(plugin, player).open();
        plugin.debug("은행 GUI 열림 (NPC 클릭): " + player.getName());
    }
}
