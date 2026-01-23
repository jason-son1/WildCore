package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.config.StockConfig;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 GUI 클릭 이벤트 리스너
 */
public class AdminGuiListener implements Listener {

    private final WildCore plugin;

    // 채팅 입력 대기 상태 관리
    private final Map<UUID, ChatInputRequest> pendingInputs = new HashMap<>();

    public AdminGuiListener(WildCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getClickedInventory() == null)
            return;

        // 주식 관리 메인 GUI
        if (event.getInventory().getHolder() instanceof StockAdminGUI adminGUI) {
            event.setCancelled(true);
            handleStockAdminClick(player, event, adminGUI);
            return;
        }

        // 주식 편집 GUI
        if (event.getInventory().getHolder() instanceof StockEditGUI editGUI) {
            event.setCancelled(true);
            handleStockEditClick(player, event, editGUI);
            return;
        }

        // 인챈트 관리 메인 GUI
        if (event.getInventory().getHolder() instanceof EnchantAdminGUI adminGUI) {
            event.setCancelled(true);
            handleEnchantAdminClick(player, event, adminGUI);
            return;
        }

        // 인챈트 편집 GUI
        if (event.getInventory().getHolder() instanceof EnchantEditGUI editGUI) {
            event.setCancelled(true);
            handleEnchantEditClick(player, event, editGUI);
            return;
        }

        // 인챈트 타입 선택 GUI (1단계)
        if (event.getInventory().getHolder() instanceof EnchantTypeSelectorGUI selectorGUI) {
            event.setCancelled(true);
            handleEnchantSelectorClick(player, event, selectorGUI);
            return;
        }

        // 인챈트 빌더 GUI (2단계)
        if (event.getInventory().getHolder() instanceof EnchantBuilderGUI builderGUI) {
            event.setCancelled(true);
            handleEnchantBuilderClick(player, event, builderGUI);
            return;
        }

