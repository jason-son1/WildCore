package com.myserver.wildcore.commands;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.CustomItemConfig;
import com.myserver.wildcore.config.ShopConfig;
import com.myserver.wildcore.gui.EnchantGUI;
import com.myserver.wildcore.gui.StockGUI;
import com.myserver.wildcore.gui.BankMainGUI;
import com.myserver.wildcore.gui.admin.EnchantAdminGUI;
import com.myserver.wildcore.gui.admin.NpcAdminGUI;
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
            case "bank" -> handleBank(sender, args);
            case "npc" -> handleNpc(sender, args);
            case "give" -> handleGive(sender, args);
            case "drop" -> handleDrop(sender, args);
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
     * 은행 명령어
     */
    private void handleBank(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (!player.hasPermission("wildcore.bank.use")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        new BankMainGUI(plugin, player).open();
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
     * 아이템 드롭 명령어 (주로 MythicMobs나 콘솔에서 사용)
     * 사용법: /wildcore drop <월드명> <x> <y> <z> <아이템ID> [수량]
     */
    private void handleDrop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildcore.admin")) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 6) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    "§c사용법: /wildcore drop <월드> <x> <y> <z> <아이템ID> [수량]");
            return;
        }

        try {
            String worldName = args[1];
            double x = Double.parseDouble(args[2]);
            double y = Double.parseDouble(args[3]);
            double z = Double.parseDouble(args[4]);
            String itemId = args[5];
            int amount = args.length >= 7 ? parseInt(args[6], 1) : 1;

            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                String errorMsg = "월드를 찾을 수 없습니다: " + worldName;
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + errorMsg);
                Bukkit.getConsoleSender()
                        .sendMessage(plugin.getConfigManager().getPrefix() + " §c[Drop Error] " + errorMsg);
                return;
            }

            CustomItemConfig itemConfig = plugin.getConfigManager().getCustomItem(itemId);
            if (itemConfig == null) {
                String errorMsg = "아이템을 찾을 수 없습니다: " + itemId;
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + errorMsg);
                sender.sendMessage(plugin.getConfigManager().getPrefix() +
                        "§7사용 가능한 아이템: " + String.join(", ",
                                plugin.getConfigManager().getCustomItems().keySet()));
                Bukkit.getConsoleSender()
                        .sendMessage(plugin.getConfigManager().getPrefix() + " §c[Drop Error] " + errorMsg);
                return;
            }

            ItemStack item = ItemUtil.createCustomItem(plugin, itemId, amount);
            if (item == null) {
                String errorMsg = "아이템 생성에 실패했습니다: " + itemId;
                sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + errorMsg);
                Bukkit.getConsoleSender()
                        .sendMessage(plugin.getConfigManager().getPrefix() + " §c[Drop Error] " + errorMsg);
                return;
            }

            org.bukkit.Location loc = new org.bukkit.Location(world, x, y, z);
            world.dropItemNaturally(loc, item);

            // 성공 로그
            Bukkit.getConsoleSender().sendMessage(plugin.getConfigManager().getPrefix() +
                    "§a아이템 드롭 완료: " + itemConfig.getDisplayName() + " §7x" + amount +
                    " §7at " + worldName + " (" + x + ", " + y + ", " + z + ")");

        } catch (NumberFormatException e) {
            String errorMsg = "좌표나 수량은 숫자여야 합니다.";
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + errorMsg);
            Bukkit.getConsoleSender()
                    .sendMessage(plugin.getConfigManager().getPrefix() + " §c[Drop Error] " + errorMsg);
            e.printStackTrace(); // 숫자 형식 오류도 스택 트레이스 출력
        } catch (Exception e) {
            String errorMsg = "아이템 드롭 중 알 수 없는 오류가 발생했습니다: " + e.getMessage();
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c" + errorMsg);

            Bukkit.getConsoleSender().sendMessage("§4========================================");
            Bukkit.getConsoleSender().sendMessage("§c[WildCore] Drop Command Critical Error!");
            Bukkit.getConsoleSender().sendMessage("§cMessage: " + e.getMessage());
            e.printStackTrace();
            Bukkit.getConsoleSender().sendMessage("§4========================================");
        }
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
            case "npc" -> new NpcAdminGUI(plugin, player).open();
            default -> sendAdminHelp(player);
        }
    }

    /**
     * NPC 명령어
     */
    private void handleNpc(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("player_only"));
            return;
        }

        if (!player.hasPermission("wildcore.admin.npc")) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("no_permission"));
            return;
        }

        if (args.length < 2) {
            // 서브커맨드 없으면 NPC 관리 GUI 열기
            new NpcAdminGUI(plugin, player).open();
            return;
        }

        String subCmd = args[1].toLowerCase();
        switch (subCmd) {
            case "list" -> handleNpcList(player);
            case "tp", "teleport" -> handleNpcTeleport(player, args);
            case "remove", "delete" -> handleNpcRemove(player, args);
            case "respawn" -> handleNpcRespawn(player, args);
            case "validate", "fix" -> handleNpcValidate(player);
            case "gui" -> new NpcAdminGUI(plugin, player).open();
            default -> sendNpcHelp(player);
        }
    }

    private void handleNpcList(Player player) {
        var npcs = plugin.getNpcManager().getAllNpcs();
        if (npcs.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§7등록된 NPC가 없습니다.");
            return;
        }

        player.sendMessage("§8§m                                        ");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6NPC 목록");
        player.sendMessage("§8§m                                        ");

        for (var data : npcs.values()) {
            String locStr = data.getLocation() != null
                    ? String.format("§7(%.1f, %.1f, %.1f)", data.getLocation().getX(), data.getLocation().getY(),
                            data.getLocation().getZ())
                    : "§c(위치 없음)";

            player.sendMessage("§e" + data.getId() + " §7- " + data.getDisplayName() + " §f["
                    + data.getType().getDisplayName() + "]");
            player.sendMessage("  " + locStr);
        }
        player.sendMessage("§8§m                                        ");
    }

    private void handleNpcTeleport(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c사용법: /wc npc tp <ID>");
            return;
        }

        String id = args[2];
        if (plugin.getNpcManager().teleportToNpc(player, id)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC(으)로 이동했습니다: " + id);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC를 찾을 수 없습니다: " + id);
        }
    }

    private void handleNpcRemove(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c사용법: /wc npc remove <ID>");
            return;
        }

        String id = args[2];
        if (plugin.getNpcManager().removeNpc(id)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC가 삭제되었습니다: " + id);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC를 찾을 수 없습니다: " + id);
        }
    }

    private void handleNpcRespawn(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§c사용법: /wc npc respawn <ID>");
            return;
        }

        String id = args[2];
        if (plugin.getNpcManager().respawnNpc(id)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC가 리스폰되었습니다: " + id);
        } else {
            player.sendMessage(plugin.getConfigManager().getPrefix() + "§cNPC를 찾을 수 없거나 위치가 유효하지 않습니다: " + id);
        }
    }

    private void handleNpcValidate(Player player) {
        plugin.getNpcManager().validateAndFixNpcs();
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§aNPC 상태 검사 및 복구가 완료되었습니다.");
    }

    private void sendNpcHelp(Player player) {
        player.sendMessage("§8§m                                        ");
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§6NPC 명령어");
        player.sendMessage("§8§m                                        ");
        player.sendMessage("§e/wc npc list §7- NPC 목록");
        player.sendMessage("§e/wc npc tp <ID> §7- NPC로 이동");
        player.sendMessage("§e/wc npc remove <ID> §7- NPC 삭제");
        player.sendMessage("§e/wc npc respawn <ID> §7- NPC 리스폰");
        player.sendMessage("§e/wc npc validate §7- NPC 상태 검사/복구");
        player.sendMessage("§e/wc npc gui §7- 관리 GUI 열기");
        player.sendMessage("§8§m                                        ");
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
        player.sendMessage("§e/wildcore admin npc §7- NPC 관리 GUI");
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
        sender.sendMessage("§e/wildcore bank §7- 은행 열기");
        sender.sendMessage("§e/wildcore npc §7- NPC 관리 GUI");
        sender.sendMessage("§e/wildcore give <플레이어> <아이템ID> [수량] §7- 아이템 지급");
        sender.sendMessage("§e/wildcore drop <월드> <x> <y> <z> <아이템ID> [수량] §7- 아이템 드롭");
        sender.sendMessage("§e/wildcore admin <stock|enchant|npc> §7- 관리자 GUI");
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
            completions.addAll(
                    Arrays.asList("reload", "stock", "enchant", "shop", "bank", "npc", "give", "drop", "admin", "debug",
                            "help"));
        } else if (args[0].equalsIgnoreCase("debug")) {
            // debug 명령어는 DebugCommand에 위임 (모든 인자 길이에서)
            return debugCommand.tabComplete(args);
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("give")) {
                // 온라인 플레이어 목록
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("drop")) {
                // 월드 목록
                completions.addAll(Bukkit.getWorlds().stream()
                        .map(org.bukkit.World::getName)
                        .collect(Collectors.toList()));
            } else if (args[0].equalsIgnoreCase("admin")) {
                completions.addAll(Arrays.asList("stock", "enchant", "npc"));
            } else if (args[0].equalsIgnoreCase("shop")) {
                completions.addAll(Arrays.asList("create", "remove", "list", "tp", "open", "admin"));
            } else if (args[0].equalsIgnoreCase("npc")) {
                completions.addAll(Arrays.asList("list", "tp", "remove", "respawn", "validate", "gui"));
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("give")) {
                // 커스텀 아이템 목록
                completions.addAll(plugin.getConfigManager().getCustomItems().keySet());
            } else if (args[0].equalsIgnoreCase("drop")) {
                // x 좌표 제안
                if (sender instanceof Player player) {
                    completions.add(String.valueOf((int) player.getLocation().getX()));
                }
            } else if (args[0].equalsIgnoreCase("shop")) {
                String subCmd = args[1].toLowerCase();
                if (subCmd.equals("remove") || subCmd.equals("delete") ||
                        subCmd.equals("tp") || subCmd.equals("teleport") ||
                        subCmd.equals("open") || subCmd.equals("admin")) {
                    // 상점 ID 목록
                    completions.addAll(plugin.getConfigManager().getShops().keySet());
                }
            }
        } else if (args[0].equalsIgnoreCase("npc")) {
            String subCmd = args[1].toLowerCase();
            if (subCmd.equals("tp") || subCmd.equals("teleport") ||
                    subCmd.equals("remove") || subCmd.equals("delete") ||
                    subCmd.equals("respawn")) {
                completions.addAll(plugin.getNpcManager().getAllNpcs().keySet());
            }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("drop")) {
            // y 좌표 제안
            if (sender instanceof Player player) {
                completions.add(String.valueOf((int) player.getLocation().getY()));
            }
        } else if (args.length == 5 && args[0].equalsIgnoreCase("drop")) {
            // z 좌표 제안
            if (sender instanceof Player player) {
                completions.add(String.valueOf((int) player.getLocation().getZ()));
            }
        } else if (args.length == 6 && args[0].equalsIgnoreCase("drop")) {
            // 커스텀 아이템 목록
            completions.addAll(plugin.getConfigManager().getCustomItems().keySet());
        }

        // 입력값으로 필터링
        String input = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(input))
                .collect(Collectors.toList());
    }
}
