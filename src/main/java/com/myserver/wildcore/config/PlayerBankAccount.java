package com.myserver.wildcore.config;

import java.util.UUID;

/**
 * 플레이어 은행 계좌 데이터
 * PlayerStockData 패턴을 따름
 */
public class PlayerBankAccount {

    private final String accountId; // 계좌 고유 ID
    private final String productId; // 가입한 상품 ID
    private double principal; // 원금 (현재 잔액)
    private double accumulatedInterest; // 누적 이자
    private final long createdTime; // 계좌 생성 시간 (타임스탬프, 밀리초)
    private long lastInterestTime; // 마지막 이자 정산 시간
    private long expiryTime; // 만기 시간 (적금용)
    private boolean matured; // 만기 도래 여부 (적금용)

    /**
     * 새 계좌 생성 (자유 예금용)
     */
    public PlayerBankAccount(String productId, double initialDeposit) {
        this.accountId = UUID.randomUUID().toString().substring(0, 8);
        this.productId = productId;
        this.principal = initialDeposit;
        this.accumulatedInterest = 0;
        this.createdTime = System.currentTimeMillis();
        this.lastInterestTime = this.createdTime;
        this.expiryTime = 0;
        this.matured = false;
    }

    /**
     * 새 계좌 생성 (정기 적금용)
     */
    public PlayerBankAccount(String productId, double initialDeposit, long durationSeconds) {
        this.accountId = UUID.randomUUID().toString().substring(0, 8);
        this.productId = productId;
        this.principal = initialDeposit;
        this.accumulatedInterest = 0;
        this.createdTime = System.currentTimeMillis();
        this.lastInterestTime = this.createdTime;
        this.expiryTime = this.createdTime + (durationSeconds * 1000);
        this.matured = false;
    }

    /**
     * 데이터 로드용 생성자 (YAML에서 로드)
     */
    public PlayerBankAccount(String accountId, String productId, double principal,
            double accumulatedInterest, long createdTime,
            long lastInterestTime, long expiryTime, boolean matured) {
        this.accountId = accountId;
        this.productId = productId;
        this.principal = principal;
        this.accumulatedInterest = accumulatedInterest;
        this.createdTime = createdTime;
        this.lastInterestTime = lastInterestTime;
        this.expiryTime = expiryTime;
        this.matured = matured;
    }

    // Getters
    public String getAccountId() {
        return accountId;
    }

    public String getProductId() {
        return productId;
    }

    public double getPrincipal() {
        return principal;
    }

    public double getAccumulatedInterest() {
        return accumulatedInterest;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public long getLastInterestTime() {
        return lastInterestTime;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public boolean isMatured() {
        return matured;
    }

    // Setters
    public void setPrincipal(double principal) {
        this.principal = principal;
    }

    public void setAccumulatedInterest(double accumulatedInterest) {
        this.accumulatedInterest = accumulatedInterest;
    }

    public void setLastInterestTime(long lastInterestTime) {
        this.lastInterestTime = lastInterestTime;
    }

    public void setMatured(boolean matured) {
        this.matured = matured;
    }

    /**
     * 입금
     */
    public void deposit(double amount) {
        this.principal += amount;
    }

    /**
     * 출금
     * 
     * @return 출금 성공 여부
     */
    public boolean withdraw(double amount) {
        if (amount > principal) {
            return false;
        }
        this.principal -= amount;
        return true;
    }

    /**
     * 이자 추가
     */
    public void addInterest(double interest) {
        this.accumulatedInterest += interest;
        this.principal += interest;
    }

    /**
     * 총 자산 (원금 + 누적 이자)
     * 주의: 이자가 원금에 이미 포함되어 있으므로 principal만 반환
     */
    public double getTotalBalance() {
        return principal;
    }

    /**
     * 만기까지 남은 시간 (밀리초)
     * 정기 적금일 경우에만 유효
     */
    public long getTimeUntilExpiry() {
        if (expiryTime <= 0)
            return -1;
        return Math.max(0, expiryTime - System.currentTimeMillis());
    }

    /**
     * 만기 도래 여부 확인 및 상태 업데이트
     */
    public boolean checkAndUpdateMaturity() {
        if (expiryTime > 0 && !matured && System.currentTimeMillis() >= expiryTime) {
            this.matured = true;
            return true;
        }
        return matured;
    }

    /**
     * 남은 기간을 읽기 쉬운 형식으로 반환
     */
    public String getFormattedTimeRemaining() {
        long remaining = getTimeUntilExpiry();
        if (remaining < 0)
            return "무기한";
        if (remaining == 0)
            return "만기";

        long days = remaining / (1000 * 60 * 60 * 24);
        long hours = (remaining % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60);

        if (days > 0) {
            return days + "일 " + hours + "시간";
        } else if (hours > 0) {
            return hours + "시간 " + minutes + "분";
        } else {
            return minutes + "분";
        }
    }
}
