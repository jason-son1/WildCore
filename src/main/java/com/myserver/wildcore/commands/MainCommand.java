package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.CustomItemConfig;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.admin.EnchantAdminGUI;
import com.myserver.wildcore.gui.admin.StockAdminGUI;
import com.myserver.wildcore.gui.shop.ShopAdminGUI;
import com.myserver.wildcore.gui.shop.ShopGUI;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * /wildcore 메인 명령어 처리
 */
public class MainCommand implements CommandExecutor, TabCompleter {

    private final WildCore plugin;
    private final DebugCommand debugCommand;

    public MainCommand(WildCore plugin) {
        this.plugin = plugin;
        this.debugCommand = new DebugCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> handleReload(sender);
            case "stock" -> handleStock(sender, args);
            case "enchant" -> handleEnchant(sender, args);
            case "shop" -> handleShop(sender, args);
            case "give" -> handleGive(sender, args);
            case "admin" -> handleAdmin(sender, args);
            case "debug" -> debugCommand.execute(sender, args);
            case "help" -> sendHelp(sender);
            default -> {
                sender.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§c알 수 없는 명령어입니다. /wildcore help 를 확인하세요.");
            }
        }

        return true;
    }

    /**
     * 상점 명령어
     */
    private void handleShop(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (args.length < 2) {
            sendShopHelp(player);
            return;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "create" -> handleShopCreate(player, args);
            case "remove", "delete" -> handleShopRemove(player, args);
            case "list" -> handleShopList(player);
            case "tp", "teleport" -> handleShopTeleport(player, args);
            case "open" -> handleShopOpen(player, args);
            case "admin" -> handleShopAdmin(player, args);
            default -> sendShopHelp(player);
        }
    }

    private void handleShopCreate(Player player, String[] args) {
        if (!player.hasPermission("wildcore.admin.shop")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wc shop create <ID> [표시이름]");
            return;
        }

        String shopId = args[2];
        String displayName = args.length >= 4 ? String.join(" ", Arrays.copyOfRange(args, 3, args.length))
                : "&a" + shopId;

        ShopConfig newShop = plugin.getShopManager().createShop(
                shopId, displayName, player.getLocation(), "VILLAGER");

        if (newShop != null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§a상점이 생성되었습니다: " + shopId);
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§7NPC를 Shift+우클릭하여 아이템을 등록하세요.");
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점 생성 실패! (이미 존재하는 ID일 수 있습니다)");
        }
    }

    private void handleShopRemove(Player player, String[] args) {
        if (!player.hasPermission("wildcore.admin.shop")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wc shop remove <ID>");
            return;
        }

        String shopId = args[2];
        if (plugin.getShopManager().deleteShop(shopId)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점이 삭제되었습니다: " + shopId);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점을 찾을 수 없습니다: " + shopId);
        }
    }

    private void handleShopList(Player player) {
        var shops = plugin.getConfigManager().getShops();
        if (shops.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§7등록된 상점이 없습니다.");
            return;
        }

        player.sendMessage("§8§m                                        ");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6상점 목록");
        player.sendMessage("§8§m                                        ");
        for (ShopConfig shop : shops.values()) {
            player.sendMessage("§e" + shop.getId() + " §7- " + shop.getDisplayName() +
                    " §8(" + shop.getItemCount() + "개 아이템)");
        }
        player.sendMessage("§8§m                                        ");
    }

    private void handleShopTeleport(Player player, String[] args) {
        if (!player.hasPermission("wildcore.admin.shop")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wc shop tp <ID>");
            return;
        }

        String shopId = args[2];
        ShopConfig shop = plugin.getConfigManager().getShop(shopId);

        if (shop == null || shop.getLocation() == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점을 찾을 수 없습니다: " + shopId);
            return;
        }

        player.teleport(shop.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§a상점 " + shop.getDisplayName() + " §a(으)로 이동했습니다.");
    }

    private void handleShopOpen(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wc shop open <ID>");
            return;
        }

        String shopId = args[2];
        ShopConfig shop = plugin.getConfigManager().getShop(shopId);

        if (shop == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점을 찾을 수 없습니다: " + shopId);
            return;
        }

        new ShopGUI(plugin, player, shop).open();
    }

    private void handleShopAdmin(Player player, String[] args) {
        if (!player.hasPermission("wildcore.admin.shop")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wc shop admin <ID>");
            return;
        }

        String shopId = args[2];
        ShopConfig shop = plugin.getConfigManager().getShop(shopId);

        if (shop == null) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c상점을 찾을 수 없습니다: " + shopId);
            return;
        }

        new ShopAdminGUI(plugin, player, shop).open();
    }

    private void sendShopHelp(Player player) {
        player.sendMessage("§8§m                                        ");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6상점 명령어");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("§e/wc shop create <ID> [이름] §7- 상점 생성");
        player.sendMessage("§e/wc shop remove <ID> §7- 상점 삭제");
        player.sendMessage("§e/wc shop list §7- 상점 목록");
        player.sendMessage("§e/wc shop tp <ID> §7- 상점으로 이동");
        player.sendMessage("§e/wc shop open <ID> §7- 상점 GUI 열기");
        player.sendMessage("§e/wc shop admin <ID> §7- 상점 관리 GUI");
        player.sendMessage("§8§m                                        ");
    }

    /**
     * 리로드 명령어
     */
    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("wildcore.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        plugin.reload();
        sender.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("config_reloaded"));
    }

    /**
     * 주식 명령어
     */
    private void handleStock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (!player.hasPermission("wildcore.stock")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        new StockGUI(plugin, player).open();
    }

    /**
     * 인챈트 명령어
     */
    private void handleEnchant(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (!player.hasPermission("wildcore.enchant")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        new EnchantGUI(plugin, player).open();
    }

    /**
     * 아이템 지급 명령어
     */
    private void handleGive(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildcore.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wildcore give <플레이어> <아이템ID> [수량]");
            return;
        }

        String playerName = args[1];
        String itemId = args[2];
        int amount = args.length >= 4 ? parseInt(args[3], 1) : 1;

        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c플레이어를 찾을 수 없습니다: " + playerName);
            return;
        }

        CustomItemConfig itemConfig = plugin.getConfigManager().getCustomItem(itemId);
        if (itemConfig == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c아이템을 찾을 수 없습니다: " + itemId);
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§7사용 가능한 아이템: " + String.join(", ",
                            plugin.getConfigManager().getCustomItems().keySet()));
            return;
        }

        ItemStack item = ItemUtil.createCustomItem(plugin, itemId, amount);
        if (item == null) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c아이템 생성에 실패했습니다.");
            return;
        }

        target.getInventory().addItem(item);

        Map<String, String> replacements = new HashMap<>();
        replacements.put("player", target.getName());
        replacements.put("item", itemConfig.getDisplayName());
        replacements.put("amount", String.valueOf(amount));
        sender.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("item_given", replacements));
    }

    /**
     * 관리자 GUI 명령어
     */
    private void handleAdmin(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (!player.hasPermission("wildcore.admin")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            sendAdminHelp(player);
            return;
        }

        String adminType = args[1].toLowerCase();
        switch (adminType) {
            case "stock" -> new StockAdminGUI(plugin, player).open();
            case "enchant" -> new EnchantAdminGUI(plugin, player).open();
            default -> sendAdminHelp(player);
        }
    }

    /**
     * 관리자 도움말
     */
    private void sendAdminHelp(Player player) {
        player.sendMessage("§8§m                                        ");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6관리자 명령어");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("§e/wildcore admin stock §7- 주식 관리 GUI");
        player.sendMessage("§e/wildcore admin enchant §7- 인챈트 관리 GUI");
        player.sendMessage("§8§m                                        ");
    }

    /**
     * 도움말 출력
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage(plugin.getConfigManager().getPrefix() + "§6명령어 도움말");
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§e/wildcore reload §7- 설정 리로드");
        sender.sendMessage("§e/wildcore stock §7- 주식 시장 열기");
        sender.sendMessage("§e/wildcore enchant §7- 강화소 열기");
        sender.sendMessage("§e/wildcore shop §7- 상점 명령어");
        sender.sendMessage("§e/wildcore give <플레이어> <아이템ID> [수량] §7- 아이템 지급");
        sender.sendMessage("§e/wildcore admin <stock|enchant|shop> §7- 관리자 GUI");
        sender.sendMessage("§e/wildcore help §7- 도움말 보기");
        sender.sendMessage("§8§m                                        ");
    }

    /**
     * 문자열을 정수로 파싱
     */
    private int parseInt(String str, int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("reload", "stock", "enchant", "shop", "give", "admin", "debug", "help"));
        } else if (args[0].equalsIgnoreCase("debug")) {
            // debug 명령어는 DebugCommand에 위임 (모든 인자 길이에서)
            return debugCommand.tabComplete(args);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                // 온라인 플레이어 목록
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("admin")) {
                completions.addAll(Arrays.asList("stock", "enchant", "shop"));
            } else if (args[0].equalsIgnoreCase("shop")) {
                completions.addAll(Arrays.asList("create", "remove", "list", "tp", "open", "admin"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                // 커스텀 아이템 목록
                completions.addAll(plugin.getConfigManager().getCustomItems().keySet());
            } else if (args[0].equalsIgnoreCase("shop")) {
                String subCmd = args[1].toLowerCase();
                if (subCmd.equals("remove") || subCmd.equals("delete") ||
                        subCmd.equals("tp") || subCmd.equals("teleport") ||
                        subCmd.equals("open") || subCmd.equals("admin")) {
                    // 상점 ID 목록
                    completions.addAll(plugin.getConfigManager().getShops().keySet());
                }
            }
        }

        // 입력값으로 필터링
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
