package com.myserver.wildcore.gui;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import com.myserver.wildcore.config.PlayerBankAccount;
import com.myserver.wildcore.util.ItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 은행 출금 금액 선택 GUI
 * - 자유 예금 계좌에서 원하는 금액을 출금
 * - 프리셋 금액 버튼 + 전액 출금 + 직접 입력
 */
public class BankWithdrawGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final String accountId;
    private Inventory inventory;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0");

    // 금액 버튼 슬롯 (6개 프리셋 + 1개 전액)
    private static final int[] AMOUNT_SLOTS = { 19, 20, 21, 22, 23, 24, 25 };
    private static final double[] PRESET_AMOUNTS = { 1000, 5000, 10000, 50000, 100000, 500000, -1 }; // -1 = 전액

    // 기타 슬롯
    private static final int SLOT_INFO = 4;
    private static final int SLOT_CUSTOM = 31;
    private static final int SLOT_BACK = 45;

    public BankWithdrawGUI(WildCore plugin, Player player, String accountId) {
        this.plugin = plugin;
        this.player = player;
        this.accountId = accountId;
        createInventory();
    }

    private void createInventory() {
        PlayerBankAccount account = plugin.getBankManager().getAccount(player.getUniqueId(), accountId);
        if (account == null)
            return;

        BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
        if (product == null)
            return;

        inventory = Bukkit.createInventory(this, 54, ItemUtil.parse("§8[ §c출금하기 §8]"));

        // 배경 채우기
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // 상단 장식 (출금 테마 = 빨간색)
        ItemStack topBorder = ItemUtil.createItem(Material.RED_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorder);
        }

        // 계좌 정보
        setupInfoItem(account, product);

        // 금액 선택 버튼
        setupAmountButtons(account, product);

        // 직접 입력 버튼
        setupCustomAmountButton(account);

        // 뒤로가기 버튼
        setupBackButton();
    }

    private void setupInfoItem(PlayerBankAccount account, BankProductConfig product) {
        Material material = Material.getMaterial(product.getMaterial());
        if (material == null)
            material = Material.GOLD_INGOT;

        // 이자 정산
        double pendingInterest = plugin.getBankManager().calculateAccruedInterest(account, product);
        long timeUntilInterest = plugin.getBankManager().getTimeUntilNextInterest(account, product);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7상품명: §f" + product.getDisplayName());
        lore.add("§7유형: §a자유 예금");
        lore.add("§7이자율: §6" + String.format("%.2f%%", product.getInterestRate() * 100) + " "
                + product.getFormattedInterestInterval());
        lore.add("");
        lore.add("§7─────────────────");
        lore.add("§7현재 잔액: §6" + moneyFormat.format(account.getPrincipal()) + "원");
        lore.add("§7누적 이자: §a+" + moneyFormat.format(account.getAccumulatedInterest()) + "원");
        if (pendingInterest > 0) {
            lore.add("§7예상 이자: §a+" + moneyFormat.format(pendingInterest) + "원");
        }
        lore.add("");
        lore.add("§7⏱ 다음 이자까지: §e" + plugin.getBankManager().formatDuration(timeUntilInterest));

        inventory.setItem(SLOT_INFO, ItemUtil.createItem(
                material, "§c[ 출금 - " + product.getDisplayName() + " ]", lore, 1, null, 0, false, null));
    }

    private void setupAmountButtons(PlayerBankAccount account, BankProductConfig product) {
        double accountBalance = account.getPrincipal();

        for (int i = 0; i < AMOUNT_SLOTS.length && i < PRESET_AMOUNTS.length; i++) {
            double amount = PRESET_AMOUNTS[i];
            boolean isFullWithdraw = (amount < 0);

            if (isFullWithdraw) {
                amount = accountBalance;
            }

            List<String> lore = new ArrayList<>();
            lore.add("");

            boolean hasBalance = accountBalance >= amount && amount > 0;

            if (isFullWithdraw) {
                lore.add("§7계좌의 전체 잔액을 출금합니다.");
                lore.add("§7출금액: §6" + moneyFormat.format(accountBalance) + "원");
                lore.add("");
                if (accountBalance > 0) {
                    lore.add("§a클릭하여 전액 출금");
                } else {
                    lore.add("§c잔액이 없습니다.");
                }

                Material buttonMaterial = accountBalance > 0 ? Material.GOLD_BLOCK : Material.IRON_BLOCK;
                inventory.setItem(AMOUNT_SLOTS[i], ItemUtil.createItem(
                        buttonMaterial,
                        (accountBalance > 0 ? "§6" : "§7") + "[ 전액 출금 ]",
                        lore, 1, null, 0, accountBalance > 0, null));
            } else {
                if (!hasBalance) {
                    lore.add("§c잔액이 부족합니다.");
                    lore.add("§7현재 잔액: §6" + moneyFormat.format(accountBalance) + "원");
                } else {
                    lore.add("§7출금 후 잔액: §6" + moneyFormat.format(accountBalance - amount) + "원");
                    lore.add("");
                    lore.add("§a클릭하여 출금");
                }

                Material buttonMaterial = hasBalance ? Material.GOLD_NUGGET : Material.IRON_NUGGET;
                inventory.setItem(AMOUNT_SLOTS[i], ItemUtil.createItem(
                        buttonMaterial,
                        (hasBalance ? "§c" : "§7") + moneyFormat.format(amount) + "원",
                        lore, 1, null, 0, false, null));
            }
        }
    }

    private void setupCustomAmountButton(PlayerBankAccount account) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7원하는 금액을 채팅으로 입력할 수 있습니다.");
        lore.add("");
        lore.add("§7현재 잔액: §6" + moneyFormat.format(account.getPrincipal()) + "원");
        lore.add("");
        lore.add("§e클릭하여 금액 직접 입력");

        inventory.setItem(SLOT_CUSTOM, ItemUtil.createItem(
                Material.NAME_TAG, "§e[ 직접 입력 ]", lore, 1, null, 0, true, null));
    }

    private void setupBackButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7클릭하여 이전 화면으로");

        inventory.setItem(SLOT_BACK, ItemUtil.createItem(
                Material.ARROW, "§c[ 뒤로가기 ]", lore, 1, null, 0, false, null));
    }

    /**
     * 클릭된 슬롯의 금액 반환
     * 
     * @return 금액 (전액 출금인 경우 계좌 잔액), 해당 슬롯이 아니면 -1
     */
    public double getAmountAtSlot(int slot) {
        PlayerBankAccount account = plugin.getBankManager().getAccount(player.getUniqueId(), accountId);
        if (account == null)
            return -1;

        for (int i = 0; i < AMOUNT_SLOTS.length && i < PRESET_AMOUNTS.length; i++) {
            if (AMOUNT_SLOTS[i] == slot) {
                double presetAmount = PRESET_AMOUNTS[i];
                if (presetAmount < 0) {
                    // 전액 출금
                    return account.getPrincipal();
                }
                return presetAmount;
            }
        }
        return -1;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public String getAccountId() {
        return accountId;
    }

    public boolean isBackSlot(int slot) {
        return slot == SLOT_BACK;
    }

    public boolean isCustomSlot(int slot) {
        return slot == SLOT_CUSTOM;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
