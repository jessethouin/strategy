package com.jessethouin.strategy.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import java.util.Collections;

public class RSI2Strategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        return buildStrategy(series, 5, 200, 2, 5, 95);
    }

    public static Strategy buildStrategy(BarSeries series, int shortSMAIndicator, int longSMAIndicator, int rsiIndicator, int crossedDownRSI, int crossedUpRSI) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, shortSMAIndicator);
        SMAIndicator longSma = new SMAIndicator(closePrice, longSMAIndicator);

        // We use a 2-period RSI indicator to identify buying
        // or selling opportunities within the bigger trend.
        RSIIndicator rsi = new RSIIndicator(closePrice, rsiIndicator);

        Collections.addAll(indicators, closePrice, shortSma, longSma, rsi);

        Rules rules = getRules(crossedDownRSI, crossedUpRSI, shortSma, longSma, rsi, shortSma, closePrice);

        return new BaseStrategy(rules.entryRule(), rules.exitRule());
    }
}