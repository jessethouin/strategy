package com.jessethouin.strategy.strategies;

import com.jessethouin.strategy.beans.CCIChartData;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.indicators.helpers.ConstantIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.lang.reflect.Field;
import java.util.Collections;

public class CCIStrategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        CCIIndicator longCci = new CCIIndicator(series, 300);
        CCIIndicator shortCci = new CCIIndicator(series, 5);
        Num plus100 = series.numOf(100);
        Num minus100 = series.numOf(-100);

        Collections.addAll(indicators, longCci, shortCci);

        Rule entryRule = new OverIndicatorRule(longCci, plus100) // Bull trend
                .and(new UnderIndicatorRule(shortCci, minus100)); // Signal

        Rule exitRule = new UnderIndicatorRule(longCci, minus100) // Bear trend
                .and(new OverIndicatorRule(shortCci, plus100)); // Signal

        Strategy strategy = new BaseStrategy(entryRule, exitRule);
        strategy.setUnstablePeriod(5);
        return strategy;
    }

    public static CCIChartData getCCIChartData(int index, Strategy strategy) {
        AndRule entryRule = (AndRule) strategy.getEntryRule();
        OverIndicatorRule overIndicatorRule = (OverIndicatorRule) entryRule.getRule1();
        Num longCCIIndicatorValue;
        Num plus100IndicatorValue;
        try {
            Field longCCIIndicatorField = overIndicatorRule.getClass().getDeclaredField("first");
            longCCIIndicatorField.setAccessible(true);
            CCIIndicator longCCIIndicator = (CCIIndicator) longCCIIndicatorField.get(overIndicatorRule);
            longCCIIndicatorValue = longCCIIndicator.getValue(index);

            Field plus100IndicatorField = overIndicatorRule.getClass().getDeclaredField("second");
            plus100IndicatorField.setAccessible(true);
            ConstantIndicator<DecimalNum> plus100Indicator = (ConstantIndicator<DecimalNum>) plus100IndicatorField.get(overIndicatorRule);
            plus100IndicatorValue = plus100Indicator.getValue(index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        UnderIndicatorRule underIndicatorRule = (UnderIndicatorRule) entryRule.getRule2();
        Num shortCCIIndicatorValue;
        Num minus100IndicatorValue;
        try {
            Field shortCCIIndicatorField = underIndicatorRule.getClass().getDeclaredField("first");
            shortCCIIndicatorField.setAccessible(true);
            CCIIndicator shortCCIIndicator = (CCIIndicator) shortCCIIndicatorField.get(underIndicatorRule);
            shortCCIIndicatorValue = shortCCIIndicator.getValue(index);

            Field minus100IndicatorField = underIndicatorRule.getClass().getDeclaredField("second");
            minus100IndicatorField.setAccessible(true);
            ConstantIndicator<DecimalNum> minus100Indicator = (ConstantIndicator<DecimalNum>) minus100IndicatorField.get(underIndicatorRule);
            minus100IndicatorValue = minus100Indicator.getValue(index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return CCIChartData.builder()
                .longCCIIndicatorValue(longCCIIndicatorValue.floatValue())
                .plus100IndicatorValue(plus100IndicatorValue.floatValue())
                .shortCCIIndicatorValue(shortCCIIndicatorValue.floatValue())
                .minus100IndicatorValue(minus100IndicatorValue.floatValue())
                .build();
    }
}