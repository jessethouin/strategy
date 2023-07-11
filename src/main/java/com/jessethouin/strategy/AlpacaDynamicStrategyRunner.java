package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.strategies.BollingerBandStrategy;
import com.jessethouin.strategy.strategies.SMAStrategy;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTrade;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class AlpacaDynamicStrategyRunner {
    private static final Logger LOG = LogManager.getLogger();
    private final Config config;
    final Num[] highestReturn = {DecimalNum.valueOf(0)};
    Map<String, Integer> winningCombination = new HashMap<>();

    public AlpacaDynamicStrategyRunner(Config config) {
        this.config = config;
    }

    public void run() throws AlpacaClientException {
        ZonedDateTime[] backtestStartAndEndTimes = StrategyRunnerUtil.getBacktestStartAndEndTimes(config.getBacktestStart(), config.getBacktestEnd());

        BaseBarSeries barSeries = new BaseBarSeriesBuilder().withName(config.getSymbol()).build();
        final ArrayList<CryptoTrade> cryptoTrades = AlpacaStrategyRunnerUtil.getCryptoTrades(backtestStartAndEndTimes[0], backtestStartAndEndTimes[1], config.getSymbol());

        AlpacaStrategyRunnerUtil.preloadCryptoTradeSeries(barSeries, cryptoTrades);

        ThreadPoolExecutor executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        List<Callable<Num>> tasks = new ArrayList<>();

        switch (config.getStrategy()) {
            case BOLLINGER_BAND -> getBollingerBandTasks(barSeries, tasks);
            case SMA -> getSMATasks(barSeries, tasks);
        }

        try {
            executorService.invokeAll(tasks);
        } catch (InterruptedException e) {
            LOG.error(e.getLocalizedMessage());
        } finally {
            executorService.shutdown();
        }

        LOG.info("Winning move is {} with return of {}", winningCombination, highestReturn[0]);
        System.exit(0);
    }

    private void getSMATasks(BaseBarSeries barSeries, List<Callable<Num>> tasks) {
        for (int i = 1; i < 20; i++) {
            int shortSMA = i;
            for (int j = 1; j < 20; j++) {
                int longSMA = j;
                tasks.add(() -> {
                    Strategy currentStrategy = SMAStrategy.buildStrategy(barSeries, shortSMA, longSMA);
                    BarSeriesManager seriesManager = new BarSeriesManager(barSeries);

                    StopWatch stopWatch = new StopWatch();
                    stopWatch.start();
                    TradingRecord tradingRecord = seriesManager.run(currentStrategy);
                    stopWatch.stop();

                    LOG.info("Time to run {}/{}: {}", shortSMA, longSMA, stopWatch.getTotalTimeMillis());

                    AbstractMap.SimpleEntry<String, Integer> shortSMAEntry = new AbstractMap.SimpleEntry<>("shortSMA", shortSMA);
                    AbstractMap.SimpleEntry<String, Integer> longSMAEntry = new AbstractMap.SimpleEntry<>("longSMA", longSMA);
                    Num grossReturn = new GrossReturnCriterion().calculate(barSeries, tradingRecord);
                    best(barSeries, tradingRecord, grossReturn, shortSMAEntry, longSMAEntry);

                    return grossReturn;

                });
            }
        }
    }

    private void getBollingerBandTasks(BaseBarSeries barSeries, List<Callable<Num>> tasks) {
        for (int i = 1; i < 100; i++) {
            int ema = i;
            tasks.add(() -> {
                if (ema % 10 == 0)
                    LOG.info("==================== Running Bollinger Band strategy with EMA of {} ======================", ema);
                Strategy currentStrategy = BollingerBandStrategy.buildStrategy(barSeries, ema);
                BarSeriesManager seriesManager = new BarSeriesManager(barSeries);

                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                TradingRecord tradingRecord = seriesManager.run(currentStrategy);
                stopWatch.stop();

                LOG.info("Time to run {}: {}", ema, stopWatch.getTotalTimeMillis());

                Num grossReturn = new GrossReturnCriterion().calculate(barSeries, tradingRecord);
                AbstractMap.SimpleEntry<String, Integer> entry = new AbstractMap.SimpleEntry<>("EMA", ema);
                best(barSeries, tradingRecord, grossReturn, entry);

                return grossReturn;
            });
        }
    }

    @SafeVarargs
    private synchronized void best(BarSeries barSeries, TradingRecord tradingRecord, Num grossReturn, AbstractMap.SimpleEntry<String, Integer>... indicators) {
        if (grossReturn.isGreaterThan(highestReturn[0])) {
            winningCombination.clear();
            StringBuilder sb = new StringBuilder();
            for (AbstractMap.SimpleEntry<String, Integer> indicator : indicators) {
                sb.append(indicator.getKey()).append(" : ").append(indicator.getValue()).append(System.lineSeparator());
                winningCombination.put(indicator.getKey(), indicator.getValue());
            }
            LOG.info("Winner: \n{}with return of {}", sb, grossReturn);
            StrategyRunnerUtil.logSeriesStats(barSeries, tradingRecord);
            highestReturn[0] = grossReturn;
        }
    }
}
