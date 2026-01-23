package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.util.ItemUtil;
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
            case "clearinv" -> handleClearInventory(sender, args);

            // === 플레이어 정보 ===
            case "playerinfo" -> handlePlayerInfo(sender, args);

            // === 시스템 ===
            case "forcereload" -> handleForceReload(sender);
            case "saveall" -> handleSaveAll(sender);
            case "toggledebug" -> handleToggleDebug(sender);

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
                Map<String, Integer> stocks = plugin.getStockManager().getPlayerStocks(target.getUniqueId());
                if (stocks.isEmpty()) {
                    sender.sendMessage("  §7(보유 주식 없음)");
                } else {
                    double totalValue = 0;
                    for (Map.Entry<String, Integer> entry : stocks.entrySet()) {
                        double price = plugin.getStockManager().getCurrentPrice(entry.getKey());
                        double value = price * entry.getValue();
                        totalValue += value;
                        sender.sendMessage("  §7" + entry.getKey() + ": §f" + entry.getValue() +
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
        Map<String, Integer> stocks = plugin.getStockManager().getPlayerStocks(target.getUniqueId());
        if (!stocks.isEmpty()) {
            sender.sendMessage("§7보유 주식:");
            for (Map.Entry<String, Integer> entry : stocks.entrySet()) {
                sender.sendMessage("  §7- " + entry.getKey() + ": §f" + entry.getValue() + "주");
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
        sender.sendMessage("§e/wildcore debug money <give|take|set> <플레이어> <금액>");
        sender.sendMessage("§e/wildcore debug balance [플레이어]");
        sender.sendMessage("");
        sender.sendMessage("§c[주식]");
        sender.sendMessage("§e/wildcore debug setprice <종목ID> <가격>");
        sender.sendMessage("§e/wildcore debug updateprices §7- 전체 가격 갱신");
        sender.sendMessage("§e/wildcore debug setstocks <플레이어> <종목ID> <수량>");
        sender.sendMessage("§e/wildcore debug clearstocks <플레이어>");
        sender.sendMessage("§e/wildcore debug stockinfo [플레이어]");
        sender.sendMessage("");
        sender.sendMessage("§c[아이템]");
        sender.sendMessage("§e/wildcore debug giveall <플레이어> [수량] §7- 모든 커스텀 아이템 지급");
        sender.sendMessage("§e/wildcore debug clearinv <플레이어> §7- 인벤토리 초기화");
        sender.sendMessage("");
        sender.sendMessage("§c[플레이어]");
        sender.sendMessage("§e/wildcore debug playerinfo [플레이어] §7- 상세 정보 조회");
        sender.sendMessage("");
        sender.sendMessage("§c[시스템]");
        sender.sendMessage("§e/wildcore debug forcereload §7- 강제 리로드");
        sender.sendMessage("§e/wildcore debug saveall §7- 데이터 저장");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * 탭 완성
     */
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            completions.addAll(List.of(
                    "money", "balance",
                    "setprice", "updateprices", "setstocks", "clearstocks", "stockinfo",
                    "giveall", "clearinv",
                    "playerinfo",
                    "forcereload", "saveall", "toggledebug"));
        } else if (args.length == 3) {
            String sub = args[1].toLowerCase();
            if (sub.equals("money") || sub.equals("setstocks") || sub.equals("clearstocks") ||
                    sub.equals("giveall") || sub.equals("clearinv") || sub.equals("playerinfo") ||
                    sub.equals("balance") || sub.equals("stockinfo")) {
                // 온라인 플레이어
                Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
            } else if (sub.equals("setprice")) {
                // 주식 종목
                completions.addAll(plugin.getConfigManager().getStocks().keySet());
            }
        } else if (args.length == 4) {
            String sub = args[1].toLowerCase();
            if (sub.equals("money")) {
                completions.addAll(List.of("give", "take", "set"));
            } else if (sub.equals("setstocks")) {
                completions.addAll(plugin.getConfigManager().getStocks().keySet());
            }
        }

        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .toList();
    }
}
