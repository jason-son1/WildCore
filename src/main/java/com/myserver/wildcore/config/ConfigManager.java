package com.myserver.wildcore.config;

import com.myserver.wildcore.WildCore;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 모든 설정 파일을 관리하는 클래스
 */
public class ConfigManager {

    private final WildCore plugin;

    // 설정 파일들
    private FileConfiguration config;
    private FileConfiguration stocksConfig;
    private FileConfiguration enchantsConfig;
    private FileConfiguration itemsConfig;

    // 파일 객체들
    private File configFile;
    private File stocksFile;
    private File enchantsFile;
    private File itemsFile;

    // 캐시된 데이터
    private Map<String, StockConfig> stocks = new HashMap<>();
    private Map<String, EnchantConfig> enchants = new HashMap<>();
    private Map<String, CustomItemConfig> customItems = new HashMap<>();
    private Map<String, String> messages = new HashMap<>();

    // 수정된 설정 캐시 (실시간 편집용)
    private Map<String, Double> modifiedStockVolatility = new HashMap<>();
    private Map<String, Double> modifiedStockMinPrice = new HashMap<>();
    private Map<String, Double> modifiedStockBasePrice = new HashMap<>();
    private Map<String, Double> modifiedStockMaxPrice = new HashMap<>();
    private Map<String, Double> modifiedEnchantSuccess = new HashMap<>();
    private Map<String, Double> modifiedEnchantFail = new HashMap<>();
    private Map<String, Double> modifiedEnchantDestroy = new HashMap<>();
    private Map<String, Double> modifiedEnchantCost = new HashMap<>();
    private Map<String, List<String>> modifiedEnchantItems = new HashMap<>();

    public ConfigManager(WildCore plugin) {
        this.plugin = plugin;
    }

