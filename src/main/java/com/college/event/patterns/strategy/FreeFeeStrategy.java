package com.college.event.patterns.strategy;

import com.college.event.model.Event;
import com.college.event.model.Student;

public class FreeFeeStrategy implements FeeCalculationStrategy {
    @Override
    public String getTierCode() {
        return "FREE";
    }

    @Override
    public String getDisplayName() {
        return "Free Tier / College Sponsored";
    }

    @Override
    public FeeBreakdown calculateFee(Event event, Student student) {
        double base = getCategoryBasePrice(event != null ? event.getCategory() : null);
        return new FeeBreakdown(
                getDisplayName(),
                getTierCode(),
                base,
                100.0,
                base,
                0.0,
                "100% sponsored by College Student Council."
        );
    }
}
