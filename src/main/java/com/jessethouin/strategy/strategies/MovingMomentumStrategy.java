package com.jessethouin.strategy.strategies;

import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.Collections;

public class MovingMomentumStrategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        return buildStrategy(series, 9, 26, 14, 18, 20, 80);
    }
    public static Strategy buildStrategy(BarSeries series, int shortEMAIndicator, int longEMAIndicator, int oscillatorKIndicator, int emaMacDIndicator, int crossDownOscillator, int crossUpOscillator) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // The bias is bullish when the shorter-moving average moves above the longer
        // moving average.
        // The bias is bearish when the shorter-moving average moves below the longer
        // moving average.
        EMAIndicator shortEma = new EMAIndicator(closePrice, shortEMAIndicator);
        EMAIndicator longEma = new EMAIndicator(closePrice, longEMAIndicator);

        StochasticOscillatorKIndicator stochasticOscillatorK = new StochasticOscillatorKIndicator(series, oscillatorKIndicator);

        MACDIndicator macD = new MACDIndicator(closePrice, shortEMAIndicator, longEMAIndicator);
        EMAIndicator emaMacD = new EMAIndicator(macD, emaMacDIndicator);

        Collections.addAll(indicators, shortEma, longEma, stochasticOscillatorK, macD, emaMacD);

        // Entry rule
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillatorK, crossDownOscillator)) // Signal 1
                .and(new OverIndicatorRule(macD, emaMacD)); // Signal 2

        // Exit rule
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillatorK, crossUpOscillator)) // Signal 1
                .and(new UnderIndicatorRule(macD, emaMacD)); // Signal 2

        return new BaseStrategy(entryRule, exitRule);
    }
}
