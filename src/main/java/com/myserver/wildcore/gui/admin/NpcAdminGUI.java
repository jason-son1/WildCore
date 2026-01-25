package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcData;
import com.myserver.wildcore.npc.NpcType;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * NPC 관리 Admin GUI
 * 강화/주식 NPC 생성 및 관리
 */
public class NpcAdminGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    public NpcAdminGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, ItemUtil.parse("§8[ §6NPC 관리 §8]"));
        updateInventory();
    }

    private void updateInventory() {
        // 배경 초기화
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // === 헤더 (슬롯 4) ===
        inventory.setItem(4, createItem(Material.VILLAGER_SPAWN_EGG, "§6[ NPC 관리 ]",
                List.of("", "§7WildCore NPC를 생성하고", "§7관리합니다.")));

        // === NPC 생성 버튼 (슬롯 19-25) ===
        inventory.setItem(20, createItem(Material.ENCHANTING_TABLE, "§d[ 강화 NPC 생성 ]",
                List.of("",
                        "§7클릭하면 현재 위치에",
                        "§7강화 NPC를 생성합니다.",
                        "",
                        "§e클릭하여 생성")));

        inventory.setItem(22, createItem(Material.EMERALD, "§a[ 주식 NPC 생성 ]",
                List.of("",
                        "§7클릭하면 현재 위치에",
                        "§7주식거래소 NPC를 생성합니다.",
                        "",
                        "§e클릭하여 생성")));

        inventory.setItem(24, createItem(Material.CHEST, "§b[ 상점 NPC 생성 ]",
                List.of("",
                        "§7상점 NPC는 /wildcore shop 명령어로",
                        "§7생성할 수 있습니다.",
                        "",
                        "§7/wildcore shop create <이름> <표시이름> [타입]")));

        inventory.setItem(26, createItem(Material.COMPASS, "§b[ 이동 NPC 생성 ]",
                List.of("",
                        "§7클릭하면 현재 위치에",
                        "§7이동 NPC를 생성합니다.",
                        "",
                        "§e클릭하여 생성")));

        // === 현재 NPC 목록 (슬롯 28-34) ===
        Map<UUID, NpcData> enchantNpcs = plugin.getNpcManager().getNpcsByType(NpcType.ENCHANT);
        Map<UUID, NpcData> stockNpcs = plugin.getNpcManager().getNpcsByType(NpcType.STOCK);
        Map<UUID, NpcData> warpNpcs = plugin.getNpcManager().getNpcsByType(NpcType.WARP);

        inventory.setItem(29, createItem(Material.PAPER, "§d[ 강화 NPC 목록 ]",
                List.of("",
                        "§7현재 생성된 강화 NPC: §f" + enchantNpcs.size() + "개",
                        "",
                        "§c우클릭하면 모든 강화 NPC가",
                        "§c제거됩니다!")));

        inventory.setItem(33, createItem(Material.PAPER, "§a[ 주식 NPC 목록 ]",
                List.of("",
                        "§7현재 생성된 주식 NPC: §f" + stockNpcs.size() + "개",
                        "",
                        "§c우클릭하면 모든 주식 NPC가",
                        "§c제거됩니다!")));

        inventory.setItem(31, createItem(Material.PAPER, "§b[ 이동 NPC 목록 ]",
                List.of("",
                        "§7현재 생성된 이동 NPC: §f" + warpNpcs.size() + "개",
                        "",
                        "§c우클릭하면 모든 이동 NPC가",
                        "§c제거됩니다!")));

        // === 액션 버튼 (슬롯 45-53) ===
        inventory.setItem(45, createItem(Material.ARROW, "§7[ 뒤로 가기 ]",
                List.of("", "§7메인 메뉴로 돌아갑니다.")));

        inventory.setItem(49, createItem(Material.TNT, "§c[ 모든 NPC 제거 ]",
                List.of("",
                        "§7모든 WildCore NPC를 제거합니다.",
                        "",
                        "§c주의: 상점 NPC는 제외됩니다.",
                        "",
                        "§c우클릭하여 제거")));
    }

    /**
     * 강화 NPC 생성
     */
    public void spawnEnchantNpc() {
        plugin.getNpcManager().spawnNpc(
                NpcType.ENCHANT,
                player.getLocation(),
                "§d[ §5강화소 §d]",
                null,
                false);
        plugin.getConfigManager().addNpcLocation(NpcType.ENCHANT, player.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a강화 NPC가 생성되었습니다.");
        updateInventory();
    }

    /**
     * 주식 NPC 생성
     */
    public void spawnStockNpc() {
        plugin.getNpcManager().spawnNpc(
                NpcType.STOCK,
                player.getLocation(),
                "§a[ §2주식거래소 §a]",
                null,
                false);
        plugin.getConfigManager().addNpcLocation(NpcType.STOCK, player.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a주식 NPC가 생성되었습니다.");
        updateInventory();
    }

    /**
     * 이동 NPC 생성
     */
    public void spawnWarpNpc(String worldName) {
        plugin.getNpcManager().spawnNpc(
                NpcType.WARP,
                player.getLocation(),
                "§b[ §3이동 도우미 §b]",
                worldName,
                false);
        plugin.getConfigManager().addNpcLocation(NpcType.WARP, player.getLocation());
        player.sendMessage(plugin.getConfigManager().getPrefix() + "§a이동 NPC가 생성되었습니다. (목적지: " + worldName + ")");
        updateInventory();
    }

    /**
     * 특정 타입의 모든 NPC 제거
     */
    public void removeAllNpcs(NpcType type) {
        plugin.getNpcManager().removeAllTaggedNpcs(type);
        plugin.getConfigManager().clearNpcLocations(type);
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§c모든 " + type.getDisplayName() + " NPC가 제거되었습니다.");
        updateInventory();
    }

    /**
     * 상점 제외 모든 NPC 제거
     */
    public void removeAllNonShopNpcs() {
        removeAllNpcs(NpcType.ENCHANT);
        removeAllNpcs(NpcType.STOCK);
        removeAllNpcs(NpcType.WARP);
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                "§c모든 강화/주식 NPC가 제거되었습니다.");
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        return ItemUtil.createItem(material, name, lore, 1, null, 0, false, null);
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void refresh() {
        updateInventory();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
