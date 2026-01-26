package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.EnchantConfig;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.gui.shop.ShopAdminGUI;
import com.myserver.wildcore.gui.shop.ShopEditorGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
import com.myserver.wildcore.gui.shop.ShopListGUI; // Import added
import com.myserver.wildcore.npc.NpcType;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
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

        // 상점 관리 GUI
        if (event.getInventory().getHolder() instanceof ShopAdminGUI shopAdminGUI) {
            event.setCancelled(true);
            handleShopAdminClick(player, event, shopAdminGUI);
            return;
        }

        // 상점 아이템 편집 GUI - 아이템 드래그를 허용해야 하므로 특별 처리
        if (event.getInventory().getHolder() instanceof ShopEditorGUI shopEditorGUI) {
            handleShopEditorClick(player, event, shopEditorGUI);
            return;
        }

        // NPC 관리 GUI
        if (event.getInventory().getHolder() instanceof NpcAdminGUI npcAdminGUI) {
            event.setCancelled(true);
            handleNpcAdminClick(player, event, npcAdminGUI);
            return;
        }

        // NPC 목록 GUI
        if (event.getInventory().getHolder() instanceof NpcListGUI npcListGUI) {
            event.setCancelled(true);
            handleNpcListClick(player, event, npcListGUI);
            return;
        }

        // 채굴 블록 목록 GUI
        if (event.getInventory().getHolder() instanceof MiningBlockListGUI miningBlockListGUI) {
            event.setCancelled(true);
            handleMiningBlockListClick(player, event, miningBlockListGUI);
            return;
        }

        // 채굴 보상 GUI
        if (event.getInventory().getHolder() instanceof MiningRewardGUI miningRewardGUI) {
            event.setCancelled(true);
            handleMiningRewardClick(player, event, miningRewardGUI);
            return;
        }

        // 상점 목록 GUI (NEW)
        if (event.getInventory().getHolder() instanceof ShopListGUI shopListGUI) {
            event.setCancelled(true);
            handleShopListClick(player, event, shopListGUI);
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

        // 표시 이름 변경 (슬롯 25)
        if (slot == 25) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 표시 이름을 입력하세요. (색상 코드 & 사용 가능)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_DISPLAY_NAME, stockId));
            return;
        }

        // 아이콘 변경 (슬롯 34)
        if (slot == 34) {
            ItemStack handItem = player.getInventory().getItemInMainHand();
            if (handItem != null && handItem.getType() != Material.AIR) {
                // 손에 든 아이템으로 설정
                String materialName = handItem.getType().name();
                plugin.getConfigManager().setStockMaterial(stockId, materialName);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a아이콘이 변경되었습니다: " + materialName);
                editGUI.refresh();
            } else {
                // 채팅으로 Material 이름 입력
                player.closeInventory();
                player.sendMessage(
                        plugin.getConfigManager().getPrefix() + "§eMaterial 이름을 입력하세요. (예: DIAMOND, GOLD_INGOT)");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.STOCK_MATERIAL, stockId));
            }
            return;
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

        // Unsafe Mode (Slot 11)
        if (slot == 11) {
            boolean current = plugin.getConfigManager().getEnchantUnsafeMode(enchantId);
            plugin.getConfigManager().setEnchantUnsafeMode(enchantId, !current);
            editGUI.refresh();
            return;
        }

        // Ignore Conflicts (Slot 12)
        if (slot == 12) {
            boolean current = plugin.getConfigManager().getEnchantIgnoreConflicts(enchantId);
            plugin.getConfigManager().setEnchantIgnoreConflicts(enchantId, !current);
            editGUI.refresh();
            return;
        }

        // Target Settings (Slot 14)
        if (slot == 14) {
            new EnchantTargetGUI(plugin, player, enchantId).open();
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

    /**
     * 채굴 블록 목록 GUI 클릭 처리
     */
    private void handleMiningBlockListClick(Player player, InventoryClickEvent event, MiningBlockListGUI gui) {
        int slot = event.getRawSlot();
        if (slot == 47) { // 블록 추가
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e추가할 블록의 이름(Material)을 입력하세요.");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.MINING_ADD_BLOCK, null));
            return;
        }

        // 페이지 네비게이션은 PaginatedGui에서 처리하지 않으므로 여기서 처리하거나,
        // PaginatedGui가 클릭 이벤트를 처리하도록 설계되어 있는지 확인 필요.
        // 보통 PaginatedGui 구현체에서 네비게이션을 처리하지 않고 Listener에서 처리함.
        if (slot == 45) {
            gui.previousPage();
            return;
        }
        if (slot == 53) {
            gui.nextPage();
            return;
        }

        Material material = gui.getItemAtSlot(slot);
        if (material != null) {
            if (event.isRightClick()) {
                plugin.getConfigManager().removeMiningDropData(material);
                plugin.getConfigManager().saveMiningDropsConfig();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§c블록 설정이 삭제되었습니다.");
                gui.refresh();
            } else {
                new MiningRewardGUI(plugin, player, material).open();
            }
        }
    }

    /**
     * 채굴 보상 GUI 클릭 처리
     */
    private void handleMiningRewardClick(Player player, InventoryClickEvent event, MiningRewardGUI gui) {
        int slot = event.getRawSlot();

        // 뒤로가기
        if (slot == 48) {
            new MiningBlockListGUI(plugin, player).open();
            return;
        }

        // 보상 추가
        if (slot == 50) {
            player.closeInventory();
            // 아이템 목록 GUI 대신 간단하게 ID 입력으로 구현 (시간/복잡도 고려)
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e추가할 커스텀 아이템 ID를 입력하세요.");
            pendingInputs.put(player.getUniqueId(),
                    new ChatInputRequest(InputType.MINING_ADD_REWARD, gui.getTargetBlock().name()));
            return;
        }

        if (slot == 45) {
            gui.previousPage();
            return;
        }
        if (slot == 53) {
            gui.nextPage();
            return;
        }

        com.myserver.wildcore.config.MiningReward reward = gui.getItemAtSlot(slot);
        if (reward != null) {
            String targetId = gui.getTargetBlock().name() + ":" + reward.getItemId();

            if (event.isRightClick()) {
                // 삭제
                plugin.getMiningDropManager().removeReward(gui.getTargetBlock(), reward);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§c보상이 삭제되었습니다.");
                gui.refresh();
                return;
            }

            if (event.isShiftClick()) {
                if (event.isLeftClick()) {
                    // 최소 수량
                    player.closeInventory();
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§e최소 수량을 입력하세요.");
                    pendingInputs.put(player.getUniqueId(),
                            new ChatInputRequest(InputType.MINING_REWARD_MIN, targetId));
                } else {
                    // 최대 수량 (Shift+Right)
                    player.closeInventory();
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§e최대 수량을 입력하세요.");
                    pendingInputs.put(player.getUniqueId(),
                            new ChatInputRequest(InputType.MINING_REWARD_MAX, targetId));
                }
            } else {
                // 확률 (Left Click)
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e확률(%)을 입력하세요.");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.MINING_REWARD_CHANCE, targetId));
            }
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
            case STOCK_DISPLAY_NAME -> {
                // 색상 코드 변환 및 저장
                plugin.getConfigManager().setStockDisplayName(request.targetId, message);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a표시 이름이 변경되었습니다.");
                new StockEditGUI(plugin, player, request.targetId).open();
            }
            case STOCK_MATERIAL -> {
                // Material 이름 검증 및 저장
                try {
                    Material material = Material.valueOf(message.toUpperCase().replace(" ", "_"));
                    plugin.getConfigManager().setStockMaterial(request.targetId, material.name());
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a아이콘이 변경되었습니다: " + material.name());
                    new StockEditGUI(plugin, player, request.targetId).open();
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c존재하지 않는 Material입니다: " + message);
                    player.sendMessage(
                            plugin.getConfigManager().getPrefix() + "§7예시: DIAMOND, GOLD_INGOT, NETHERITE_INGOT");
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
            case NEW_WARP_WORLD_NAME -> {
                // 월드 존재 여부 확인 (경고만 표시하고 생성은 허용)
                if (org.bukkit.Bukkit.getWorld(message) == null) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c주의: 해당 이름의 월드가 현재 로드되어 있지 않습니다.");
                }
                new NpcAdminGUI(plugin, player).spawnWarpNpc(message);
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
            // === 상점 관련 입력 ===
            case SHOP_DISPLAY_NAME -> {
                ShopConfig shop = plugin.getConfigManager().getShop(request.targetId);
                if (shop != null) {
                    shop.setDisplayName(message);
                    plugin.getConfigManager().saveShop(shop);
                    // NPC 이름도 갱신
                    plugin.getShopManager().removeNPC(shop);
                    plugin.getShopManager().spawnNPC(shop);
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a상점 이름이 변경되었습니다.");
                    new ShopAdminGUI(plugin, player, shop).open();
                }
            }
            case SHOP_BUY_PRICE -> {
                ShopEditorGUI editor = pendingShopEditors.remove(player.getUniqueId());
                if (editor != null) {
                    try {
                        double price = Double.parseDouble(message.replace(",", ""));
                        editor.setBuyPrice(price);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                        editor.cancelPriceSetting();
                    }
                }
            }
            case SHOP_SELL_PRICE -> {
                ShopEditorGUI editor = pendingShopEditors.remove(player.getUniqueId());
                if (editor != null) {
                    try {
                        double price = Double.parseDouble(message.replace(",", ""));
                        editor.setSellPrice(price);
                    } catch (NumberFormatException e) {
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                        editor.cancelPriceSetting();
                    }
                }
            }
            case NEW_SHOP_ID -> {
                ShopConfig newShop = plugin.getShopManager().createShop(
                        message,
                        "&a새 상점",
                        player.getLocation(),
                        "VILLAGER");
                if (newShop != null) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a새 상점이 생성되었습니다: " + message);
                    new ShopAdminGUI(plugin, player, newShop).open();
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c상점 생성에 실패했습니다. (이미 존재하는 ID일 수 있습니다)");
                }
            }
            case NPC_RENAME -> {
                if (plugin.getNpcManager().renameNpc(request.targetId, message)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC 이름이 변경되었습니다.");
                    new NpcListGUI(plugin, player).open();
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC를 찾을 수 없습니다.");
                }
            }
            // === 채굴 드랍 관련 ===
            case MINING_ADD_BLOCK -> {
                try {
                    Material material = Material.valueOf(message.toUpperCase().replace(" ", "_"));
                    plugin.getMiningDropManager().addBlock(material);
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§a블록이 추가되었습니다: " + material.name());
                    new MiningBlockListGUI(plugin, player).open();
                } catch (IllegalArgumentException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c유효하지 않은 블록 이름입니다.");
                }
            }
            case MINING_ADD_REWARD -> {
                // message = itemId
                Material material = Material.valueOf(request.targetId);
                // 아이템 유효성 검사 (ConfigManager에 로드된 커스텀 아이템인지)
                if (plugin.getConfigManager().getCustomItem(message) == null) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c존재하지 않는 커스텀 아이템 ID입니다.");
                    new MiningRewardGUI(plugin, player, material).open();
                    return;
                }

                // 새 보상 추가 (기본값 설정)
                com.myserver.wildcore.config.MiningReward newReward = new com.myserver.wildcore.config.MiningReward(
                        message, 10.0, 1, 1);
                plugin.getMiningDropManager().addReward(material, newReward);
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a보상이 추가되었습니다.");
                new MiningRewardGUI(plugin, player, material).open();
            }
            case MINING_REWARD_CHANCE -> {
                try {
                    double chance = Double.parseDouble(message.replace(",", ""));
                    String[] parts = request.targetId.split(":");
                    Material material = Material.valueOf(parts[0]);
                    String itemId = parts[1];

                    com.myserver.wildcore.config.MiningReward reward = plugin.getMiningDropManager().getReward(material,
                            itemId);
                    if (reward != null) {
                        reward.setChance(chance);
                        plugin.getMiningDropManager().saveConfig();
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a확률이 수정되었습니다: " + chance + "%");
                        new MiningRewardGUI(plugin, player, material).open();
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 숫자를 입력하세요.");
                }
            }
            case MINING_REWARD_MIN -> {
                try {
                    int amount = Integer.parseInt(message.replace(",", ""));
                    String[] parts = request.targetId.split(":");
                    Material material = Material.valueOf(parts[0]);
                    String itemId = parts[1];

                    com.myserver.wildcore.config.MiningReward reward = plugin.getMiningDropManager().getReward(material,
                            itemId);
                    if (reward != null) {
                        reward.setMinAmount(amount);
                        // Max가 Min보다 작으면 자동으로 맞춤
                        if (reward.getMaxAmount() < amount) {
                            reward.setMaxAmount(amount);
                        }
                        plugin.getMiningDropManager().saveConfig();
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a최소 수량이 수정되었습니다: " + amount);
                        new MiningRewardGUI(plugin, player, material).open();
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 정수를 입력하세요.");
                }
            }
            case MINING_REWARD_MAX -> {
                try {
                    int amount = Integer.parseInt(message.replace(",", ""));
                    String[] parts = request.targetId.split(":");
                    Material material = Material.valueOf(parts[0]);
                    String itemId = parts[1];

                    com.myserver.wildcore.config.MiningReward reward = plugin.getMiningDropManager().getReward(material,
                            itemId);
                    if (reward != null) {
                        reward.setMaxAmount(amount);
                        // Min이 Max보다 크면 자동으로 맞춤
                        if (reward.getMinAmount() > amount) {
                            reward.setMinAmount(amount);
                        }
                        plugin.getMiningDropManager().saveConfig();
                        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a최대 수량이 수정되었습니다: " + amount);
                        new MiningRewardGUI(plugin, player, material).open();
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§c올바른 정수를 입력하세요.");
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
        STOCK_DISPLAY_NAME, // 주식 표시 이름 변경
        STOCK_MATERIAL, // 주식 아이콘(Material) 변경
        ENCHANT_COST,
        ENCHANT_ITEMS,
        NEW_STOCK_ID,
        NEW_WARP_WORLD_NAME, // 이동 NPC 월드 이름 입력
        NEW_ENCHANT_ID,
        ENCHANT_BUILDER_LEVEL,
        ENCHANT_BUILDER_COST,
        ENCHANT_BUILDER_ITEMS,
        // 상점 관련
        SHOP_DISPLAY_NAME,
        SHOP_BUY_PRICE,
        SHOP_SELL_PRICE,
        NEW_SHOP_ID,
        // NPC 관련
        NPC_RENAME,
        // 채굴 드랍 관련
        MINING_ADD_BLOCK,
        MINING_REWARD_CHANCE,
        MINING_REWARD_MIN,
        MINING_REWARD_MAX,
        MINING_ADD_REWARD
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
            gui.back();
            return;
        }

        // 완료
        if (slot == 53) {
            gui.saveAndClose();
        }
    }

    // === NPC 관리 GUI 핸들러 ===

    /**
     * NPC 관리 GUI 클릭 처리
     */
    private void handleNpcAdminClick(Player player, InventoryClickEvent event, NpcAdminGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;
        if (clicked.getType() == Material.ORANGE_STAINED_GLASS_PANE)
            return;

        ClickType click = event.getClick();

        // NPC 생성 버튼
        if (gui.isCreateEnchantSlot(slot)) {
            gui.spawnEnchantNpc();
            return;
        }
        if (gui.isCreateStockSlot(slot)) {
            gui.spawnStockNpc();
            return;
        }
        if (gui.isCreateBankSlot(slot)) {
            gui.spawnBankNpc();
            return;
        }
        if (gui.isCreateWarpSlot(slot)) {
            player.closeInventory();
            player.sendMessage(
                    plugin.getConfigManager().getPrefix() + "§e이동할 월드 이름을 입력하세요. (예: world, spawn, minigame)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.NEW_WARP_WORLD_NAME, null));
            return;
        }

        // NPC 목록 버튼
        if (gui.isListAllSlot(slot)) {
            new NpcListGUI(plugin, player).open();
            return;
        }
        if (gui.isListEnchantSlot(slot)) {
            if (click.isRightClick()) {
                gui.removeAllNpcs(NpcType.ENCHANT);
            } else {
                new NpcListGUI(plugin, player, NpcType.ENCHANT).open();
            }
            return;
        }
        if (gui.isListStockSlot(slot)) {
            if (click.isRightClick()) {
                gui.removeAllNpcs(NpcType.STOCK);
            } else {
                new NpcListGUI(plugin, player, NpcType.STOCK).open();
            }
            return;
        }
        if (gui.isListBankSlot(slot)) {
            if (click.isRightClick()) {
                gui.removeAllNpcs(NpcType.BANK);
            } else {
                new NpcListGUI(plugin, player, NpcType.BANK).open();
            }
            return;
        }
        if (gui.isListWarpSlot(slot)) {
            if (click.isRightClick()) {
                gui.removeAllNpcs(NpcType.WARP);
            } else {
                new NpcListGUI(plugin, player, NpcType.WARP).open();
            }
            return;
        }

        // 하단 버튼
        if (gui.isBackSlot(slot)) {
            player.closeInventory();
            return;
        }
        if (gui.isRefreshSlot(slot)) {
            gui.refresh();
            return;
        }
        if (gui.isDeleteAllSlot(slot) && click.isRightClick()) {
            gui.removeAllNonShopNpcs();
        }
    }

    /**
     * NPC 목록 GUI 클릭 처리
     */
    private void handleNpcListClick(Player player, InventoryClickEvent event, NpcListGUI gui) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 페이지네이션 처리
        if (slot == com.myserver.wildcore.gui.PaginatedGui.SLOT_PREV_PAGE) {
            if (gui.getCurrentPage() > 0) {
                gui.previousPage();
            }
            return;
        }
        if (slot == com.myserver.wildcore.gui.PaginatedGui.SLOT_NEXT_PAGE) {
            if (gui.getCurrentPage() < gui.getTotalPages() - 1) {
                gui.nextPage();
            }
            return;
        }
        // 정보 아이콘 클릭: 필터 초기화 (전체 목록으로)
        if (slot == com.myserver.wildcore.gui.PaginatedGui.SLOT_INFO) {
            if (gui.getFilterType() != null) {
                new NpcListGUI(plugin, player).open();
            }
            return;
        }
        // 네비게이션 바 영역 무시
        if (slot >= 45 && slot <= 53) {
            return;
        }

        // 배경 클릭 무시
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        // NPC 아이템 클릭
        if (slot >= 0 && slot < com.myserver.wildcore.gui.PaginatedGui.ITEMS_PER_PAGE) {
            String npcId = gui.getNpcIdAtSlot(slot);
            if (npcId == null)
                return;

            com.myserver.wildcore.npc.NpcData npcData = gui.getNpcDataAtSlot(slot);
            if (npcData == null)
                return;

            if (click.isLeftClick() && !click.isShiftClick()) {
                // 좌클릭: 텔레포트
                if (npcData.getLocation() != null) {
                    player.teleport(npcData.getLocation());
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC 위치로 이동했습니다.");
                } else {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC 위치를 찾을 수 없습니다.");
                }
            } else if (click.isRightClick() && !click.isShiftClick()) {
                // 우클릭: 이름 변경
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 NPC 이름을 입력하세요. (색상 코드 & 사용 가능)");
                pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.NPC_RENAME, npcId));
            } else if (click.isShiftClick() && click.isRightClick()) {
                // Shift+우클릭: 삭제
                if (plugin.getNpcManager().removeNpc(npcId)) {
                    player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC가 삭제되었습니다.");
                    gui.refresh();
                }
            }
        }
    }

    // === 상점 GUI 핸들러 ===

    /**
     * 상점 관리 GUI 클릭 처리
     */
    private void handleShopAdminClick(Player player, InventoryClickEvent event, ShopAdminGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR)
            return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE)
            return;

        ShopConfig shop = gui.getShop();

        // 상점 이름 변경
        if (slot == ShopAdminGUI.getSlotName()) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 상점 이름을 입력하세요. (색상 코드 & 사용 가능)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.SHOP_DISPLAY_NAME, shop.getId()));
            return;
        }

        // 위치 이동
        if (slot == ShopAdminGUI.getSlotLocation()) {
            plugin.getShopManager().moveShop(shop, player.getLocation());
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§a상점이 현재 위치로 이동되었습니다.");
            gui.refresh();
            return;
        }

        // NPC 타입 변경
        if (slot == ShopAdminGUI.getSlotNpcType()) {
            String newType = shop.isVillager() ? "ARMOR_STAND" : "VILLAGER";
            plugin.getShopManager().changeNpcType(shop, newType);
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC 타입이 변경되었습니다: " + newType);
            gui.refresh();
            return;
        }

        // 아이템 편집 모드
        if (slot == ShopAdminGUI.getSlotEditItems()) {
            new ShopEditorGUI(plugin, player, shop).open();
            return;
        }

        // 뒤로 가기
        if (slot == ShopAdminGUI.getSlotBack()) {
            new ShopGUI(plugin, player, shop).open();
            return;
        }

        // 상점 삭제
        if (slot == ShopAdminGUI.getSlotDelete()) {
            if (event.getClick().isShiftClick()) {
                plugin.getShopManager().deleteShop(shop.getId());
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§c상점이 삭제되었습니다: " + shop.getId());
                player.closeInventory();
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§c삭제하려면 Shift+클릭하세요.");
            }
            return;
        }

        // 닫기
        if (slot == ShopAdminGUI.getSlotClose()) {
            player.closeInventory();
        }
    }

    /**
     * 상점 아이템 편집 GUI 클릭 처리
     */
    private void handleShopEditorClick(Player player, InventoryClickEvent event, ShopEditorGUI gui) {
        int slot = event.getRawSlot();
        ItemStack clicked = event.getCurrentItem();
        ClickType click = event.getClick();

        // 플레이어 인벤토리 클릭은 허용 (아이템을 집을 수 있도록)
        if (event.getClickedInventory() == player.getInventory()) {
            // Shift 클릭으로 상단 GUI로 이동하는 것은 막음
            if (click.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        // 상단 GUI 클릭은 기본적으로 취소
        event.setCancelled(true);

        // 네비게이션 영역 (45~53)
        if (slot >= 45 && slot <= 53) {
            if (slot == ShopEditorGUI.getSlotBack()) {
                new ShopAdminGUI(plugin, player, gui.getShop()).open();
                return;
            }

            if (slot == ShopEditorGUI.getSlotSave()) {
                player.sendMessage(plugin.getConfigManager().getPrefix() + "§a상점 아이템이 저장되었습니다.");
                new ShopAdminGUI(plugin, player, gui.getShop()).open();
                return;
            }

            if (slot == ShopEditorGUI.getSlotHelp()) {
                return; // 도움말 클릭 무시
            }
            return;
        }

        // 아이템 영역 (0~44)
        if (gui.isItemSlot(slot)) {
            // 기존 아이템이 있는 슬롯 클릭
            if (clicked != null && clicked.getType() != Material.AIR
                    && clicked.getType() != Material.LIGHT_GRAY_STAINED_GLASS_PANE) {

                if (click.isShiftClick() && click.isRightClick()) {
                    // Shift+우클릭: 아이템 제거
                    gui.removeItem(slot);
                } else if (click.isLeftClick()) {
                    // 좌클릭: 가격 설정
                    gui.startPriceSetting(slot);
                }
            } else {
                // 빈 슬롯에 커서에 든 아이템 등록
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    gui.registerItem(slot, cursor.clone());
                    // 커서 아이템 소모
                    player.setItemOnCursor(null);
                }
            }
        }
    }

    /**
     * 상점 목록 GUI 클릭 처리
     */
    private void handleShopListClick(Player player, InventoryClickEvent event, ShopListGUI gui) {
        int slot = event.getRawSlot();
        ClickType click = event.getClick();

        // 페이지네이션
        if (slot == com.myserver.wildcore.gui.PaginatedGui.SLOT_PREV_PAGE) {
            if (gui.getCurrentPage() > 0) {
                gui.previousPage();
            }
            return;
        }
        if (slot == com.myserver.wildcore.gui.PaginatedGui.SLOT_NEXT_PAGE) {
            if (gui.getCurrentPage() < gui.getTotalPages() - 1) {
                gui.nextPage();
            }
            return;
        }

        // 새 상점 생성 (49번 슬롯)
        if (slot == 49) {
            player.closeInventory();
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§e새 상점의 ID를 입력하세요. (영문/숫자)");
            pendingInputs.put(player.getUniqueId(), new ChatInputRequest(InputType.NEW_SHOP_ID, null));
            return;
        }

        // 아이템 영역
        ShopConfig shop = gui.getItemAtSlot(slot);
        if (shop != null) {
            if (click.isShiftClick() && click.isLeftClick()) {
                // Shift+Left: Teleport
                if (shop.getLocation() != null) {
                    player.teleport(shop.getLocation());
                    player.sendMessage(
                            plugin.getConfigManager().getPrefix() + "§a상점으로 이동했습니다: " + shop.getDisplayName());
                }
            } else if (click.isRightClick()) {
                // Right: Open Shop (User View)
                new ShopGUI(plugin, player, shop).open();
            } else {
                // Left / Default: Open Admin GUI
                new ShopAdminGUI(plugin, player, shop).open();
            }
        }
    }

    /**
     * 상점 편집기 참조 저장 (가격 입력용)
     */
    private final Map<UUID, ShopEditorGUI> pendingShopEditors = new HashMap<>();

    public ShopEditorGUI getPendingShopEditor(UUID uuid) {
        return pendingShopEditors.get(uuid);
    }

    public void setPendingShopEditor(UUID uuid, ShopEditorGUI editor) {
        pendingShopEditors.put(uuid, editor);
    }

    public void removePendingShopEditor(UUID uuid) {
        pendingShopEditors.remove(uuid);
    }
}
