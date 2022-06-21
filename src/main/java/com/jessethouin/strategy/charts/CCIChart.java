package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.rules.AndRule;
import org.ta4j.core.rules.OverIndicatorRule;
import reactor.core.publisher.Flux;

@Component
public class CCIChart extends AbstractChart {

    public CCIChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 3;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        AndRule entryRule = (AndRule) strategy.getEntryRule();
        OverIndicatorRule overIndicatorRule = (OverIndicatorRule) entryRule.getRule1();


        return new float[]{0, 0};
    }

}
