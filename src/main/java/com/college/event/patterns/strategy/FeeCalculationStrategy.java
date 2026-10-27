package com.college.event.patterns.strategy;

import com.college.event.model.Event;
import com.college.event.model.Student;

/**
 * Strategy Pattern: Interchangeable algorithms behind one interface.
 * Implements interchangeable fee calculation algorithms: FREE, STANDARD, EARLY_BIRD.
 */
public interface FeeCalculationStrategy {
    String getTierCode();
    String getDisplayName();
    FeeBreakdown calculateFee(Event event, Student student);

    default double getCategoryBasePrice(String category) {
        if (category == null) return 50.0;
        return switch (category.toUpperCase()) {
            case "TECHNICAL" -> 150.0;
            case "CULTURAL" -> 100.0;
            case "SPORTS" -> 75.0;
            case "WORKSHOP" -> 200.0;
            default -> 50.0;
        };
    }
}
