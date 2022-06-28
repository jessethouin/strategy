package com.jessethouin.strategy.listeners;

import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.beans.MarketData;
import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.trade.CryptoTradeMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.trade.StockTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
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
    private final Sinks.Many<ChartData> alpacaChartDataSink;
    private final Sinks.Many<MarketData> alpacaMarketDataSink;

    public AlpacaMarketDataListener(BarSeries barSeries, Sinks.Many<ChartData> alpacaChartDataSink, Sinks.Many<MarketData> alpacaMarketDataSink, Config config) {
        this.barSeries = barSeries;
        this.alpacaChartDataSink = alpacaChartDataSink;
        this.alpacaMarketDataSink = alpacaMarketDataSink;
        this.config = config;
    }

    public MarketDataListener getCryptoMarketDataListener() {
        return (messageType, message) -> {
            ZonedDateTime timestamp;
            DecimalNum open;
            DecimalNum high;
            DecimalNum low;
            DecimalNum volume;
            DecimalNum close;

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

                    alpacaMarketDataSink.tryEmitNext(new MarketData(true, close));
                }
                case TRADE -> {
                    CryptoTradeMessage cryptoTrade = (CryptoTradeMessage) message;

                    Exchange exchange = cryptoTrade.getExchange();
                    if (!Exchange.COINBASE.equals(exchange)) return;

                    close = DecimalNum.valueOf(cryptoTrade.getPrice());
                    LOG.debug("{} ===> {} [{}]: {}", exchange.value(), messageType, DATE_TIME_FORMATTER.format(cryptoTrade.getTimestamp()), close);

                    boolean newBar = StrategyRunnerUtil.addTradeToBar(barSeries, cryptoTrade.getTimestamp(), cryptoTrade.getSize(), cryptoTrade.getPrice());
                    alpacaMarketDataSink.tryEmitNext(new MarketData(newBar, close));
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> LOG.info("===> " + messageType + " [" + message.toString() + "]");
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaMarketDataListener.getCryptoMarketDataListener()");
            }
        };
    }

    public MarketDataListener getStockMarketDataListener() {
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
                    barSeries.addBar(timestamp, open, high, low, close, volume);

                    alpacaMarketDataSink.tryEmitNext(new MarketData(true, close));
                }
                case TRADE -> {
                    StockTradeMessage stockTrade = (StockTradeMessage) message;
                    close = DecimalNum.valueOf(stockTrade.getPrice());
                    LOG.debug("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(stockTrade.getTimestamp()), close);

                    boolean newBar = StrategyRunnerUtil.addTradeToBar(barSeries, stockTrade.getTimestamp(), stockTrade.getSize().doubleValue(), stockTrade.getPrice());
                    alpacaMarketDataSink.tryEmitNext(new MarketData(newBar, close));
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> LOG.info("===> " + messageType + " [" + message.toString() + "]");
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaMarketDataListener.getStockMarketDataListener()");
            }

        };
    }
}