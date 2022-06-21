package com.jessethouin.strategy.subscriptions;

import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.MarketData;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.conf.MarketOperation;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class MarketDataSubscription {
    private final Config config;
    private final BarSeries barSeries;
    private final Strategy strategy;
    private final BaseTradingRecord tradingRecord;
    private final Flux<MarketData> alpacaMarketDataFlux;
    private final Sinks.Many<MarketOperation> alpacaMarketOperationSink;

    public MarketDataSubscription(Config config, BarSeries barSeries, Strategy strategy, BaseTradingRecord tradingRecord, Flux<MarketData> alpacaMarketDataFlux, Sinks.Many<MarketOperation> alpacaMarketOperationSink) {
        this.config = config;
        this.barSeries = barSeries;
        this.strategy = strategy;
        this.tradingRecord = tradingRecord;
        this.alpacaMarketDataFlux = alpacaMarketDataFlux;
        this.alpacaMarketOperationSink = alpacaMarketOperationSink;
    }

    public void subscribe() {
        alpacaMarketDataFlux.subscribe(marketData -> {
            MarketOperation marketOperation = StrategyRunnerUtil.exerciseStrategy(barSeries, strategy, tradingRecord, marketData.getClose(), config.getCash());
            alpacaMarketOperationSink.tryEmitNext(marketOperation);
        });
    }
}
