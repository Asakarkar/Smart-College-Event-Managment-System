package com.college.event.patterns.strategy;

import com.college.event.model.Event;
import com.college.event.model.Student;

import java.util.HashMap;
import java.util.Map;

/**
 * Context class for the Strategy Pattern.
 * Holds reference to current strategy and allows swapping algorithms interchangeably at runtime.
 */
public class FeeStrategyContext {
    private FeeCalculationStrategy strategy;
    private static final Map<String, FeeCalculationStrategy> REGISTRY = new HashMap<>();

    static {
        registerStrategy(new FreeFeeStrategy());
        registerStrategy(new StandardFeeStrategy());
        registerStrategy(new EarlyBirdFeeStrategy());
    }

    public static void registerStrategy(FeeCalculationStrategy s) {
        REGISTRY.put(s.getTierCode().toUpperCase(), s);
    }

    public static FeeCalculationStrategy getStrategyByCode(String code) {
        if (code == null) return REGISTRY.get("STANDARD");
        FeeCalculationStrategy s = REGISTRY.get(code.toUpperCase());
        return s != null ? s : REGISTRY.get("STANDARD");
    }

    public FeeStrategyContext() {
        this(new StandardFeeStrategy());
    }

    public FeeStrategyContext(FeeCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    public FeeStrategyContext(String strategyCode) {
        this.strategy = getStrategyByCode(strategyCode);
    }

    public void setStrategy(FeeCalculationStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategyByCode(String code) {
        this.strategy = getStrategyByCode(code);
    }

    public FeeCalculationStrategy getStrategy() {
        return strategy;
    }

    public FeeBreakdown executeStrategy(Event event, Student student) {
        if (strategy == null) {
            strategy = new StandardFeeStrategy();
        }
        return strategy.calculateFee(event, student);
    }

    public static Map<String, FeeCalculationStrategy> getAllStrategies() {
        return new HashMap<>(REGISTRY);
    }
}
