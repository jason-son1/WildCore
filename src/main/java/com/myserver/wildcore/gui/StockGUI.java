package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 주식 시장 GUI (페이지네이션 지원)
 */
public class StockGUI extends PaginatedGui<StockConfig> {

    public StockGUI(WildCore plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected List<StockConfig> getItems() {
        // 이름순으로 정렬된 주식 목록 반환
        List<StockConfig> stocks = new ArrayList<>(plugin.getConfigManager().getStocks().values());
        stocks.sort(Comparator.comparing(StockConfig::getDisplayName));
        return stocks;
    }

    @Override
    protected ItemStack createItemDisplay(StockConfig stock) {
        Material material = Material.getMaterial(stock.getMaterial());
        if (material == null) {
            material = Material.PAPER;
        }

        // Lore 생성 (플레이스홀더 치환)
        List<String> lore = new ArrayList<>();
        for (String line : stock.getLore()) {
            line = line.replace("%price%", plugin.getStockManager().getFormattedPrice(stock.getId()));
            line = line.replace("%change%", getFormattedChangeWithArrow(stock.getId()));
            line = line.replace("%trend%", plugin.getStockManager().getTrendIcons(stock.getId(), 5));
            lore.add(line);
        }

        // 만약 lore에 %trend%가 없다면 마지막에 추가 (기본 제공)
        if (!stock.getLore().stream().anyMatch(l -> l.contains("%trend%"))) {
            lore.add("§7최근 추세: " + plugin.getStockManager().getTrendIcons(stock.getId(), 5));
        }

        // 보유량 정보 추가
        int holdings = plugin.getStockManager().getPlayerStockAmount(player.getUniqueId(), stock.getId());
        lore.add("");
        lore.add("§7보유량: §f" + holdings + "주");

        if (holdings > 0) {
            double avgPrice = plugin.getStockManager().getPlayerAveragePrice(player.getUniqueId(), stock.getId());
            double currentPrice = plugin.getStockManager().getCurrentPrice(stock.getId());
            double profitLoss = (currentPrice - avgPrice) * holdings;
            double roi = 0;
            if (avgPrice > 0) {
                roi = ((currentPrice - avgPrice) / avgPrice) * 100;
            }

            lore.add("§7평단가: §6" + String.format("%,.0f", avgPrice) + "원");

            String plColor = profitLoss >= 0 ? "§a+" : "§c";
            lore.add("§7평가손익: " + plColor + String.format("%,.0f", profitLoss) + "원");

            String roiColor = roi >= 0 ? "§a+" : "§c";
            lore.add("§7수익률: " + roiColor + String.format("%.2f", roi) + "%");
        }
        lore.add("");
        lore.add("§e좌클릭: §f1주 매수");
        lore.add("§e우클릭: §f1주 매도");
        lore.add("§eShift+좌클릭: §f10주 매수");
        lore.add("§eShift+우클릭: §f10주 매도");

        return createItem(material, stock.getDisplayName(), lore);
    }

    /**
     * 등락률을 화살표와 함께 포맷팅합니다.
     * 상승: §c▲ [등락폭]% (빨강)
     * 하락: §9▼ [등락폭]% (파랑)
     */
    private String getFormattedChangeWithArrow(String stockId) {
        double changePercent = plugin.getStockManager().getChangePercent(stockId);

        if (changePercent > 0) {
            return "§c▲ +" + String.format("%.2f", changePercent) + "%";
        } else if (changePercent < 0) {
            return "§9▼ " + String.format("%.2f", changePercent) + "%";
        } else {
            return "§7- 0.00%";
        }
    }

    @Override
    protected String getTitle(int page, int totalPages) {
        if (totalPages <= 1) {
            return "§8[ §a주식 시장 §8]";
        }
        return "§8[ §a주식 시장 §8] §7(" + page + "/" + totalPages + ")";
    }

    @Override
    protected Material getMainBackgroundMaterial() {
        return Material.GRAY_STAINED_GLASS_PANE;
    }

    @Override
    protected ItemStack createInfoItem(int page, int totalPages, int totalItems) {
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
        lore.add("§7현재 페이지: §e" + page + " / " + totalPages);
        lore.add("§7총 종목: §e" + totalItems + "개");
        lore.add("");
        lore.add("§7⏱ 다음 가격 변동: §e" + plugin.getStockManager().getFormattedTimeUntilNextUpdate());

        return createItem(Material.GOLD_INGOT, "§6[ 내 포트폴리오 ]", lore);
    }

    @Override
    public void open(int page) {
        super.open(page);
        // 자동 새로고침 시작 (1초마다 타이머 업데이트)
        AutoRefreshGUI.startAutoRefresh(plugin, player, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StockGUI) {
                updateInfoItemOnly();
            } else {
                AutoRefreshGUI.stopAutoRefresh(player);
            }
        }, 20L);
    }

    /**
     * 정보 아이콘만 업데이트 (타이머 갱신용)
     */
    private void updateInfoItemOnly() {
        if (inventory == null)
            return;
        ItemStack infoItem = createInfoItem(currentPage + 1, getTotalPages(), getItems().size());
        inventory.setItem(SLOT_INFO, infoItem);
    }

    /**
     * 특정 슬롯에 해당하는 주식 ID를 반환합니다.
     */
    public String getStockIdAtSlot(int slot) {
        StockConfig stock = getItemAtSlot(slot);
        return stock != null ? stock.getId() : null;
    }
}
