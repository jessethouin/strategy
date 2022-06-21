package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import reactor.core.publisher.Flux;

@Component
public class SMAChart extends AbstractChart {

    public SMAChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 3;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        CrossedUpIndicatorRule entryRule = (CrossedUpIndicatorRule) strategy.getEntryRule();

        SMAIndicator shortSMAIndicator = (SMAIndicator) entryRule.getLow();
        Num shortSMAIndicatorValue = shortSMAIndicator.getValue(index);

        SMAIndicator longSMAIndicator = (SMAIndicator) entryRule.getUp();
        Num longSMAIndicatorValue = longSMAIndicator.getValue(index);

        return new float[]{shortSMAIndicatorValue.floatValue(), longSMAIndicatorValue.floatValue()};
    }
}