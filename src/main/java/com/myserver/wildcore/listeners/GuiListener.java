package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopItemConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
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
            return;
        }

        // 상점 GUI 처리
        if (event.getInventory().getHolder() instanceof ShopGUI shopGUI) {
            event.setCancelled(true);
            handleShopClick(player, event, shopGUI);
            return;
        }
    }

    /**
     * 주식 GUI 클릭 처리 (페이지네이션 지원)
     */
    private void handleStockClick(Player player, InventoryClickEvent event, StockGUI stockGUI) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 네비게이션 처리
        if (handlePaginationNavigation(slot, stockGUI)) {
            return;
        }

        // 배경 클릭 무시
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BARRIER)
            return;

        // 아이템 영역(0~44) 클릭 확인
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            String stockId = stockGUI.getStockIdAtSlot(slot);
            if (stockId != null) {
                int amount = click.isShiftClick() ? 10 : 1;

                if (click.isLeftClick()) {
                    // 매수
                    plugin.getStockManager().buyStock(player, stockId, amount);
                } else if (click.isRightClick()) {
                    // 매도
                    plugin.getStockManager().sellStock(player, stockId, amount);
                }

                // GUI 갱신
                stockGUI.refresh();
            }
        }
    }

    /**
     * 인챈트 GUI 클릭 처리 (페이지네이션 지원)
     */
    private void handleEnchantClick(Player player, InventoryClickEvent event, EnchantGUI enchantGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();

        // 네비게이션 처리
        if (handlePaginationNavigation(slot, enchantGUI)) {
            return;
        }

        // 배경 클릭 무시
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.PURPLE_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BARRIER)
            return;

        // 상단 정보 아이템 무시 (슬롯 4: 손에 든 아이템)
        if (slot == 4) {
            return;
        }

        // 아이템 영역(0~44) 클릭 확인
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            String enchantId = enchantGUI.getEnchantIdAtSlot(slot);
            if (enchantId != null) {
                // 인챈트 시도
                player.closeInventory();
                plugin.getEnchantManager().tryEnchant(player, enchantId);
            }
        }
    }

    /**
     * 페이지네이션 네비게이션 처리
     * 
     * @return true if navigation was handled, false otherwise
     */
    private boolean handlePaginationNavigation(int slot, PaginatedGui<?> gui) {
        // 이전 페이지 (슬롯 45)
        if (slot == PaginatedGui.SLOT_PREV_PAGE) {
            if (gui.getCurrentPage() > 0) {
                gui.previousPage();
            }
            return true;
        }

        // 다음 페이지 (슬롯 53)
        if (slot == PaginatedGui.SLOT_NEXT_PAGE) {
            if (gui.getCurrentPage() < gui.getTotalPages() - 1) {
                gui.nextPage();
            }
            return true;
        }

        // 정보 아이콘 (슬롯 49)
        if (slot == PaginatedGui.SLOT_INFO) {
            return true; // 정보 아이콘은 클릭해도 아무것도 하지 않음
        }

        // 네비게이션 바 영역(45~53) 전체 무시
        if (slot >= 45 && slot <= 53) {
            return true;
        }

        return false;
    }

    /**
     * 상점 GUI 클릭 처리 (페이지네이션 지원)
     */
    private void handleShopClick(Player player, InventoryClickEvent event, ShopGUI shopGUI) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 네비게이션 처리
        if (handlePaginationNavigation(slot, shopGUI)) {
            return;
        }

        // 배경 클릭 무시
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.LIME_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BARRIER)
            return;

        // 아이템 영역(0~44) 클릭 확인
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            ShopItemConfig shopItem = shopGUI.getShopItemAtSlot(slot);
            if (shopItem != null) {
                // 구매/판매 처리
                if (click.isLeftClick()) {
                    // 구매
                    int amount = click.isShiftClick() ? 64 : 1;
                    plugin.getShopManager().buyItem(player, shopGUI.getShop(), shopItem.getSlot(), amount);
                } else if (click.isRightClick()) {
                    // 판매
                    if (click.isShiftClick()) {
                        // 전량 판매
                        plugin.getShopManager().sellAllItems(player, shopGUI.getShop(), shopItem.getSlot());
                    } else {
                        // 1개 판매
                        plugin.getShopManager().sellItem(player, shopGUI.getShop(), shopItem.getSlot(), 1);
                    }
                } else if (click == ClickType.MIDDLE) {
                    // 휠클릭: 전량 판매
                    plugin.getShopManager().sellAllItems(player, shopGUI.getShop(), shopItem.getSlot());
                }

                // GUI 갱신
                shopGUI.refresh();
            }
        }
    }
}
