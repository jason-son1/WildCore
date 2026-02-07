package com.myserver.wildcore.config;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.npc.NpcType;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;
import org.bukkit.Material;

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
    private FileConfiguration shopsConfig;
    private FileConfiguration locationsConfig;
    private FileConfiguration banksConfig;
    private FileConfiguration buffBlocksConfig;
    private FileConfiguration miningDropsConfig;
    private FileConfiguration messagesConfig;

    // 파일 객체들
    private File configFile;
    private File stocksFile;
    private File enchantsFile;
    private File itemsFile;
    private File shopsFile;
    private File locationsFile;
    private File banksFile;
    private File buffBlocksFile;
    private File miningDropsFile;
    private File messagesFile;

    // 캐시된 데이터
    private Map<String, StockConfig> stocks = new HashMap<>();
    private Map<String, EnchantConfig> enchants = new HashMap<>();
    private Map<String, CustomItemConfig> customItems = new HashMap<>();
    private Map<String, ShopConfig> shops = new HashMap<>();
    private Map<String, BankProductConfig> bankProducts = new HashMap<>();
    private Map<String, BuffBlockConfig> buffBlocks = new HashMap<>();
    private Map<org.bukkit.Material, MiningDropData> miningDrops = new HashMap<>();
    private Map<String, String> messages = new HashMap<>();
    private Map<String, ClaimItemConfig> claimItemConfigs = new HashMap<>();

    // 저장 동기화를 위한 락 객체
    private final Object saveLock = new Object();

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
    private Map<String, Boolean> modifiedEnchantUnsafeMode = new HashMap<>(); // [NEW]
    private Map<String, Boolean> modifiedEnchantIgnoreConflicts = new HashMap<>(); // [NEW]
    private Map<String, Set<String>> modifiedEnchantTargetGroups = new HashMap<>(); // [NEW]
    private Map<String, Set<String>> modifiedEnchantTargetWhitelist = new HashMap<>(); // [NEW]

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
        saveDefaultFile("shops.yml");
        saveDefaultFile("locations.yml");
        saveDefaultFile("messages.yml");
        saveDefaultFile("banks.yml");
        saveDefaultFile("buff_blocks.yml");
        saveDefaultFile("mining_drops.yml");

        // 파일 로드
        configFile = new File(plugin.getDataFolder(), "config.yml");
        stocksFile = new File(plugin.getDataFolder(), "stocks.yml");
        enchantsFile = new File(plugin.getDataFolder(), "enchants.yml");
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        locationsFile = new File(plugin.getDataFolder(), "locations.yml");
        banksFile = new File(plugin.getDataFolder(), "banks.yml");
        buffBlocksFile = new File(plugin.getDataFolder(), "buff_blocks.yml");
        miningDropsFile = new File(plugin.getDataFolder(), "mining_drops.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        stocksConfig = YamlConfiguration.loadConfiguration(stocksFile);
        enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
        locationsConfig = YamlConfiguration.loadConfiguration(locationsFile);
        banksConfig = YamlConfiguration.loadConfiguration(banksFile);
        buffBlocksConfig = YamlConfiguration.loadConfiguration(buffBlocksFile);
        miningDropsConfig = YamlConfiguration.loadConfiguration(miningDropsFile);
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 데이터 파싱
        loadMessages();
        loadStocks();
        loadEnchants();
        loadCustomItems();
        loadShops();
        loadBankProducts();
        loadBuffBlocks();
        loadMiningDrops();

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
        modifiedEnchantUnsafeMode.clear();
        modifiedEnchantIgnoreConflicts.clear();
        modifiedEnchantTargetGroups.clear();
        modifiedEnchantTargetWhitelist.clear();
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
        // messages.yml의 모든 키를 가져옴 (nested 포함)
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages.put(key, colorize(messagesConfig.getString(key)));
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
            boolean changed = false;
            for (String key : enchantsConfig.getConfigurationSection("tiers").getKeys(false)) {
                String path = "tiers." + key;

                // Read basic values first to generate lore
                List<String> whitelist = enchantsConfig.getStringList(path + ".target_whitelist");
                List<String> groups = enchantsConfig.getStringList(path + ".target_groups");
                double costMoney = enchantsConfig.getDouble(path + ".cost.money");
                List<String> costItems = enchantsConfig.getStringList(path + ".cost.items");
                double success = enchantsConfig.getDouble(path + ".probability.success");
                double fail = enchantsConfig.getDouble(path + ".probability.fail");
                double destroy = enchantsConfig.getDouble(path + ".probability.destroy");

                // Generate Lore
                List<String> generatedLore = generateEnchantLore(whitelist, groups, success, fail, destroy, costMoney,
                        costItems);

                // Set generated lore to config
                enchantsConfig.set(path + ".lore", generatedLore);
                changed = true;

                EnchantConfig enchant = new EnchantConfig(
                        key,
                        colorize(enchantsConfig.getString(path + ".display_name")),
                        enchantsConfig.getString(path + ".material"),
                        enchantsConfig.getInt(path + ".slot"),
                        whitelist,
                        groups,
                        enchantsConfig.getString(path + ".result.enchantment"),
                        enchantsConfig.getInt(path + ".result.level"),
                        costMoney,
                        costItems,
                        success,
                        fail,
                        destroy,
                        colorize(generatedLore),
                        enchantsConfig.getBoolean(path + ".unsafe_mode", false),
                        enchantsConfig.getBoolean(path + ".ignore_conflicts", false));
                enchants.put(key, enchant);
            }

            if (changed) {
                try {
                    enchantsConfig.save(enchantsFile);
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to save generated lore to enchants.yml: " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("인챈트 " + enchants.size() + "개 로드됨");
    }

    private List<String> generateEnchantLore(List<String> whitelist, List<String> groups,
            double success, double fail, double destroy,
            double costMoney, List<String> costItems) {
        List<String> lore = new ArrayList<>();
        lore.add("");

        // Target
        StringBuilder targetBuilder = new StringBuilder("&7적용 대상: &f");
        if (!groups.isEmpty()) {
            List<String> koreanGroups = new ArrayList<>();
            for (String group : groups) {
                switch (group.toUpperCase()) {
                    case "WEAPON" -> koreanGroups.add("무기");
                    case "ARMOR" -> koreanGroups.add("방어구");
                    case "TOOL" -> koreanGroups.add("도구");
                    case "PICKAXE" -> koreanGroups.add("곡괭이");
                    case "SWORD" -> koreanGroups.add("검");
                    case "AXE" -> koreanGroups.add("도끼");
                    case "SHOVEL" -> koreanGroups.add("삽");
                    case "HOE" -> koreanGroups.add("괭이");
                    case "BOW" -> koreanGroups.add("활");
                    default -> koreanGroups.add(group);
                }
            }
            targetBuilder.append(String.join(", ", koreanGroups));
        } else if (!whitelist.isEmpty()) {
            targetBuilder.append("지정 아이템");
        } else {
            targetBuilder.append("전체");
        }

        // Simplify whitelist details if mixed with groups (e.g. " (다이아/네더라이트)")
        // This is a heuristic approximation based on user request example
        if (!whitelist.isEmpty()) {
            boolean hasDiamond = false;
            boolean hasNetherite = false;
            boolean hasIron = false;
            boolean hasGold = false;

            for (String mat : whitelist) {
                if (mat.contains("DIAMOND"))
                    hasDiamond = true;
                if (mat.contains("NETHERITE"))
                    hasNetherite = true;
                if (mat.contains("IRON"))
                    hasIron = true;
                if (mat.contains("GOLD"))
                    hasGold = true;
            }

            List<String> types = new ArrayList<>();
            if (hasDiamond)
                types.add("다이아");
            if (hasNetherite)
                types.add("네더라이트");
            if (hasIron)
                types.add("철");
            if (hasGold)
                types.add("금");
            if (!types.isEmpty()) {
                targetBuilder.append(" (").append(String.join("/", types)).append(")");
            }
        }
        lore.add(targetBuilder.toString());

        // Probability
        lore.add("&7성공 확률: &a" + (int) success + "%");
        lore.add("&7실패 확률: &e" + (int) fail + "%");
        lore.add("&7파괴 확률: &c" + (int) destroy + "%");
        lore.add("");

        // Cost
        lore.add(String.format("&7비용: &6%,d원", (long) costMoney));

        // Materials
        if (!costItems.isEmpty()) {
            StringBuilder materialsBuilder = new StringBuilder("&7재료: &b");
            List<String> materialParts = new ArrayList<>();
            for (String itemStr : costItems) {
                String[] parts = itemStr.split(":");
                if (parts.length >= 2) {
                    String materialName = parts[0];
                    String amount = parts[parts.length - 1];

                    String koreanName;
                    if (materialName.equalsIgnoreCase("custom") && parts.length >= 3) {
                        String customId = parts[1];
                        CustomItemConfig customItem = getCustomItem(customId);
                        koreanName = (customItem != null) ? ChatColor.stripColor(customItem.getDisplayName())
                                : customId;
                    } else {
                        org.bukkit.Material mat = org.bukkit.Material.getMaterial(materialName.toUpperCase());
                        koreanName = (mat != null) ? com.myserver.wildcore.util.KoreanMaterialUtil.getName(mat)
                                : materialName;
                    }

                    materialParts.add(koreanName + " x" + amount);
                }
            }
            materialsBuilder.append(String.join(", ", materialParts));
            lore.add(materialsBuilder.toString());
        } else {
            lore.add("&7재료: &b없음");
        }

        return lore;
    }

    /**
     * 디스크에서 인챈트 설정을 다시 읽어오고 로드합니다.
     */
    public void reloadEnchantsFromDisk() {
        if (enchantsFile == null) {
            enchantsFile = new File(plugin.getDataFolder(), "enchants.yml");
        }
        enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
        loadEnchants();
        plugin.getLogger().info("인챈트 설정이 디스크에서 리로드되었습니다.");
    }

    /**
     * 디스크에서 주식 설정을 다시 읽어오고 로드합니다.
     */
    public void reloadStocksFromDisk() {
        if (stocksFile == null) {
            stocksFile = new File(plugin.getDataFolder(), "stocks.yml");
        }
        stocksConfig = YamlConfiguration.loadConfiguration(stocksFile);
        loadStocks();
        plugin.getLogger().info("주식 설정이 디스크에서 리로드되었습니다.");
    }

    /**
     * 디스크에서 은행 설정을 다시 읽어오고 로드합니다.
     */
    public void reloadBanksFromDisk() {
        if (banksFile == null) {
            banksFile = new File(plugin.getDataFolder(), "banks.yml");
        }
        banksConfig = YamlConfiguration.loadConfiguration(banksFile);
        loadBankProducts();
        plugin.getLogger().info("은행 설정이 디스크에서 리로드되었습니다.");
    }

    /**
     * 커스텀 아이템 설정 로드
     */
    private void loadCustomItems() {
        customItems.clear();
        if (itemsConfig.isConfigurationSection("items")) {
            for (String key : itemsConfig.getConfigurationSection("items").getKeys(false)) {
                String path = "items." + key;
                // Parse function(s)
                List<String> functions = new ArrayList<>();
                if (itemsConfig.isList(path + ".function")) {
                    functions.addAll(itemsConfig.getStringList(path + ".function"));
                } else if (itemsConfig.isString(path + ".function")) {
                    functions.add(itemsConfig.getString(path + ".function"));
                }

                CustomItemConfig item = new CustomItemConfig(
                        key,
                        itemsConfig.getString(path + ".material"),
                        colorize(itemsConfig.getString(path + ".display_name")),
                        itemsConfig.getInt(path + ".custom_model_data", 0),
                        itemsConfig.getBoolean(path + ".glow", false),
                        colorize(itemsConfig.getStringList(path + ".lore")),
                        functions);
                customItems.put(key, item);
            }
        }

        plugin.getLogger().info("커스텀 아이템 " + customItems.size() + "개 로드됨");
    }

    /**
     * 은행 상품 설정 로드
     */
    private void loadBankProducts() {
        bankProducts.clear();
        if (banksConfig.isConfigurationSection("products")) {
            for (String key : banksConfig.getConfigurationSection("products").getKeys(false)) {
                String path = "products." + key;

                BankProductType type = BankProductType.fromString(
                        banksConfig.getString(path + ".type", "SAVINGS"));

                BankProductConfig product = new BankProductConfig(
                        key,
                        colorize(banksConfig.getString(path + ".display_name", key)),
                        banksConfig.getString(path + ".material", "GOLD_INGOT"),
                        type,
                        banksConfig.getDouble(path + ".interest_rate", 0.001),
                        banksConfig.getLong(path + ".interest_interval", 3600),
                        banksConfig.getLong(path + ".duration", 0),
                        banksConfig.getDouble(path + ".min_deposit", 1000),
                        banksConfig.getDouble(path + ".max_deposit", 10000000),
                        banksConfig.getDouble(path + ".early_withdrawal_penalty", 0.05),
                        banksConfig.getBoolean(path + ".compound_interest", false),
                        colorize(banksConfig.getStringList(path + ".lore")));

                bankProducts.put(key, product);
            }
        }
        plugin.getLogger().info("은행 상품 " + bankProducts.size() + "개 로드됨");
    }

    /**
     * 버프 블록 설정 로드
     */
    private void loadBuffBlocks() {
        buffBlocks.clear();
        if (buffBlocksConfig.isConfigurationSection("buffs")) {
            for (String key : buffBlocksConfig.getConfigurationSection("buffs").getKeys(false)) {
                String path = "buffs." + key;

                String worldName = buffBlocksConfig.getString(path + ".world");
                String blockType = buffBlocksConfig.getString(path + ".block");

                List<BuffBlockConfig.BuffEffect> effects = new ArrayList<>();
                if (buffBlocksConfig.isList(path + ".effects")) {
                    for (Map<?, ?> effectMap : buffBlocksConfig.getMapList(path + ".effects")) {
                        String type = (String) effectMap.get("type");
                        int duration = (int) effectMap.get("duration");
                        int amplifier = (int) effectMap.get("amplifier");
                        effects.add(new BuffBlockConfig.BuffEffect(type, duration, amplifier));
                    }
                }

                BuffBlockConfig config = new BuffBlockConfig(key, worldName, blockType, effects);
                buffBlocks.put(key, config);
            }
        }
        plugin.getLogger().info("버프 블록 " + buffBlocks.size() + "개 로드됨");
    }

    /**
     * 마이닝 드랍 설정 로드
     */
    private void loadMiningDrops() {
        miningDrops.clear();
        if (miningDropsConfig.isConfigurationSection("drops")) {
            for (String key : miningDropsConfig.getConfigurationSection("drops").getKeys(false)) {
                String path = "drops." + key;
                if (!miningDropsConfig.getBoolean(path + ".enabled", true)) {
                    continue;
                }

                org.bukkit.Material material = org.bukkit.Material.getMaterial(key);
                if (material == null) {
                    plugin.getLogger().warning("Invalid material in mining_drops.yml: " + key);
                    continue;
                }

                List<MiningReward> rewards = new ArrayList<>();
                if (miningDropsConfig.isList(path + ".rewards")) {
                    for (Map<?, ?> rewardMap : miningDropsConfig.getMapList(path + ".rewards")) {
                        String itemId = (String) rewardMap.get("itemId");
                        double chance = 0;
                        if (rewardMap.get("chance") instanceof Number) {
                            chance = ((Number) rewardMap.get("chance")).doubleValue();
                        }
                        int minAmount = (int) rewardMap.get("minAmount");
                        int maxAmount = (int) rewardMap.get("maxAmount");
                        rewards.add(new MiningReward(itemId, chance, minAmount, maxAmount));
                    }
                }

                MiningDropData data = new MiningDropData(material, true, rewards);
                data.setVanillaDrops(miningDropsConfig.getBoolean(path + ".vanilla_drops", true)); // [NEW]
                miningDrops.put(material, data);
            }
        }
        plugin.getLogger().info("마이닝 드랍 설정 " + miningDrops.size() + "개 로드됨");
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
     * 인챈트 Unsafe 모드 설정 (메모리)
     */
    public void setEnchantUnsafeMode(String enchantId, boolean unsafeMode) {
        modifiedEnchantUnsafeMode.put(enchantId, unsafeMode);
    }

    public boolean getEnchantUnsafeMode(String enchantId) {
        if (modifiedEnchantUnsafeMode.containsKey(enchantId)) {
            return modifiedEnchantUnsafeMode.get(enchantId);
        }
        EnchantConfig enchant = enchants.get(enchantId);
        return enchant != null && enchant.isUnsafeMode();
    }

    /**
     * 인챈트 충돌 무시 설정 (메모리)
     */
    public void setEnchantIgnoreConflicts(String enchantId, boolean ignoreConflicts) {
        modifiedEnchantIgnoreConflicts.put(enchantId, ignoreConflicts);
    }

    public boolean getEnchantIgnoreConflicts(String enchantId) {
        if (modifiedEnchantIgnoreConflicts.containsKey(enchantId)) {
            return modifiedEnchantIgnoreConflicts.get(enchantId);
        }
        EnchantConfig enchant = enchants.get(enchantId);
        return enchant != null && enchant.isIgnoreConflicts();
    }

    /**
     * 인챈트 타겟 그룹 설정 (메모리)
     */
    public void setEnchantTargetGroups(String enchantId, Set<String> groups) {
        modifiedEnchantTargetGroups.put(enchantId, groups);
    }

    public Set<String> getEnchantTargetGroups(String enchantId) {
        if (modifiedEnchantTargetGroups.containsKey(enchantId)) {
            return modifiedEnchantTargetGroups.get(enchantId);
        }
        EnchantConfig enchant = enchants.get(enchantId);
        return enchant != null ? new HashSet<>(enchant.getTargetGroups()) : new HashSet<>();
    }

    /**
     * 인챈트 타겟 화이트리스트 설정 (메모리)
     */
    public void setEnchantTargetWhitelist(String enchantId, Set<String> whitelist) {
        modifiedEnchantTargetWhitelist.put(enchantId, whitelist);
    }

    public Set<String> getEnchantTargetWhitelist(String enchantId) {
        if (modifiedEnchantTargetWhitelist.containsKey(enchantId)) {
            return modifiedEnchantTargetWhitelist.get(enchantId);
        }
        EnchantConfig enchant = enchants.get(enchantId);
        return enchant != null ? new HashSet<>(enchant.getTargetWhitelist()) : new HashSet<>();
    }

    /**
     * 주식 설정 파일에 저장
     */
    public void saveStockConfig(String stockId) {
        synchronized (saveLock) {
            // 디스크에서 최신 데이터를 다시 읽어와 수동 수정 사항 보존
            stocksConfig = YamlConfiguration.loadConfiguration(stocksFile);
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
                loadStocks(); // 메모리 리로드
                plugin.getLogger().info("주식 설정 저장됨: " + stockId);
            } catch (IOException e) {
                plugin.getLogger().severe("주식 설정 저장 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 인챈트 설정 파일에 저장
     */
    public void saveEnchantConfig(String enchantId) {
        synchronized (saveLock) {
            // 디스크에서 최신 데이터를 다시 읽어와 수동 수정 사항 보존
            enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);
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
            if (modifiedEnchantUnsafeMode.containsKey(enchantId)) {
                enchantsConfig.set(path + ".unsafe_mode", modifiedEnchantUnsafeMode.get(enchantId));
            }
            if (modifiedEnchantIgnoreConflicts.containsKey(enchantId)) {
                enchantsConfig.set(path + ".ignore_conflicts", modifiedEnchantIgnoreConflicts.get(enchantId));
            }
            if (modifiedEnchantTargetGroups.containsKey(enchantId)) {
                enchantsConfig.set(path + ".target_groups",
                        new ArrayList<>(modifiedEnchantTargetGroups.get(enchantId)));
            }
            if (modifiedEnchantTargetWhitelist.containsKey(enchantId)) {
                enchantsConfig.set(path + ".target_whitelist",
                        new ArrayList<>(modifiedEnchantTargetWhitelist.get(enchantId)));
            }

            try {
                enchantsConfig.save(enchantsFile);
                loadEnchants(); // 메모리 리로드
                plugin.getLogger().info("인챈트 설정 저장됨: " + enchantId);
            } catch (IOException e) {
                plugin.getLogger().severe("인챈트 설정 저장 실패: " + e.getMessage());
            }
        }
    }

    /**
     * 인챈트 설정 파일 저장하고 리로드 (새 인챈트 생성용)
     */
    public boolean saveAndReloadEnchants() {
        try {
            enchantsConfig.save(enchantsFile);
            loadEnchants();
            plugin.getLogger().info("인챈트 설정 저장 및 리로드 완료");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("인챈트 설정 저장 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 주식 설정 파일 저장하고 리로드 (새 주식 생성용)
     */
    public boolean saveAndReloadStocks() {
        try {
            stocksConfig.save(stocksFile);
            loadStocks();
            plugin.getLogger().info("주식 설정 저장 및 리로드 완료");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("주식 설정 저장 실패: " + e.getMessage());
            return false;
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
        return messages.getOrDefault("prefix", "&8[&6WildCore&8] &f");
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public String getMessage(String key, Map<String, String> replacements) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            String value = entry.getValue();
            message = message.replace("%" + entry.getKey() + "%", value);
            message = message.replace("{" + entry.getKey() + "}", value);
        }
        return colorize(message); // [FIX] 최종 결과에 색상 코드 적용 (플레이어 닉네임 등 변수에 &코드가 있을 경우를 위해)
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

    /**
     * 정렬된 주식 목록 반환 (이름순)
     */
    public List<StockConfig> getAllStocksSorted() {
        List<StockConfig> list = new ArrayList<>(stocks.values());
        list.sort(Comparator.comparing(StockConfig::getDisplayName));
        return list;
    }

    /**
     * 정렬된 인챈트 목록 반환 (이름순)
     */
    public List<EnchantConfig> getAllEnchantsSorted() {
        List<EnchantConfig> list = new ArrayList<>(enchants.values());
        list.sort(Comparator.comparing(EnchantConfig::getDisplayName));
        return list;
    }

    /**
     * 주식 표시 이름 설정 (메모리 + 파일)
     */
    public void setStockDisplayName(String stockId, String displayName) {
        String path = "stocks." + stockId + ".display_name";
        stocksConfig.set(path, displayName);
        try {
            stocksConfig.save(stocksFile);
            loadStocks(); // 리로드
            plugin.getLogger().info("주식 표시 이름 변경됨: " + stockId + " -> " + displayName);
        } catch (IOException e) {
            plugin.getLogger().severe("주식 표시 이름 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 주식 아이콘(Material) 설정 (메모리 + 파일)
     */
    public void setStockMaterial(String stockId, String material) {
        String path = "stocks." + stockId + ".material";
        stocksConfig.set(path, material);
        try {
            stocksConfig.save(stocksFile);
            loadStocks(); // 리로드
            plugin.getLogger().info("주식 아이콘 변경됨: " + stockId + " -> " + material);
        } catch (IOException e) {
            plugin.getLogger().severe("주식 아이콘 저장 실패: " + e.getMessage());
        }
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

    public FileConfiguration getShopsConfig() {
        return shopsConfig;
    }

    public FileConfiguration getMiningDropsConfig() {
        return miningDropsConfig;
    }

    public Map<org.bukkit.Material, MiningDropData> getMiningDrops() {
        return miningDrops;
    }

    public MiningDropData getMiningDropData(org.bukkit.Material material) {
        return miningDrops.get(material);
    }

    public void setMiningDropData(org.bukkit.Material material, MiningDropData data) {
        miningDrops.put(material, data);
    }

    public void removeMiningDropData(org.bukkit.Material material) {
        miningDrops.remove(material);
    }

    /**
     * 마이닝 드랍 설정 저장
     */
    public void saveMiningDropsConfig() {
        synchronized (saveLock) {
            // 마이닝 드랍은 전체 구조가 동적일 수 있으므로 덮어쓰기 위험이 있음
            // 하지만 현재 구조상 메모리 데이터를 기반으로 전체를 새로 쓰므로,
            // 최대한 락을 통해 멀티 스레드 접근만 막습니다.
            miningDropsConfig.set("drops", null);

            for (Map.Entry<org.bukkit.Material, MiningDropData> entry : miningDrops.entrySet()) {
                String path = "drops." + entry.getKey().name();
                MiningDropData data = entry.getValue();

                miningDropsConfig.set(path + ".enabled", data.isEnabled());
                miningDropsConfig.set(path + ".vanilla_drops", data.isVanillaDrops()); // [NEW]

                List<Map<String, Object>> rewardsList = new ArrayList<>();
                for (MiningReward reward : data.getRewards()) {
                    Map<String, Object> rewardMap = new HashMap<>();
                    rewardMap.put("itemId", reward.getItemId());
                    rewardMap.put("chance", reward.getChance());
                    rewardMap.put("minAmount", reward.getMinAmount());
                    rewardMap.put("maxAmount", reward.getMaxAmount());
                    rewardsList.add(rewardMap);
                }
                miningDropsConfig.set(path + ".rewards", rewardsList);
            }

            try {
                miningDropsConfig.save(miningDropsFile);
                plugin.getLogger().info("마이닝 드랍 설정이 저장되었습니다.");
            } catch (IOException e) {
                plugin.getLogger().severe("마이닝 드랍 설정 저장 실패: " + e.getMessage());
            }
        }
    }

    // =====================
    // 상점 관련 메서드
    // =====================

    /**
     * 상점 설정 로드
     */
    private void loadShops() {
        shops.clear();
        if (shopsConfig.isConfigurationSection("shops")) {
            for (String shopId : shopsConfig.getConfigurationSection("shops").getKeys(false)) {
                String path = "shops." + shopId;

                // 위치 정보 파싱
                String worldName = shopsConfig.getString(path + ".location.world", "world");
                double x = shopsConfig.getDouble(path + ".location.x", 0);
                double y = shopsConfig.getDouble(path + ".location.y", 64);
                double z = shopsConfig.getDouble(path + ".location.z", 0);
                float yaw = (float) shopsConfig.getDouble(path + ".location.yaw", 0);
                Location location = ShopConfig.createLocation(worldName, x, y, z, yaw);

                // 엔티티 UUID 파싱
                String uuidStr = shopsConfig.getString(path + ".entity_uuid", "");
                UUID entityUuid = null;
                if (uuidStr != null && !uuidStr.isEmpty()) {
                    try {
                        entityUuid = UUID.fromString(uuidStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                // 아이템 파싱
                Map<Integer, ShopItemConfig> items = new HashMap<>();
                if (shopsConfig.isConfigurationSection(path + ".items")) {
                    for (String slotStr : shopsConfig.getConfigurationSection(path + ".items").getKeys(false)) {
                        try {
                            int slot = Integer.parseInt(slotStr);
                            String itemPath = path + ".items." + slotStr;
                            ShopItemConfig item = new ShopItemConfig(
                                    slot,
                                    shopsConfig.getString(itemPath + ".type", "VANILLA"),
                                    shopsConfig.getString(itemPath + ".id", "STONE"),
                                    shopsConfig.getDouble(itemPath + ".buy_price", -1),
                                    shopsConfig.getDouble(itemPath + ".sell_price", -1),
                                    colorize(shopsConfig.getString(itemPath + ".display_name")), // [NEW] 표시 이름 로드
                                    colorize(shopsConfig.getStringList(itemPath + ".lore")) // [NEW] 설명 로드
                            );
                            items.put(slot, item);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }

                ShopConfig shop = new ShopConfig(
                        shopId,
                        colorize(shopsConfig.getString(path + ".display_name", "&f상점")),
                        shopsConfig.getString(path + ".npc_type", "VILLAGER"),
                        location,
                        entityUuid,
                        items);
                shops.put(shopId, shop);
            }
        }
        plugin.getLogger().info("상점 " + shops.size() + "개 로드됨");
    }

    /**
     * 특정 상점 설정 리로드 (디스크에서 새로 읽기)
     */
    public ShopConfig reloadShop(String shopId) {
        // 파일 다시 로드
        shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);

        String path = "shops." + shopId;
        if (!shopsConfig.contains(path)) {
            return null;
        }

        // 위치 정보 파싱
        String worldName = shopsConfig.getString(path + ".location.world", "world");
        double x = shopsConfig.getDouble(path + ".location.x", 0);
        double y = shopsConfig.getDouble(path + ".location.y", 64);
        double z = shopsConfig.getDouble(path + ".location.z", 0);
        float yaw = (float) shopsConfig.getDouble(path + ".location.yaw", 0);
        Location location = ShopConfig.createLocation(worldName, x, y, z, yaw);

        // 엔티티 UUID 파싱
        String uuidStr = shopsConfig.getString(path + ".entity_uuid", "");
        UUID entityUuid = null;
        if (uuidStr != null && !uuidStr.isEmpty()) {
            try {
                entityUuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ignored) {
            }
        }

        // 아이템 파싱
        Map<Integer, ShopItemConfig> items = new HashMap<>();
        if (shopsConfig.isConfigurationSection(path + ".items")) {
            for (String slotStr : shopsConfig.getConfigurationSection(path + ".items").getKeys(false)) {
                try {
                    int slot = Integer.parseInt(slotStr);
                    String itemPath = path + ".items." + slotStr;
                    ShopItemConfig item = new ShopItemConfig(
                            slot,
                            shopsConfig.getString(itemPath + ".type", "VANILLA"),
                            shopsConfig.getString(itemPath + ".id", "STONE"),
                            shopsConfig.getDouble(itemPath + ".buy_price", -1),
                            shopsConfig.getDouble(itemPath + ".sell_price", -1),
                            colorize(shopsConfig.getString(itemPath + ".display_name")),
                            colorize(shopsConfig.getStringList(itemPath + ".lore")));
                    items.put(slot, item);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        ShopConfig shop = new ShopConfig(
                shopId,
                colorize(shopsConfig.getString(path + ".display_name", "&f상점")),
                shopsConfig.getString(path + ".npc_type", "VILLAGER"),
                location,
                entityUuid,
                items);

        // 캐시 업데이트
        shops.put(shopId, shop);

        return shop;
    }

    /**
     * 상점 설정 저장
     */
    public boolean saveShop(ShopConfig shop) {
        synchronized (saveLock) {
            // 디스크에서 최신 설정을 다시 읽어옵니다.
            shopsConfig = YamlConfiguration.loadConfiguration(shopsFile);
            String path = "shops." + shop.getId();

            shopsConfig.set(path + ".display_name", shop.getDisplayName());
            shopsConfig.set(path + ".npc_type", shop.getNpcType());

            // 위치 저장
            if (shop.getLocation() != null) {
                Location loc = shop.getLocation();
                shopsConfig.set(path + ".location.world", loc.getWorld() != null ? loc.getWorld().getName() : "world");
                shopsConfig.set(path + ".location.x", loc.getX());
                shopsConfig.set(path + ".location.y", loc.getY());
                shopsConfig.set(path + ".location.z", loc.getZ());
                shopsConfig.set(path + ".location.yaw", loc.getYaw());
            }

            // UUID 저장
            shopsConfig.set(path + ".entity_uuid",
                    shop.getEntityUuid() != null ? shop.getEntityUuid().toString() : "");

            // 아이템 저장
            shopsConfig.set(path + ".items", null); // 기존 해당 상점 아이템만 삭제
            for (Map.Entry<Integer, ShopItemConfig> entry : shop.getItems().entrySet()) {
                String itemPath = path + ".items." + entry.getKey();
                ShopItemConfig item = entry.getValue();
                shopsConfig.set(itemPath + ".type", item.getType());
                shopsConfig.set(itemPath + ".id", item.getId());
                shopsConfig.set(itemPath + ".buy_price", item.getBuyPrice());
                shopsConfig.set(itemPath + ".sell_price", item.getSellPrice());
                shopsConfig.set(itemPath + ".display_name", item.getDisplayName()); // [NEW]
                shopsConfig.set(itemPath + ".lore", item.getLore()); // [NEW]
            }

            try {
                shopsConfig.save(shopsFile);
                shops.put(shop.getId(), shop); // 캐시 업데이트
                plugin.getLogger().info("상점 저장됨: " + shop.getId());
                return true;
            } catch (IOException e) {
                plugin.getLogger().severe("상점 저장 실패: " + e.getMessage());
                return false;
            }
        }
    }

    /**
     * 상점 삭제
     */
    public boolean deleteShop(String shopId) {
        shopsConfig.set("shops." + shopId, null);
        try {
            shopsConfig.save(shopsFile);
            shops.remove(shopId);
            plugin.getLogger().info("상점 삭제됨: " + shopId);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("상점 삭제 실패: " + e.getMessage());
            return false;
        }
    }

    // =====================
    // 워프 관련 메서드
    // =====================

    /**
     * 워프 위치 가져오기
     */
    public Location getWarpLocation(String warpName) {
        if (!locationsConfig.contains("warps." + warpName)) {
            return null;
        }

        String path = "warps." + warpName;
        String worldName = locationsConfig.getString(path + ".world");
        if (worldName == null)
            return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null)
            return null;

        double x = locationsConfig.getDouble(path + ".x");
        double y = locationsConfig.getDouble(path + ".y");
        double z = locationsConfig.getDouble(path + ".z");
        float yaw = (float) locationsConfig.getDouble(path + ".yaw");
        float pitch = (float) locationsConfig.getDouble(path + ".pitch");

        return new Location(world, x, y, z, yaw, pitch);
    }

    /**
     * 워프 위치 설정
     */
    public void setWarpLocation(String warpName, Location location) {
        String path = "warps." + warpName;
        locationsConfig.set(path + ".world", location.getWorld().getName());
        locationsConfig.set(path + ".x", location.getX());
        locationsConfig.set(path + ".y", location.getY());
        locationsConfig.set(path + ".z", location.getZ());
        locationsConfig.set(path + ".yaw", location.getYaw());
        locationsConfig.set(path + ".pitch", location.getPitch());

        saveLocationsConfig();
    }

    /**
     * 위치 설정 파일 저장
     */
    private void saveLocationsConfig() {
        try {
            locationsConfig.save(locationsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("위치 설정 저장 실패: " + e.getMessage());
        }
    }

    /**
     * 상점 설정 저장 및 리로드
     */
    public boolean saveAndReloadShops() {
        try {
            shopsConfig.save(shopsFile);
            loadShops();
            plugin.getLogger().info("상점 설정 저장 및 리로드 완료");
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("상점 설정 저장 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * 모든 상점 가져오기
     */
    public Map<String, ShopConfig> getShops() {
        return shops;
    }

    /**
     * 특정 상점 가져오기
     */
    public ShopConfig getShop(String id) {
        return shops.get(id);
    }

    /**
     * 정렬된 상점 목록 반환 (이름순)
     */
    public List<ShopConfig> getAllShopsSorted() {
        List<ShopConfig> list = new ArrayList<>(shops.values());
        list.sort(Comparator.comparing(ShopConfig::getDisplayName));
        return list;
    }

    /**
     * 상점 GUI 제목
     */
    public String getShopGuiTitle() {
        return colorize(shopsConfig.getString("gui.title", "&8[ &a상점 &8]"));
    }

    /**
     * 상점 NPC 무적 여부
     */
    public boolean isShopNpcInvulnerable() {
        return shopsConfig.getBoolean("settings.npc_invulnerable", true);
    }

    /**
     * 상점 NPC 무음 여부
     */
    public boolean isShopNpcSilent() {
        return shopsConfig.getBoolean("settings.npc_silent", true);
    }

    /**
     * 상점 NPC AI 비활성화 여부
     */
    public boolean isShopNpcNoAi() {
        return shopsConfig.getBoolean("settings.npc_no_ai", true);
    }

    // =====================
    // NPC 위치 관리 (locations.yml) - Deprecated/Removed
    // 이제 NPC 위치는 npcs.yml에서 NpcManager에 의해 관리됩니다.
    // =====================

    /**
     * NPC 위치 추가 (더 이상 사용되지 않음)
     */
    @Deprecated
    public void addNpcLocation(NpcType type, Location loc) {
        // Legacy support removed
    }

    /**
     * NPC 위치 목록 가져오기 (더 이상 사용되지 않음)
     */
    @Deprecated
    public List<Location> getNpcLocations(NpcType type) {
        return new ArrayList<>();
    }

    /**
     * 특정 타입의 모든 NPC 위치 삭제 (더 이상 사용되지 않음)
     */
    @Deprecated
    public void clearNpcLocations(NpcType type) {
        // Legacy support removed
    }

    /**
     * 특정 타입의 효과음을 가져옵니다.
     */
    public String getSound(String type) {
        return config.getString("settings.sounds." + type, "");
    }

    /**
     * 플레이어에게 효과음을 재생합니다.
     */
    public void playSound(org.bukkit.entity.Player player, String soundType) {
        String soundName = getSound(soundType);
        if (soundName == null || soundName.isEmpty())
            return;

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("잘못된 사운드 설정: " + soundName);
        }
    }

    public boolean isActionBarEnabled() {
        return config.getBoolean("settings.actionbar.enabled", true);
    }

    public String getActionBarFormat() {
        return colorize(config.getString("settings.actionbar.format", "&6보유 금액: &f%money%원"));
    }

    public long getActionBarUpdateInterval() {
        return config.getLong("settings.actionbar.update_interval", 20L);
    }

    // =====================
    // 은행 관련 메서드
    // =====================

    /**
     * 모든 은행 상품 가져오기
     */
    public Map<String, BankProductConfig> getBankProducts() {
        return bankProducts;
    }

    /**
     * 특정 은행 상품 가져오기
     */
    public BankProductConfig getBankProduct(String id) {
        return bankProducts.get(id);
    }

    /**
     * 정렬된 은행 상품 목록 반환 (이름순)
     */
    public List<BankProductConfig> getAllBankProductsSorted() {
        List<BankProductConfig> list = new ArrayList<>(bankProducts.values());
        list.sort(Comparator.comparing(BankProductConfig::getDisplayName));
        return list;
    }

    /**
     * 자유 예금 상품만 반환
     */
    public List<BankProductConfig> getSavingsProducts() {
        return bankProducts.values().stream()
                .filter(BankProductConfig::isSavings)
                .sorted(Comparator.comparing(BankProductConfig::getDisplayName))
                .toList();
    }

    /**
     * 정기 적금 상품만 반환
     */
    public List<BankProductConfig> getTermDepositProducts() {
        return bankProducts.values().stream()
                .filter(BankProductConfig::isTermDeposit)
                .sorted(Comparator.comparing(BankProductConfig::getDisplayName))
                .toList();
    }

    /**
     * 은행 GUI 제목
     */
    public String getBankGuiTitle() {
        return colorize(banksConfig.getString("gui.title", "&8[ &6은행 &8]"));
    }

    /**
     * banks.yml 설정 가져오기
     */
    public FileConfiguration getBanksConfig() {
        return banksConfig;
    }

    /**
     * 버프 블록 설정 가져오기
     */
    /**
     * 주식 시스템 활성화 여부
     */
    public boolean isStockSystemEnabled() {
        return stocksConfig.getBoolean("enabled", true);
    }

    /**
     * 은행 시스템 활성화 여부
     */
    public boolean isBankSystemEnabled() {
        return banksConfig.getBoolean("enabled", true);
    }

    public Map<String, BuffBlockConfig> getBuffBlocks() {
        return buffBlocks;
    }

    // =====================
    // 클레임 시스템 메서드들
    // =====================

    /**
     * 클레임 시스템 활성화 여부
     */
    public boolean isClaimSystemEnabled() {
        return config.getBoolean("claim-system.enabled", true);
    }

    /**
     * 클레임 미리보기 지속 시간 (초)
     */
    public int getClaimPreviewDuration() {
        return config.getInt("claim-system.preview-duration", 5);
    }

    /**
     * 클레임 블록 자동 지급 여부
     */
    public boolean isClaimAutoGrantBlocks() {
        return config.getBoolean("claim-system.auto-grant-blocks", true);
    }

    /**
     * 울타리 자동 설치 여부
     */
    public boolean isClaimAutoFence() {
        return config.getBoolean("claim-system.auto-fence", true);
    }

    /**
     * 바닥 블록 자동 설치 여부
     */
    public boolean isClaimAutoFloor() {
        return config.getBoolean("claim-system.auto-floor", true);
    }

    /**
     * 울타리 설치 시 블록 처리 모드 (SKIP/REPLACE)
     */
    public String getClaimFenceBlockMode() {
        return config.getString("claim-system.fence-block-mode", "SKIP");
    }

    /**
     * 울타리 설치 최대 높이 차이
     */
    public int getClaimMaxFenceHeightDiff() {
        return config.getInt("claim-system.max-fence-height-diff", 5);
    }

    /**
     * 허용된 월드 목록
     */
    public List<String> getClaimAllowedWorlds() {
        return config.getStringList("claim-system.allowed-worlds");
    }

    /**
     * 클레임 효과음 가져오기
     */
    public String getClaimSound(String type) {
        return config.getString("claim-system.sounds." + type, "");
    }

    /**
     * 클레임 메시지 가져오기
     */
    public String getClaimMessage(String key) {
        return colorize(config.getString("claim-system.messages." + key, ""));
    }

    /**
     * 아이템별 클레임 설정 가져오기
     */
    public ClaimItemConfig getClaimItemConfig(String itemId) {
        if (claimItemConfigs.containsKey(itemId)) {
            return claimItemConfigs.get(itemId);
        }

        // items.yml에서 해당 아이템의 claim 섹션 로드
        if (itemsConfig.isConfigurationSection("items." + itemId + ".claim")) {
            String path = "items." + itemId + ".claim";
            int radius = itemsConfig.getInt(path + ".radius", 5);

            String fenceStr = itemsConfig.getString(path + ".fence_material", "OAK_FENCE");
            Material fence = Material.getMaterial(fenceStr);
            if (fence == null)
                fence = Material.OAK_FENCE;

            String gateStr = itemsConfig.getString(path + ".gate_material", "OAK_FENCE_GATE");
            Material gate = Material.getMaterial(gateStr);
            if (gate == null)
                gate = Material.OAK_FENCE_GATE;

            String floorStr = itemsConfig.getString(path + ".floor_material", "LIME_CONCRETE");
            Material floor = Material.getMaterial(floorStr);
            if (floor == null)
                floor = Material.LIME_CONCRETE;

            ClaimItemConfig claimConfig = new ClaimItemConfig(itemId, radius, fence, gate, floor);
            claimItemConfigs.put(itemId, claimConfig);
            return claimConfig;
        }

        return null;
    }
}
