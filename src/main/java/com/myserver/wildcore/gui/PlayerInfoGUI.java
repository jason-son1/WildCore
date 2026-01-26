package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import com.myserver.wildcore.config.PlayerBankAccount;
import com.myserver.wildcore.config.PlayerStockData;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 플레이어 정보 GUI
 * Shift + F 키를 눌렀을 때 표시되는 내 정보 창
 */
public class PlayerInfoGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Inventory inventory;

    // 슬롯 상수
    private static final int SLOT_HEAD = 4; // 플레이어 헤드
    private static final int SLOT_MONEY = 19; // 자산 정보
    private static final int SLOT_STOCK = 21; // 주식 정보
    private static final int SLOT_BANK = 23; // 은행 계좌 정보
    private static final int SLOT_LOCATION = 25; // 위치 정보
    private static final int SLOT_SERVER = 40; // 서버 정보

    public PlayerInfoGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 45, ItemUtil.parse("§8[ §a나의 정보 §8]"));
        setupInventory();
    }

    private void setupInventory() {
        // 배경 채우기
        ItemStack bg = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 45; i++) {
            inventory.setItem(i, bg);
        }

        // 상단 장식
        ItemStack topBorder = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorder);
        }

        // 하단 장식
        ItemStack bottomBorder = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, bottomBorder);
        }

        // 1. 플레이어 헤드 (슬롯 4)
        setupPlayerHead();

        // 2. 자산 정보 (슬롯 19)
        setupMoneyInfo();

        // 3. 주식 정보 (슬롯 21)
        setupStockInfo();

        // 4. 은행 계좌 정보 (슬롯 23)
        setupBankInfo();

        // 5. 위치 정보 (슬롯 25)
        setupLocationInfo();

        // 6. 서버 정보 (슬롯 40)
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

        // 은행 총 예치금 계산
        double totalBankBalance = plugin.getBankManager().getTotalBalance(player.getUniqueId());
        double totalBankInterest = plugin.getBankManager().getTotalPendingInterest(player.getUniqueId());

        double totalAssets = balance + totalBankBalance + totalBankInterest;

        List<String> moneyLore = new ArrayList<>();
        moneyLore.add("");
        moneyLore.add("§7보유 현금: §e" + String.format("%,.0f", balance) + "원");
        moneyLore.add("§7은행 예치금: §6" + String.format("%,.0f", totalBankBalance + totalBankInterest) + "원");
        moneyLore.add("");
        moneyLore.add("§7─────────────────");
        moneyLore.add("§f총 자산: §e§l" + String.format("%,.0f", totalAssets) + "원");

        inventory.setItem(SLOT_MONEY, ItemUtil.createItem(Material.EMERALD, "§e§l자산 정보",
                moneyLore, 1, null, 0, false, plugin));
    }

    private void setupStockInfo() {
        Map<String, PlayerStockData> holdings = plugin.getStockManager().getPlayerStocks(player.getUniqueId());

        List<String> stockLore = new ArrayList<>();
        stockLore.add("");

        if (holdings.isEmpty()) {
            stockLore.add("§7보유 주식이 없습니다.");
        } else {
            double totalValue = 0;
            int stockCount = 0;
            for (Map.Entry<String, PlayerStockData> entry : holdings.entrySet()) {
                String stockId = entry.getKey();
                int amount = entry.getValue().getAmount();
                double price = plugin.getStockManager().getCurrentPrice(stockId);
                totalValue += price * amount;
                stockCount += amount;
            }
            stockLore.add("§7보유 종목: §f" + holdings.size() + "개");
            stockLore.add("§7총 보유량: §f" + stockCount + "주");
            stockLore.add("§7총 평가액: §a" + String.format("%,.0f", totalValue) + "원");
        }

        stockLore.add("");
        // stockLore.add("§e클릭하여 상세 확인");

        inventory.setItem(SLOT_STOCK, ItemUtil.createItem(Material.DIAMOND, "§d§l주식 정보",
                stockLore, 1, null, 0, false, plugin));
    }

    private void setupBankInfo() {
        List<PlayerBankAccount> accounts = plugin.getBankManager().getPlayerAccounts(player.getUniqueId());

        List<String> bankLore = new ArrayList<>();
        bankLore.add("");

        if (accounts.isEmpty()) {
            bankLore.add("§7개설된 계좌가 없습니다.");
            bankLore.add("");
            bankLore.add("§7은행을 이용하여 계좌를 개설하고");
            bankLore.add("§7이자 수익을 받아보세요!");
        } else {
            double totalBalance = 0;
            double totalInterest = 0;
            int savingsCount = 0;
            int termDepositCount = 0;

            for (PlayerBankAccount account : accounts) {
                totalBalance += account.getPrincipal();

                BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
                if (product != null) {
                    totalInterest += plugin.getBankManager().calculateAccruedInterest(account, product);
                    if (product.isSavings()) {
                        savingsCount++;
                    } else {
                        termDepositCount++;
                    }
                }
            }

            bankLore.add("§7총 계좌 수: §f" + accounts.size() + "개");
            if (savingsCount > 0) {
                bankLore.add("§7  §a자유예금: " + savingsCount + "개");
            }
            if (termDepositCount > 0) {
                bankLore.add("§7  §b정기적금: " + termDepositCount + "개");
            }
            bankLore.add("");
            bankLore.add("§7예치금 합계: §6" + String.format("%,.0f", totalBalance) + "원");
            bankLore.add("§7누적 이자: §a+" + String.format("%,.0f", totalInterest) + "원");
            bankLore.add("");
            bankLore.add("§7─────────────────");
            bankLore.add("§f총 은행자산: §6§l" + String.format("%,.0f", totalBalance + totalInterest) + "원");
        }

        bankLore.add("");
        // bankLore.add("§e클릭하여 은행 열기");

        inventory.setItem(SLOT_BANK, ItemUtil.createItem(Material.GOLD_INGOT, "§6§l은행 계좌",
                bankLore, 1, null, 0, false, plugin));
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

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    // 슬롯 확인 메서드
    public boolean isStockSlot(int slot) {
        return slot == SLOT_STOCK;
    }

    public boolean isBankSlot(int slot) {
        return slot == SLOT_BANK;
    }
}
