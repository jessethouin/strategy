package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.BollingerBandsChartData;
import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.strategies.BollingerBandStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import reactor.core.publisher.Flux;

@Component
public class BollingerBandsChart extends AbstractChart {

    public BollingerBandsChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 4;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        BollingerBandsChartData bollingerBandsChartData = BollingerBandStrategy.getBollingerBandsChartData(index, strategy);
        return new float[]{bollingerBandsChartData.getUpperIndicatorValue(), bollingerBandsChartData.getMiddleIndicatorValue(), bollingerBandsChartData.getLowerIndicatorValue()};
    }
}