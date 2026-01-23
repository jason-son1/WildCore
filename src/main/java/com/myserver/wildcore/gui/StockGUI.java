package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
 * 주식 시장 GUI
 */
public class StockGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    public StockGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    /**
     * 인벤토리 생성
     */
    private void createInventory() {
        String title = plugin.getConfigManager().getStockGuiTitle();
        int size = plugin.getConfigManager().getStockGuiSize();

        Component titleComponent = LegacyComponentSerializer.legacySection().deserialize(title);
        inventory = Bukkit.createInventory(this, size, titleComponent);

        // 배경 유리판 채우기
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, background);
        }

        // 주식 아이콘 배치
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            ItemStack stockItem = createStockItem(stock);
            inventory.setItem(stock.getSlot(), stockItem);
        }

        // 정보 아이콘 추가
        ItemStack infoItem = createInfoItem();
        inventory.setItem(4, infoItem);

        // 새로고침 버튼
        ItemStack refreshItem = createItem(Material.CLOCK, "§e[ 새로고침 ]",
                List.of("", "§7클릭하여 가격을 갱신합니다."));
        inventory.setItem(size - 5, refreshItem);
    }

    /**
     * 주식 아이템 생성
     */
    private ItemStack createStockItem(StockConfig stock) {
        Material material = Material.getMaterial(stock.getMaterial());
        if (material == null)
            material = Material.PAPER;

        // Lore 생성 (플레이스홀더 치환)
        List<String> lore = new ArrayList<>();
        for (String line : stock.getLore()) {
            line = line.replace("%price%", plugin.getStockManager().getFormattedPrice(stock.getId()));
            line = line.replace("%change%", plugin.getStockManager().getFormattedChange(stock.getId()));
            lore.add(line);
        }

        // 보유량 정보 추가
        int holdings = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stock.getId());
        lore.add("");
        lore.add("§7보유량: §f" + holdings + "주");

        return createItem(material, stock.getDisplayName(), lore);
    }

    /**
     * 정보 아이콘 생성
     */
    private ItemStack createInfoItem() {
        double totalValue = 0;
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            int amount = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stock.getId());
            totalValue += plugin.getStockManager().getCurrentPrice(stock.getId()) * amount;
        }

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7총 자산 가치: §6" + String.format("%,.0f", totalValue) + "원");
        lore.add("§7현재 보유 현금: §6" + String.format("%,.0f", plugin.getEconomy().getBalance(player)) + "원");
        lore.add("");
        lore.add("§e좌클릭: §f1주 매수");
        lore.add("§e우클릭: §f1주 매도");
        lore.add("§eShift+좌클릭: §f10주 매수");
        lore.add("§eShift+우클릭: §f10주 매도");

        return createItem(Material.GOLD_INGOT, "§6[ 내 포트폴리오 ]", lore);
    }

    /**
     * 아이템 생성 헬퍼
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * GUI 새로고침
     */
    public void refresh() {
        createInventory();
        player.openInventory(inventory);
    }

    /**
     * GUI 열기
     */
    public void open() {
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
