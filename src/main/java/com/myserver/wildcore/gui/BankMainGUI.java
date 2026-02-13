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
 * 은행 메인 GUI
 * - 내 계좌 현황 표시
 * - 새 상품 가입 버튼
 * - 보유 계좌 목록
 */
public class BankMainGUI implements InventoryHolder {

    private final WildCore plugin;
    private final Player player;
    private Inventory inventory;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0");

    // 슬롯 상수
    private static final int SLOT_INFO = 4;
    private static final int SLOT_NEW_PRODUCT = 8;
    private static final int[] ACCOUNT_SLOTS = {
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public BankMainGUI(WildCore plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        createInventory();
    }

    private void createInventory() {
        inventory = Bukkit.createInventory(this, 54,
                ItemUtil.parse(plugin.getConfigManager().getBankGuiTitle()));

        // 배경 채우기
        ItemStack background = ItemUtil.createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, background);
        }

        // 상단 장식
        ItemStack topBorder = ItemUtil.createItem(Material.YELLOW_STAINED_GLASS_PANE, " ", null, 1, null, 0, false,
                null);
        for (int i = 0; i < 9; i++) {
            inventory.setItem(i, topBorder);
        }

        // 정보 아이콘
        setupInfoItem();

        // 새 상품 가입 버튼
        setupNewProductButton();

