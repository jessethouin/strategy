package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.beans.SMAChartData;
import com.jessethouin.strategy.strategies.SMAStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import reactor.core.publisher.Flux;

@Component
public class SMAChart extends AbstractChart {

    public SMAChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 3;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        SMAChartData smaChartData = SMAStrategy.getSMAChartData(index, strategy);
        return new float[]{smaChartData.getShortMA(), smaChartData.getLongMA()};
    }
}