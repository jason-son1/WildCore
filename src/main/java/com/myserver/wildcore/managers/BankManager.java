package com.myserver.wildcore.managers;

import com.myserver.wildcore.WildCore;
import com.myserver.wildcore.config.BankProductConfig;
import com.myserver.wildcore.config.PlayerBankAccount;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

/**
 * 은행 시스템 매니저
 * - 예금/적금 관리
 * - 이자 계산 (Lazy Evaluation)
 * - 입출금 처리
 * - 플레이어 계좌 데이터 관리
 */
public class BankManager {

    private final WildCore plugin;

    // 플레이어별 은행 계좌 (UUID -> (계좌ID -> 계좌데이터))
    private Map<UUID, Map<String, PlayerBankAccount>> playerAccounts = new HashMap<>();

    // 데이터 파일
    private File dataFile;
    private FileConfiguration dataConfig;

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0");
    private final DecimalFormat percentFormat = new DecimalFormat("0.00");

    public BankManager(WildCore plugin) {
        this.plugin = plugin;
        loadData();
    }

    /**
     * 데이터 파일 로드
     */
    private void loadData() {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        dataFile = new File(dataFolder, "bank_data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("은행 데이터 파일 생성 실패: " + e.getMessage());
            }
        }

        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        // 플레이어 은행 데이터 로드
        if (dataConfig.isConfigurationSection("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, PlayerBankAccount> accounts = new HashMap<>();

                String accountsPath = "players." + uuidStr + ".accounts";
                if (dataConfig.isConfigurationSection(accountsPath)) {
                    for (String accountId : dataConfig.getConfigurationSection(accountsPath).getKeys(false)) {
                        String path = accountsPath + "." + accountId;

                        PlayerBankAccount account = new PlayerBankAccount(
                                accountId,
                                dataConfig.getString(path + ".product_id"),
                                dataConfig.getDouble(path + ".principal"),
                                dataConfig.getDouble(path + ".accumulated_interest"),
                                dataConfig.getLong(path + ".created_time"),
                                dataConfig.getLong(path + ".last_interest_time"),
                                dataConfig.getLong(path + ".expiry_time", 0),
                                dataConfig.getBoolean(path + ".is_matured", false));

                        accounts.put(accountId, account);
                    }
                }
                playerAccounts.put(uuid, accounts);
            }
        }