    /**
     * 모든 설정 파일 로드
     */
    public void loadAllConfigs() {
        // 기본 설정 파일 생성
        plugin.saveDefaultConfig();
        saveDefaultFile("stocks.yml");
        saveDefaultFile("enchants.yml");
        saveDefaultFile("items.yml");

        // 파일 로드
        configFile = new File(plugin.getDataFolder(), "config.yml");
        stocksFile = new File(plugin.getDataFolder(), "stocks.yml");
        enchantsFile = new File(plugin.getDataFolder(), "enchants.yml");
        itemsFile = new File(plugin.getDataFolder(), "items.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        stocksConfig = YamlConfiguration.loadConfiguration(stocksFile);
        enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

        // 데이터 파싱
        loadMessages();
        loadStocks();
        loadEnchants();
        loadCustomItems();

        // 수정 캐시 초기화
        clearModifiedCaches();

        plugin.getLogger().info("모든 설정 파일이 로드되었습니다.");
    }

    private void clearModifiedCaches() {
        modifiedStockVolatility.clear();
        modifiedStockMinPrice.clear();
        modifiedStockBasePrice.clear();
        modifiedStockMaxPrice.clear();
        modifiedEnchantSuccess.clear();
        modifiedEnchantFail.clear();
        modifiedEnchantDestroy.clear();
        modifiedEnchantCost.clear();
        modifiedEnchantItems.clear();
    }

    /**
     * 기본 설정 파일 저장
     */
    private void saveDefaultFile(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    /**
     * 메시지 로드
     */
    private void loadMessages() {
        messages.clear();
        if (config.isConfigurationSection("messages")) {
            for (String key : config.getConfigurationSection("messages").getKeys(false)) {
                messages.put(key, colorize(config.getString("messages." + key)));
            }
        }
    }

    /**
     * 주식 설정 로드
     */
    private void loadStocks() {
        stocks.clear();
        if (stocksConfig.isConfigurationSection("stocks")) {
            for (String key : stocksConfig.getConfigurationSection("stocks").getKeys(false)) {
                String path = "stocks." + key;
                StockConfig stock = new StockConfig(
                        key,
                        colorize(stocksConfig.getString(path + ".display_name")),
                        stocksConfig.getString(path + ".material"),
                        stocksConfig.getInt(path + ".slot"),
                        stocksConfig.getDouble(path + ".base_price"),
                        stocksConfig.getDouble(path + ".min_price"),
                        stocksConfig.getDouble(path + ".max_price"),
                        stocksConfig.getDouble(path + ".volatility"),
                        colorize(stocksConfig.getStringList(path + ".lore")));
                stocks.put(key, stock);
            }
        }
        plugin.getLogger().info("주식 " + stocks.size() + "개 로드됨");
    }

    /**
     * 인챈트 설정 로드
     */
    private void loadEnchants() {
        enchants.clear();
        if (enchantsConfig.isConfigurationSection("tiers")) {
            for (String key : enchantsConfig.getConfigurationSection("tiers").getKeys(false)) {
                String path = "tiers." + key;
                EnchantConfig enchant = new EnchantConfig(
                        key,
                        colorize(enchantsConfig.getString(path + ".display_name")),
                        enchantsConfig.getString(path + ".material"),
                        enchantsConfig.getInt(path + ".slot"),
                        enchantsConfig.getStringList(path + ".target_whitelist"),
                        enchantsConfig.getStringList(path + ".target_groups"),
                        enchantsConfig.getString(path + ".result.enchantment"),
                        enchantsConfig.getInt(path + ".result.level"),
                        enchantsConfig.getDouble(path + ".cost.money"),
                        enchantsConfig.getStringList(path + ".cost.items"),
                        enchantsConfig.getDouble(path + ".probability.success"),
                        enchantsConfig.getDouble(path + ".probability.fail"),
                        enchantsConfig.getDouble(path + ".probability.destroy"),
                        colorize(enchantsConfig.getStringList(path + ".lore")),
                        enchantsConfig.getBoolean(path + ".unsafe_mode", false),
                        enchantsConfig.getBoolean(path + ".ignore_conflicts", false));
                enchants.put(key, enchant);
            }
        }
        plugin.getLogger().info("인챈트 " + enchants.size() + "개 로드됨");
    }

    /**
     * 커스텀 아이템 설정 로드
     */
    private void loadCustomItems() {
        customItems.clear();
        if (itemsConfig.isConfigurationSection("items")) {
            for (String key : itemsConfig.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                CustomItemConfig item = new CustomItemConfig(
                        key,
                        itemsConfig.getString(path + ".material"),
                        colorize(itemsConfig.getString(path + ".display_name")),
                        itemsConfig.getInt(path + ".custom_model_data", 0),
                        itemsConfig.getBoolean(path + ".glow", false),
                        colorize(itemsConfig.getStringList(path + ".lore")));
                customItems.put(key, item);
            }
        }
        plugin.getLogger().info("커스텀 아이템 " + customItems.size() + "개 로드됨");
    }

    /**
     * 색상 코드 변환
     */
    public String colorize(String text) {
        if (text == null)
            return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    /**
     * 리스트 색상 코드 변환
     */
    public List<String> colorize(List<String> list) {
        return list.stream().map(this::colorize).toList();
    }

    // =====================
    // 관리자 기능 메서드들
    // =====================

    /**
     * 주식 변동성 가져오기 (수정된 값 우선)
     */
    public double getStockVolatility(String stockId) {
        if (modifiedStockVolatility.containsKey(stockId)) {
            return modifiedStockVolatility.get(stockId);
        }
        StockConfig stock = stocks.get(stockId);
        return stock != null ? stock.getVolatility() : 0.1;
    }

    /**
     * 주식 변동성 설정 (메모리)
     */
    public void setStockVolatility(String stockId, double value) {
        modifiedStockVolatility.put(stockId, value);
    }

    /**
     * 주식 가격 설정 (min/base/max)
     */
    public void setStockPrice(String stockId, String type, double value) {
        switch (type) {
            case "min" -> modifiedStockMinPrice.put(stockId, value);
            case "base" -> modifiedStockBasePrice.put(stockId, value);
            case "max" -> modifiedStockMaxPrice.put(stockId, value);
        }
    }

    /**
     * 인챈트 확률 가져오기 (수정된 값 우선)
     */
    public double getEnchantProbability(String enchantId, String type) {
        Map<String, Double> cache = switch (type) {
            case "success" -> modifiedEnchantSuccess;
            case "fail" -> modifiedEnchantFail;
            case "destroy" -> modifiedEnchantDestroy;
            default -> null;
        };

        if (cache != null && cache.containsKey(enchantId)) {
            return cache.get(enchantId);
        }

        EnchantConfig enchant = enchants.get(enchantId);
        if (enchant == null)
            return 0;

        return switch (type) {
            case "success" -> enchant.getSuccessRate();
            case "fail" -> enchant.getFailRate();
            case "destroy" -> enchant.getDestroyRate();
            default -> 0;
        };
    }

    /**
     * 인챈트 확률 설정 (메모리)
     */
    public void setEnchantProbability(String enchantId, String type, double value) {
        switch (type) {
            case "success" -> modifiedEnchantSuccess.put(enchantId, value);
            case "fail" -> modifiedEnchantFail.put(enchantId, value);
            case "destroy" -> modifiedEnchantDestroy.put(enchantId, value);
        }
    }

    /**
     * 인챈트 비용 설정 (메모리)
     */
    public void setEnchantCost(String enchantId, double cost) {
        modifiedEnchantCost.put(enchantId, cost);
    }

    /**
     * 인챈트 재료 아이템 설정 (메모리)
     */
    public void setEnchantItems(String enchantId, String itemsStr) {
        List<String> items = Arrays.asList(itemsStr.split(","));
        modifiedEnchantItems.put(enchantId, items);
    }

    /**
     * 주식 설정 파일에 저장
     */
    public void saveStockConfig(String stockId) {
        String path = "stocks." + stockId;

        // 수정된 값들 저장
        if (modifiedStockVolatility.containsKey(stockId)) {
            stocksConfig.set(path + ".volatility", modifiedStockVolatility.get(stockId));
        }
        if (modifiedStockMinPrice.containsKey(stockId)) {
            stocksConfig.set(path + ".min_price", modifiedStockMinPrice.get(stockId));
        }
        if (modifiedStockBasePrice.containsKey(stockId)) {
            stocksConfig.set(path + ".base_price", modifiedStockBasePrice.get(stockId));
        }
        if (modifiedStockMaxPrice.containsKey(stockId)) {
            stocksConfig.set(path + ".max_price", modifiedStockMaxPrice.get(stockId));
        }

        try {
            stocksConfig.save(stocksFile);
            loadStocks(); // 리로드
            plugin.getLogger().info("주식 설정 저장됨: " + stockId);
        } catch (IOException e) {
            plugin.getLogger().severe("주식 설정 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 인챈트 설정 파일에 저장
     */
    public void saveEnchantConfig(String enchantId) {
        String path = "tiers." + enchantId;

        // 수정된 값들 저장
        if (modifiedEnchantSuccess.containsKey(enchantId)) {
            enchantsConfig.set(path + ".probability.success", modifiedEnchantSuccess.get(enchantId));
        }
        if (modifiedEnchantFail.containsKey(enchantId)) {
            enchantsConfig.set(path + ".probability.fail", modifiedEnchantFail.get(enchantId));
        }
        if (modifiedEnchantDestroy.containsKey(enchantId)) {
            enchantsConfig.set(path + ".probability.destroy", modifiedEnchantDestroy.get(enchantId));
        }
        if (modifiedEnchantCost.containsKey(enchantId)) {
            enchantsConfig.set(path + ".cost.money", modifiedEnchantCost.get(enchantId));
        }
        if (modifiedEnchantItems.containsKey(enchantId)) {
            enchantsConfig.set(path + ".cost.items", modifiedEnchantItems.get(enchantId));
        }

        try {
            enchantsConfig.save(enchantsFile);
            loadEnchants(); // 리로드
            plugin.getLogger().info("인챈트 설정 저장됨: " + enchantId);
        } catch (IOException e) {
            plugin.getLogger().severe("인챈트 설정 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 주식 삭제
     */
    public void deleteStock(String stockId) {
        stocksConfig.set("stocks." + stockId, null);
        try {
            stocksConfig.save(stocksFile);
            stocks.remove(stockId);
        } catch (IOException e) {
            plugin.getLogger().severe("주식 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 인챈트 삭제
     */
    public void deleteEnchant(String enchantId) {
        enchantsConfig.set("tiers." + enchantId, null);
        try {
            enchantsConfig.save(enchantsFile);
            enchants.remove(enchantId);
        } catch (IOException e) {
            plugin.getLogger().severe("인챈트 삭제 실패: " + e.getMessage());
        }
    }

    /**
     * 새 주식 생성
     */
    public boolean createNewStock(String stockId) {
        if (stocks.containsKey(stockId))
            return false;

        String path = "stocks." + stockId;
        stocksConfig.set(path + ".display_name", "&f새 주식: " + stockId);
        stocksConfig.set(path + ".material", "PAPER");
        stocksConfig.set(path + ".slot", getNextAvailableSlot("stock"));
        stocksConfig.set(path + ".base_price", 1000.0);
        stocksConfig.set(path + ".min_price", 100.0);
        stocksConfig.set(path + ".max_price", 10000.0);
        stocksConfig.set(path + ".volatility", 0.1);
        stocksConfig.set(path + ".lore", Arrays.asList("", "&7현재 가격: &6%price%원", "&7변동률: %change%"));

        try {
            stocksConfig.save(stocksFile);
            loadStocks();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("주식 생성 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 새 인챈트 생성
     */
    public boolean createNewEnchant(String enchantId) {
        if (enchants.containsKey(enchantId))
            return false;

        String path = "tiers." + enchantId;
        enchantsConfig.set(path + ".display_name", "&f새 인챈트: " + enchantId);
        enchantsConfig.set(path + ".material", "ENCHANTED_BOOK");
        enchantsConfig.set(path + ".slot", getNextAvailableSlot("enchant"));
        enchantsConfig.set(path + ".target_whitelist", Arrays.asList("DIAMOND_SWORD"));
        enchantsConfig.set(path + ".target_groups", Arrays.asList("WEAPON"));
        enchantsConfig.set(path + ".result.enchantment", "sharpness");
        enchantsConfig.set(path + ".result.level", 1);
        enchantsConfig.set(path + ".cost.money", 1000.0);
        enchantsConfig.set(path + ".cost.items", Arrays.asList("DIAMOND:1"));
        enchantsConfig.set(path + ".probability.success", 50.0);
        enchantsConfig.set(path + ".probability.fail", 40.0);
        enchantsConfig.set(path + ".probability.destroy", 10.0);
        enchantsConfig.set(path + ".lore", Arrays.asList("", "&7성공 확률: &a50%"));
        enchantsConfig.set(path + ".unsafe_mode", false);
        enchantsConfig.set(path + ".ignore_conflicts", false);

        try {
            enchantsConfig.save(enchantsFile);
            loadEnchants();
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("인챈트 생성 실패: " + e.getMessage());
            return false;
        }
    }

    private int getNextAvailableSlot(String type) {
        int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34 };
        int usedCount = (type.equals("stock")) ? stocks.size() : enchants.size();
        return slots[Math.min(usedCount, slots.length - 1)];
    }

    // =====================
    // 기존 Getter 메서드들
    // =====================

    public String getPrefix() {
        return colorize(config.getString("prefix", "&8[&6WildCore&8] &f"));
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public String getMessage(String key, Map<String, String> replacements) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return message;
    }

    public int getStockUpdateInterval() {
        return config.getInt("settings.stock_update_interval", 1800);
    }

    public boolean isVanillaEnchantBlocked() {
        return config.getBoolean("settings.block_vanilla_enchant_table", true);
    }

    public boolean isDebugEnabled() {
        return config.getBoolean("settings.debug", false);
    }

    public Map<String, StockConfig> getStocks() {
        return stocks;
    }

    public StockConfig getStock(String id) {
        return stocks.get(id);
    }

    public Map<String, EnchantConfig> getEnchants() {
        return enchants;
    }

    public EnchantConfig getEnchant(String id) {
        return enchants.get(id);
    }

    public Map<String, CustomItemConfig> getCustomItems() {
        return customItems;
    }

    public CustomItemConfig getCustomItem(String id) {
        return customItems.get(id);
    }

    public String getStockGuiTitle() {
        return colorize(stocksConfig.getString("gui.title", "&8[ &a주식 시장 &8]"));
    }

    public int getStockGuiSize() {
        return stocksConfig.getInt("gui.size", 27);
    }

    public String getEnchantGuiTitle() {
        return colorize(enchantsConfig.getString("gui.title", "&8[ &5강화소 &8]"));
    }

    public int getEnchantGuiSize() {
        return enchantsConfig.getInt("gui.size", 27);
    }

    public FileConfiguration getStocksConfig() {
        return stocksConfig;
    }

    public FileConfiguration getEnchantsConfig() {
        return enchantsConfig;
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }
}
