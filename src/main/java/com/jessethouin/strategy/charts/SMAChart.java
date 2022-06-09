package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;

@Component
public class SMAChart extends AbstractChart {

    public SMAChart(Flux<ChartData> alpacaChartFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartFlux, series, strategy, tradingRecord);
    }

    protected void createDatasets() {
        List<Bar> barData = series.getBarData();
        int barCount = barData.size();

        final float[] shorts = new float[barCount];
        final float[] longs = new float[barCount];
        final float[] closes = new float[barCount];

        for (int i = 0; i < barCount; i++) {
            float[] smaData = getIndicatorData(i);
            shorts[i] = smaData[0];
            longs[i] = smaData[1];
            closes[i] = barData.get(i).getClosePrice().floatValue();
        }

        dynamicTimeSeriesCollection = new DynamicTimeSeriesCollection(3, barCount, new Second());
        dynamicTimeSeriesCollection.setTimeBase(new Second(new Date()));
        dynamicTimeSeriesCollection.addSeries(shorts, 0, "Short");
        dynamicTimeSeriesCollection.addSeries(longs, 1, "Long" );
        dynamicTimeSeriesCollection.addSeries(closes, 2, "Close" );
    }

    public void addChartData(float close) {
        float[] smaData = getIndicatorData(series.getEndIndex());
        dynamicTimeSeriesCollection.advanceTime();
        dynamicTimeSeriesCollection.appendData(new float[]{smaData[0], smaData[1], close});
    }

    protected float[] getIndicatorData(int index) {
        CrossedUpIndicatorRule entryRule = (CrossedUpIndicatorRule) strategy.getEntryRule();

        SMAIndicator shortSMAIndicator = (SMAIndicator) entryRule.getLow();
        Num shortSMAIndicatorValue = shortSMAIndicator.getValue(index);

        SMAIndicator longSMAIndicator = (SMAIndicator) entryRule.getUp();
        Num longSMAIndicatorValue = longSMAIndicator.getValue(index);

        return new float[]{shortSMAIndicatorValue.floatValue(), longSMAIndicatorValue.floatValue()};
    }

    protected PlotRenderer getPlotRenderer() {
        return new PlotRenderer(true, true) {
            @Override
            public boolean getItemShapeVisible(int chartSeries, int item) {
                if (chartSeries != 2) return false;
                int itemCount = dynamicTimeSeriesCollection.getItemCount(2);
                return isEnterOrExitPosition(tradingRecord, item, itemCount, series.getEndIndex());
            }
        };
    }
}
