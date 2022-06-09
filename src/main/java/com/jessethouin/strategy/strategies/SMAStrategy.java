package com.jessethouin.strategy.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.StopGainRule;
import org.ta4j.core.rules.StopLossRule;

public class SMAStrategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        SMAIndicator shortSma = new SMAIndicator(closePrice, 500);
        SMAIndicator longSma = new SMAIndicator(closePrice, 1003);

        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new StopLossRule(closePrice, 3.0))
                .or(new StopGainRule(closePrice, 2.0));

        return new BaseStrategy(buyingRule, sellingRule);
    }
}
