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
 * 은행 입금 금액 선택 GUI
 * - 상품 가입 또는 추가 입금 시 금액 선택
 */
public class BankDepositGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private final String productId;
    private final String accountId; // null이면 새 계좌 생성
    private Inventory inventory;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0");

    // 금액 버튼 슬롯
    private static final int[] AMOUNT_SLOTS = { 19, 20, 21, 22, 23, 24, 25 };
    private static final double[] PRESET_AMOUNTS = { 1000, 5000, 10000, 50000, 100000, 500000, 1000000 };

    // 기타 슬롯
    private static final int SLOT_INFO = 4;
    private static final int SLOT_CUSTOM = 31;
    private static final int SLOT_BACK = 45;

    public BankDepositGUI(WildCore plugin, Player player, String productId, String accountId) {
        this.plugin = plugin;
        this.player = player;
        this.productId = productId;
        this.accountId = accountId;
        createInventory();
    }

    private void createInventory() {
        BankProductConfig product = plugin.getConfigManager().getBankProduct(productId);
        if (product == null)
            return;

        String title = accountId == null ? "§8[ §6계좌 개설 §8]" : "§8[ §6입금하기 §8]";
        inventory = Bukkit.createInventory(this, 54, ItemUtil.parse(title));

        // 배경 채우기
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // 상단 장식 (입금 테마 = 녹색)
        ItemStack topBorder = ItemUtil.createItem(Material.LIME_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorder);
        }

        // 상품 정보
        setupInfoItem(product);

        // 금액 선택 버튼
        setupAmountButtons(product);

        // 직접 입력 버튼
        setupCustomAmountButton(product);

        // 뒤로가기 버튼
        setupBackButton();
    }

    private void setupInfoItem(BankProductConfig product) {
        Material material = Material.getMaterial(product.getMaterial());
        if (material == null)
            material = Material.GOLD_INGOT;

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7상품명: §f" + product.getDisplayName());

        if (product.isSavings()) {
            lore.add("§7유형: §a자유 예금");
            lore.add("§7이자율: §6" + String.format("%.2f%%", product.getInterestRate() * 100) + " "
                    + product.getFormattedInterestInterval());
            if (product.isCompoundInterest()) {
                lore.add("§d✦ 복리 적용");
            }
        } else if (product.isTermDeposit()) {
            lore.add("§7유형: §b정기 적금");
            lore.add("§7만기 이자: §6" + String.format("%.1f%%", product.getInterestRate() * 100));
            lore.add("§7기간: §f" + product.getFormattedDuration());
        }

        lore.add("");
        lore.add("§7─────────────────");
        lore.add("§7최소 입금: §6" + moneyFormat.format(product.getMinDeposit()) + "원");
        lore.add("§7최대 입금: §6" + moneyFormat.format(product.getMaxDeposit()) + "원");
        lore.add("");
        lore.add("§7보유 금액: §e" + moneyFormat.format(plugin.getEconomy().getBalance(player)) + "원");

        // 기존 계좌라면 현재 잔액 표시
        if (accountId != null) {
            PlayerBankAccount account = plugin.getBankManager().getAccount(player.getUniqueId(), accountId);
            if (account != null) {
                lore.add("§7현재 계좌 잔액: §6" + moneyFormat.format(account.getPrincipal()) + "원");
            }
        }

        String title = accountId == null ? "§a[ 계좌 개설 - " + product.getDisplayName() + " ]"
                : "§a[ 입금 - " + product.getDisplayName() + " ]";
        inventory.setItem(SLOT_INFO, ItemUtil.createItem(
                material, title, lore, 1, null, 0, false, null));
    }

    private void setupAmountButtons(BankProductConfig product) {
        double playerBalance = plugin.getEconomy().getBalance(player);
        double currentAccountBalance = 0;
        if (accountId != null) {
            PlayerBankAccount account = plugin.getBankManager().getAccount(player.getUniqueId(), accountId);
            if (account != null) {
                currentAccountBalance = account.getPrincipal();
            }
        }

        for (int i = 0; i < AMOUNT_SLOTS.length && i < PRESET_AMOUNTS.length; i++) {
            double amount = PRESET_AMOUNTS[i];

            List<String> lore = new ArrayList<>();
            lore.add("");

            boolean canAfford = playerBalance >= amount;
            boolean withinLimits = amount >= product.getMinDeposit()
                    && (currentAccountBalance + amount) <= product.getMaxDeposit();

            if (!canAfford) {
                lore.add("§c보유 금액이 부족합니다.");
                lore.add("§7보유: §6" + moneyFormat.format(playerBalance) + "원");
            } else if (!withinLimits) {
                if (amount < product.getMinDeposit()) {
                    lore.add("§c최소 입금액보다 적습니다.");
                } else {
                    lore.add("§c최대 입금액을 초과합니다.");
                }
            } else {
                lore.add("§7입금 후 잔여금: §e" + moneyFormat.format(playerBalance - amount) + "원");
                if (accountId != null) {
                    lore.add("§7입금 후 계좌: §6" + moneyFormat.format(currentAccountBalance + amount) + "원");
                }
                lore.add("");
                lore.add("§a클릭하여 입금");
            }

            Material buttonMaterial = canAfford && withinLimits ? Material.GOLD_NUGGET : Material.IRON_NUGGET;

            inventory.setItem(AMOUNT_SLOTS[i], ItemUtil.createItem(
                    buttonMaterial,
                    (canAfford && withinLimits ? "§a" : "§7") + moneyFormat.format(amount) + "원",
                    lore, 1, null, 0, false, null));
        }
    }

    private void setupCustomAmountButton(BankProductConfig product) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7원하는 금액을 채팅으로 입력할 수 있습니다.");
        lore.add("");
        lore.add("§7최소: §6" + moneyFormat.format(product.getMinDeposit()) + "원");
        lore.add("§7최대: §6" + moneyFormat.format(product.getMaxDeposit()) + "원");
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
     * @return 금액, 해당 슬롯이 아니면 -1
     */
    public double getAmountAtSlot(int slot) {
        for (int i = 0; i < AMOUNT_SLOTS.length && i < PRESET_AMOUNTS.length; i++) {
            if (AMOUNT_SLOTS[i] == slot) {
                return PRESET_AMOUNTS[i];
            }
        }
        return -1;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public String getProductId() {
        return productId;
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
