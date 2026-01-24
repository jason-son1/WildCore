package com.myserver.wildcore.config;

/**
 * 플레이어 주식 데이터
 * 보유 수량, 평단가, 총 투자금액 관리
 */
public class PlayerStockData {
    private int amount; // 보유 수량
    private double averagePrice; // 평단가 (매수 시마다 갱신)
    private double totalInvested; // 총 투자금액 (수익률 계산용)

    public PlayerStockData(int amount) {
        this.amount = amount;
        this.averagePrice = 0; // 초기화 시 0 (마이그레이션 시 별도 처리 필요)
        this.totalInvested = 0;
    }

    public PlayerStockData(int amount, double averagePrice, double totalInvested) {
        this.amount = amount;
        this.averagePrice = averagePrice;
        this.totalInvested = totalInvested;
    }

    /**
     * 주식 매수 (평단가 갱신)
     */
    public void addPurchase(int quantity, double pricePerShare) {
        double newInvestment = quantity * pricePerShare;

        // 기존 총액 + 신규 투자액
        double currentTotalValue = (this.amount * this.averagePrice);
        // 평단가 계산 로직: (기존총액 + 신규투자액) / 전체수량
        // 단, 기존 데이터가 없을 때(amount=0)를 대비해 totalInvested도 활용 가능하지만,
        // 평단가 개념상 (보유수량 * 평단가)가 현재 가치 합계와 같아야 함.

        if (this.amount == 0) {
            this.averagePrice = pricePerShare;
        } else {
            // 가중 평균
            this.averagePrice = (currentTotalValue + newInvestment) / (this.amount + quantity);
        }

        this.amount += quantity;
        this.totalInvested += newInvestment;
    }

    /**
     * 주식 매도 (수량 차감, 평단가 유지)
     */
    public void removeSale(int quantity) {
        this.amount -= quantity;
        if (this.amount <= 0) {
            this.amount = 0;
            this.averagePrice = 0;
            this.totalInvested = 0;
        } else {
            // 매도 시 평단가는 변하지 않음
            // 다만 totalInvested(총 투자 원금)는?
            // 보통 ROI 계산 시: (현재가치 - 투자원금) / 투자원금
            // 부분 매도 시 투자원금도 비율만큼 줄여야 ROI가 유지됨.

            // 보유 비율만큼 투자금 차감
            double sellRatio = (double) quantity / (this.amount + quantity);
            this.totalInvested -= (this.totalInvested * sellRatio);
        }
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public double getAveragePrice() {
        return averagePrice;
    }

    public void setAveragePrice(double averagePrice) {
        this.averagePrice = averagePrice;
    }

    public double getTotalInvested() {
        return totalInvested;
    }

    public void setTotalInvested(double totalInvested) {
        this.totalInvested = totalInvested;
    }
}
