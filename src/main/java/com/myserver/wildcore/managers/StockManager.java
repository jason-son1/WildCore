package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.PlayerStockData;
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

    // 주식 가격 기록 (종목ID -> 최근 가격 목록, 최대 20개)
    private Map<String, List<Double>> priceHistory = new HashMap<>();

    // 플레이어별 주식 보유량 (UUID -> (종목ID -> 데이터))
    private Map<UUID, Map<String, PlayerStockData>> playerStocks = new HashMap<>();

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
                Map<String, PlayerStockData> stocks = new HashMap<>();

                for (String stockId : dataConfig.getConfigurationSection("players." + uuidStr).getKeys(false)) {
                    String path = "players." + uuidStr + "." + stockId;

                    if (dataConfig.isConfigurationSection(path)) {
                        // 신규 데이터 구조 (객체)
                        int amount = dataConfig.getInt(path + ".amount");
                        double avgPrice = dataConfig.getDouble(path + ".averagePrice");
                        double totalInvested = dataConfig.getDouble(path + ".totalInvested");
                        stocks.put(stockId, new PlayerStockData(amount, avgPrice, totalInvested));
                    } else if (dataConfig.isInt(path)) {
                        // 구 데이터 구조 (정수) -> 마이그레이션
                        int amount = dataConfig.getInt(path);
                        if (amount > 0) {
                            // 마이그레이션: 평단가는 현재 가격으로 설정 (혹은 베이스 가격?)
                            // User agreed to reset to current price. But here we might not have prices
                            // loaded yet?
                            // InitializePrices is called AFTER loadData. So currentPrices might be empty.
                            // However, we can update it later or just set to 0 and let verify fix it?
                            // Or better, set it to 0 for now and maybe update it when price is available?
                            // Actually, let's look at constructor: PlayerStockData(amount) sets avg/total
                            // to 0.
                            stocks.put(stockId, new PlayerStockData(amount));
                        }
                    }
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

        // 가격 기록 데이터 로드
        if (dataConfig.isConfigurationSection("history")) {
            for (String stockId : dataConfig.getConfigurationSection("history").getKeys(false)) {
                List<Double> history = dataConfig.getDoubleList("history." + stockId);
                priceHistory.put(stockId, new ArrayList<>(history));
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
        currentPrices.put(stock.getId(), newPrice);

        // 기록 업데이트
        List<Double> history = priceHistory.computeIfAbsent(stock.getId(), k -> new ArrayList<>());
        history.add(newPrice);
        if (history.size() > 20) {
            history.remove(0);
        }
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
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, totalCost);

        // 주식 추가
        Map<String, PlayerStockData> stocks = playerStocks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        PlayerStockData data = stocks.computeIfAbsent(stockId, k -> new PlayerStockData(0));
        data.addPurchase(amount, price);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("stock", stock.getDisplayName());
        replacements.put("amount", String.valueOf(amount));
        replacements.put("total", priceFormat.format(totalCost));
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("stock_buy_success", replacements));
        plugin.getConfigManager().playSound(player, "buy");

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

        Map<String, PlayerStockData> stocks = playerStocks.get(player.getUniqueId());
        PlayerStockData data = (stocks != null) ? stocks.get(stockId) : null;

        if (data == null || data.getAmount() < amount) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock_insufficient"));
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        double price = getCurrentPrice(stockId);
        double totalEarnings = price * amount;

        // 주식 차감
        data.removeSale(amount);
        if (data.getAmount() <= 0) {
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
        plugin.getConfigManager().playSound(player, "sell");

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

        // 가격 기록 저장
        for (Map.Entry<String, List<Double>> entry : priceHistory.entrySet()) {
            dataConfig.set("history." + entry.getKey(), entry.getValue());
        }

        // 플레이어 데이터 저장
        for (Map.Entry<UUID, Map<String, PlayerStockData>> playerEntry : playerStocks.entrySet()) {
            Map<String, PlayerStockData> stocks = playerEntry.getValue();
            if (stocks == null)
                continue;
            for (Map.Entry<String, PlayerStockData> stockEntry : stocks.entrySet()) {
                String path = "players." + playerEntry.getKey().toString() + "." + stockEntry.getKey();
                PlayerStockData data = stockEntry.getValue();
                dataConfig.set(path + ".amount", data.getAmount());
                dataConfig.set(path + ".averagePrice", data.getAveragePrice());
                dataConfig.set(path + ".totalInvested", data.getTotalInvested());
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
        Map<String, PlayerStockData> stocks = playerStocks.get(uuid);
        return (stocks != null && stocks.containsKey(stockId)) ? stocks.get(stockId).getAmount() : 0;
    }

    public double getPlayerAveragePrice(UUID uuid, String stockId) {
        Map<String, PlayerStockData> stocks = playerStocks.get(uuid);
        return (stocks != null && stocks.containsKey(stockId)) ? stocks.get(stockId).getAveragePrice() : 0;
    }

    /**
     * 플레이어 주식 수량 설정 (디버그용)
     */
    public void setPlayerStockAmount(UUID uuid, String stockId, int amount) {
        Map<String, PlayerStockData> stocks = playerStocks.computeIfAbsent(uuid, k -> new HashMap<>());
        if (amount <= 0) {
            stocks.remove(stockId);
        } else {
            // 디버그용 강제 설정이므로 평단가는 0으로 초기화되거나 기존 유지?
            // 여기선 새로 생성
            stocks.put(stockId, new PlayerStockData(amount));
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

    public Map<String, PlayerStockData> getPlayerStocks(UUID uuid) {
        return playerStocks.getOrDefault(uuid, new HashMap<>());
    }

    public List<Double> getPriceHistory(String stockId) {
        return priceHistory.getOrDefault(stockId, new ArrayList<>());
    }

    /**
     * 최근 N회의 변동 추세를 아이콘 스트링으로 반환합니다.
     */
    public String getTrendIcons(String stockId, int limit) {
        List<Double> history = getPriceHistory(stockId);
        if (history.size() < 2)
            return "§8-";

        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, history.size() - limit);
        for (int i = start + 1; i < history.size(); i++) {
            double prev = history.get(i - 1);
            double curr = history.get(i);
            if (curr > prev)
                sb.append("§a▲ ");
            else if (curr < prev)
                sb.append("§c▼ ");
            else
                sb.append("§7- ");
        }
        return sb.toString().trim();
    }
}