        // 인챈트 타겟 GUI (3단계)
        if (event.getInventory().getHolder() instanceof EnchantTargetGUI targetGUI) {
            event.setCancelled(true);
            handleEnchantTargetClick(player, event, targetGUI);
            return;
        }
    }

    /**
     * 주식 관리 메인 GUI 클릭 처리
     */
    private void handleStockAdminClick(Player player, InventoryClickEvent event, StockAdminGUI adminGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
            return;

        // 닫기 버튼
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        // 전체 가격 갱신
        if (slot == 45) {
            plugin.getStockManager().updateAllPrices();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a모든 주식 가격이 갱신되었습니다.");
            adminGUI.refresh();
            return;
        }

        // 새 주식 추가
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 주식 ID를 채팅으로 입력하세요. (취소: 'cancel')");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.NEW_STOCK_ID, null));
            return;
        }

        // 주식 종목 클릭
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int index = 0;
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            if (index < slots.length && slots[index] == slot) {
                new StockEditGUI(plugin, player, stock.getId()).open();
                return;
            }
            index++;
        }
    }

    /**
     * 주식 편집 GUI 클릭 처리
     */
    private void handleStockEditClick(Player player, InventoryClickEvent event, StockEditGUI editGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String stockId = editGUI.getStockId();

        // 뒤로 가기
        if (slot == 45) {
            new StockAdminGUI(plugin, player).open();
            return;
        }

        // 가격 조정
        double currentPrice = plugin.getStockManager().getCurrentPrice(stockId);
        switch (slot) {
            case 19 -> adjustStockPrice(stockId, -1000, player, editGUI);
            case 20 -> adjustStockPrice(stockId, -100, player, editGUI);
            case 22 -> adjustStockPrice(stockId, 100, player, editGUI);
            case 23 -> adjustStockPrice(stockId, 1000, player, editGUI);
            case 21 -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 가격을 입력하세요. (현재: "
                        + String.format("%,.0f", currentPrice) + ")");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_PRICE, stockId));
            }
        }

        // 변동성 조정
        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock != null) {
            switch (slot) {
                case 28 -> adjustVolatility(stockId, -0.05, player, editGUI);
                case 29 -> adjustVolatility(stockId, -0.01, player, editGUI);
                case 31 -> adjustVolatility(stockId, 0.01, player, editGUI);
                case 32 -> adjustVolatility(stockId, 0.05, player, editGUI);
            }
        }

        // 가격 범위 설정 (채팅 입력)
        switch (slot) {
            case 37 -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e최소가를 입력하세요.");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_MIN_PRICE, stockId));
            }
            case 39 -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e기준가를 입력하세요.");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_BASE_PRICE, stockId));
            }
            case 41 -> {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e최대가를 입력하세요.");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_MAX_PRICE, stockId));
            }
        }

        // 저장
        if (slot == 48) {
            plugin.getConfigManager().saveStockConfig(stockId);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a주식 설정이 저장되었습니다.");
        }

        // 삭제
        if (slot == 50) {
            plugin.getConfigManager().deleteStock(stockId);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c주식이 삭제되었습니다: " + stockId);
            new StockAdminGUI(plugin, player).open();
        }
    }

    /**
     * 인챈트 관리 메인 GUI 클릭 처리
     */
    private void handleEnchantAdminClick(Player player, InventoryClickEvent event, EnchantAdminGUI adminGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
            return;

        // 닫기 버튼
        if (slot == 53) {
            player.closeInventory();
            return;
        }

        // 새 인챈트 추가 - 위저드 GUI로 이동
        if (slot == 49) {
            new EnchantTypeSelectorGUI(plugin, player).open();
            return;
        }

        // 인챈트 클릭
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int index = 0;
        for (EnchantConfig enchant : plugin.getConfigManager().getEnchants().values()) {
            if (index < slots.length && slots[index] == slot) {
                new EnchantEditGUI(plugin, player, enchant.getId()).open();
                return;
            }
            index++;
        }
    }

    /**
     * 인챈트 편집 GUI 클릭 처리
     */
    private void handleEnchantEditClick(Player player, InventoryClickEvent event, EnchantEditGUI editGUI) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        String enchantId = editGUI.getEnchantId();

        // 뒤로 가기
        if (slot == 45) {
            new EnchantAdminGUI(plugin, player).open();
            return;
        }

        // 성공 확률 조정
        switch (slot) {
            case 19 -> adjustEnchantProbability(enchantId, "success", -5, player, editGUI);
            case 20 -> adjustEnchantProbability(enchantId, "success", -1, player, editGUI);
            case 22 -> adjustEnchantProbability(enchantId, "success", 1, player, editGUI);
            case 23 -> adjustEnchantProbability(enchantId, "success", 5, player, editGUI);
        }

        // 실패 확률 조정
        switch (slot) {
            case 28 -> adjustEnchantProbability(enchantId, "fail", -5, player, editGUI);
            case 29 -> adjustEnchantProbability(enchantId, "fail", -1, player, editGUI);
            case 31 -> adjustEnchantProbability(enchantId, "fail", 1, player, editGUI);
            case 32 -> adjustEnchantProbability(enchantId, "fail", 5, player, editGUI);
        }

        // 파괴 확률 조정
        switch (slot) {
            case 37 -> adjustEnchantProbability(enchantId, "destroy", -5, player, editGUI);
            case 38 -> adjustEnchantProbability(enchantId, "destroy", -1, player, editGUI);
            case 40 -> adjustEnchantProbability(enchantId, "destroy", 1, player, editGUI);
            case 41 -> adjustEnchantProbability(enchantId, "destroy", 5, player, editGUI);
        }

        // 비용 설정 (채팅 입력)
        if (slot == 24) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e비용(금액)을 입력하세요.");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.ENCHANT_COST, enchantId));
        }

        // 재료 아이템 설정
        if (slot == 25) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e재료 아이템을 입력하세요. (형식: DIAMOND:5,EMERALD:3)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.ENCHANT_ITEMS, enchantId));
        }

        // 저장
        if (slot == 48) {
            plugin.getConfigManager().saveEnchantConfig(enchantId);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a인챈트 설정이 저장되었습니다.");
        }

        // 삭제
        if (slot == 50) {
            plugin.getConfigManager().deleteEnchant(enchantId);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c인챈트가 삭제되었습니다: " + enchantId);
            new EnchantAdminGUI(plugin, player).open();
        }
    }

    // === 헬퍼 메서드 ===

    private void adjustStockPrice(String stockId, double delta, Player player, StockEditGUI gui) {
        double current = plugin.getStockManager().getCurrentPrice(stockId);
        double newPrice = Math.max(0, current + delta);
        plugin.getStockManager().setCurrentPrice(stockId, newPrice);
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7가격 변경: §6" + String.format("%,.0f", current) + " §7→ §6" + String.format("%,.0f", newPrice));
        gui.refresh();
    }

    private void adjustVolatility(String stockId, double delta, Player player, StockEditGUI gui) {
        double current = plugin.getConfigManager().getStockVolatility(stockId);
        double newValue = Math.max(0.01, Math.min(1.0, current + delta));
        plugin.getConfigManager().setStockVolatility(stockId, newValue);
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7변동성 변경: §e" + String.format("%.0f%%", current * 100) + " §7→ §e"
                + String.format("%.0f%%", newValue * 100));
        gui.refresh();
    }

    private void adjustEnchantProbability(String enchantId, String type, double delta, Player player,
            EnchantEditGUI gui) {
        double current = plugin.getConfigManager().getEnchantProbability(enchantId, type);
        double newValue = Math.max(0, Math.min(100, current + delta));
        plugin.getConfigManager().setEnchantProbability(enchantId, type, newValue);

        String typeName = switch (type) {
            case "success" -> "성공";
            case "fail" -> "실패";
            case "destroy" -> "파괴";
            default -> type;
        };

        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§7" + typeName + " 확률 변경: §e" + String.format("%.1f%%", current) + " §7→ §e"
                + String.format("%.1f%%", newValue));
        gui.refresh();
    }

    // === 채팅 입력 처리 ===

    public boolean hasPendingInput(UUID uuid) {
        return pendingInputs.containsKey(uuid);
    }

    public void handleChatInput(Player player, String message) {
        ChatInputRequest request = pendingInputs.remove(player.getUniqueId());
        if (request == null)
            return;

        if (message.equalsIgnoreCase("cancel")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c입력이 취소되었습니다.");
            return;
        }

        switch (request.type) {
            case STOCK_PRICE -> {
                try {
                    double price = Double.parseDouble(message.replace(",", ""));
                    plugin.getStockManager().setCurrentPrice(request.targetId, price);
                    player.sendMessage(
                            plugin.getConfigManager().getPrefix() + "§a가격이 설정되었습니다: " + String.format("%,.0f", price));
                    new StockEditGUI(plugin, player, request.targetId).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                }
            }
            case STOCK_MIN_PRICE, STOCK_BASE_PRICE, STOCK_MAX_PRICE -> {
                try {
                    double price = Double.parseDouble(message.replace(",", ""));
                    String priceType = switch (request.type) {
                        case STOCK_MIN_PRICE -> "min";
                        case STOCK_BASE_PRICE -> "base";
                        case STOCK_MAX_PRICE -> "max";
                        default -> "base";
                    };
                    plugin.getConfigManager().setStockPrice(request.targetId, priceType, price);
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a가격이 설정되었습니다.");
                    new StockEditGUI(plugin, player, request.targetId).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                }
            }
            case ENCHANT_COST -> {
                try {
                    double cost = Double.parseDouble(message.replace(",", ""));
                    plugin.getConfigManager().setEnchantCost(request.targetId, cost);
                    player.sendMessage(
                            plugin.getConfigManager().getPrefix() + "§a비용이 설정되었습니다: " + String.format("%,.0f", cost));
                    new EnchantEditGUI(plugin, player, request.targetId).open();
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                }
            }
            case ENCHANT_ITEMS -> {
                plugin.getConfigManager().setEnchantItems(request.targetId, message);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a재료 아이템이 설정되었습니다.");
                new EnchantEditGUI(plugin, player, request.targetId).open();
            }
            case NEW_STOCK_ID -> {
                if (plugin.getConfigManager().createNewStock(message)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a새 주식이 생성되었습니다: " + message);
                    new StockEditGUI(plugin, player, message).open();
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c주식 생성에 실패했습니다.");
                }
            }
            case NEW_ENCHANT_ID -> {
                if (plugin.getConfigManager().createNewEnchant(message)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a새 인챈트가 생성되었습니다: " + message);
                    new EnchantEditGUI(plugin, player, message).open();
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c인챈트 생성에 실패했습니다.");
                }
            }
            case ENCHANT_BUILDER_LEVEL -> {
                EnchantBuilderGUI builderGui = pendingBuilders.remove(player.getUniqueId());
                if (builderGui != null) {
                    try {
                        int level = Integer.parseInt(message.replace(",", ""));
                        builderGui.setLevel(level);
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a레벨이 설정되었습니다: " + level);
                        builderGui.open();
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                        builderGui.open();
                    }
                }
            }
            case ENCHANT_BUILDER_COST -> {
                EnchantBuilderGUI builderGui = pendingBuilders.remove(player.getUniqueId());
                if (builderGui != null) {
                    try {
                        double cost = Double.parseDouble(message.replace(",", ""));
                        builderGui.setCostMoney(cost);
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a비용이 설정되었습니다: "
                                + String.format("%,.0f", cost));
                        builderGui.open();
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                        builderGui.open();
                    }
                }
            }
            case ENCHANT_BUILDER_ITEMS -> {
                EnchantBuilderGUI builderGui = pendingBuilders.remove(player.getUniqueId());
                if (builderGui != null) {
                    builderGui.setCostItems(message);
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a재료 아이템이 설정되었습니다.");
                    builderGui.open();
                }
            }
        }
    }

    // === 내부 클래스 ===

    public enum InputType {
        STOCK_PRICE,
        STOCK_MIN_PRICE,
        STOCK_BASE_PRICE,
        STOCK_MAX_PRICE,
        ENCHANT_COST,
        ENCHANT_ITEMS,
        NEW_STOCK_ID,
        NEW_ENCHANT_ID,
        ENCHANT_BUILDER_LEVEL,
        ENCHANT_BUILDER_COST,
        ENCHANT_BUILDER_ITEMS
    }

    // 빌더 GUI 참조 저장
    private final Map<UUID, EnchantBuilderGUI> pendingBuilders = new HashMap<>();

    public record ChatInputRequest(InputType type, String targetId) {
    }

    // === 인챈트 위저드 GUI 핸들러 ===

    /**
     * 인챈트 타입 선택 GUI 클릭 처리 (1단계)
     */
    private void handleEnchantSelectorClick(Player player, InventoryClickEvent event, EnchantTypeSelectorGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.BLACK_STAINED_GLASS_PANE)
            return;

        // 필터 버튼 (0-8)
        switch (slot) {
            case 0 -> gui.setFilter("ALL");
            case 1 -> gui.setFilter("WEAPON");
            case 2 -> gui.setFilter("ARMOR");
            case 3 -> gui.setFilter("TOOL");
            case 5 -> gui.setFilter("BOW");
            case 6 -> gui.setFilter("FISHING");
            case 7 -> gui.setFilter("TRIDENT");
            case 8 -> gui.setFilter("OTHER");
        }

        // 이전 페이지
        if (slot == 45) {
            gui.previousPage();
            return;
        }

        // 다음 페이지
        if (slot == 53) {
            gui.nextPage();
            return;
        }

        // 뒤로 가기
        if (slot == 48) {
            new EnchantAdminGUI(plugin, player).open();
            return;
        }

        // 컨텐츠 영역 클릭 - 인챈트 선택
        if (gui.isContentSlot(slot)) {
            Enchantment enchant = gui.getEnchantmentAtSlot(slot);
            if (enchant != null) {
                new EnchantBuilderGUI(plugin, player, enchant).open();
            }
        }
    }

    /**
     * 인챈트 빌더 GUI 클릭 처리 (2단계)
     */
    private void handleEnchantBuilderClick(Player player, InventoryClickEvent event, EnchantBuilderGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        // 레벨 조정
        switch (slot) {
            case 10 -> gui.adjustLevel(-100);
            case 11 -> gui.adjustLevel(-10);
            case 12 -> gui.adjustLevel(-1);
            case 13 -> {
                // 레벨 직접 입력
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e레벨을 입력하세요. (취소: 'cancel')");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.ENCHANT_BUILDER_LEVEL, null));
                pendingBuilders.put(player.getUniqueId(), gui);
            }
            case 14 -> gui.adjustLevel(1);
            case 15 -> gui.adjustLevel(10);
            case 16 -> gui.adjustLevel(100);
        }

        // 옵션 토글
        if (slot == 20) {
            gui.toggleUnsafeMode();
            return;
        }
        if (slot == 22) {
            gui.toggleIgnoreConflicts();
            return;
        }

        // 적용 대상 설정 (3단계로 이동)
        if (slot == 24) {
            new EnchantTargetGUI(plugin, player, gui).open();
            return;
        }

        // 확률 조정
        switch (slot) {
            case 29 -> gui.adjustSuccessRate(5);
            case 30 -> gui.adjustSuccessRate(-5);
            case 32 -> gui.adjustFailRate(5);
            case 33 -> gui.adjustFailRate(-5);
        }

        // 비용 설정
        if (slot == 38) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e비용(금액)을 입력하세요.");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.ENCHANT_BUILDER_COST, null));
            pendingBuilders.put(player.getUniqueId(), gui);
            return;
        }
        if (slot == 40) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e재료 아이템을 입력하세요. (형식: DIAMOND:5,EMERALD:3)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.ENCHANT_BUILDER_ITEMS, null));
            pendingBuilders.put(player.getUniqueId(), gui);
            return;
        }

        // 뒤로 가기
        if (slot == 45) {
            new EnchantTypeSelectorGUI(plugin, player).open();
            return;
        }

        // 생성 및 저장
        if (slot == 49) {
            if (gui.saveEnchant()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a인챈트가 생성되었습니다!");
                new EnchantAdminGUI(plugin, player).open();
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§c인챈트 생성에 실패했습니다.");
            }
            return;
        }

        // 취소
        if (slot == 53) {
            player.closeInventory();
        }
    }

    /**
     * 인챈트 타겟 GUI 클릭 처리 (3단계)
     */
    private void handleEnchantTargetClick(Player player, InventoryClickEvent event, EnchantTargetGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        // 그룹 토글 (10-16)
        String group = gui.getGroupAtSlot(slot);
        if (group != null) {
            gui.toggleGroup(group);
            return;
        }

        // 손에 든 아이템 토글
        if (slot == 22) {
            gui.toggleHandItem();
            return;
        }

        // 전체 초기화
        if (slot == 49) {
            gui.clearAll();
            return;
        }

        // 뒤로 가기
        if (slot == 45) {
            gui.applyToBuilder();
            gui.getParentBuilder().open();
            return;
        }

        // 완료
        if (slot == 53) {
            gui.applyToBuilder();
            gui.getParentBuilder().open();
        }
    }
}
