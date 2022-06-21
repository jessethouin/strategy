package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import reactor.core.publisher.Flux;

@Component
public class BollingerBandsChart extends AbstractChart {

    public BollingerBandsChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 4;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        CrossedUpIndicatorRule exitRule = (CrossedUpIndicatorRule) strategy.getExitRule();
        BollingerBandsUpperIndicator upperIndicator = (BollingerBandsUpperIndicator) exitRule.getUp();
        Num upperIndicatorValue = upperIndicator.getValue(index);

        CrossedDownIndicatorRule entryRule = (CrossedDownIndicatorRule) strategy.getEntryRule();
        BollingerBandsLowerIndicator lowerIndicator = (BollingerBandsLowerIndicator) entryRule.getLow();
        Num lowerIndicatorValue = lowerIndicator.getValue(index);

        Num middleIndicatorValue = upperIndicatorValue.plus(lowerIndicatorValue).dividedBy(DecimalNum.valueOf(2));

        return new float[]{upperIndicatorValue.floatValue(), middleIndicatorValue.floatValue(), lowerIndicatorValue.floatValue()};
    }
}