        plugin.getLogger().info("은행 데이터 로드 완료 (플레이어: " + playerAccounts.size() + "명)");
    }

    /**
     * 모든 데이터 저장
     */
    public void saveAllData() {
        // 기존 데이터 클리어
        dataConfig.set("players", null);

        // 플레이어 데이터 저장
        for (Map.Entry<UUID, Map<String, PlayerBankAccount>> playerEntry : playerAccounts.entrySet()) {
            String uuidStr = playerEntry.getKey().toString();
            Map<String, PlayerBankAccount> accounts = playerEntry.getValue();

            if (accounts == null || accounts.isEmpty())
                continue;

            for (Map.Entry<String, PlayerBankAccount> accountEntry : accounts.entrySet()) {
                String path = "players." + uuidStr + ".accounts." + accountEntry.getKey();
                PlayerBankAccount account = accountEntry.getValue();

                dataConfig.set(path + ".product_id", account.getProductId());
                dataConfig.set(path + ".principal", account.getPrincipal());
                dataConfig.set(path + ".accumulated_interest", account.getAccumulatedInterest());
                dataConfig.set(path + ".created_time", account.getCreatedTime());
                dataConfig.set(path + ".last_interest_time", account.getLastInterestTime());
                dataConfig.set(path + ".expiry_time", account.getExpiryTime());
                dataConfig.set(path + ".is_matured", account.isMatured());
            }
        }

        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("은행 데이터 저장 실패: " + e.getMessage());
        }
    }

    // =====================
    // 이자 계산 (핵심 로직)
    // =====================

    /**
     * 누적 이자 계산 (Lazy Evaluation)
     * 플레이어가 GUI를 열거나 명령어를 사용할 때 호출
     */
    public double calculateAccruedInterest(PlayerBankAccount account, BankProductConfig product) {
        if (product == null || account == null)
            return 0;

        // 자유 예금: 시간 비례 이자
        if (product.isSavings()) {
            return calculateSavingsInterest(account, product);
        }

        // 정기 적금: 만기 시 이자 (만기 전에는 0)
        if (product.isTermDeposit()) {
            return calculateTermDepositInterest(account, product);
        }

        return 0;
    }

    /**
     * 자유 예금 이자 계산
     * 공식: (경과 시간 / 이자 간격) * 이자율 * 원금
     * 복리일 경우: 원금 * (1 + 이자율)^(경과 횟수) - 원금
     */
    private double calculateSavingsInterest(PlayerBankAccount account, BankProductConfig product) {
        long currentTime = System.currentTimeMillis();
        long lastInterestTime = account.getLastInterestTime();
        long elapsedMillis = currentTime - lastInterestTime;
        long intervalMillis = product.getInterestIntervalSeconds() * 1000;

        if (intervalMillis <= 0)
            return 0;

        // 정산 횟수 계산
        long periods = elapsedMillis / intervalMillis;
        if (periods <= 0)
            return 0;

        double principal = account.getPrincipal();
        double rate = product.getInterestRate();

        if (product.isCompoundInterest()) {
            // 복리: P * (1 + r)^n - P
            return principal * (Math.pow(1 + rate, periods) - 1);
        } else {
            // 단리: P * r * n
            return principal * rate * periods;
        }
    }

    /**
     * 정기 적금 이자 계산
     * 만기 전: 0
     * 만기 후: 원금 * 이자율
     */
    private double calculateTermDepositInterest(PlayerBankAccount account, BankProductConfig product) {
        if (account.checkAndUpdateMaturity()) {
            return account.getPrincipal() * product.getInterestRate();
        }
        return 0;
    }

    /**
     * 이자 정산 및 적용
     */
    public double applyInterest(UUID playerId, String accountId) {
        PlayerBankAccount account = getAccount(playerId, accountId);
        if (account == null)
            return 0;

        BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
        if (product == null)
            return 0;

        double interest = calculateAccruedInterest(account, product);

        if (interest > 0 && product.isSavings()) {
            account.addInterest(interest);
            // 마지막 정산 시간 업데이트 (정산 단위로 갱신)
            long intervalMillis = product.getInterestIntervalSeconds() * 1000;
            long currentTime = System.currentTimeMillis();
            long elapsedMillis = currentTime - account.getLastInterestTime();
            long periods = elapsedMillis / intervalMillis;
            account.setLastInterestTime(account.getLastInterestTime() + (periods * intervalMillis));
            saveAllData();
        }

        return interest;
    }

    // =====================
    // 계좌 관리
    // =====================

    /**
     * 새 계좌 생성
     * 
     * @return 생성된 계좌 ID, 실패 시 null
     */
    public String createAccount(Player player, String productId, double initialDeposit) {
        BankProductConfig product = plugin.getConfigManager().getBankProduct(productId);
        if (product == null) {
            sendMessage(player, "bank_product_not_found");
            return null;
        }

        // 입금 한도 확인
        if (initialDeposit < product.getMinDeposit()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("min", moneyFormat.format(product.getMinDeposit()));
            sendMessage(player, "bank_min_deposit", replacements);
            return null;
        }

        if (initialDeposit > product.getMaxDeposit()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("max", moneyFormat.format(product.getMaxDeposit()));
            sendMessage(player, "bank_limit_exceeded", replacements);
            return null;
        }

        // 돈 확인 및 차감
        if (!plugin.getEconomy().has(player, initialDeposit)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", moneyFormat.format(initialDeposit));
            sendMessage(player, "insufficient_funds", replacements);
            plugin.getConfigManager().playSound(player, "error");
            return null;
        }

        plugin.getEconomy().withdrawPlayer(player, initialDeposit);

        // 계좌 생성
        PlayerBankAccount account;
        if (product.isTermDeposit()) {
            account = new PlayerBankAccount(productId, initialDeposit, product.getDurationSeconds());
        } else {
            account = new PlayerBankAccount(productId, initialDeposit);
        }

        Map<String, PlayerBankAccount> accounts = playerAccounts.computeIfAbsent(
                player.getUniqueId(), k -> new HashMap<>());
        accounts.put(account.getAccountId(), account);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("product", product.getDisplayName());
        replacements.put("amount", moneyFormat.format(initialDeposit));
        sendMessage(player, "bank_account_created", replacements);
        plugin.getConfigManager().playSound(player, "buy");

        saveAllData();
        return account.getAccountId();
    }

    /**
     * 입금 (자유 예금용)
     */
    public boolean deposit(Player player, String accountId, double amount) {
        PlayerBankAccount account = getAccount(player.getUniqueId(), accountId);
        if (account == null) {
            sendMessage(player, "bank_account_not_found");
            return false;
        }

        BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
        if (product == null || !product.isSavings()) {
            sendMessage(player, "bank_deposit_not_allowed");
            return false;
        }

        // 한도 확인
        if (account.getPrincipal() + amount > product.getMaxDeposit()) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("max", moneyFormat.format(product.getMaxDeposit()));
            sendMessage(player, "bank_limit_exceeded", replacements);
            return false;
        }

        // 이자 먼저 정산
        applyInterest(player.getUniqueId(), accountId);

        // 돈 확인 및 차감
        if (!plugin.getEconomy().has(player, amount)) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("amount", moneyFormat.format(amount));
            sendMessage(player, "insufficient_funds", replacements);
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        plugin.getEconomy().withdrawPlayer(player, amount);
        account.deposit(amount);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("product", product.getDisplayName());
        replacements.put("amount", moneyFormat.format(amount));
        sendMessage(player, "bank_deposit_success", replacements);
        plugin.getConfigManager().playSound(player, "buy");

        saveAllData();
        return true;
    }

    /**
     * 출금 (자유 예금용)
     */
    public boolean withdraw(Player player, String accountId, double amount) {
        PlayerBankAccount account = getAccount(player.getUniqueId(), accountId);
        if (account == null) {
            sendMessage(player, "bank_account_not_found");
            return false;
        }

        BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
        if (product == null || !product.isSavings()) {
            sendMessage(player, "bank_withdraw_not_allowed");
            return false;
        }

        // 이자 먼저 정산
        double interest = applyInterest(player.getUniqueId(), accountId);

        // 잔액 확인
        if (account.getPrincipal() < amount) {
            sendMessage(player, "bank_insufficient_balance");
            plugin.getConfigManager().playSound(player, "error");
            return false;
        }

        account.withdraw(amount);
        plugin.getEconomy().depositPlayer(player, amount);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("product", product.getDisplayName());
        replacements.put("amount", moneyFormat.format(amount));
        replacements.put("interest", moneyFormat.format(interest));
        sendMessage(player, "bank_withdraw_success", replacements);
        plugin.getConfigManager().playSound(player, "sell");

        saveAllData();
        return true;
    }

    /**
     * 계좌 해지
     * 
     * @param forceEarly 만기 전 강제 해지 여부 (적금용)
     */
    public boolean closeAccount(Player player, String accountId, boolean forceEarly) {
        PlayerBankAccount account = getAccount(player.getUniqueId(), accountId);
        if (account == null) {
            sendMessage(player, "bank_account_not_found");
            return false;
        }

        BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
        if (product == null) {
            sendMessage(player, "bank_product_not_found");
            return false;
        }

        double payout;

        if (product.isSavings()) {
            // 자유 예금: 이자 정산 후 전액 지급
            applyInterest(player.getUniqueId(), accountId);
            payout = account.getPrincipal();
        } else if (product.isTermDeposit()) {
            // 정기 적금
            if (account.checkAndUpdateMaturity()) {
                // 만기 도달: 원금 + 이자 지급
                double interest = account.getPrincipal() * product.getInterestRate();
                payout = account.getPrincipal() + interest;
            } else if (forceEarly) {
                // 중도 해지: 페널티 적용
                double penalty = account.getPrincipal() * product.getEarlyWithdrawalPenalty();
                payout = account.getPrincipal() - penalty;

                Map<String, String> replacements = new HashMap<>();
                replacements.put("penalty", percentFormat.format(product.getEarlyWithdrawalPenalty() * 100));
                sendMessage(player, "bank_early_withdrawal_applied", replacements);
            } else {
                // 중도 해지 확인 필요
                Map<String, String> replacements = new HashMap<>();
                replacements.put("penalty", percentFormat.format(product.getEarlyWithdrawalPenalty() * 100));
                sendMessage(player, "bank_early_withdrawal_warning", replacements);
                return false;
            }
        } else {
            payout = account.getPrincipal();
        }

        // 지급 및 계좌 삭제
        plugin.getEconomy().depositPlayer(player, payout);
        playerAccounts.get(player.getUniqueId()).remove(accountId);

        // 메시지 전송
        Map<String, String> replacements = new HashMap<>();
        replacements.put("total", moneyFormat.format(payout));
        sendMessage(player, "bank_account_closed", replacements);
        plugin.getConfigManager().playSound(player, "sell");

        saveAllData();
        return true;
    }

    // =====================
    // 유틸리티 메서드
    // =====================

    public PlayerBankAccount getAccount(UUID playerId, String accountId) {
        Map<String, PlayerBankAccount> accounts = playerAccounts.get(playerId);
        return accounts != null ? accounts.get(accountId) : null;
    }

    public List<PlayerBankAccount> getPlayerAccounts(UUID playerId) {
        Map<String, PlayerBankAccount> accounts = playerAccounts.get(playerId);
        return accounts != null ? new ArrayList<>(accounts.values()) : new ArrayList<>();
    }

    public int getPlayerAccountCount(UUID playerId) {
        Map<String, PlayerBankAccount> accounts = playerAccounts.get(playerId);
        return accounts != null ? accounts.size() : 0;
    }

    /**
     * 플레이어의 총 예금 자산
     */
    public double getTotalBalance(UUID playerId) {
        double total = 0;
        for (PlayerBankAccount account : getPlayerAccounts(playerId)) {
            total += account.getPrincipal();
        }
        return total;
    }

    /**
     * 플레이어의 총 예상 이자 (아직 정산되지 않은 이자)
     */
    public double getTotalPendingInterest(UUID playerId) {
        double total = 0;
        for (PlayerBankAccount account : getPlayerAccounts(playerId)) {
            BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
            if (product != null) {
                total += calculateAccruedInterest(account, product);
            }
        }
        return total;
    }

    /**
     * 만기된 적금 계좌 목록
     */
    public List<PlayerBankAccount> getMaturedAccounts(UUID playerId) {
        List<PlayerBankAccount> matured = new ArrayList<>();
        for (PlayerBankAccount account : getPlayerAccounts(playerId)) {
            BankProductConfig product = plugin.getConfigManager().getBankProduct(account.getProductId());
            if (product != null && product.isTermDeposit() && account.checkAndUpdateMaturity()) {
                matured.add(account);
            }
        }
        return matured;
    }

    private void sendMessage(Player player, String key) {
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage(key));
    }

    private void sendMessage(Player player, String key, Map<String, String> replacements) {
        player.sendMessage(plugin.getConfigManager().getPrefix() +
                plugin.getConfigManager().getMessage(key, replacements));
    }

    public String formatMoney(double amount) {
        return moneyFormat.format(amount);
    }

    public void reload() {
        loadData();
    }
}
