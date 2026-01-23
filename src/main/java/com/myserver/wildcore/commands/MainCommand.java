package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.CustomItemConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.admin.EnchantAdminGUI;
import com.myserver.wildcore.gui.admin.StockAdminGUI;
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
        sender.sendMessage("§e/wildcore give <플레이어> <아이템ID> [수량] §7- 아이템 지급");
        sender.sendMessage("§e/wildcore admin <stock|enchant> §7- 관리자 GUI");
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
            completions.addAll(Arrays.asList("reload", "stock", "enchant", "give", "admin", "debug", "help"));
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                // 온라인 플레이어 목록
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("admin")) {
                completions.addAll(Arrays.asList("stock", "enchant"));
            } else if (args[0].equalsIgnoreCase("debug")) {
                completions.addAll(debugCommand.tabComplete(args));
                return completions;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                // 커스텀 아이템 목록
                completions.addAll(plugin.getConfigManager().getCustomItems().keySet());
            }
        }

        // 입력값으로 필터링
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
