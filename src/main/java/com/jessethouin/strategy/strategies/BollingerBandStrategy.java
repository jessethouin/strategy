package com.jessethouin.strategy.strategies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandWidthIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.util.Collections;

public class BollingerBandStrategy extends AbstractStrategy {
    private static final Logger LOG = LogManager.getLogger(BollingerBandStrategy.class);

    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        int moving = 300;
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        EMAIndicator ema = new EMAIndicator(closePrice, moving);
        StandardDeviationIndicator standardDeviationIndicator = new StandardDeviationIndicator(closePrice, moving);

        BollingerBandsMiddleIndicator middleIndicator = new BollingerBandsMiddleIndicator(ema);
        BollingerBandsLowerIndicator lowerIndicator = new BollingerBandsLowerIndicator(middleIndicator, standardDeviationIndicator);
        BollingerBandsUpperIndicator upperIndicator = new BollingerBandsUpperIndicator(middleIndicator, standardDeviationIndicator);
        BollingerBandWidthIndicator bandWidthIndicator = new BollingerBandWidthIndicator(upperIndicator, middleIndicator, lowerIndicator);

        Collections.addAll(indicators, closePrice, standardDeviationIndicator, middleIndicator, lowerIndicator, upperIndicator, bandWidthIndicator);

        Rule entryRule = new CrossedDownIndicatorRule(closePrice, lowerIndicator);
        Rule exitRule = new CrossedUpIndicatorRule(closePrice, upperIndicator);
        return new BaseStrategy(entryRule, exitRule, 300);
    }
}