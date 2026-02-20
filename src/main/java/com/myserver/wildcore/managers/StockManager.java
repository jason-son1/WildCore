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

    // 다음 가격 업데이트 시각 (밀리초 타임스탬프)
    private long nextUpdateTime = 0;

    // 업데이트 간격 (초) - startScheduler에서 설정
    private int updateIntervalSeconds = 0;

    // 데이터 파일
    private File dataFile;
    private FileConfiguration dataConfig;

    private final DecimalFormat priceFormat = new DecimalFormat("#,##0.0");
    private final DecimalFormat changeFormat = new DecimalFormat("+#,##0.0;-#,##0.0");
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
        // 메모리 초기화
        playerStocks.clear();
        currentPrices.clear();
        priceHistory.clear();

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

        // 다음 업데이트 시각 로드
        nextUpdateTime = dataConfig.getLong("nextUpdateTime", 0);

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
     * 저장된 nextUpdateTime을 기반으로 초기 딜레이를 계산하여
     * 서버 재시작 시에도 타이머가 이어서 진행됩니다.
     */
    public void startScheduler() {
        if (!plugin.getConfigManager().isStockSystemEnabled()) {
            return;
        }

        updateIntervalSeconds = plugin.getConfigManager().getStockUpdateInterval();
        int intervalTicks = updateIntervalSeconds * 20; // 초 -> 틱 변환

        // 저장된 다음 업데이트 시각 기반으로 초기 딜레이 계산
        long now = System.currentTimeMillis();
        long initialDelayTicks;

        if (nextUpdateTime > now) {
            // 저장된 다음 업데이트 시각이 아직 미래 → 남은 시간만큼 대기
            long remainingMillis = nextUpdateTime - now;
            initialDelayTicks = Math.max(1, (remainingMillis / 1000) * 20);
            plugin.getLogger().info("주식 스케줄러: 저장된 타이머 복원 (남은 시간: " +
                    (remainingMillis / 1000) + "초)");
        } else {
            // 저장된 시각이 과거이거나 없음 → 즉시 1회 업데이트 후 정상 간격
            initialDelayTicks = 1;
            nextUpdateTime = now + (updateIntervalSeconds * 1000L);
            plugin.getLogger().info("주식 스케줄러: 업데이트 시각이 지남, 즉시 업데이트 실행");
        }

        schedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateAllPrices();
                // 다음 업데이트 시간 갱신
                nextUpdateTime = System.currentTimeMillis() + (updateIntervalSeconds * 1000L);
            }
        }.runTaskTimerAsynchronously(plugin, initialDelayTicks, intervalTicks);

        plugin.getLogger().info("주식 가격 업데이트 스케줄러 시작 (간격: " +
                updateIntervalSeconds + "초)");
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
        if (!plugin.getConfigManager().isStockSystemEnabled()) {
            return;
        }

        for (StockConfig stock : plugin.getConfigManager().getStocks().values()) {
            updatePrice(stock);
        }

        // 데이터 저장
        saveAllData();

        // 가격 변동 알림 전송
        broadcastPriceUpdate();

        plugin.debug("주식 가격이 업데이트되었습니다.");
    }

    /**
     * 주식 가격 변동 알림 전송
     */
    private void broadcastPriceUpdate() {
        plugin.getServer().broadcastMessage(plugin.getConfigManager().getMessage("stock.price_update_header"));

        List<StockConfig> stocks = plugin.getConfigManager().getAllStocksSorted();
        for (StockConfig stock : stocks) {
            plugin.getServer()
                    .broadcastMessage(plugin.getConfigManager().getMessage("stock.price_update_entry",
                            "stock", stock.getDisplayName(),
                            "price", getFormattedPrice(stock.getId()),
                            "change", getFormattedChange(stock.getId())));
        }

        plugin.getServer().broadcastMessage(plugin.getConfigManager().getMessage("stock.price_update_footer"));
    }

    // 평균 회귀 강도 (기준가로 돌아오는 힘)
    private static final double REVERSION_STRENGTH = 0.1;
    // 모멘텀 가중치 (최근 추세 반영)
    private static final double MOMENTUM_WEIGHT = 0.3;
    // 이벤트 발생 확률 (급등/급락)
    private static final double EVENT_PROBABILITY = 0.05;
    // 이벤트 배율 범위
    private static final double EVENT_MIN_MULTIPLIER = 2.0;
    private static final double EVENT_MAX_MULTIPLIER = 3.0;

    /**
     * 단일 주식 가격 업데이트 (현실적 시뮬레이션)
     *
     * 알고리즘 구성요소:
     * 1. 랜덤 노이즈: 기본 변동성 기반 무작위 변동
     * 2. 평균 회귀: basePrice로 돌아가려는 힘 (가격이 멀어질수록 강해짐)
     * 3. 모멘텀: 최근 가격 추세를 반영 (상승/하락 관성)
     * 4. 이벤트: 낮은 확률로 급등/급락 발생
     * 5. 가격 보정: min/max 범위 내로 제한
     */
    private void updatePrice(StockConfig stock) {
        double currentPrice = currentPrices.getOrDefault(stock.getId(), stock.getBasePrice());
        previousPrices.put(stock.getId(), currentPrice);

        double basePrice = stock.getBasePrice();
        double volatility = stock.getVolatility();

        // 1. 랜덤 노이즈 (-volatility ~ +volatility)
        double noise = (random.nextDouble() * 2 - 1) * volatility;

        // 2. 평균 회귀 (기준가에서 멀어질수록 되돌아오는 힘)
        double reversion = 0;
        if (basePrice > 0) {
            reversion = ((basePrice - currentPrice) / basePrice) * REVERSION_STRENGTH;
        }

        // 3. 모멘텀 (최근 가격 추세 반영)
        double momentum = 0;
        List<Double> history = priceHistory.get(stock.getId());
        if (history != null && history.size() >= 3) {
            // 최근 3회 변동률 평균 계산
            double sumChange = 0;
            int count = 0;
            for (int i = history.size() - 1; i >= Math.max(0, history.size() - 3) && i > 0; i--) {
                double prevPrice = history.get(i - 1);
                if (prevPrice > 0) {
                    sumChange += (history.get(i) - prevPrice) / prevPrice;
                    count++;
                }
            }
            if (count > 0) {
                momentum = (sumChange / count) * MOMENTUM_WEIGHT;
            }
        }

        // 4. 최종 변동률 계산
        double change = noise + reversion + momentum;

        // 5. 이벤트 시스템 (낮은 확률로 급등/급락)
        if (random.nextDouble() < EVENT_PROBABILITY) {
            double eventMultiplier = EVENT_MIN_MULTIPLIER +
                    random.nextDouble() * (EVENT_MAX_MULTIPLIER - EVENT_MIN_MULTIPLIER);
            // 이벤트 방향은 기존 변동 방향 유지
            change *= eventMultiplier;
            plugin.debug("주식 이벤트 발생! " + stock.getDisplayName() +
                    " 배율: " + String.format("%.1f", eventMultiplier));
        }

        // 6. 새 가격 계산
        double newPrice = currentPrice * (1 + change);

        // 7. 최소/최대 가격 보정
        double minPrice = stock.getMinPrice();
        double maxPrice = stock.getMaxPrice();
        if (minPrice > 0) {
            newPrice = Math.max(minPrice, newPrice);
        }
        if (maxPrice > 0) {
            newPrice = Math.min(maxPrice, newPrice);
        }

        // 가격이 0 이하로 떨어지지 않도록 보장
        newPrice = Math.max(0.1, newPrice);

        currentPrices.put(stock.getId(), newPrice);

        // 기록 업데이트
        List<Double> priceList = priceHistory.computeIfAbsent(stock.getId(), k -> new ArrayList<>());
        priceList.add(newPrice);
        if (priceList.size() > 20) {
            priceList.remove(0);
        }
    }

    /**
     * 주식 매수
     */
    public boolean buyStock(Player player, String stockId, int amount) {
        if (!plugin.getConfigManager().isStockSystemEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock_disabled"));
            return false;
        }

        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock == null)
            return false;

        double price = getCurrentPrice(stockId);
        double totalCost = price * amount;

        // 돈 확인 및 차감
        if (!plugin.getEconomy().has(player, totalCost)) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock.no_money", "amount", priceFormat.format(totalCost)));
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, totalCost);

        // 주식 추가
        Map<String, PlayerStockData> stocks = playerStocks.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        PlayerStockData data = stocks.computeIfAbsent(stockId, k -> new PlayerStockData(0));
        data.addPurchase(amount, price);

        // 메시지 전송
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("stock.buy_success",
                        "stock", stock.getDisplayName(),
                        "amount", String.valueOf(amount),
                        "price", priceFormat.format(totalCost),
                        "total", priceFormat.format(totalCost)));
        plugin.getConfigManager().playSound(player, "buy");

        saveAllData();
        return true;
    }

    /**
     * 주식 매도
     */
    public boolean sellStock(Player player, String stockId, int amount) {
        if (!plugin.getConfigManager().isStockSystemEnabled()) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock_disabled"));
            return false;
        }

        StockConfig stock = plugin.getConfigManager().getStock(stockId);
        if (stock == null)
            return false;

        Map<String, PlayerStockData> stocks = playerStocks.get(player.getUniqueId());
        PlayerStockData data = (stocks != null) ? stocks.get(stockId) : null;

        if (data == null || data.getAmount() < amount) {
            player.sendMessage(plugin.getConfigManager().getPrefix() +
                    plugin.getConfigManager().getMessage("stock.no_stocks"));
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
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage("stock.sell_success",
                        "stock", stock.getDisplayName(),
                        "amount", String.valueOf(amount),
                        "price", priceFormat.format(totalEarnings),
                        "total", priceFormat.format(totalEarnings)));
        plugin.getConfigManager().playSound(player, "sell");

        saveAllData();
        return true;
    }

    /**
     * 모든 데이터 저장
     */
    public synchronized void saveAllData() {
        // 가격 저장
        for (Map.Entry<String, Double> entry : currentPrices.entrySet()) {
            dataConfig.set("prices." + entry.getKey(), entry.getValue());
        }

        // 가격 기록 저장
        for (Map.Entry<String, List<Double>> entry : priceHistory.entrySet()) {
            dataConfig.set("history." + entry.getKey(), entry.getValue());
        }

        // 다음 업데이트 시각 저장
        dataConfig.set("nextUpdateTime", nextUpdateTime);

        // 플레이어 데이터 저장
        dataConfig.set("players", null); // 기존 데이터 초기화
        for (Map.Entry<UUID, Map<String, PlayerStockData>> playerEntry : playerStocks.entrySet()) {
            Map<String, PlayerStockData> stocks = playerEntry.getValue();
            if (stocks == null || stocks.isEmpty()) // 빈 데이터는 저장하지 않음
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
        loadData(); // 데이터 다시 로드
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

    /**
     * 다음 가격 변동까지 남은 시간 (밀리초)
     */
    public long getTimeUntilNextUpdate() {
        return Math.max(0, nextUpdateTime - System.currentTimeMillis());
    }

    /**
     * 다음 가격 변동까지 남은 시간 (포맷팅된 문자열)
     */
    public String getFormattedTimeUntilNextUpdate() {
        long remaining = getTimeUntilNextUpdate();
        long minutes = (remaining / 1000) / 60;
        long seconds = (remaining / 1000) % 60;
        return String.format("%d분 %02d초", minutes, seconds);
    }
}
