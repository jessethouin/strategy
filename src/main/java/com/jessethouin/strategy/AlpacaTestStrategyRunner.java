package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AlpacaTestStrategyRunner {
    private static final Logger LOG = LogManager.getLogger();
    public final Config config;
    public final BarSeries barSeries;
    public final BaseTradingRecord tradingRecord;
    public final Strategy strategy;

    public AlpacaTestStrategyRunner(Config config, BarSeries barSeries, BaseTradingRecord tradingRecord, Strategy strategy) {
        this.config = config;
        this.barSeries = barSeries;
        this.tradingRecord = tradingRecord;
        this.strategy = strategy;
    }

    public void run() throws AlpacaClientException {
        int seriesBarCount = preloadTestSeries(barSeries);
        LOG.info("Processing {} bars using the {} strategy.", seriesBarCount, config.getStrategy());
        barSeries.setMaximumBarCount(seriesBarCount);

        switch (config.getBacktest()) {
            case BATCH -> {
                BarSeriesManager seriesManager = new BarSeriesManager(barSeries);
                TradingRecord tradingRecord = seriesManager.run(strategy);
                StrategyRunnerUtil.logSeriesStats(barSeries, tradingRecord);
            }
            case SINGLE -> {
                AtomicInteger index = new AtomicInteger(barSeries.getBeginIndex());
                barSeries.getBarData().forEach(bar -> StrategyRunnerUtil.exerciseStrategy(barSeries, strategy, tradingRecord, (DecimalNum) bar.getClosePrice(), config.getCash(), index.getAndIncrement()));
                StrategyRunnerUtil.logSeriesStats(barSeries, tradingRecord);
            }
        }
    }

    private int preloadTestSeries(BarSeries series) throws AlpacaClientException {
        ZonedDateTime start = StrategyRunnerUtil.getBacktestStart(config.getBacktestStart());
        ZonedDateTime end = StrategyRunnerUtil.getBacktestEnd(config.getBacktestEnd());
        if (start.isAfter(end) || end.isAfter(ZonedDateTime.now()) || start.isAfter(ZonedDateTime.now())) {
            LOG.error("Backtest start date cannot be after backtest end date, and backtest dates must be in the past. Using default of 60 minutes ago through now.\rGiven backtest start: {}\rGiven backtest end: {}", config.getBacktestStart(), config.getBacktestEnd());
            start = ZonedDateTime.now().minusMinutes(60);
            end = ZonedDateTime.now();
        }

        AlpacaStrategyRunnerUtil.preloadSeries(series, start, end, config.getFeed(), config.getMaxBars(), config.getCurrencyPair());
        return series.getBarCount();
    }
}