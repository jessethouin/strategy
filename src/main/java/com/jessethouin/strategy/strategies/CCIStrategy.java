package com.jessethouin.strategy.strategies;

import com.jessethouin.strategy.beans.CCIChartData;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseStrategy;
import org.ta4j.core.Rule;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.CCIIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.lang.reflect.Field;
import java.util.Collections;

public class CCIStrategy extends AbstractStrategy {
    public static Strategy buildStrategy(BarSeries series) {
        return buildStrategy(series, 300, 5);
    }

    public static Strategy buildStrategy(BarSeries series, int longIndicator, int shortIndicator) {
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }

        CCIIndicator longCci = new CCIIndicator(series, longIndicator);
        CCIIndicator shortCci = new CCIIndicator(series, shortIndicator);
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
        Num longCCIIndicatorValue = reflectIndicatorValue(index, overIndicatorRule);

        UnderIndicatorRule underIndicatorRule = (UnderIndicatorRule) entryRule.getRule2();
        Num shortCCIIndicatorValue = reflectIndicatorValue(index, underIndicatorRule);

        return CCIChartData.builder()
                .longCCIIndicatorValue(longCCIIndicatorValue.floatValue())
                .plus100IndicatorValue(100f)
                .shortCCIIndicatorValue(shortCCIIndicatorValue.floatValue())
                .minus100IndicatorValue(-100f)
                .build();
    }

    private static Num reflectIndicatorValue(int index, Rule rule) {
        Num cciIndicatorValue;
        try {
            Field cciIndicatorField = rule.getClass().getDeclaredField("first");
            cciIndicatorField.setAccessible(true);
            CCIIndicator cciIndicator = (CCIIndicator) cciIndicatorField.get(rule);
            cciIndicatorValue = cciIndicator.getValue(index);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return cciIndicatorValue;
    }
}