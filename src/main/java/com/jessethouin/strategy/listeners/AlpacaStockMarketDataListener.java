package com.jessethouin.strategy.listeners;

import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.MarketData;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.bar.StockBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.realtime.trade.StockTradeMessage;
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
public class AlpacaStockMarketDataListener {
    private static final Logger LOG = LogManager.getLogger();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS Z");
    private final BarSeries barSeries;
    private final Sinks.Many<MarketData> alpacaMarketDataSink;

    public AlpacaStockMarketDataListener(BarSeries barSeries, Sinks.Many<MarketData> alpacaMarketDataSink) {
        this.barSeries = barSeries;
        this.alpacaMarketDataSink = alpacaMarketDataSink;
    }

    public MarketDataListener getStockMarketDataListener() {
        return (messageType, message) -> {
            DecimalNum close;

            switch (messageType) {
                case BAR -> {
                    StockBarMessage stockBarMessage = (StockBarMessage) message;
                    Bar bar = StrategyRunnerUtil.getBar(stockBarMessage, 60);
                    LOG.info("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(bar.getEndTime()), bar.getClosePrice());
                    barSeries.addBar(bar);
                    alpacaMarketDataSink.tryEmitNext(new MarketData(true, (DecimalNum) bar.getClosePrice()));
                }
                case TRADE -> {
                    StockTradeMessage stockTrade = (StockTradeMessage) message;
                    close = DecimalNum.valueOf(stockTrade.getPrice());
                    LOG.debug("===> {} [{}]: {}", messageType, DATE_TIME_FORMATTER.format(stockTrade.getTimestamp()), close);

                    boolean newBar = StrategyRunnerUtil.addTradeToBar(barSeries, stockTrade.getTimestamp(), stockTrade.getSize().doubleValue(), stockTrade.getPrice());
                    alpacaMarketDataSink.tryEmitNext(new MarketData(newBar, close));
                }
                case SUBSCRIPTION, SUCCESS, ERROR -> LOG.info("===> " + messageType + " [" + message.toString() + "]");
                default -> throw new IllegalArgumentException("Unknown messageType in AlpacaCryptoMarketDataListener.getStockMarketDataListener()");
            }
        };
    }
}