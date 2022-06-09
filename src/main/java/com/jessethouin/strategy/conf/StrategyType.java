package com.jessethouin.strategy.conf;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum StrategyType {
    BOLLINGER_BAND("BollingerBand"),
    CCI("CCI"),
    DEFAULT("Default"),
    MOVING_MOMENTUM("MovingMomentum"),
    RSI2("RSI2"),
    SMA("SMA");

    @Getter
    private final String strategyType;
    private static final Map<String, StrategyType> ENUM_MAP;

    StrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    static {
        Map<String, StrategyType> map = new ConcurrentHashMap<>();
        for (StrategyType instance : StrategyType.values()) {
            map.put(instance.getStrategyType().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static StrategyType get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
}
