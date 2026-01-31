package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.PlayerStockData;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.util.ItemUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 디버깅 및 테스트용 명령어
 * /wildcore debug <subcommand> [args...]
 */
public class DebugCommand {

    private final WildCore plugin;

    public DebugCommand(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 디버그 명령어 처리
     */
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildcore.debug")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length < 2) {
            sendDebugHelp(sender);
            return;
        }

        String subCommand = args[1].toLowerCase();

        switch (subCommand) {
            // === 경제 관련 ===
            case "money" -> handleMoney(sender, args);
            case "balance" -> handleBalance(sender, args);

            // === 주식 관련 ===
            case "setprice" -> handleSetPrice(sender, args);
            case "updateprices" -> handleUpdatePrices(sender);
            case "setstocks" -> handleSetStocks(sender, args);
            case "clearstocks" -> handleClearStocks(sender, args);
            case "stockinfo" -> handleStockInfo(sender, args);

            // === 아이템 관련 ===
            case "giveall" -> handleGiveAll(sender, args);
            case "giveitem" -> handleGiveItem(sender, args);
            case "clearinv" -> handleClearInventory(sender, args);
            case "iteminfo" -> handleItemInfo(sender, args);

            // === 인챈트 관련 ===
            case "enchantinfo" -> handleEnchantInfo(sender, args);
            case "testenchant" -> handleTestEnchant(sender, args);

            // === 플레이어 정보 ===
            case "playerinfo" -> handlePlayerInfo(sender, args);

            // === 시스템 ===
            case "forcereload" -> handleForceReload(sender);
            case "saveall" -> handleSaveAll(sender);
            case "toggledebug" -> handleToggleDebug(sender);
            case "status" -> handleStatus(sender);
            case "check" -> handleCheck(sender, args);

            default -> sendDebugHelp(sender);
        }
    }

    // === 경제 관련 ===

    private void handleMoney(CommandSender sender, String[] args) {
        // /wildcore debug money <give|take|set> <player> <amount>
        if (args.length < 5) {
            sender.sendMessage("§c사용법: /wildcore debug money <give|take|set> <플레이어> <금액>");
            return;
        }

        String action = args[2].toLowerCase();
        Player target = Bukkit.getPlayer(args[3]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[3]);
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 숫자를 입력하세요.");
            return;
        }

        switch (action) {
            case "give" -> {
                plugin.getEconomy().depositPlayer(target, amount);
                sender.sendMessage("§a" + target.getName() + "에게 " + format(amount) + "원을 지급했습니다.");
                sender.sendMessage("§7현재 잔액: " + format(plugin.getEconomy().getBalance(target)) + "원");
            }
            case "take" -> {
                plugin.getEconomy().withdrawPlayer(target, amount);
                sender.sendMessage("§c" + target.getName() + "에게서 " + format(amount) + "원을 차감했습니다.");
                sender.sendMessage("§7현재 잔액: " + format(plugin.getEconomy().getBalance(target)) + "원");
            }
            case "set" -> {
                double current = plugin.getEconomy().getBalance(target);
                plugin.getEconomy().withdrawPlayer(target, current);
                plugin.getEconomy().depositPlayer(target, amount);
                sender.sendMessage("§e" + target.getName() + "의 잔액을 " + format(amount) + "원으로 설정했습니다.");
            }
            default -> sender.sendMessage("§c알 수 없는 작업: " + action + " (give/take/set)");
        }
    }

    private void handleBalance(CommandSender sender, String[] args) {
        // /wildcore debug balance [player]
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c플레이어를 지정하세요.");
            return;
        }

        sender.sendMessage("§6" + target.getName() + "§7의 잔액: §f" +
                format(plugin.getEconomy().getBalance(target)) + "원");
    }

    // === 주식 관련 ===

    private void handleSetPrice(CommandSender sender, String[] args) {
        // /wildcore debug setprice <stockId> <price>
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /wildcore debug setprice <종목ID> <가격>");
            return;
        }

        String stockId = args[2];
        if (plugin.getConfigManager().getStock(stockId) == null) {
            sender.sendMessage("§c존재하지 않는 주식: " + stockId);
            sender.sendMessage("§7사용 가능: " + String.join(", ", plugin.getConfigManager().getStocks().keySet()));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 숫자를 입력하세요.");
            return;
        }

        plugin.getStockManager().setCurrentPrice(stockId, price);
        sender.sendMessage("§a주식 " + stockId + "의 가격을 " + format(price) + "원으로 설정했습니다.");
    }

    private void handleUpdatePrices(CommandSender sender) {
        plugin.getStockManager().updateAllPrices();
        sender.sendMessage("§a모든 주식 가격이 강제 갱신되었습니다.");

        // 갱신된 가격 표시
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            double price = plugin.getStockManager().getCurrentPrice(stock.getId());
            String change = plugin.getStockManager().getFormattedChange(stock.getId());
            sender.sendMessage("§7- " + stock.getDisplayName() + "§7: §f" + format(price) + "원 " + change);
        }
    }

    private void handleSetStocks(CommandSender sender, String[] args) {
        // /wildcore debug setstocks <player> <stockId> <amount>
        if (args.length < 5) {
            sender.sendMessage("§c사용법: /wildcore debug setstocks <플레이어> <종목ID> <수량>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        String stockId = args[3];
        if (plugin.getConfigManager().getStock(stockId) == null) {
            sender.sendMessage("§c존재하지 않는 주식: " + stockId);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§c올바른 정수를 입력하세요.");
            return;
        }

        // 직접 주식 수량 설정 (StockManager에 메서드 필요)
        plugin.getStockManager().setPlayerStockAmount(target.getUniqueId(), stockId, amount);
        sender.sendMessage("§a" + target.getName() + "의 " + stockId + " 보유량을 " + amount + "주로 설정했습니다.");
    }

    private void handleClearStocks(CommandSender sender, String[] args) {
        // /wildcore debug clearstocks <player>
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /wildcore debug clearstocks <플레이어>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        plugin.getStockManager().clearPlayerStocks(target.getUniqueId());
        sender.sendMessage("§c" + target.getName() + "의 모든 주식 보유량을 초기화했습니다.");
    }

    private void handleStockInfo(CommandSender sender, String[] args) {
        // /wildcore debug stockinfo [player]
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ 주식 시스템 정보 ]");
        sender.sendMessage("§8§m                                        ");

        // 모든 주식 현황
        sender.sendMessage("§e현재 주식 가격:");
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            double price = plugin.getStockManager().getCurrentPrice(stock.getId());
            double prev = plugin.getStockManager().getPreviousPrice(stock.getId());
            String change = plugin.getStockManager().getFormattedChange(stock.getId());
            sender.sendMessage("  §7" + stock.getId() + ": §f" + format(price) + "원 " + change +
                    " §8(이전: " + format(prev) + "원)");
        }

        // 특정 플레이어 정보
        if (args.length >= 3) {
            Player target = Bukkit.getPlayer(args[2]);
            if (target != null) {
                sender.sendMessage("");
                sender.sendMessage("§e" + target.getName() + "의 보유 주식:");
                Map<String, PlayerStockData> stocks = plugin.getStockManager().getPlayerStocks(target.getUniqueId());
                if (stocks.isEmpty()) {
                    sender.sendMessage("  §7(보유 주식 없음)");
                } else {
                    double totalValue = 0;
                    for (Map.Entry<String, PlayerStockData> entry : stocks.entrySet()) {
                        double price = plugin.getStockManager().getCurrentPrice(entry.getKey());
                        int amount = entry.getValue().getAmount();
                        double value = price * amount;
                        totalValue += value;
                        sender.sendMessage("  §7" + entry.getKey() + ": §f" + amount +
                                "주 §8(가치: " + format(value) + "원)");
                    }
                    sender.sendMessage("  §6총 자산 가치: §f" + format(totalValue) + "원");
                }
            }
        }
    }

    // === 아이템 관련 ===

    private void handleGiveAll(CommandSender sender, String[] args) {
        // /wildcore debug giveall <player> [amount]
        if (!(args.length >= 3)) {
            sender.sendMessage("§c사용법: /wildcore debug giveall <플레이어> [수량]");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;

        // 모든 커스텀 아이템 지급
        int count = 0;
        for (String itemId : plugin.getConfigManager().getCustomItems().keySet()) {
            ItemStack item = ItemUtil.createCustomItem(plugin, itemId, amount);
            if (item != null) {
                target.getInventory().addItem(item);
                count++;
            }
        }

        sender.sendMessage("§a" + target.getName() + "에게 모든 커스텀 아이템 " + count + "종을 각 " + amount + "개씩 지급했습니다.");
    }

    private void handleClearInventory(CommandSender sender, String[] args) {
        // /wildcore debug clearinv <player>
        if (args.length < 3) {
            sender.sendMessage("§c사용법: /wildcore debug clearinv <플레이어>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        target.getInventory().clear();
        sender.sendMessage("§c" + target.getName() + "의 인벤토리를 초기화했습니다.");
    }

    // === 플레이어 정보 ===

    private void handlePlayerInfo(CommandSender sender, String[] args) {
        // /wildcore debug playerinfo <player>
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c플레이어를 지정하세요.");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ 플레이어 정보: " + target.getName() + " ]");
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§7UUID: §f" + target.getUniqueId());
        sender.sendMessage("§7잔액: §6" + format(plugin.getEconomy().getBalance(target)) + "원");
        sender.sendMessage("§7레벨: §a" + target.getLevel() + " §7(경험치: " + target.getExp() + ")");
        sender.sendMessage("§7위치: §f" + target.getWorld().getName() + " " +
                (int) target.getLocation().getX() + ", " +
                (int) target.getLocation().getY() + ", " +
                (int) target.getLocation().getZ());

        // 주식 보유 현황
        Map<String, PlayerStockData> stocks = plugin.getStockManager().getPlayerStocks(target.getUniqueId());
        if (!stocks.isEmpty()) {
            sender.sendMessage("§7보유 주식:");
            for (Map.Entry<String, PlayerStockData> entry : stocks.entrySet()) {
                sender.sendMessage("  §7- " + entry.getKey() + ": §f" + entry.getValue().getAmount() + "주");
            }
        } else {
            sender.sendMessage("§7보유 주식: §8(없음)");
        }

        // 인벤토리 내 커스텀 아이템
        sender.sendMessage("§7보유 커스텀 아이템:");
        boolean hasCustomItems = false;
        for (ItemStack item : target.getInventory().getContents()) {
            String customId = ItemUtil.getCustomItemId(plugin, item);
            if (customId != null) {
                sender.sendMessage("  §7- " + customId + ": §f" + item.getAmount() + "개");
                hasCustomItems = true;
            }
        }
        if (!hasCustomItems) {
            sender.sendMessage("  §8(없음)");
        }
    }

    // === 시스템 ===

    private void handleForceReload(CommandSender sender) {
        sender.sendMessage("§e설정 강제 리로드 중...");
        plugin.reload();
        sender.sendMessage("§a완료! 모든 설정이 리로드되었습니다.");
    }

    private void handleSaveAll(CommandSender sender) {
        sender.sendMessage("§e모든 데이터 저장 중...");
        plugin.getStockManager().saveAllData();
        sender.sendMessage("§a완료! 주식 데이터가 저장되었습니다.");
    }

    private void handleToggleDebug(CommandSender sender) {
        // config.yml의 debug 설정은 런타임에 변경 불가하므로 임시 토글
        sender.sendMessage("§e디버그 모드를 토글하려면 config.yml에서 debug: true/false로 설정 후 /wildcore reload를 실행하세요.");
        sender.sendMessage("§7현재 디버그 모드: " + (plugin.getConfigManager().isDebugEnabled() ? "§a활성화" : "§c비활성화"));
    }

    // === 새 추가 명령어들 ===

    private void handleGiveItem(CommandSender sender, String[] args) {
        // /wildcore debug giveitem <player> <itemId> [amount]
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /wildcore debug giveitem <플레이어> <아이템ID> [수량]");
            sender.sendMessage(
                    "§7사용 가능한 아이템: " + String.join(", ", plugin.getConfigManager().getCustomItems().keySet()));
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        String itemId = args[3];
        if (plugin.getConfigManager().getCustomItem(itemId) == null) {
            sender.sendMessage("§c존재하지 않는 아이템: " + itemId);
            sender.sendMessage("§7사용 가능: " + String.join(", ", plugin.getConfigManager().getCustomItems().keySet()));
            return;
        }

        int amount = args.length >= 5 ? parseInt(args[4], 1) : 1;

        ItemStack item = ItemUtil.createCustomItem(plugin, itemId, amount);
        if (item != null) {
            target.getInventory().addItem(item);
            sender.sendMessage("§a" + target.getName() + "에게 " + itemId + " " + amount + "개를 지급했습니다.");
        } else {
            sender.sendMessage("§c아이템 생성에 실패했습니다.");
        }
    }

    private void handleItemInfo(CommandSender sender, String[] args) {
        // /wildcore debug iteminfo [player] - 손에 든 아이템 정보
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c플레이어를 지정하세요.");
            return;
        }

        ItemStack item = target.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            sender.sendMessage("§c손에 아이템이 없습니다.");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ 아이템 정보 ]");
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§7타입: §f" + item.getType().name());
        sender.sendMessage("§7수량: §f" + item.getAmount());

        // 커스텀 아이템 ID
        String customId = ItemUtil.getCustomItemId(plugin, item);
        sender.sendMessage("§7WildCore ID: " + (customId != null ? "§a" + customId : "§8(없음)"));

        // 인챈트 정보
        if (!item.getEnchantments().isEmpty()) {
            sender.sendMessage("§7인챈트:");
            item.getEnchantments()
                    .forEach((ench, level) -> sender.sendMessage("  §7- " + ench.getKey().getKey() + " §fLv." + level));
        }

        // 메타 정보
        if (item.hasItemMeta()) {
            var meta = item.getItemMeta();
            if (meta.hasDisplayName()) {
                Component displayName = meta.displayName();
                String name = displayName != null ? LegacyComponentSerializer.legacySection().serialize(displayName)
                        : "null";
                sender.sendMessage("§7이름: §f" + name);
            }
            if (meta.hasCustomModelData()) {
                sender.sendMessage("§7CustomModelData: §f" + meta.getCustomModelData());
            }
        }
    }

    private void handleEnchantInfo(CommandSender sender, String[] args) {
        // /wildcore debug enchantinfo [enchantId|all]
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ 인챈트 설정 정보 ]");
        sender.sendMessage("§8§m                                        ");

        if (args.length >= 3 && !args[2].equalsIgnoreCase("all")) {
            // 특정 인챈트 상세 정보
            String enchantId = args[2];
            var enchant = plugin.getConfigManager().getEnchant(enchantId);
            if (enchant == null) {
                sender.sendMessage("§c존재하지 않는 인챈트: " + enchantId);
                sender.sendMessage("§7사용 가능: " + String.join(", ", plugin.getConfigManager().getEnchants().keySet()));
                return;
            }

            sender.sendMessage("§e" + enchantId);
            sender.sendMessage("  §7표시명: " + enchant.getDisplayName());
            sender.sendMessage("  §7결과: §f" + enchant.getResultEnchantment() + " Lv." + enchant.getResultLevel());
            sender.sendMessage("  §7성공/실패/파괴: §a" + enchant.getSuccessRate() + "% §e" +
                    enchant.getFailRate() + "% §c" + enchant.getDestroyRate() + "%");
            sender.sendMessage("  §7비용: §6" + format(enchant.getCostMoney()) + "원");
            sender.sendMessage("  §7재료: §f" + String.join(", ", enchant.getCostItems()));
            sender.sendMessage("  §7타겟 그룹: §f" + String.join(", ", enchant.getTargetGroups()));
            sender.sendMessage("  §7Unsafe 모드: " + (enchant.isUnsafeMode() ? "§a활성화" : "§c비활성화"));
            sender.sendMessage("  §7충돌 무시: " + (enchant.isIgnoreConflicts() ? "§a활성화" : "§c비활성화"));
        } else {
            // 전체 목록
            sender.sendMessage("§7총 " + plugin.getConfigManager().getEnchants().size() + "개의 인챈트:");
            for (var entry : plugin.getConfigManager().getEnchants().entrySet()) {
                var e = entry.getValue();
                sender.sendMessage("  §7" + entry.getKey() + ": §f" + e.getResultEnchantment() +
                        " Lv." + e.getResultLevel() + " §7(성공 §a" + e.getSuccessRate() + "%§7)");
            }
            sender.sendMessage("");
            sender.sendMessage("§7상세 보기: §e/wildcore debug enchantinfo <ID>");
        }
    }

    private void handleTestEnchant(CommandSender sender, String[] args) {
        // /wildcore debug testenchant <player> <enchantId>
        if (args.length < 4) {
            sender.sendMessage("§c사용법: /wildcore debug testenchant <플레이어> <인챈트ID>");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
            return;
        }

        String enchantId = args[3];
        if (plugin.getConfigManager().getEnchant(enchantId) == null) {
            sender.sendMessage("§c존재하지 않는 인챈트: " + enchantId);
            return;
        }

        // 인챈트 시도 (실제 확률 적용)
        ItemStack handItem = target.getInventory().getItemInMainHand();
        if (handItem == null || handItem.getType().isAir()) {
            sender.sendMessage("§c대상 플레이어가 손에 아이템을 들고 있지 않습니다.");
            return;
        }

        sender.sendMessage("§e" + target.getName() + "에게 " + enchantId + " 인챈트를 시도합니다...");
        plugin.getEnchantManager().tryEnchant(target, enchantId);
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ WildCore 상태 ]");
        sender.sendMessage("§8§m                                        ");

        // 플러그인 정보
        sender.sendMessage("§7버전: §f" + plugin.getPluginMeta().getVersion());
        sender.sendMessage("§7디버그 모드: " + (plugin.getConfigManager().isDebugEnabled() ? "§a활성화" : "§c비활성화"));

        // 데이터 현황
        sender.sendMessage("");
        sender.sendMessage("§e[데이터 현황]");
        sender.sendMessage("§7주식 종목: §f" + plugin.getConfigManager().getStocks().size() + "개");
        sender.sendMessage("§7인챈트 설정: §f" + plugin.getConfigManager().getEnchants().size() + "개");
        sender.sendMessage("§7커스텀 아이템: §f" + plugin.getConfigManager().getCustomItems().size() + "개");

        // 경제 연동
        sender.sendMessage("");
        sender.sendMessage("§e[경제 연동]");
        sender.sendMessage("§7Vault: §a연결됨");
        sender.sendMessage("§7경제 플러그인: §f" + plugin.getEconomy().getName());

        // 온라인 플레이어 현황
        sender.sendMessage("");
        sender.sendMessage("§e[온라인 현황]");
        sender.sendMessage("§7온라인 플레이어: §f" + Bukkit.getOnlinePlayers().size() + "명");
    }

    // === 헬퍼 ===

    private String format(double value) {
        return String.format("%,.0f", value);
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void sendDebugHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c디버그 명령어");
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§c[경제]");
        sender.sendMessage("§e/wc debug money <give|take|set> <플레이어> <금액>");
        sender.sendMessage("§e/wc debug balance [플레이어]");
        sender.sendMessage("");
        sender.sendMessage("§c[주식]");
        sender.sendMessage("§e/wc debug setprice <종목ID> <가격>");
        sender.sendMessage("§e/wc debug updateprices §7- 전체 가격 갱신");
        sender.sendMessage("§e/wc debug setstocks <플레이어> <종목ID> <수량>");
        sender.sendMessage("§e/wc debug clearstocks <플레이어>");
        sender.sendMessage("§e/wc debug stockinfo [플레이어]");
        sender.sendMessage("");
        sender.sendMessage("§c[아이템]");
        sender.sendMessage("§e/wc debug giveall <플레이어> [수량] §7- 모든 커스텀 아이템");
        sender.sendMessage("§e/wc debug giveitem <플레이어> <아이템ID> [수량]");
        sender.sendMessage("§e/wc debug clearinv <플레이어> §7- 인벤토리 초기화");
        sender.sendMessage("§e/wc debug iteminfo [플레이어] §7- 손에 든 아이템 정보");
        sender.sendMessage("");
        sender.sendMessage("§c[인챈트]");
        sender.sendMessage("§e/wc debug enchantinfo [인챈트ID|all] §7- 설정 정보");
        sender.sendMessage("§e/wc debug testenchant <플레이어> <인챈트ID>");
        sender.sendMessage("");
        sender.sendMessage("§c[플레이어]");
        sender.sendMessage("§e/wc debug playerinfo [플레이어] §7- 상세 정보 조회");
        sender.sendMessage("");
        sender.sendMessage("§c[시스템]");
        sender.sendMessage("§e/wc debug forcereload §7- 강제 리로드");
        sender.sendMessage("§e/wc debug saveall §7- 데이터 저장");
        sender.sendMessage("§e/wc debug status §7- 플러그인 상태 조회");
        sender.sendMessage("§e/wc debug toggledebug §7- 디버그 모드 상태");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * 탭 완성 - 각 서브커맨드별 세부 완성 지원
     */
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        // args[0] = "debug"
        // args[1] = 서브커맨드
        // args[2+] = 인자

        if (args.length == 2) {
            // 서브커맨드 목록 (카테고리별 정렬)
            completions.addAll(List.of(
                    // 경제
                    "money", "balance",
                    // 주식
                    "setprice", "updateprices", "setstocks", "clearstocks", "stockinfo",
                    // 아이템
                    "giveall", "giveitem", "clearinv", "iteminfo",
                    // 인챈트
                    "enchantinfo", "testenchant",
                    // 플레이어
                    "playerinfo",
                    // 시스템
                    "forcereload", "saveall", "toggledebug", "status", "check"));
        } else if (args.length >= 3) {
            String sub = args[1].toLowerCase();

            switch (sub) {
                // /wildcore debug money <give|take|set> <player> <amount>
                case "money" -> {
                    if (args.length == 3) {
                        completions.addAll(List.of("give", "take", "set"));
                    } else if (args.length == 4) {
                        addOnlinePlayers(completions);
                    } else if (args.length == 5) {
                        completions.addAll(List.of("1000", "10000", "100000", "1000000"));
                    }
                }

                // /wildcore debug balance [player]
                case "balance" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }

                // /wildcore debug setprice <stockId> <price>
                case "setprice" -> {
                    if (args.length == 3) {
                        completions.addAll(plugin.getConfigManager().getStocks().keySet());
                    } else if (args.length == 4) {
                        // 해당 주식의 현재 가격 기반 제안
                        String stockId = args[2];
                        if (plugin.getConfigManager().getStock(stockId) != null) {
                            double current = plugin.getStockManager().getCurrentPrice(stockId);
                            completions.addAll(List.of(
                                    String.valueOf((int) (current * 0.5)), // 50%
                                    String.valueOf((int) current), // 현재
                                    String.valueOf((int) (current * 1.5)), // 150%
                                    String.valueOf((int) (current * 2)) // 200%
                            ));
                        }
                    }
                }

                // /wildcore debug setstocks <player> <stockId> <amount>
                case "setstocks" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    } else if (args.length == 4) {
                        completions.addAll(plugin.getConfigManager().getStocks().keySet());
                    } else if (args.length == 5) {
                        completions.addAll(List.of("1", "10", "50", "100", "500", "1000"));
                    }
                }

                // /wildcore debug clearstocks <player>
                case "clearstocks" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }

                // /wildcore debug stockinfo [player]
                case "stockinfo" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }

                // /wildcore debug giveall <player> [amount]
                case "giveall" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    } else if (args.length == 4) {
                        completions.addAll(List.of("1", "5", "10", "64"));
                    }
                }

                // /wildcore debug giveitem <player> <itemId> [amount]
                case "giveitem" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    } else if (args.length == 4) {
                        completions.addAll(plugin.getConfigManager().getCustomItems().keySet());
                    } else if (args.length == 5) {
                        completions.addAll(List.of("1", "5", "10", "64"));
                    }
                }

                // /wildcore debug clearinv <player>
                case "clearinv" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }

                // /wildcore debug iteminfo [player]
                case "iteminfo" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }

                // /wildcore debug enchantinfo [enchantId]
                case "enchantinfo" -> {
                    if (args.length == 3) {
                        completions.addAll(plugin.getConfigManager().getEnchants().keySet());
                        completions.add("all");
                    }
                }

                // /wildcore debug testenchant <player> <enchantId>
                case "testenchant" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    } else if (args.length == 4) {
                        completions.addAll(plugin.getConfigManager().getEnchants().keySet());
                    }
                }

                // /wildcore debug playerinfo [player]
                case "playerinfo" -> {
                    if (args.length == 3) {
                        addOnlinePlayers(completions);
                    }
                }
            }
        }

        // 입력값으로 필터링
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .toList();
    }

    private void handleCheck(CommandSender sender, String[] args) {
        // /wildcore debug check [player]
        Player target;
        if (args.length >= 3) {
            target = Bukkit.getPlayer(args[2]);
            if (target == null) {
                sender.sendMessage("§c플레이어를 찾을 수 없습니다: " + args[2]);
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage("§c플레이어를 지정하세요.");
            return;
        }

        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§6[ 시스템 점검: " + target.getName() + " ]");
        sender.sendMessage("§8§m                                        ");

        // 1. 권한 확인
        sender.sendMessage("§e1. 권한 확인");
        checkPermission(sender, target, "wildcore.use");
        checkPermission(sender, target, "wildcore.stock");
        checkPermission(sender, target, "wildcore.enchant");
        checkPermission(sender, target, "wildcore.bank.use");
        checkPermission(sender, target, "wildcore.bank.use");
        checkPermission(sender, target, "wildcore.admin");
        checkPermission(sender, target, "wildcore.actionbar.money");

        // 2. 시스템 활성화 확인
        sender.sendMessage("");
        sender.sendMessage("§e2. 시스템 설정");
        boolean stockEnabled = plugin.getConfigManager().isStockSystemEnabled();
        sender.sendMessage("  §7주식 시스템: " + (stockEnabled ? "§a활성화" : "§c비활성화"));
        boolean bankEnabled = plugin.getConfigManager().isBankSystemEnabled();
        sender.sendMessage("  §7은행 시스템: " + (bankEnabled ? "§a활성화" : "§c비활성화"));

        // 3. GUI 초기화 테스트
        sender.sendMessage("");
        sender.sendMessage("§e3. GUI 초기화 테스트");

        // 주식 GUI
        try {
            new com.myserver.wildcore.gui.StockGUI(plugin, target);
            sender.sendMessage("  §7StockGUI: §a정상 생성됨");
        } catch (Exception e) {
            sender.sendMessage("  §7StockGUI: §c오류 발생 (" + e.getMessage() + ")");
            e.printStackTrace();
        }

        // 인챈트 GUI
        try {
            new com.myserver.wildcore.gui.EnchantGUI(plugin, target);
            sender.sendMessage("  §7EnchantGUI: §a정상 생성됨");
        } catch (Exception e) {
            sender.sendMessage("  §7EnchantGUI: §c오류 발생 (" + e.getMessage() + ")");
            e.printStackTrace();
        }

        // 은행 GUI
        try {
            new com.myserver.wildcore.gui.BankMainGUI(plugin, target);
            sender.sendMessage("  §7BankMainGUI: §a정상 생성됨");
        } catch (Exception e) {
            sender.sendMessage("  §7BankMainGUI: §c오류 발생 (" + e.getMessage() + ")");
            e.printStackTrace();
        }
    }

    private void checkPermission(CommandSender sender, Player target, String permission) {
        boolean has = target.hasPermission(permission);
        sender.sendMessage("  §7" + permission + ": " + (has ? "§aO" : "§cX"));
    }

    /**
     * 온라인 플레이어 목록 추가
     */
    private void addOnlinePlayers(List<String> completions) {
        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
    }
}
