package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.CCIChartData;
import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.strategies.CCIStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import reactor.core.publisher.Flux;

@Component
public class CCIChart extends AbstractChart {

    public CCIChart(Flux<ChartData> alpacaChartDataFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartDataFlux, series, strategy, tradingRecord);
        seriesCount = 5;
        lastSeriesIsClose = true;
    }

    protected float[] getIndicatorData(int index) {
        CCIChartData cciChartData = CCIStrategy.getCCIChartData(index, strategy);
        return new float[]{cciChartData.getLongCCIIndicatorValue(), cciChartData.getPlus100IndicatorValue(), cciChartData.getShortCCIIndicatorValue(), cciChartData.getMinus100IndicatorValue()};
    }
}
