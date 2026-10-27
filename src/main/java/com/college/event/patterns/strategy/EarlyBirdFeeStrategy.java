package com.college.event.patterns.strategy;

import com.college.event.model.Event;
import com.college.event.model.Student;

public class EarlyBirdFeeStrategy implements FeeCalculationStrategy {
    private final double discountPercentage;

    public EarlyBirdFeeStrategy() {
        this(30.0); // 30% default discount
    }

    public EarlyBirdFeeStrategy(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    @Override
    public String getTierCode() {
        return "EARLY_BIRD";
    }

    @Override
    public String getDisplayName() {
        return "Early Bird Special (" + (int) discountPercentage + "% Off)";
    }

    @Override
    public FeeBreakdown calculateFee(Event event, Student student) {
        double base = getCategoryBasePrice(event != null ? event.getCategory() : null);
        double discountAmount = Math.round((base * (discountPercentage / 100.0)) * 100.0) / 100.0;
        double finalFee = Math.max(0.0, base - discountAmount);

        return new FeeBreakdown(
                getDisplayName(),
                getTierCode(),
                base,
                discountPercentage,
                discountAmount,
                finalFee,
                "Early bird discount applied! Eligible for fast-track check-in kit."
        );
    }
}
