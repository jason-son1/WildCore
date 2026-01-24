package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.StockConfig;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 주식 시스템 매니저
 * - 가격 변동 스케줄러
 * - 매수/매도 로직
 * - 플레이어 주식 데이터 관리
 */
public class StockManager {

    private final WildCore plugin;

    // 현재 주식 가격 (종목ID -> 현재가)
    private Map<String, Double> currentPrices = new HashMap<>();

    // 이전 주식 가격 (변동률 계산용)
    private Map<String, Double> previousPrices = new HashMap<>();

    // 플레이어별 주식 보유량 (UUID -> (종목ID -> 수량))
    private Map<UUID, Map<String, Integer>> playerStocks = new HashMap<>();

    // 스케줄러 태스크
    private BukkitTask schedulerTask;

    // 데이터 파일
    private File dataFile;
    private FileConfiguration dataConfig;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat changeFormat = new DecimalFormat("+#,##0.00;-#,##0.00");
    private final Random random = new Random();

    public StockManager(WildCore plugin) {
        this.plugin = plugin;
        loadData();
        initializePrices();
    }

    /**
     * 데이터 파일 로드
     */
    private void loadData() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "stocks_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("주식 데이터 파일 생성 실패: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 플레이어 주식 데이터 로드
        if (dataConfig.isConfigurationSection("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, Integer> stocks = new HashMap<>();

                for (String stockId : dataConfig.getConfigurationSection("players." + uuidStr).getKeys(false)) {
                    stocks.put(stockId, dataConfig.getInt("players." + uuidStr + "." + stockId));
                }
                playerStocks.put(uuid, stocks);
            }
        }

        // 현재 가격 데이터 로드
        if (dataConfig.isConfigurationSection("prices")) {
            for (String stockId : dataConfig.getConfigurationSection("prices").getKeys(false)) {
                currentPrices.put(stockId, dataConfig.getDouble("prices." + stockId));
            }
        }

