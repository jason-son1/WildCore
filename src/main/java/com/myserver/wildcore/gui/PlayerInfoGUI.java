package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import com.myserver.wildcore.config.PlayerBankAccount;
import com.myserver.wildcore.config.PlayerStockData;
import com.myserver.wildcore.config.StockConfig;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 플레이어 정보 GUI (개편)
 * Shift + F 키를 눌렀을 때 표시되는 내 정보 창
 * View-Only 모드: 상세 정보만 표시, 클릭 시 안내 메시지
 */
public class PlayerInfoGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Inventory inventory;

    // 슬롯 상수 (54슬롯 레이아웃)
    private static final int SLOT_HEAD = 4; // 플레이어 헤드 (Row 1)
    private static final int SLOT_MONEY = 10; // 자산 정보 (Row 2)
    private static final int SLOT_LOCATION = 16; // 위치 정보 (Row 2)

    // 주식 섹션 (Row 3: 슬롯 18-26)
    private static final int SLOT_STOCK_SUMMARY = 18; // 주식 요약
    private static final int[] SLOTS_STOCK_DETAIL = { 20, 21, 22, 23, 24 }; // 개별 종목 (최대 5개)

    // 은행 섹션 (Row 4: 슬롯 27-35)
    private static final int SLOT_BANK_SUMMARY = 27; // 은행 요약
    private static final int[] SLOTS_BANK_DETAIL = { 29, 30, 31, 32, 33 }; // 개별 계좌 (최대 5개)

    private static final int SLOT_SERVER = 49; // 서버 정보 (Row 6)

    // 클릭 가능한 슬롯 목록 (View-Only 안내용)
    private final Set<Integer> stockSlots = new HashSet<>();
    private final Set<Integer> bankSlots = new HashSet<>();

    public PlayerInfoGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, ItemUtil.parse("§8[ §a나의 정보 §8]"));
        setupInventory();
    }

    private void setupInventory() {
        // 배경 채우기
        ItemStack bg = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, bg);
        }

        // 상단 장식 (Row 1)
        ItemStack topBorder = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorder);
        }

        // 하단 장식 (Row 6)
        ItemStack bottomBorder = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, bottomBorder);
        }

        // 섹션 구분선
        ItemStack divider = ItemUtil.createItem(Material.BLACK_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        inventory.setItem(19, divider);
        inventory.setItem(25, divider);
        inventory.setItem(28, divider);
        inventory.setItem(34, divider);

        // 1. 플레이어 헤드
        setupPlayerHead();

        // 2. 자산 정보
        setupMoneyInfo();

        // 3. 위치 정보
        setupLocationInfo();

        // 4. 주식 정보 (상세)
        setupStockSection();

        // 5. 은행 정보 (상세)
        setupBankSection();

        // 6. 서버 정보
        setupServerInfo();
    }

    private void setupPlayerHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(player);
            headMeta.displayName(ItemUtil.parse("§a§l" + player.getName()));

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7레벨: §a" + player.getLevel());
            lore.add("§7경험치: §a" + (int) (player.getExp() * 100) + "%");
            lore.add("");
            lore.add("§7체력: §c" + (int) player.getHealth() + " / "
                    + (int) player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            lore.add("§7배고픔: §6" + player.getFoodLevel() + " / 20");

            headMeta.lore(ItemUtil.parseList(lore));
            head.setItemMeta(headMeta);
        }
        inventory.setItem(SLOT_HEAD, head);
    }

    private void setupMoneyInfo() {
        double balance = plugin.getEconomy().getBalance(player);
        double totalBankBalance = plugin.getBankManager().getTotalBalance(player.getUniqueId());
        double totalBankInterest = plugin.getBankManager().getTotalPendingInterest(player.getUniqueId());

        // 주식 평가액 계산
        double stockValue = calculateTotalStockValue();

        double totalAssets = balance + totalBankBalance + totalBankInterest + stockValue;

        List<String> moneyLore = new ArrayList<>();
        moneyLore.add("");
        moneyLore.add("§7보유 현금: §e" + String.format("%,.1f", balance) + "원");
        moneyLore.add("§7은행 예치금: §6" + String.format("%,.1f", totalBankBalance + totalBankInterest) + "원");
        moneyLore.add("§7주식 평가액: §d" + String.format("%,.1f", stockValue) + "원");
        moneyLore.add("");
        moneyLore.add("§7─────────────────");
        moneyLore.add("§f총 자산: §e§l" + String.format("%,.1f", totalAssets) + "원");

        inventory.setItem(SLOT_MONEY, ItemUtil.createItem(Material.EMERALD, "§e§l자산 정보",
                moneyLore, 1, null, 0, false, plugin));
    }

    private void setupLocationInfo() {
        Location loc = player.getLocation();
        List<String> locLore = new ArrayList<>();
        locLore.add("");
        locLore.add("§7월드: §f" + loc.getWorld().getName());
        locLore.add("§7좌표: §fX:" + loc.getBlockX() + ", Y:" + loc.getBlockY() + ", Z:" + loc.getBlockZ());
        locLore.add("");
        locLore.add("§7바이옴: §f" + loc.getBlock().getBiome().name());

        inventory.setItem(SLOT_LOCATION, ItemUtil.createItem(Material.COMPASS, "§b§l위치 정보",
                locLore, 1, null, 0, false, plugin));
    }

    private void setupStockSection() {
        Map<String, PlayerStockData> holdings = plugin.getStockManager().getPlayerStocks(player.getUniqueId());

        // 주식 요약
        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");

        double totalValue = 0;
        double totalInvested = 0;
        int stockCount = 0;

        if (holdings.isEmpty()) {
            summaryLore.add("§7보유 주식이 없습니다.");
        } else {
            for (Map.Entry<String, PlayerStockData> entry : holdings.entrySet()) {
                String stockId = entry.getKey();
                PlayerStockData data = entry.getValue();
                double price = plugin.getStockManager().getCurrentPrice(stockId);
                totalValue += price * data.getAmount();
                totalInvested += data.getTotalInvested();
                stockCount += data.getAmount();
            }

            double profitLoss = totalValue - totalInvested;
            double profitPercent = totalInvested > 0 ? (profitLoss / totalInvested * 100) : 0;
            String profitColor = profitLoss >= 0 ? "§a+" : "§c";

            summaryLore.add("§7보유 종목: §f" + holdings.size() + "개");
            summaryLore.add("§7총 보유량: §f" + stockCount + "주");
            summaryLore.add("");
            summaryLore.add("§7총 투자금: §f" + String.format("%,.0f", totalInvested) + "원");
            summaryLore.add("§7총 평가액: §a" + String.format("%,.0f", totalValue) + "원");
            summaryLore.add("§7수익률: " + profitColor + String.format("%.1f%%", profitPercent));
            summaryLore.add("§7손익: " + profitColor + String.format("%,.0f", profitLoss) + "원");
        }

        summaryLore.add("");
        summaryLore.add("§7─────────────────");
        summaryLore.add("§7⏱ 다음 변동까지: §e" + plugin.getStockManager().getFormattedTimeUntilNextUpdate());
        summaryLore.add("");
        summaryLore.add("§8클릭: 주식 거래소(/stock) 이용");

        inventory.setItem(SLOT_STOCK_SUMMARY, ItemUtil.createItem(Material.DIAMOND, "§d§l주식 포트폴리오",
                summaryLore, 1, null, 0, true, plugin));
        stockSlots.add(SLOT_STOCK_SUMMARY);

        // 개별 종목 표시 (평가액 순 정렬, 상위 5개)
        List<Map.Entry<String, PlayerStockData>> sortedHoldings = new ArrayList<>(holdings.entrySet());
        sortedHoldings.sort((a, b) -> {
            double valueA = plugin.getStockManager().getCurrentPrice(a.getKey()) * a.getValue().getAmount();
            double valueB = plugin.getStockManager().getCurrentPrice(b.getKey()) * b.getValue().getAmount();
            return Double.compare(valueB, valueA);
        });

        for (int i = 0; i < SLOTS_STOCK_DETAIL.length && i < sortedHoldings.size(); i++) {
            Map.Entry<String, PlayerStockData> entry = sortedHoldings.get(i);
            String stockId = entry.getKey();
            PlayerStockData data = entry.getValue();

            StockConfig stockConfig = plugin.getConfigManager().getStock(stockId);
            String displayName = stockConfig != null ? stockConfig.getDisplayName() : stockId;

            double currentPrice = plugin.getStockManager().getCurrentPrice(stockId);
            double avgPrice = data.getAveragePrice();
            double evalValue = currentPrice * data.getAmount();
            double profitLoss = evalValue - data.getTotalInvested();
            double profitPercent = data.getTotalInvested() > 0 ? (profitLoss / data.getTotalInvested() * 100) : 0;

            String trendColor = currentPrice >= avgPrice ? "§a" : "§c";
            String profitColor = profitLoss >= 0 ? "§a+" : "§c";

            List<String> stockLore = new ArrayList<>();
            stockLore.add("");
            stockLore.add("§7보유량: §f" + data.getAmount() + "주");
            stockLore.add("§7평단가: §f" + String.format("%,.0f", avgPrice) + "원");
            stockLore.add("§7현재가: " + trendColor + String.format("%,.0f", currentPrice) + "원");
            stockLore.add("");
            stockLore.add("§7─────────────────");
            stockLore.add("§7평가액: §a" + String.format("%,.0f", evalValue) + "원");
            stockLore.add("§7손익: " + profitColor + String.format("%,.0f", profitLoss) + "원");
            stockLore.add("§7수익률: " + profitColor + String.format("%.1f%%", profitPercent));

            String changeStr = plugin.getStockManager().getFormattedChange(stockId);
            String itemName = trendColor + displayName + " " + changeStr;

            inventory.setItem(SLOTS_STOCK_DETAIL[i], ItemUtil.createItem(Material.PAPER, itemName,
                    stockLore, 1, null, 0, false, plugin));
            stockSlots.add(SLOTS_STOCK_DETAIL[i]);
        }
    }

    private void setupBankSection() {
        List<PlayerBankAccount> accounts = plugin.getBankManager().getPlayerAccounts(player.getUniqueId());

        // 은행 요약
        List<String> summaryLore = new ArrayList<>();
        summaryLore.add("");

        double totalBalance = 0;
        double totalPendingInterest = 0;
        int savingsCount = 0;
        int termCount = 0;

        if (accounts.isEmpty()) {
            summaryLore.add("§7개설된 계좌가 없습니다.");
            summaryLore.add("");
            summaryLore.add("§7은행을 이용하여 계좌를 개설하고");
            summaryLore.add("§7이자 수익을 받아보세요!");
        } else {
            for (PlayerBankAccount account : accounts) {
                totalBalance += account.getPrincipal();
                BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
                if (product != null) {
                    totalPendingInterest += plugin.getBankManager().calculateAccruedInterest(account, product);
                    if (product.isSavings()) {
                        savingsCount++;
                    } else {
                        termCount++;
                    }
                }
            }

            summaryLore.add("§7총 계좌 수: §f" + accounts.size() + "개");
            if (savingsCount > 0)
                summaryLore.add("§7  §a자유예금: " + savingsCount + "개");
            if (termCount > 0)
                summaryLore.add("§7  §b정기적금: " + termCount + "개");
            summaryLore.add("");
            summaryLore.add("§7예치금 합계: §6" + String.format("%,.0f", totalBalance) + "원");
            summaryLore.add("§7예상 이자: §a+" + String.format("%,.0f", totalPendingInterest) + "원");
            summaryLore.add("");
            summaryLore.add("§7─────────────────");
            summaryLore.add("§f총 은행자산: §6§l" + String.format("%,.0f", totalBalance + totalPendingInterest) + "원");
        }

        summaryLore.add("");
        summaryLore.add("§8클릭: 은행원(/bank) 이용");

        inventory.setItem(SLOT_BANK_SUMMARY, ItemUtil.createItem(Material.GOLD_INGOT, "§6§l은행 계좌",
                summaryLore, 1, null, 0, true, plugin));
        bankSlots.add(SLOT_BANK_SUMMARY);

        // 개별 계좌 표시 (최대 5개)
        for (int i = 0; i < SLOTS_BANK_DETAIL.length && i < accounts.size(); i++) {
            PlayerBankAccount account = accounts.get(i);
            BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());

            if (product == null)
                continue;

            String productName = product.getDisplayName();
            List<String> accountLore = new ArrayList<>();
            accountLore.add("");

            if (product.isSavings()) {
                // 자유 예금
                double pendingInterest = plugin.getBankManager().calculateAccruedInterest(account, product);
                long timeUntilInterest = plugin.getBankManager().getTimeUntilNextInterest(account, product);

                accountLore.add("§7잔액: §e" + String.format("%,.0f", account.getPrincipal()) + "원");
                accountLore.add("§7누적 이자: §a+" + String.format("%,.0f", account.getAccumulatedInterest()) + "원");
                accountLore.add("");
                accountLore.add("§7─────────────────");
                accountLore.add("§7⏱ 다음 이자까지: §e" + plugin.getBankManager().formatDuration(timeUntilInterest));
                accountLore.add("§7예상 이자: §a+" + String.format("%,.0f", pendingInterest) + "원");

                inventory.setItem(SLOTS_BANK_DETAIL[i], ItemUtil.createItem(Material.SUNFLOWER,
                        "§a" + productName + " §7(#" + account.getAccountId().substring(0, 4) + ")",
                        accountLore, 1, null, 0, false, plugin));
            } else {
                // 정기 적금
                double maturityInterest = account.getPrincipal() * product.getInterestRate();
                boolean isMatured = account.checkAndUpdateMaturity();

                accountLore.add("§7원금: §e" + String.format("%,.0f", account.getPrincipal()) + "원");
                accountLore.add("");
                accountLore.add("§7─────────────────");

                if (isMatured) {
                    accountLore.add("§a§l만기 도달! 수령 가능");
                    accountLore.add("§7이자: §a+" + String.format("%,.0f", maturityInterest) + "원");
                } else {
                    accountLore.add("§7⏱ 만기까지: §e" + account.getFormattedTimeRemaining());
                    accountLore.add("§7만기 시 이자: §a+" + String.format("%,.0f", maturityInterest) + "원");
                }

                Material iconMaterial = isMatured ? Material.NETHER_STAR : Material.CLOCK;
                inventory.setItem(SLOTS_BANK_DETAIL[i], ItemUtil.createItem(iconMaterial,
                        "§b" + productName + " §7(#" + account.getAccountId().substring(0, 4) + ")",
                        accountLore, 1, null, 0, isMatured, plugin));
            }
            bankSlots.add(SLOTS_BANK_DETAIL[i]);
        }
    }

    private void setupServerInfo() {
        List<String> timeLore = new ArrayList<>();
        timeLore.add("");
        timeLore.add("§7현재 시간: §f" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        timeLore.add("§7접속자 수: §f" + Bukkit.getOnlinePlayers().size() + "명");
        timeLore.add("");
        timeLore.add("§7TPS: §a" + String.format("%.1f", Bukkit.getTPS()[0]));

        inventory.setItem(SLOT_SERVER, ItemUtil.createItem(Material.CLOCK, "§f§l서버 정보",
                timeLore, 1, null, 0, false, plugin));
    }

    private double calculateTotalStockValue() {
        Map<String, PlayerStockData> holdings = plugin.getStockManager().getPlayerStocks(player.getUniqueId());
        double total = 0;
        for (Map.Entry<String, PlayerStockData> entry : holdings.entrySet()) {
            double price = plugin.getStockManager().getCurrentPrice(entry.getKey());
            total += price * entry.getValue().getAmount();
        }
        return total;
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // 슬롯 확인 메서드
    public boolean isStockSlot(int slot) {
        return stockSlots.contains(slot);
    }

    public boolean isBankSlot(int slot) {
        return bankSlots.contains(slot);
    }
}
