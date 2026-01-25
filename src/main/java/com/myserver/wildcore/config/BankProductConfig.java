package com.myserver.wildcore.config;

import java.util.List;

/**
 * 은행 상품 설정 데이터 객체
 * StockConfig 패턴을 따름
 */
public class BankProductConfig {

    private final String id;
    private final String displayName;
    private final String material;
    private final BankProductType type;
    private final double interestRate; // 이자율 (예금: 시간당 %, 적금: 만기 시 %)
    private final long interestIntervalSeconds; // 이자 정산 단위 시간 (예금용, 초)
    private final long durationSeconds; // 만기까지 기간 (적금용, 초)
    private final double minDeposit; // 최소 입금액
    private final double maxDeposit; // 최대 입금액 (한도)
    private final double earlyWithdrawalPenalty; // 중도 해지 시 손실률 (0.0~1.0)
    private final boolean compoundInterest; // 복리 적용 여부
    private final List<String> lore;

    public BankProductConfig(String id, String displayName, String material,
            BankProductType type, double interestRate,
            long interestIntervalSeconds, long durationSeconds,
            double minDeposit, double maxDeposit,
            double earlyWithdrawalPenalty, boolean compoundInterest,
            List<String> lore) {
        this.id = id;
        this.displayName = displayName;
        this.material = material;
        this.type = type;
        this.interestRate = interestRate;
        this.interestIntervalSeconds = interestIntervalSeconds;
        this.durationSeconds = durationSeconds;
        this.minDeposit = minDeposit;
        this.maxDeposit = maxDeposit;
        this.earlyWithdrawalPenalty = earlyWithdrawalPenalty;
        this.compoundInterest = compoundInterest;
        this.lore = lore;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMaterial() {
        return material;
    }

    public BankProductType getType() {
        return type;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public long getInterestIntervalSeconds() {
        return interestIntervalSeconds;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public double getMinDeposit() {
        return minDeposit;
    }

    public double getMaxDeposit() {
        return maxDeposit;
    }

    public double getEarlyWithdrawalPenalty() {
        return earlyWithdrawalPenalty;
    }

    public boolean isCompoundInterest() {
        return compoundInterest;
    }

    public List<String> getLore() {
        return lore;
    }

    /**
     * 자유 예금 상품인지 확인
     */
    public boolean isSavings() {
        return type == BankProductType.SAVINGS;
    }

    /**
     * 정기 적금 상품인지 확인
     */
    public boolean isTermDeposit() {
        return type == BankProductType.TERM_DEPOSIT;
    }

    /**
     * 기간을 읽기 쉬운 형식으로 반환 (예: "7일", "30일")
     */
    public String getFormattedDuration() {
        if (durationSeconds <= 0)
            return "무기한";

        long days = durationSeconds / 86400;
        long hours = (durationSeconds % 86400) / 3600;
        long minutes = (durationSeconds % 3600) / 60;

        if (days > 0) {
            return days + "일" + (hours > 0 ? " " + hours + "시간" : "");
        } else if (hours > 0) {
            return hours + "시간" + (minutes > 0 ? " " + minutes + "분" : "");
        } else {
            return minutes + "분";
        }
    }

    /**
     * 이자 정산 간격을 읽기 쉬운 형식으로 반환
     */
    public String getFormattedInterestInterval() {
        if (interestIntervalSeconds <= 0)
            return "즉시";

        long hours = interestIntervalSeconds / 3600;
        long minutes = (interestIntervalSeconds % 3600) / 60;

        if (hours > 0) {
            return hours + "시간" + (minutes > 0 ? " " + minutes + "분" : "") + "마다";
        } else {
            return minutes + "분마다";
        }
    }
}