        plugin.getLogger().info("주식 데이터 로드 완료");
    }

    /**
     * 초기 가격 설정
     */
    private void initializePrices() {
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            if (!currentPrices.containsKey(stock.getId())) {
                currentPrices.put(stock.getId(), stock.getBasePrice());
            }
            previousPrices.put(stock.getId(), currentPrices.get(stock.getId()));
        }
    }

    /**
     * 스케줄러 시작
     */
    public void startScheduler() {
        int interval = plugin.getConfigManager().getStockUpdateInterval() * 20; // 초 -> 틱 변환

        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPrices();
            }
        }.runTaskTimerAsynchronously(plugin, interval, interval);

        plugin.getLogger().info("주식 가격 업데이트 스케줄러 시작 (간격: " +
                plugin.getConfigManager().getStockUpdateInterval() + "초)");
    }

    /**
     * 스케줄러 중지
     */
    public void stopScheduler() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
    }

    /**
     * 모든 주식 가격 업데이트
     */
    public void updateAllPrices() {
        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            updatePrice(stock);
        }

        // 데이터 저장
        saveAllData();

        plugin.debug("주식 가격이 업데이트되었습니다.");
    }

    /**
     * 단일 주식 가격 업데이트
     * 공식: P_new = P_old * (1 + Random(-volatility, +volatility))
     */
    private void updatePrice(StockConfig stock) {
        double currentPrice = currentPrices.getOrDefault(stock.getId(), stock.getBasePrice());
        previousPrices.put(stock.getId(), currentPrice);

        // 변동폭 계산 (-volatility ~ +volatility)
        double change = (random.nextDouble() * 2 - 1) * stock.getVolatility();
        double newPrice = currentPrice * (1 + change);

        // 최소/최대 가격 보정
        newPrice = Math.max(stock.getMinPrice(), Math.min(stock.getMaxPrice(), newPrice));

        currentPrices.put(stock.getId(), newPrice);
    }

    /**
     * 주식 매수
     */
    public boolean buyStock(Player player, String stockId, int amount) {
        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock == null)
            return false;

        double price = getCurrentPrice(stockId);
        double totalCost = price * amount;

        // 돈 확인 및 차감
        if (!plugin.getEconomy().has(player, totalCost)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", priceFormat.format(totalCost));
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("insufficient_funds", replacements));
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, totalCost);

        // 주식 추가
        Map<String, Integer> stocks = playerStocks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        stocks.put(stockId, stocks.getOrDefault(stockId, 0) + amount);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("stock", stock.getDisplayName());
        replacements.put("amount", String.valueOf(amount));
        replacements.put("total", priceFormat.format(totalCost));
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("stock_buy_success", replacements));

        saveAllData();
        return true;
    }

    /**
     * 주식 매도
     */
    public boolean sellStock(Player player, String stockId, int amount) {
        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock == null)
            return false;

        Map<String, Integer> stocks = playerStocks.get(player.getUniqueId());
        if (stocks == null || stocks.getOrDefault(stockId, 0) < amount) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock_insufficient"));
            return false;
        }

        double price = getCurrentPrice(stockId);
        double totalEarnings = price * amount;

        // 주식 차감
        stocks.put(stockId, stocks.get(stockId) - amount);
        if (stocks.get(stockId) <= 0) {
            stocks.remove(stockId);
        }

        // 돈 지급
        plugin.getEconomy().depositPlayer(player, totalEarnings);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("stock", stock.getDisplayName());
        replacements.put("amount", String.valueOf(amount));
        replacements.put("total", priceFormat.format(totalEarnings));
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("stock_sell_success", replacements));

        saveAllData();
        return true;
    }

    /**
     * 모든 데이터 저장
     */
    public void saveAllData() {
        // 가격 저장
        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            dataConfig.set("prices." + entry.getKey(), entry.getValue());
        }

        // 플레이어 데이터 저장
        for (Map.Entry<UUID, Map<String, Integer>> playerEntry : playerStocks.entrySet()) {
            for (Map.Entry<String, Integer> stockEntry : playerEntry.getValue().entrySet()) {
                dataConfig.set("players." + playerEntry.getKey().toString() + "." + stockEntry.getKey(),
                        stockEntry.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("주식 데이터 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 리로드
     */
    public void reload() {
        stopScheduler();
        initializePrices();
        startScheduler();
    }

    // Getter 및 Setter 메서드들
    public double getCurrentPrice(String stockId) {
        return currentPrices.getOrDefault(stockId, 0.0);
    }

    /**
     * 현재 가격 수동 설정 (관리자용)
     */
    public void setCurrentPrice(String stockId, double price) {
        previousPrices.put(stockId, currentPrices.getOrDefault(stockId, price));
        currentPrices.put(stockId, price);
    }

    public double getPreviousPrice(String stockId) {
        return previousPrices.getOrDefault(stockId, 0.0);
    }

    public String getFormattedPrice(String stockId) {
        return priceFormat.format(getCurrentPrice(stockId));
    }

    public String getFormattedChange(String stockId) {
        double current = getCurrentPrice(stockId);
        double previous = getPreviousPrice(stockId);
        double change = current - previous;
        double changePercent = (previous > 0) ? (change / previous * 100) : 0;

        String color = change >= 0 ? "§a▲" : "§c▼";
        return color + String.format("%.2f%%", Math.abs(changePercent));
    }

    /**
     * 등락률을 퍼센트 값으로 반환합니다.
     * 상승: 양수, 하락: 음수
     */
    public double getChangePercent(String stockId) {
        double current = getCurrentPrice(stockId);
        double previous = getPreviousPrice(stockId);
        if (previous <= 0)
            return 0;
        return ((current - previous) / previous) * 100;
    }

    public int getPlayerStockAmount(UUID uuid, String stockId) {
        Map<String, Integer> stocks = playerStocks.get(uuid);
        return stocks != null ? stocks.getOrDefault(stockId, 0) : 0;
    }

    /**
     * 플레이어 주식 수량 설정 (디버그용)
     */
    public void setPlayerStockAmount(UUID uuid, String stockId, int amount) {
        Map<String, Integer> stocks = playerStocks.computeIfAbsent(uuid, k -> new HashMap<>());
        if (amount <= 0) {
            stocks.remove(stockId);
        } else {
            stocks.put(stockId, amount);
        }
        saveAllData();
    }

    /**
     * 플레이어 모든 주식 초기화 (디버그용)
     */
    public void clearPlayerStocks(UUID uuid) {
        playerStocks.remove(uuid);
        saveAllData();
    }

    public Map<String, Integer> getPlayerStocks(UUID uuid) {
        return playerStocks.getOrDefault(uuid, new HashMap<>());
    }
}
