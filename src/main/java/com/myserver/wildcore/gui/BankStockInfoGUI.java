package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import com.myserver.wildcore.config.PlayerBankAccount;
import com.myserver.wildcore.config.PlayerStockData;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 은행 및 주식 정보 전용 GUI
 * 플레이어 정보창에서 해당 아이템 클릭 시 표시됨
 */
public class BankStockInfoGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final Inventory inventory;

    private static final int SLOT_STOCK = 12; // 주식 정보 위치 (왼쪽)
    private static final int SLOT_BANK = 14; // 은행 정보 위치 (오른쪽)

    public BankStockInfoGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 27, ItemUtil.parse("§8[ §d금융 자산 정보 §8]"));
        setupInventory();
    }

    private void setupInventory() {
        // 배경 채우기
        ItemStack bg = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false, null);
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, bg);
        }

        // 주식 정보 (슬롯 12)
        setupStockInfo();

        // 은행 정보 (슬롯 14)
        setupBankInfo();

        // 뒤로가기 버튼 (선택사항, 일단은 닫기 버튼으로 사용하거나 생략)
        // inventory.setItem(22, ItemUtil.createItem(Material.BARRIER, "§c닫기", null, 1,
        // null, 0, false, null));
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
            stockLore.add("§7총 평가액: §a" + String.format("%,.1f", totalValue) + "원");

            stockLore.add("");
            stockLore.add("§e----- 보유 상세 -----");
            for (Map.Entry<String, PlayerStockData> entry : holdings.entrySet()) {
                String stockId = entry.getKey();
                int amount = entry.getValue().getAmount();
                double price = plugin.getStockManager().getCurrentPrice(stockId);
                // 종목 이름 가져오기 (Config 등에서 가져와야 하지만 여기선 ID로 대체하거나 필요시 추가 조회)
                // String stockName = plugin.getConfigManager().getStock(stockId).getName(); //
                // 가정
                stockLore.add("§7- " + stockId + ": §f" + amount + "주 §7(§a" + String.format("%,.1f", price * amount)
                        + "원§7)");
            }
        }

        inventory.setItem(SLOT_STOCK, ItemUtil.createItem(Material.DIAMOND, "§d§l주식 정보",
                stockLore, 1, null, 0, false, plugin));
    }

    private void setupBankInfo() {
        List<PlayerBankAccount> accounts = plugin.getBankManager().getPlayerAccounts(player.getUniqueId());

        List<String> bankLore = new ArrayList<>();
        bankLore.add("");

        if (accounts.isEmpty()) {
            bankLore.add("§7개설된 계좌가 없습니다.");
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
            bankLore.add("§7예치금 합계: §6" + String.format("%,.1f", totalBalance) + "원");
            bankLore.add("§7누적 이자: §a+" + String.format("%,.1f", totalInterest) + "원");
            bankLore.add("");
            bankLore.add("§7─────────────────");
            bankLore.add("§f총 은행자산: §6§l" + String.format("%,.1f", totalBalance + totalInterest) + "원");

            bankLore.add("");
            bankLore.add("§e----- 계좌 상세 -----");
            for (PlayerBankAccount account : accounts) {
                BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
                String productName = (product != null) ? product.getDisplayName() : "알 수 없음";
                double balance = account.getPrincipal();
                bankLore.add("§7- " + productName + ": §6" + String.format("%,.1f", balance) + "원");
            }
        }

        inventory.setItem(SLOT_BANK, ItemUtil.createItem(Material.GOLD_INGOT, "§6§l은행 계좌",
                bankLore, 1, null, 0, false, plugin));
    }

    public void open() {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
