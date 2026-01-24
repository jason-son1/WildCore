package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.PlayerStockData;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 내 주식 정보 GUI
 * 플레이어가 보유한 주식만 모아서 보여주는 GUI
 */
public class MyStockGUI extends PaginatedGui<String> {

    private final Map<String, PlayerStockData> myStocks;

    public MyStockGUI(WildCore plugin, Player player) {
        super(plugin, player);
        this.myStocks = plugin.getStockManager().getPlayerStocks(player.getUniqueId());
    }

    @Override
    protected List<String> getItems() {
        if (myStocks == null || myStocks.isEmpty()) {
            return Collections.emptyList();
        }
        // 보유한 주식 ID 목록 반환
        return new ArrayList<>(myStocks.keySet());
    }

    @Override
    protected ItemStack createItemDisplay(String stockId) {
        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        PlayerStockData data = myStocks.get(stockId);

        if (stock == null || data == null) {
            return ItemUtil.createItem(Material.BARRIER, "§c알 수 없는 주식", null, 1, null, 0, false, null);
        }

        Material material = Material.getMaterial(stock.getMaterial());
        if (material == null)
            material = Material.PAPER;

        double currentPrice = plugin.getStockManager().getCurrentPrice(stockId);
        double avgPrice = data.getAveragePrice();
        int amount = data.getAmount();

        // 수익률 계산
        double totalValue = currentPrice * amount;
        double totalInvested = data.getTotalInvested();
        double profit = totalValue - totalInvested;
        double profitPercent = (totalInvested > 0) ? (profit / totalInvested * 100) : 0;

        String profitColor = profit >= 0 ? "§a" : "§c";

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7현재가: §e" + plugin.getStockManager().getFormattedPrice(stockId) + "원");
        lore.add("§7변동률: " + plugin.getStockManager().getFormattedChange(stockId));
        lore.add("");
        lore.add("§f[ 보유 현황 ]");
        lore.add("§7보유량: §f" + amount + "주");
        lore.add("§7평단가: §f" + String.format("%,.0f", avgPrice) + "원");
        lore.add("");
        lore.add("§7평가액: §e" + String.format("%,.0f", totalValue) + "원");
        lore.add("§7수익금: " + profitColor + String.format("%+, .0f", profit) + "원");
        lore.add("§7수익률: " + profitColor + String.format("%+.2f%%", profitPercent));
        lore.add("");
        lore.add("§e클릭하여 주식 거래소로 이동");

        return ItemUtil.createItem(material, stock.getDisplayName(), lore, 1, stockId, 0, false,
                plugin);
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        return "§8[ §d내 주식 정보 §8] §7(" + page + "/" + totalPages + ")";
    }

    /**
     * 특정 슬롯에 해당하는 주식 ID를 반환합니다.
     */
    public String getStockIdAtSlot(int slot) {
        return getItemAtSlot(slot);
    }
}
