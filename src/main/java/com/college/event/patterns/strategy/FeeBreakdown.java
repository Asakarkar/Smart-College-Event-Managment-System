package com.college.event.patterns.strategy;

public class FeeBreakdown {
    private final String strategyName;
    private final double basePrice;
    private final double discountPercentage;
    private final double discountAmount;
    private final double finalFee;
    private final String description;
    private final String tierCode;

    public FeeBreakdown(String strategyName, String tierCode, double basePrice,
                        double discountPercentage, double discountAmount,
                        double finalFee, String description) {
        this.strategyName = strategyName;
        this.tierCode = tierCode;
        this.basePrice = basePrice;
        this.discountPercentage = discountPercentage;
        this.discountAmount = discountAmount;
        this.finalFee = finalFee;
        this.description = description;
    }

    public String getStrategyName() { return strategyName; }
    public String getTierCode() { return tierCode; }
    public double getBasePrice() { return basePrice; }
    public double getDiscountPercentage() { return discountPercentage; }
    public double getDiscountAmount() { return discountAmount; }
    public double getFinalFee() { return finalFee; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("[%s] Base: $%.2f | Discount: %.0f%% (-$%.2f) | Final: $%.2f (%s)",
                tierCode, basePrice, discountPercentage, discountAmount, finalFee, description);
    }
}
