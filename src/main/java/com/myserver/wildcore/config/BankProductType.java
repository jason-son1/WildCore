package com.myserver.wildcore.config;

/**
 * 은행 상품 유형
 */
public enum BankProductType {
    /**
     * 자유 예금 - 언제든 입출금 가능, 시간 비례 이자
     */
    SAVINGS("자유 예금"),

    /**
     * 정기 적금 - 만기 시 이자 지급, 중도 해지 시 페널티
     */
    TERM_DEPOSIT("정기 적금");

    private final String displayName;

    BankProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * 문자열에서 BankProductType을 파싱합니다.
     * 대소문자를 구분하지 않습니다.
     */
    public static BankProductType fromString(String value) {
        if (value == null)
            return SAVINGS;

        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SAVINGS;
        }
    }
}
