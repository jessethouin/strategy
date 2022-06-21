package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.MarketOperation;
import com.jessethouin.strategy.conf.StrategyType;
import com.jessethouin.strategy.strategies.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.ta4j.core.*;
import org.ta4j.core.analysis.criteria.ReturnOverMaxDrawdownCriterion;
import org.ta4j.core.analysis.criteria.VersusBuyAndHoldCriterion;
import org.ta4j.core.analysis.criteria.WinningPositionsRatioCriterion;
import org.ta4j.core.analysis.criteria.pnl.GrossReturnCriterion;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

public class StrategyRunnerUtil {
    private static final Logger LOG = LogManager.getLogger();

    public static ZonedDateTime getBacktestStart(String backtestStart) {
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(60);
        if (backtestStart != null && !backtestStart.isBlank()) {
            try {
                start = ZonedDateTime.parse(backtestStart);
            } catch (DateTimeParseException e) {
                LOG.error("Unable to parse backtest start time, using default of 60 minutes ago.\n\rGiven: {}", backtestStart);
                start = ZonedDateTime.now().minusMinutes(60);
            }
        }
        return start;
    }

    public static ZonedDateTime getBacktestEnd(String backtestEnd) {
        ZonedDateTime end = ZonedDateTime.now();
        if (backtestEnd != null && !backtestEnd.isBlank()) {
            try {
                end = ZonedDateTime.parse(backtestEnd);
            } catch (DateTimeParseException e) {
                LOG.error("Unable to parse backtest end time, using default of now.\n\rGiven: {}", backtestEnd);
                end = ZonedDateTime.now();
            }
        }
        return end;
    }

    public static Strategy chooseStrategy(StrategyType strategy, BarSeries barSeries) {
        return switch (strategy) {
            case SMA -> SMAStrategy.buildStrategy(barSeries);
            case BOLLINGER_BAND -> BollingerBandStrategy.buildStrategy(barSeries);
            case MOVING_MOMENTUM -> MovingMomentumStrategy.buildStrategy(barSeries);
            case RSI2 -> RSI2Strategy.buildStrategy(barSeries);
            case CCI -> CCIStrategy.buildStrategy(barSeries);
            case DEFAULT -> DefaultStrategy.buildStrategy(barSeries);
        };
    }

    public static boolean addTradeToBar(BarSeries barSeries, ZonedDateTime timestamp, Double size, Double price) {
        boolean newBar = false;
        // when do we create a new bar? When the current trade timestamp is after the current bar end time
        while (timestamp.isAfter(barSeries.getLastBar().getEndTime())) {
            System.out.print("New bar " + barSeries.getEndIndex() + " Price: " + price + "\r");
            BaseBar bar = BaseBar.builder()
                    .volume(DecimalNum.valueOf(0))
                    .amount(DecimalNum.valueOf(0))
                    .highPrice(barSeries.getLastBar().getClosePrice())
                    .lowPrice(barSeries.getLastBar().getClosePrice())
                    .openPrice(barSeries.getLastBar().getClosePrice())
                    .closePrice(barSeries.getLastBar().getClosePrice())
                    .endTime(barSeries.getLastBar().getEndTime().plusSeconds(1))
                    .timePeriod(Duration.ofSeconds(1)).build();
            barSeries.addBar(bar);
            newBar = true;
        }
        barSeries.addTrade(DecimalNum.valueOf(size), DecimalNum.valueOf(price));
        return newBar;
    }

    public static void logSeriesStats(BarSeries barSeries, TradingRecord tradingRecord) {
        LOG.info("\tNumber of positions for the strategy: {}", tradingRecord.getPositionCount());
        LOG.info("\tTotal return for the strategy: {}", new GrossReturnCriterion().calculate(barSeries, tradingRecord));

        AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new GrossReturnCriterion());
        LOG.info("\tOur profit vs buy-and-hold profit: {}", vsBuyAndHold.calculate(barSeries, tradingRecord));

        // Getting the profitable trades ratio
        AnalysisCriterion winningPositionsRatio = new WinningPositionsRatioCriterion();
        LOG.info("\tWinning trades ratio: {}", winningPositionsRatio.calculate(barSeries, tradingRecord));

        // Getting the reward-risk ratio
        AnalysisCriterion rewardRiskRatio = new ReturnOverMaxDrawdownCriterion();
        LOG.info("\tReward-risk ratio: {}", rewardRiskRatio.calculate(barSeries, tradingRecord));
    }

    synchronized public static MarketOperation exerciseStrategy(BarSeries barSeries, Strategy strategy, TradingRecord tradingRecord, DecimalNum close, DecimalNum cash)  {
        return exerciseStrategy(barSeries, strategy, tradingRecord, close, cash, barSeries.getEndIndex());
    }

    synchronized public static MarketOperation exerciseStrategy(BarSeries barSeries, Strategy strategy, TradingRecord tradingRecord, DecimalNum close, DecimalNum cash, int index)  {
        MarketOperation marketOperation = MarketOperation.NONE;

        if (strategy.shouldEnter(index)) {
            LOG.debug("Strategy should ENTER at {} on {}", close, index);
            boolean entered = tradingRecord.enter(index, close, get90PercentBuyBudget(close, cash));
            if (entered) {
                marketOperation = MarketOperation.ENTER;
                Trade entry = tradingRecord.getLastEntry();
                LOG.info("Entered on {} (price={}, amount={})", entry.getIndex(), entry.getNetPrice(), entry.getAmount());
                StrategyRunnerUtil.logSeriesStats(barSeries, tradingRecord);
            }
        } else if (strategy.shouldExit(index)) {
            LOG.debug("Strategy should EXIT on {}", index);

            Num availableToExit = (tradingRecord.getCurrentPosition().isOpened() && tradingRecord.getCurrentPosition().getEntry() != null) ? tradingRecord.getCurrentPosition().getEntry().getAmount() : DecimalNum.valueOf(0);
            boolean exited = tradingRecord.exit(index, close, availableToExit);
            if (exited) {
                marketOperation = MarketOperation.EXIT;
                Trade exit = tradingRecord.getLastExit();
                LOG.info("Exited on {} (price={}, amount={})", exit.getIndex(), exit.getNetPrice(), exit.getAmount());
                StrategyRunnerUtil.logSeriesStats(barSeries, tradingRecord);
            }
        }

        return marketOperation;
    }

    @NotNull
    public static DecimalNum get90PercentBuyBudget(DecimalNum close, DecimalNum cash) {
        return (DecimalNum) cash.multipliedBy(DecimalNum.valueOf(.90)).dividedBy(close);
    }

    @NotNull
    public static DecimalNum get95PercentBuyBudget(DecimalNum close, DecimalNum cash) {
        return (DecimalNum) cash.multipliedBy(DecimalNum.valueOf(.95)).dividedBy(close);
    }
}
