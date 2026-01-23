package com.myserver.wildcore.gui.admin;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 주식 관리자 메인 GUI - 주식 목록 및 관리 옵션
 */
public class StockAdminGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    public StockAdminGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54, "§8[ §6주식 관리 §8]");

        // 배경
        ItemStack background = createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // 상단 정보
        ItemStack infoItem = createItem(Material.BOOK, "§6[ 주식 관리 시스템 ]",
                List.of(
                        "",
                        "§7주식 종목을 선택하여",
                        "§7설정을 변경할 수 있습니다.",
                        "",
                        "§e클릭§7: 상세 설정 열기"));
        inventory.setItem(4, infoItem);

        // 주식 목록 표시 (10-16, 19-25, 28-34)
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int slotIndex = 0;

        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            if (slotIndex >= slots.length)
                break;

            Material material = Material.getMaterial(stock.getMaterial());
            if (material == null)
                material = Material.PAPER;

            double currentPrice = plugin.getStockManager().getCurrentPrice(stock.getId());
            String change = plugin.getStockManager().getFormattedChange(stock.getId());

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7ID: §f" + stock.getId());
            lore.add("§7현재 가격: §6" + String.format("%,.0f", currentPrice) + "원");
            lore.add("§7변동률: " + change);
            lore.add("");
            lore.add("§7기준가: §f" + String.format("%,.0f", stock.getBasePrice()));
            lore.add("§7최소가: §f" + String.format("%,.0f", stock.getMinPrice()));
            lore.add("§7최대가: §f" + String.format("%,.0f", stock.getMaxPrice()));
            lore.add("§7변동성: §f" + (stock.getVolatility() * 100) + "%");
            lore.add("");
            lore.add("§e클릭하여 설정 변경");

            ItemStack stockItem = createItem(material, stock.getDisplayName(), lore);
            inventory.setItem(slots[slotIndex++], stockItem);
        }

        // 새 주식 추가 버튼
        ItemStack addStock = createItem(Material.EMERALD_BLOCK, "§a[ + 새 주식 추가 ]",
                List.of("", "§7클릭하여 새 주식 종목을 추가합니다."));
        inventory.setItem(49, addStock);

        // 전체 가격 갱신 버튼
        ItemStack updateAll = createItem(Material.CLOCK, "§e[ 전체 가격 갱신 ]",
                List.of("", "§7모든 주식 가격을 즉시 갱신합니다."));
        inventory.setItem(45, updateAll);

        // 뒤로 가기
        ItemStack back = createItem(Material.ARROW, "§c[ 닫기 ]", null);
        inventory.setItem(53, back);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null)
                meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void refresh() {
        createInventory();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
