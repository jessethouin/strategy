package com.jessethouin.strategy.conf;

import lombok.Getter;

public enum StrategyType {
    BOLLINGER_BAND("BollingerBand"),
    CCI("CCI"),
    DEFAULT("Default"),
    MOVING_MOMENTUM("MovingMomentum"),
    RSI2("RSI2"),
    SMA("SMA");

    @Getter
    private final String strategyType;

    StrategyType(String strategyType) {
        this.strategyType = strategyType;
    }
}
