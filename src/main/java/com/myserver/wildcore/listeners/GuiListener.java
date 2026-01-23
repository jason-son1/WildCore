package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.StockGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GUI 클릭 이벤트 리스너
 */
public class GuiListener implements Listener {

    private final WildCore plugin;

    public GuiListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getClickedInventory() == null)
            return;

        // 주식 GUI 처리
        if (event.getInventory().getHolder() instanceof StockGUI stockGUI) {
            event.setCancelled(true);
            handleStockClick(player, event, stockGUI);
            return;
        }

        // 인챈트 GUI 처리
        if (event.getInventory().getHolder() instanceof EnchantGUI enchantGUI) {
            event.setCancelled(true);
            handleEnchantClick(player, event, enchantGUI);
        }
    }

    /**
     * 주식 GUI 클릭 처리
     */
    private void handleStockClick(Player player, InventoryClickEvent event, StockGUI stockGUI) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 새로고침 버튼 (마지막 줄 중앙)
        int size = plugin.getConfigManager().getStockGuiSize();
        if (slot == size - 5) {
            stockGUI.refresh();
            return;
        }

        // 주식 종목 찾기
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            if (stock.getSlot() == slot) {
                int amount = click.isShiftClick() ? 10 : 1;

                if (click.isLeftClick()) {
                    // 매수
                    plugin.getStockManager().buyStock(player, stock.getId(), amount);
                } else if (click.isRightClick()) {
                    // 매도
                    plugin.getStockManager().sellStock(player, stock.getId(), amount);
                }

                // GUI 갱신
                stockGUI.refresh();
                return;
            }
        }
    }

    /**
     * 인챈트 GUI 클릭 처리
     */
    private void handleEnchantClick(Player player, InventoryClickEvent event, EnchantGUI enchantGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.PURPLE_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BARRIER)
            return;

        // 인챈트 옵션 찾기
        for (EnchantConfig enchant : plugin.getConfigManager().getEnchants().values()) {
            if (enchant.getSlot() == slot) {
                // 확인 메시지
                player.closeInventory();

                // 인챈트 시도
                plugin.getEnchantManager().tryEnchant(player, enchant.getId());
                return;
            }
        }
    }
}
