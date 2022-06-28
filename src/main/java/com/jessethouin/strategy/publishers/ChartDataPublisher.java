package com.jessethouin.strategy.publishers;

import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.beans.MarketData;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.strategies.BollingerBandStrategy;
import com.jessethouin.strategy.strategies.CCIStrategy;
import com.jessethouin.strategy.strategies.SMAStrategy;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class ChartDataPublisher {
    private final Config config;
    private final BarSeries barSeries;
    private final Strategy strategy;
    private final BaseTradingRecord tradingRecord;
    private final Flux<MarketData> alpacaMarketDataFlux;
    private final Sinks.Many<ChartData> alpacaChartDataSink;

    public ChartDataPublisher(Config config, BarSeries barSeries, Strategy strategy, BaseTradingRecord tradingRecord, Flux<MarketData> alpacaMarketDataFlux, Sinks.Many<ChartData> alpacaChartDataSink) {
        this.config = config;
        this.barSeries = barSeries;
        this.strategy = strategy;
        this.tradingRecord = tradingRecord;
        this.alpacaMarketDataFlux = alpacaMarketDataFlux;
        this.alpacaChartDataSink = alpacaChartDataSink;
    }

    public void subscribe() {
        alpacaMarketDataFlux.subscribe(marketData -> {
            if (!marketData.isNewBar()) return;

            ChartData chartData = new ChartData();

            switch (config.getStrategy()) {
                case BOLLINGER_BAND -> chartData = BollingerBandStrategy.getBollingerBandsChartData(barSeries.getEndIndex(), strategy);
                case CCI -> chartData = CCIStrategy.getCCIChartData(barSeries.getEndIndex(), strategy);
                case DEFAULT -> {}
                case MOVING_MOMENTUM -> {}
                case RSI2 -> {}
                case SMA -> chartData = SMAStrategy.getSMAChartData(barSeries.getEndIndex(), strategy);
            }

            DecimalNum close = marketData.getClose();
            Bar lastBar = barSeries.getLastBar();

            chartData.setOpen(lastBar.getOpenPrice().floatValue());
            chartData.setClose(close.floatValue());
            chartData.setHigh(lastBar.getHighPrice().floatValue());
            chartData.setLow(lastBar.getLowPrice().floatValue());
            chartData.setVolume(lastBar.getVolume().floatValue());

            alpacaChartDataSink.tryEmitNext(chartData);
        });
    }
}