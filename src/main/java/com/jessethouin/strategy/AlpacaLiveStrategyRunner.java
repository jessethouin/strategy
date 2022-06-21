package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.AlpacaApiServices;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.listeners.AlpacaAccountListener;
import com.jessethouin.strategy.listeners.AlpacaMarketDataListener;
import com.jessethouin.strategy.subscriptions.MarketDataSubscription;
import com.jessethouin.strategy.subscriptions.MarketOperationSubscription;
import com.jessethouin.strategy.subscriptions.OrderDataSubscription;
import lombok.Getter;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.num.DecimalNum;

import java.time.ZonedDateTime;
import java.util.TimerTask;

import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_ACCOUNT_API;
import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_POSITIONS_API;

@Component
public class AlpacaLiveStrategyRunner {
    private static final Logger LOG = LogManager.getLogger();
    public final Config config;
    public final BarSeries barSeries;
    public final BaseTradingRecord tradingRecord;
    public final AlpacaMarketDataListener alpacaMarketDataListener;
    public final AlpacaAccountListener alpacaAccountListener;
    public final OrderDataSubscription orderDataSubscription;
    public final MarketDataSubscription marketDataSubscription;
    public final MarketOperationSubscription marketOperationSubscription;

    public AlpacaLiveStrategyRunner(Config config, BarSeries barSeries, BaseTradingRecord tradingRecord, AlpacaMarketDataListener alpacaMarketDataListener, AlpacaAccountListener alpacaAccountListener, OrderDataSubscription orderDataSubscription, MarketDataSubscription marketDataSubscription, MarketOperationSubscription marketOperationSubscription) {
        this.config = config;
        this.barSeries = barSeries;
        this.tradingRecord = tradingRecord;
        this.alpacaMarketDataListener = alpacaMarketDataListener;
        this.alpacaAccountListener = alpacaAccountListener;
        this.orderDataSubscription = orderDataSubscription;
        this.marketDataSubscription = marketDataSubscription;
        this.marketOperationSubscription = marketOperationSubscription;
    }

    public void run() throws AlpacaClientException {
        int seriesBarCount = preloadLiveSeries();
        barSeries.setMaximumBarCount(seriesBarCount);
        LOG.info("Processing {} bars using the {} strategy.", seriesBarCount, config.getStrategy());

        config.setCash(DecimalNum.valueOf(ALPACA_ACCOUNT_API.get().getCash()));
        LOG.info("Starting cash: {}", config.getCash());

        ALPACA_POSITIONS_API.get().stream().filter(position -> position.getSymbol().equals(config.getSymbol())).forEach(position -> {
            DecimalNum qty = DecimalNum.valueOf(position.getQuantity());
            DecimalNum price = DecimalNum.valueOf(position.getAverageEntryPrice());
            tradingRecord.enter(barSeries.getEndIndex(), price, qty);
        });

        orderDataSubscription.subscribe();
        marketDataSubscription.subscribe();
        marketOperationSubscription.subscribe();

        AlpacaApiServices.startOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
        switch (config.getMarketType()) {
            case CRYPTO -> AlpacaApiServices.startCryptoMarketDataListener(alpacaMarketDataListener.getCryptoMarketDataListener(), config);
            case STOCK -> AlpacaApiServices.startStockMarketDataListener(alpacaMarketDataListener.getStockMarketDataListener(), config);
        }

    }

    private int preloadLiveSeries() throws AlpacaClientException {
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(60);
        ZonedDateTime end = ZonedDateTime.now();

        switch (config.getMarketType()) {
            case CRYPTO -> AlpacaStrategyRunnerUtil.preloadSeries(barSeries, start, end, config);
            // using start.minusMinutes(15) and end.minusMinutes(15) in order to circumvent the subscription requirements. TODO: In production, remove this limitation.
            case STOCK -> AlpacaStrategyRunnerUtil.preloadSeries(barSeries, start.minusMinutes(15), end.minusMinutes(15), config);
        }
        return barSeries.getBarCount();
    }

    @Getter
    private final TimerTask reconnect = new TimerTask() {
        public void run() {
            LOG.info("Reconnecting to order and crypto market streams on purpose.");
            AlpacaApiServices.restartOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
            switch (config.getMarketType()) {
                case CRYPTO -> AlpacaApiServices.restartCryptoMarketDataListener(alpacaMarketDataListener.getCryptoMarketDataListener(), config);
                case STOCK -> AlpacaApiServices.restartStockMarketDataListener(alpacaMarketDataListener.getStockMarketDataListener(), config);
            }
        }
    };
}