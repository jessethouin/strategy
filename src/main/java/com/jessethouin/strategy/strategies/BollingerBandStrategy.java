package com.jessethouin.strategy.strategies;

import com.jessethouin.strategy.beans.BollingerBandsChartData;
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
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;

import java.util.Collections;

public class BollingerBandStrategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        return buildStrategy(series, 1500);
    }

    public static Strategy buildStrategy(BarSeries series, int moving) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

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
        return new BaseStrategy(entryRule, exitRule, moving);
    }

    public static BollingerBandsChartData getBollingerBandsChartData(int index, Strategy strategy) {
        CrossedUpIndicatorRule exitRule = (CrossedUpIndicatorRule) strategy.getExitRule();
        BollingerBandsUpperIndicator upperIndicator = (BollingerBandsUpperIndicator) exitRule.getUp();
        Num upperIndicatorValue = upperIndicator.getValue(index);

        CrossedDownIndicatorRule entryRule = (CrossedDownIndicatorRule) strategy.getEntryRule();
        BollingerBandsLowerIndicator lowerIndicator = (BollingerBandsLowerIndicator) entryRule.getLow();
        Num lowerIndicatorValue = lowerIndicator.getValue(index);

        Num middleIndicatorValue = upperIndicatorValue.plus(lowerIndicatorValue).dividedBy(DecimalNum.valueOf(2));

        return BollingerBandsChartData.builder()
                .upperIndicatorValue(upperIndicatorValue.floatValue())
                .middleIndicatorValue(middleIndicatorValue.floatValue())
                .lowerIndicatorValue(lowerIndicatorValue.floatValue())
                .build();
    }
}