        // 내 계좌 목록
        setupAccountItems();
    }

    private void setupInfoItem() {
        double totalBalance = plugin.getBankManager().getTotalBalance(player.getUniqueId());
        double pendingInterest = plugin.getBankManager().getTotalPendingInterest(player.getUniqueId());
        int accountCount = plugin.getBankManager().getPlayerAccountCount(player.getUniqueId());
        double walletBalance = plugin.getEconomy().getBalance(player);

        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7지갑 잔액: §6" + moneyFormat.format(walletBalance) + "원");
        lore.add("");
        lore.add("§7보유 계좌: §e" + accountCount + "개");
        lore.add("§7총 예금액: §6" + moneyFormat.format(totalBalance) + "원");
        lore.add("§7예상 이자: §a+" + moneyFormat.format(pendingInterest) + "원");
        lore.add("");
        lore.add("§7총 자산: §6" + moneyFormat.format(walletBalance + totalBalance) + "원");

        inventory.setItem(SLOT_INFO, ItemUtil.createItem(
                Material.GOLD_BLOCK, "§6[ 내 자산 현황 ]", lore, 1, null, 0, false, null));
    }

    private void setupNewProductButton() {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("§7새로운 예금 또는 적금 상품에");
        lore.add("§7가입할 수 있습니다.");
        lore.add("");
        lore.add("§a• 자유 예금: §f언제든 입출금 가능");
        lore.add("§b• 정기 적금: §f고수익 장기 상품");
        lore.add("");
        lore.add("§e클릭하여 상품 목록 보기");

        inventory.setItem(SLOT_NEW_PRODUCT, ItemUtil.createItem(
                Material.EMERALD, "§a[ 새 상품 가입 ]", lore, 1, null, 0, true, null));
    }

    private void setupAccountItems() {
        List<PlayerBankAccount> accounts = plugin.getBankManager().getPlayerAccounts(player.getUniqueId());

        if (accounts.isEmpty()) {
            // 계좌 없음 안내
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§7보유 중인 계좌가 없습니다.");
            lore.add("");
            lore.add("§e우측 상단의 '새 상품 가입' 버튼을");
            lore.add("§e클릭하여 계좌를 개설하세요!");

            inventory.setItem(22, ItemUtil.createItem(
                    Material.BARRIER, "§c계좌 없음", lore, 1, null, 0, false, null));
            return;
        }

        int slotIndex = 0;
        for (PlayerBankAccount account : accounts) {
            if (slotIndex >= ACCOUNT_SLOTS.length)
                break;

            BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
            if (product == null)
                continue;

            // 적금 만기 확인
            account.checkAndUpdateMaturity();

            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.add("§8계좌 #" + account.getAccountId());
            lore.add("");

            // 자산 정보 섹션
            lore.add("§7잔액: §6" + moneyFormat.format(account.getPrincipal()) + "원");
            lore.add("§7누적 이자: §a+" + moneyFormat.format(account.getAccumulatedInterest()) + "원");

            // 예상 이자 (아직 정산되지 않은)
            double pendingInterest = plugin.getBankManager().calculateAccruedInterest(account, product);
            if (pendingInterest > 0) {
                lore.add("§7예상 이자: §a+" + moneyFormat.format(pendingInterest) + "원");
            }

            lore.add("");
            lore.add("§7─────────────────");

            Material iconMaterial;
            boolean glow = false;
            String titlePrefix;

            if (product.isSavings()) {
                iconMaterial = Material.SUNFLOWER;
                titlePrefix = "§a";

                // 이자 정보
                lore.add("§7유형: §a자유 예금");
                lore.add("§7이자율: §6" + String.format("%.2f%%", product.getInterestRate() * 100) + " "
                        + product.getFormattedInterestInterval());
                if (product.isCompoundInterest()) {
                    lore.add("§d✦ 복리 적용");
                }

                // 다음 이자 타이머 + 프로그레스 바
                long timeUntilInterest = plugin.getBankManager().getTimeUntilNextInterest(account, product);
                long intervalMillis = product.getInterestIntervalSeconds() * 1000L;
                lore.add("");
                lore.add("§7⏱ 다음 이자까지: §e" + plugin.getBankManager().formatDuration(timeUntilInterest));

                if (intervalMillis > 0 && timeUntilInterest >= 0) {
                    double progress = 1.0 - ((double) timeUntilInterest / intervalMillis);
                    lore.add("§7" + buildProgressBar(progress, 20));
                }

                // 조작 안내
                lore.add("");
                lore.add("§a▶ 좌클릭: §f입금하기");
                lore.add("§c▶ 우클릭: §f출금하기");
                lore.add("§4▶ Shift+우클릭: §f계좌 해지");
            } else if (product.isTermDeposit()) {
                // 정기 적금
                lore.add("§7유형: §b정기 적금");

                if (account.isMatured()) {
                    iconMaterial = Material.NETHER_STAR;
                    titlePrefix = "§6";
                    glow = true;

                    double interest = account.getPrincipal() * product.getInterestRate();
                    lore.add("");
                    lore.add("§6§l⭐ 만기 도달!");
                    lore.add("§7만기 이자: §a+" + moneyFormat.format(interest) + "원");
                    lore.add("§7수령 가능액: §6§l" + moneyFormat.format(account.getPrincipal() + interest) + "원");
                    lore.add("");
                    lore.add("§7" + buildProgressBar(1.0, 20));
                    lore.add("");
                    lore.add("§e▶ 클릭하여 수령하기");
                } else {
                    iconMaterial = Material.CLOCK;
                    titlePrefix = "§b";

                    long remaining = account.getTimeUntilExpiry();
                    long totalDuration = product.getDurationSeconds() * 1000L;
                    double progress = totalDuration > 0 ? 1.0 - ((double) remaining / totalDuration) : 0;

                    lore.add("§7만기 이자율: §6" + String.format("%.1f%%", product.getInterestRate() * 100));
                    lore.add("");
                    lore.add("§7⏱ 만기까지: §e" + account.getFormattedTimeRemaining());
                    lore.add("§7진행률: §f" + String.format("%.1f%%", progress * 100));
                    lore.add("§7" + buildProgressBar(progress, 20));
                    lore.add("");
                    lore.add("§c▶ Shift+우클릭: §f중도 해지");
                    lore.add("§8  (페널티 " + String.format("%.1f%%", product.getEarlyWithdrawalPenalty() * 100) + ")");
                }
            } else {
                iconMaterial = Material.GOLD_INGOT;
                titlePrefix = "§e";
            }

            String displayTitle = titlePrefix + product.getDisplayName() + " §7(#"
                    + account.getAccountId().substring(0, 4) + ")";
            inventory.setItem(ACCOUNT_SLOTS[slotIndex], ItemUtil.createItem(
                    iconMaterial, displayTitle, lore, 1, null, 0, glow, null));

            slotIndex++;
        }
    }

    /**
     * 프로그레스 바 생성
     * 
     * @param progress 0.0 ~ 1.0
     * @param length   바 길이 (칸 수)
     */
    private String buildProgressBar(double progress, int length) {
        progress = Math.max(0, Math.min(1.0, progress));
        int filled = (int) (progress * length);
        int empty = length - filled;

        StringBuilder bar = new StringBuilder("§8[");
        bar.append("§a");
        for (int i = 0; i < filled; i++)
            bar.append("■");
        bar.append("§7");
        for (int i = 0; i < empty; i++)
            bar.append("□");
        bar.append("§8]");
        bar.append(" §f").append(String.format("%.0f%%", progress * 100));
        return bar.toString();
    }

    /**
     * 특정 슬롯의 계좌 ID 반환
     */
    public String getAccountIdAtSlot(int slot) {
        List<PlayerBankAccount> accounts = plugin.getBankManager().getPlayerAccounts(player.getUniqueId());

        for (int i = 0; i < ACCOUNT_SLOTS.length; i++) {
            if (ACCOUNT_SLOTS[i] == slot && i < accounts.size()) {
                return accounts.get(i).getAccountId();
            }
        }
        return null;
    }

    public void open() {
        player.openInventory(inventory);
        // 자동 새로고침 시작 (1초마다 계좌 정보 갱신)
        AutoRefreshGUI.startAutoRefresh(plugin, player, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof BankMainGUI) {
                updateDynamicItems();
            } else {
                AutoRefreshGUI.stopAutoRefresh(player);
            }
        }, 20L);
    }

    public void refresh() {
        createInventory();
        player.openInventory(inventory);
    }

    /**
     * 동적 아이템만 갱신 (정보 및 계좌 타이머)
     */
    private void updateDynamicItems() {
        setupInfoItem();
        setupAccountItems();
    }

    public boolean isNewProductSlot(int slot) {
        return slot == SLOT_NEW_PRODUCT;
    }

    public boolean isAccountSlot(int slot) {
        for (int s : ACCOUNT_SLOTS) {
            if (s == slot)
                return true;
        }
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }
}
