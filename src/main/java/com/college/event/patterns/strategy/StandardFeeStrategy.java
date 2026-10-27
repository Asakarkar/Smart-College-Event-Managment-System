package com.college.event.patterns.strategy;

import com.college.event.model.Event;
import com.college.event.model.Student;

public class StandardFeeStrategy implements FeeCalculationStrategy {
    @Override
    public String getTierCode() {
        return "STANDARD";
    }

    @Override
    public String getDisplayName() {
        return "Standard General Admission";
    }

    @Override
    public FeeBreakdown calculateFee(Event event, Student student) {
        double base = getCategoryBasePrice(event != null ? event.getCategory() : null);
        return new FeeBreakdown(
                getDisplayName(),
                getTierCode(),
                base,
                0.0,
                0.0,
                base,
                "Standard event pass with full delegate access."
        );
    }
}
