package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import org.ta4j.core.cost.LinearTransactionCostModel;
import org.ta4j.core.cost.ZeroCostModel;
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

        LinearTransactionCostModel linearTransactionCostModel = new LinearTransactionCostModel(config.getFee());
        ZeroCostModel zeroCostModel = new ZeroCostModel();

        switch (config.getBacktest()) {
            case BATCH -> {
                BarSeriesManager seriesManager = new BarSeriesManager(barSeries, linearTransactionCostModel, zeroCostModel);
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
        ZonedDateTime[] backtestStartAndEndTimes = StrategyRunnerUtil.getBacktestStartAndEndTimes(config.getBacktestStart(), config.getBacktestEnd());
        AlpacaStrategyRunnerUtil.preloadSeries(series, backtestStartAndEndTimes[0], backtestStartAndEndTimes[1], config);
        return series.getBarCount();
    }
}