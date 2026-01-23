package com.myserver.wildcore.placeholder;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI 확장
 * 
 * 사용 가능한 플레이스홀더:
 * - %wildcore_stock_price_<종목ID>% : 현재 가격
 * - %wildcore_stock_change_<종목ID>% : 변동률
 * - %wildcore_stock_holdings_<종목ID>% : 보유 수량
 * - %wildcore_stock_value_<종목ID>% : 보유 가치
 * - %wildcore_stock_total_value% : 총 자산 가치
 */
public class WildCorePlaceholder extends PlaceholderExpansion {

    private final WildCore plugin;

    public WildCorePlaceholder(WildCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "wildcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        String[] parts = params.split("_");

        // stock_price_<stockId>
        if (params.startsWith("stock_price_") && parts.length >= 3) {
            String stockId = params.substring("stock_price_".length());
            return plugin.getStockManager().getFormattedPrice(stockId);
        }

        // stock_change_<stockId>
        if (params.startsWith("stock_change_") && parts.length >= 3) {
            String stockId = params.substring("stock_change_".length());
            return plugin.getStockManager().getFormattedChange(stockId);
        }

        // stock_holdings_<stockId>
        if (params.startsWith("stock_holdings_") && parts.length >= 3) {
            String stockId = params.substring("stock_holdings_".length());
            int holdings = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stockId);
            return String.valueOf(holdings);
        }

        // stock_value_<stockId>
        if (params.startsWith("stock_value_") && parts.length >= 3) {
            String stockId = params.substring("stock_value_".length());
            int holdings = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stockId);
            double price = plugin.getStockManager().getCurrentPrice(stockId);
            return String.format("%,.0f", holdings * price);
        }

        // stock_total_value
        if (params.equals("stock_total_value")) {
            double totalValue = 0;
            for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
                int holdings = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stock.getId());
                double price = plugin.getStockManager().getCurrentPrice(stock.getId());
                totalValue += holdings * price;
            }
            return String.format("%,.0f", totalValue);
        }

        return null;
    }
}
