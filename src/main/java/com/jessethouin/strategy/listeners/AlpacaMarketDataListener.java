package com.jessethouin.strategy.listeners;

import com.jessethouin.strategy.AlpacaStrategyRunnerUtil;
import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.conf.MarketOperation;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.trade.CryptoTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import org.ta4j.core.num.DecimalNum;
import reactor.core.publisher.Sinks;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AlpacaMarketDataListener {
    private static final Logger LOG = LogManager.getLogger();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z");
    private final Config config;
    private final BarSeries barSeries;
    private final Strategy strategy;
    private final BaseTradingRecord tradingRecord;

    final Sinks.Many<ChartData> alpacaChartSink;

    public AlpacaMarketDataListener(Config config, BarSeries barSeries, Strategy strategy, BaseTradingRecord tradingRecord, Sinks.Many<ChartData> alpacaChartSink) {
        this.config = config;
        this.barSeries = barSeries;
        this.strategy = strategy;
        this.tradingRecord = tradingRecord;
        this.alpacaChartSink = alpacaChartSink;
    }

    public MarketDataListener getCryptoMarketDataListener() {
        return (messageType, message) -> {
            ZonedDateTime timestamp;
            DecimalNum open;
            DecimalNum high;
            DecimalNum low;
            DecimalNum volume;
            DecimalNum close;
            boolean newBar;

            switch (messageType) {
                case BAR -> {
                    CryptoBarMessage cryptoBar = (CryptoBarMessage) message;

                    Exchange exchange = cryptoBar.getExchange();
                    if (!Exchange.COINBASE.equals(exchange)) return;

                    timestamp = cryptoBar.getTimestamp();
                    open = DecimalNum.valueOf(cryptoBar.getOpen());
                    high = DecimalNum.valueOf(cryptoBar.getHigh());
                    low = DecimalNum.valueOf(cryptoBar.getLow());
                    close = DecimalNum.valueOf(cryptoBar.getClose());
                    volume = DecimalNum.valueOf(cryptoBar.getVolume());
                    LOG.info("{} ===> {} [{}]: {}", exchange.value(), messageType, DATE_TIME_FORMATTER.format(timestamp), close);
                    barSeries.addBar(timestamp, open, high, low, close, volume);
                    newBar = true;
                }
                case TRADE -> {
                    CryptoTradeMessage cryptoTrade = (CryptoTradeMessage) message;

                    Exchange exchange = cryptoTrade.getExchange();
                    if (!Exchange.COINBASE.equals(exchange)) return;

                    close = DecimalNum.valueOf(cryptoTrade.getPrice());
                    LOG.debug("{} ===> {} [{}]: {}", exchange.value(), messageType, DATE_TIME_FORMATTER.format(cryptoTrade.getTimestamp()), close);

                    newBar = StrategyRunnerUtil.addTradeToBar(barSeries, cryptoTrade.getTimestamp(), cryptoTrade.getSize(), cryptoTrade.getPrice());
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> {
                    LOG.info("===> " + messageType + " [" + message.toString() + "]");
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaCryptoMarketSubscription.subscribe()");
            }

            MarketOperation marketOperation = StrategyRunnerUtil.exerciseStrategy(barSeries, strategy, tradingRecord, close, config.getCash());
            AlpacaStrategyRunnerUtil.exerciseAlpacaStrategy(marketOperation, close, config.getCash(), config.getCurrencyPair());

            if (newBar) {
                alpacaChartSink.tryEmitNext(new ChartData(close));
            }
        };
    }

/*
    public static MarketDataListener getStockMarketDataListener(BarSeries series, Strategy strategy, TradingRecord tradingRecord) {
        return (messageType, message) -> {
            ZonedDateTime timestamp;
            DecimalNum open;
            DecimalNum high;
            DecimalNum low;
            DecimalNum volume;
            DecimalNum close;
            switch (messageType) {
                case BAR -> {
                    StockBarMessage stockBar = (StockBarMessage) message;
                    timestamp = stockBar.getTimestamp();
                    open = DecimalNum.valueOf(stockBar.getOpen());
                    high = DecimalNum.valueOf(stockBar.getHigh());
                    low = DecimalNum.valueOf(stockBar.getLow());
                    close = DecimalNum.valueOf(stockBar.getClose());
                    volume = DecimalNum.valueOf(stockBar.getVolume());
                    LOG.info("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(timestamp), close);
                    series.addBar(timestamp, open, high, low, close, volume);
                }
                case TRADE -> {
                    StockTradeMessage stockTrade = (StockTradeMessage) message;
                    close = DecimalNum.valueOf(stockTrade.getPrice());
                    LOG.debug("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(stockTrade.getTimestamp()), close);

                    StrategyRunnerUtil.addTradeToBar(series, stockTrade.getTimestamp(), stockTrade.getSize().doubleValue(), stockTrade.getPrice());
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> {
                    LOG.info("===> " + messageType + " [" + message.toString() + "]");
                    return;
                }
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaCryptoMarketSubscription.subscribe()");
            }

            double cash = Config.get().getCash();
            MarketOperation marketOperation = StrategyRunnerUtil.exerciseStrategy(series, strategy, tradingRecord, close, series.getEndIndex(), cash);
            switch (marketOperation) {
                case ENTER -> ALPACA_LIVE_STRATEGY_RUNNER.alpacaBuy("AAPL", StrategyRunnerUtil.getBuyBudget(close, cash).getDelegate());
                case EXIT -> ALPACA_LIVE_STRATEGY_RUNNER.alpacaSell("AAPL");
            }
        };
    }
*/
}