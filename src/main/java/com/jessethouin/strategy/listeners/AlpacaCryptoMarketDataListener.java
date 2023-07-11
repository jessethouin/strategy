package com.jessethouin.strategy.listeners;

import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.MarketData;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.trade.CryptoTradeMessage;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import reactor.core.publisher.Sinks;

import java.time.format.DateTimeFormatter;

@Component
public class AlpacaCryptoMarketDataListener {
    private static final Logger LOG = LogManager.getLogger();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z");
    private final BarSeries barSeries;
    private final Sinks.Many<MarketData> alpacaMarketDataSink;

    public AlpacaCryptoMarketDataListener(BarSeries barSeries, Sinks.Many<MarketData> alpacaMarketDataSink) {
        this.barSeries = barSeries;
        this.alpacaMarketDataSink = alpacaMarketDataSink;
    }

    public MarketDataListener getCryptoMarketDataListener() {
        return (messageType, message) -> {
            DecimalNum close;

            switch (messageType) {
                case BAR -> {
                    CryptoBarMessage cryptoBarMessage = (CryptoBarMessage) message;

                    Bar bar = StrategyRunnerUtil.getBar(cryptoBarMessage, 60);
                    LOG.info("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(bar.getEndTime()), bar.getClosePrice());
                    barSeries.addBar(bar);

                    alpacaMarketDataSink.tryEmitNext(new MarketData(true, (DecimalNum) bar.getClosePrice()));
                }
                case TRADE -> {
                    CryptoTradeMessage cryptoTrade = (CryptoTradeMessage) message;

                    close = DecimalNum.valueOf(cryptoTrade.getPrice());
                    LOG.debug("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(cryptoTrade.getTimestamp()), close);

                    boolean newBar = StrategyRunnerUtil.addTradeToBar(barSeries, cryptoTrade.getTimestamp(), cryptoTrade.getSize(), cryptoTrade.getPrice());
                    alpacaMarketDataSink.tryEmitNext(new MarketData(newBar, close));
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> LOG.info("===> " + messageType + " [" + message.toString() + "]");
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaCryptoMarketDataListener.getCryptoMarketDataListener()");
            }
        };
    }
}