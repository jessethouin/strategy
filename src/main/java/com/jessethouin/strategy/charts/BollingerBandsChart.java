package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
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

import java.util.Date;
import java.util.List;

@Component
public class BollingerBandsChart extends AbstractChart {

    public BollingerBandsChart(Flux<ChartData> alpacaChartFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super(alpacaChartFlux, series, strategy, tradingRecord);
    }

    protected void createDatasets() {
        List<Bar> barData = series.getBarData();
        int barCount = barData.size();

        final float[] upper = new float[barCount];
        final float[] middle = new float[barCount];
        final float[] lower = new float[barCount];
        final float[] close = new float[barCount];

        for (int i = 0; i < barCount; i++) {
            float[] bollingerBandData = getIndicatorData(i);
            upper[i] = bollingerBandData[0];
            middle[i] = bollingerBandData[1];
            lower[i] = bollingerBandData[2];
            close[i] = barData.get(i).getClosePrice().floatValue();
        }

        dynamicTimeSeriesCollection = new DynamicTimeSeriesCollection(4, barCount, new Second());
        dynamicTimeSeriesCollection.setTimeBase(new Second(new Date()));
        dynamicTimeSeriesCollection.addSeries(upper, 0, "Upper");
        dynamicTimeSeriesCollection.addSeries(middle, 1, "Middle" );
        dynamicTimeSeriesCollection.addSeries(lower, 2, "Lower" );
        dynamicTimeSeriesCollection.addSeries(close, 3, "Close" );
    }

    protected void addChartData(float close) {
        float[] bollingerBandData = getIndicatorData(series.getEndIndex());
        dynamicTimeSeriesCollection.advanceTime();
        dynamicTimeSeriesCollection.appendData(new float[]{bollingerBandData[0], bollingerBandData[1], bollingerBandData[2], close});
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

    protected PlotRenderer getPlotRenderer() {
        return new PlotRenderer(true, true) {
            @Override
            public boolean getItemShapeVisible(int chartSeries, int item) {
                if (chartSeries != 3) return false;
                int itemCount = dynamicTimeSeriesCollection.getItemCount(3);
                return isEnterOrExitPosition(tradingRecord, item, itemCount, series.getEndIndex());
            }
        };
    }
}