package com.jessethouin.strategy.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

public class DefaultStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        return SMAStrategy.buildStrategy(series);
    }
}
