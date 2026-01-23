package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.config.StockConfig;
import org.bukkit.Material;
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

        // 새 인챈트 추가
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 인챈트 ID를 채팅으로 입력하세요. (취소: 'cancel')");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.NEW_ENCHANT_ID, null));
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
        NEW_ENCHANT_ID
    }

    public record ChatInputRequest(InputType type, String targetId) {
    }
}
