package com.myserver.wildcore.listeners;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.ShopItemConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.PaginatedGui;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
import com.myserver.wildcore.gui.BankMainGUI;
import com.myserver.wildcore.gui.BankProductListGUI;
import com.myserver.wildcore.gui.BankDepositGUI;
import com.myserver.wildcore.gui.BankStockInfoGUI; // Import added
import com.myserver.wildcore.gui.EnchantSelectGUI;
import com.myserver.wildcore.gui.RepairGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import com.myserver.wildcore.gui.PlayerInfoGUI;

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

        // 강화/수리 선택 GUI 처리
        if (event.getInventory().getHolder() instanceof EnchantSelectGUI selectGUI) {
            event.setCancelled(true);
            handleEnchantSelectClick(player, event, selectGUI);
            return;
        }

        // 수리 GUI 처리
        if (event.getInventory().getHolder() instanceof RepairGUI repairGUI) {
            event.setCancelled(true);
            handleRepairClick(player, event, repairGUI);
            return;
        }

        // 상점 GUI 처리
        if (event.getInventory().getHolder() instanceof ShopGUI shopGUI) {
            event.setCancelled(true);
            handleShopClick(player, event, shopGUI);
            return;
        }

        // 금융 자산 정보 GUI 처리 (NEW: 아이템 가져오기 방지)
        if (event.getInventory().getHolder() instanceof BankStockInfoGUI) {
            event.setCancelled(true);
            return;
        }

        // 내 정보 GUI 처리
        if (event.getInventory().getHolder() instanceof PlayerInfoGUI playerInfoGUI) {
            event.setCancelled(true);

            int slot = event.getRawSlot();

            // 주식/은행 정보 버튼 클릭 (상호작용 없음 -> 상호작용 추가)
            if (playerInfoGUI.isStockSlot(slot) || playerInfoGUI.isBankSlot(slot)) {

                // BankStockInfoGUI 열기
                new com.myserver.wildcore.gui.BankStockInfoGUI(plugin, player).open();
                return;
            }
            return;
        }
        // 내 주식 정보 GUI 처리
        if (event.getInventory().getHolder() instanceof com.myserver.wildcore.gui.MyStockGUI myStockGUI) {
            event.setCancelled(true);
            handleMyStockClick(player, event, myStockGUI);
            return;
        }

        // 은행 메인 GUI 처리
        if (event.getInventory().getHolder() instanceof BankMainGUI bankMainGUI) {
            event.setCancelled(true);
            handleBankMainClick(player, event, bankMainGUI);
            return;
        }

        // 은행 상품 목록 GUI 처리
        if (event.getInventory().getHolder() instanceof BankProductListGUI productListGUI) {
            event.setCancelled(true);
            handleBankProductListClick(player, event, productListGUI);
            return;
        }

        // 은행 입금 GUI 처리
        if (event.getInventory().getHolder() instanceof BankDepositGUI depositGUI) {
            event.setCancelled(true);
            handleBankDepositClick(player, event, depositGUI);
            return;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PlayerInfoGUI) {
            event.setCancelled(true);
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
                com.myserver.wildcore.managers.EnchantManager.EnchantProcess process = plugin.getEnchantManager()
                        .prepareEnchant(player, enchantId);

                if (process != null && !process.result.isError()) {
                    // 애니메이션 GUI 열기
                    new com.myserver.wildcore.gui.EnchantProcessGUI(plugin, player, process).open();
                }
            }
        }
    }

    /**
     * 강화 진행 GUI 클릭 방지
     */
    @EventHandler
    public void onEnchantProcessClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof com.myserver.wildcore.gui.EnchantProcessGUI) {
            event.setCancelled(true);
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

    /**
     * 내 주식 정보 GUI 클릭 처리 - 주식 거래소로 이동
     */
    private void handleMyStockClick(Player player, InventoryClickEvent event,
            com.myserver.wildcore.gui.MyStockGUI myStockGUI) {
        int slot = event.getRawSlot();

        // 페이지 네비게이션
        if (handlePaginationNavigation(slot, myStockGUI)) {
            return;
        }

        // 아이템 클릭 시 주식 거래소 오픈
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            String stockId = myStockGUI.getStockIdAtSlot(slot);
            if (stockId != null) {
                // 주식 거래소 오픈
                new StockGUI(plugin, player).open();
            }
        }
    }

    // =====================
    // 은행 GUI 핸들러
    // =====================

    /**
     * 은행 메인 GUI 클릭 처리
     */
    private void handleBankMainClick(Player player, InventoryClickEvent event, BankMainGUI bankMainGUI) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 배경 클릭 무시
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.YELLOW_STAINED_GLASS_PANE)
            return;

        // 새 상품 가입 버튼 클릭
        if (bankMainGUI.isNewProductSlot(slot)) {
            new BankProductListGUI(plugin, player).open();
            return;
        }

        // 계좌 영역 클릭
        if (bankMainGUI.isAccountSlot(slot)) {
            String accountId = bankMainGUI.getAccountIdAtSlot(slot);
            if (accountId == null)
                return;

            var account = plugin.getBankManager().getAccount(player.getUniqueId(), accountId);
            if (account == null)
                return;

            var product = plugin.getConfigManager().getBankProduct(account.getProductId());
            if (product == null)
                return;

            if (product.isSavings()) {
                // 자유 예금: 입금/출금/해지
                if (click.isLeftClick() && !click.isShiftClick()) {
                    // 입금 GUI 열기
                    new BankDepositGUI(plugin, player, product.getId(), accountId).open();
                } else if (click.isRightClick() && !click.isShiftClick()) {
                    // 출금 (10000원 고정, 후에 GUI로 개선 가능)
                    plugin.getBankManager().withdraw(player, accountId, 10000);
                    bankMainGUI.refresh();
                } else if (click.isShiftClick() && click.isRightClick()) {
                    // 계좌 해지
                    plugin.getBankManager().closeAccount(player, accountId, false);
                    bankMainGUI.refresh();
                }
            } else if (product.isTermDeposit()) {
                // 정기 적금
                if (account.isMatured()) {
                    // 만기 도달: 수령
                    plugin.getBankManager().closeAccount(player, accountId, false);
                    bankMainGUI.refresh();
                } else if (click.isRightClick()) {
                    // 중도 해지 시도
                    if (click.isShiftClick()) {
                        // Shift+우클릭: 강제 중도 해지
                        plugin.getBankManager().closeAccount(player, accountId, true);
                        bankMainGUI.refresh();
                    } else {
                        // 우클릭: 경고 메시지
                        plugin.getBankManager().closeAccount(player, accountId, false);
                    }
                }
            }
        }
    }

    /**
     * 은행 상품 목록 GUI 클릭 처리
     */
    private void handleBankProductListClick(Player player, InventoryClickEvent event,
            BankProductListGUI productListGUI) {
        int slot = event.getRawSlot();

        // 네비게이션 처리
        if (handlePaginationNavigation(slot, productListGUI)) {
            return;
        }

        // 배경 클릭 무시
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.LIGHT_GRAY_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.BARRIER)
            return;

        // 아이템 영역(0~44) 클릭 확인
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            String productId = productListGUI.getProductIdAtSlot(slot);
            if (productId != null) {
                // 입금 금액 선택 GUI 열기 (새 계좌 개설)
                new BankDepositGUI(plugin, player, productId, null).open();
            }
        }
    }

    /**
     * 은행 입금 GUI 클릭 처리
     */
    private void handleBankDepositClick(Player player, InventoryClickEvent event, BankDepositGUI depositGUI) {
        int slot = event.getRawSlot();

        // 뷰로가기
        if (depositGUI.isBackSlot(slot)) {
            if (depositGUI.getAccountId() == null) {
                // 새 계좌 개설 중이었으므로 상품 목록으로
                new BankProductListGUI(plugin, player).open();
            } else {
                // 기존 계좌 입금 중이었으므로 메인으로
                new BankMainGUI(plugin, player).open();
            }
            return;
        }

        // 직접 입력 버튼 (TODO: 채팅 입력 구현 필요)
        if (depositGUI.isCustomSlot(slot)) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "&7입금할 금액을 채팅으로 입력해주세요. (취소: 'cancel')");
            // ChatListener에서 처리할 수 있도록 상태 저장 필요
            // 지금은 기본 금액 버튼만 지원
            return;
        }

        // 금액 버튼 클릭
        double amount = depositGUI.getAmountAtSlot(slot);
        if (amount > 0) {
            String productId = depositGUI.getProductId();
            String accountId = depositGUI.getAccountId();

            if (accountId == null) {
                // 새 계좌 개설
                String newAccountId = plugin.getBankManager().createAccount(player, productId, amount);
                if (newAccountId != null) {
                    player.closeInventory();
                    new BankMainGUI(plugin, player).open();
                }
            } else {
                // 기존 계좌 입금
                if (plugin.getBankManager().deposit(player, accountId, amount)) {
                    player.closeInventory();
                    new BankMainGUI(plugin, player).open();
                }
            }
        }
    }

    /**
     * 강화/수리 선택 GUI 클릭 처리
     */
    private void handleEnchantSelectClick(Player player, InventoryClickEvent event, EnchantSelectGUI selectGUI) {
        int slot = event.getRawSlot();

        // 인챈트 버튼 클릭
        if (selectGUI.isEnchantSlot(slot)) {
            new EnchantGUI(plugin, player).open();
            return;
        }

        // 수리 버튼 클릭
        if (selectGUI.isRepairSlot(slot)) {
            new RepairGUI(plugin, player).open();
            return;
        }
    }

    /**
     * 수리 GUI 클릭 처리
     */
    private void handleRepairClick(Player player, InventoryClickEvent event, RepairGUI repairGUI) {
        int slot = event.getRawSlot();

        // 뒤로가기/닫기 버튼
        if (repairGUI.isBackSlot(slot)) {
            new EnchantSelectGUI(plugin, player).open();
            return;
        }

        // 페이지 네비게이션
        if (slot == PaginatedGui.SLOT_PREV_PAGE) {
            repairGUI.previousPage();
            return;
        }
        if (slot == PaginatedGui.SLOT_NEXT_PAGE) {
            repairGUI.nextPage();
            return;
        }

        // 수리 옵션 선택
        if (slot >= 0 && slot < PaginatedGui.ITEMS_PER_PAGE) {
            String repairId = repairGUI.getRepairIdAtSlot(slot);
            if (repairId != null) {
                boolean success = plugin.getRepairManager().tryRepair(player, repairId);
                if (success) {
                    // 수리 성공 시 GUI 업데이트
                    new RepairGUI(plugin, player).open();
                }
            }
        }
    }
}
