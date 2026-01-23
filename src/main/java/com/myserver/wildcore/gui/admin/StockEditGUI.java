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

import java.util.List;

/**
 * 개별 주식 설정 편집 GUI
 */
public class StockEditGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final String stockId;
    private Inventory inventory;

    public StockEditGUI(WildCore plugin, Player player, String stockId) {
        this.plugin = plugin;
        this.player = player;
        this.stockId = stockId;
        createInventory();
    }

    private void createInventory() {
        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock == null)
            return;

        inventory = Bukkit.createInventory(this, 54, "§8[ §6주식 편집: " + stock.getDisplayName() + " §8]");

        // 배경
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        double currentPrice = plugin.getStockManager().getCurrentPrice(stockId);

        // 현재 상태 표시
        Material stockMaterial = Material.getMaterial(stock.getMaterial());
        if (stockMaterial == null)
            stockMaterial = Material.PAPER;

        ItemStack infoItem = createItem(stockMaterial, stock.getDisplayName(),
                List.of(
                        "",
                        "§7ID: §f" + stockId,
                        "§7현재 가격: §6" + String.format("%,.0f", currentPrice) + "원",
                        "§7변동률: " + plugin.getStockManager().getFormattedChange(stockId)));
        inventory.setItem(4, infoItem);

        // === 가격 설정 ===
        // 현재 가격 수동 설정
        inventory.setItem(19, createItem(Material.GOLD_NUGGET, "§c[ 가격 -1000 ]",
                List.of("", "§7현재 가격에서 1000원 감소")));
        inventory.setItem(20, createItem(Material.GOLD_NUGGET, "§c[ 가격 -100 ]",
                List.of("", "§7현재 가격에서 100원 감소")));
        inventory.setItem(21, createItem(Material.GOLD_INGOT, "§6[ 현재 가격 ]",
                List.of("", "§7현재: §6" + String.format("%,.0f", currentPrice) + "원",
                        "", "§e가운데 클릭: 직접 입력")));
        inventory.setItem(22, createItem(Material.GOLD_NUGGET, "§a[ 가격 +100 ]",
                List.of("", "§7현재 가격에서 100원 증가")));
        inventory.setItem(23, createItem(Material.GOLD_NUGGET, "§a[ 가격 +1000 ]",
                List.of("", "§7현재 가격에서 1000원 증가")));

        // === 변동성 설정 ===
        inventory.setItem(28, createItem(Material.REDSTONE, "§c[ 변동성 -5% ]",
                List.of("", "§7변동성을 5% 감소")));
        inventory.setItem(29, createItem(Material.REDSTONE, "§c[ 변동성 -1% ]",
                List.of("", "§7변동성을 1% 감소")));
        inventory.setItem(30, createItem(Material.BLAZE_POWDER, "§e[ 변동성 ]",
                List.of("", "§7현재: §e" + (stock.getVolatility() * 100) + "%",
                        "", "§7변동성이 높을수록 가격 변화폭이 큽니다.")));
        inventory.setItem(31, createItem(Material.REDSTONE, "§a[ 변동성 +1% ]",
                List.of("", "§7변동성을 1% 증가")));
        inventory.setItem(32, createItem(Material.REDSTONE, "§a[ 변동성 +5% ]",
                List.of("", "§7변동성을 5% 증가")));

        // === 가격 범위 설정 ===
        inventory.setItem(37, createItem(Material.IRON_INGOT, "§7[ 최소가 설정 ]",
                List.of("", "§7현재: §f" + String.format("%,.0f", stock.getMinPrice()) + "원",
                        "", "§e클릭하여 채팅으로 입력")));
        inventory.setItem(39, createItem(Material.DIAMOND, "§b[ 기준가 설정 ]",
                List.of("", "§7현재: §f" + String.format("%,.0f", stock.getBasePrice()) + "원",
                        "", "§e클릭하여 채팅으로 입력")));
        inventory.setItem(41, createItem(Material.NETHERITE_INGOT, "§6[ 최대가 설정 ]",
                List.of("", "§7현재: §f" + String.format("%,.0f", stock.getMaxPrice()) + "원",
                        "", "§e클릭하여 채팅으로 입력")));

        // === 액션 버튼 ===
        inventory.setItem(48, createItem(Material.EMERALD, "§a[ 설정 저장 ]",
                List.of("", "§7변경사항을 파일에 저장합니다.")));

        inventory.setItem(50, createItem(Material.TNT, "§c[ 주식 삭제 ]",
                List.of("", "§c이 주식을 삭제합니다.", "§4주의: 되돌릴 수 없습니다!")));

        // 뒤로 가기
        inventory.setItem(45, createItem(Material.ARROW, "§7[ 뒤로 가기 ]", null));
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

    public String getStockId() {
        return stockId;
    }
}
