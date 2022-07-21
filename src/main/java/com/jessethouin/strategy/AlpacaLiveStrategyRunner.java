package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.AlpacaApiServices;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.listeners.AlpacaAccountListener;
import com.jessethouin.strategy.listeners.AlpacaCryptoMarketDataListener;
import com.jessethouin.strategy.listeners.AlpacaStockMarketDataListener;
import com.jessethouin.strategy.publishers.ChartDataPublisher;
import com.jessethouin.strategy.publishers.MarketOperationPublisher;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_ACCOUNT_API;
import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_POSITIONS_API;

@Component
public class AlpacaLiveStrategyRunner {
    private static final Logger LOG = LogManager.getLogger();
    public final Config config;
    public final BarSeries barSeries;
    public final BaseTradingRecord tradingRecord;
    public final AlpacaCryptoMarketDataListener alpacaCryptoMarketDataListener;
    public final AlpacaStockMarketDataListener alpacaStockMarketDataListener;
    public final AlpacaAccountListener alpacaAccountListener;
    public final ChartDataPublisher chartDataPublisher;
    public final OrderDataSubscription orderDataSubscription;
    public final MarketOperationPublisher marketOperationPublisher;
    public final MarketOperationSubscription marketOperationSubscription;

    public AlpacaLiveStrategyRunner(Config config, BarSeries barSeries, BaseTradingRecord tradingRecord, AlpacaCryptoMarketDataListener alpacaCryptoMarketDataListener, AlpacaStockMarketDataListener alpacaStockMarketDataListener, AlpacaAccountListener alpacaAccountListener, ChartDataPublisher chartDataPublisher, OrderDataSubscription orderDataSubscription, MarketOperationPublisher marketOperationPublisher, MarketOperationSubscription marketOperationSubscription) {
        this.config = config;
        this.barSeries = barSeries;
        this.tradingRecord = tradingRecord;
        this.alpacaCryptoMarketDataListener = alpacaCryptoMarketDataListener;
        this.alpacaStockMarketDataListener = alpacaStockMarketDataListener;
        this.alpacaAccountListener = alpacaAccountListener;
        this.chartDataPublisher = chartDataPublisher;
        this.orderDataSubscription = orderDataSubscription;
        this.marketOperationPublisher = marketOperationPublisher;
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

        chartDataPublisher.subscribe();
        orderDataSubscription.subscribe();
        marketOperationPublisher.subscribe();
        marketOperationSubscription.subscribe();

        AlpacaApiServices.startOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
        switch (config.getMarketType()) {
            case CRYPTO -> AlpacaApiServices.startCryptoMarketDataListener(alpacaCryptoMarketDataListener.getCryptoMarketDataListener(), config);
            case STOCK -> AlpacaApiServices.startStockMarketDataListener(alpacaStockMarketDataListener.getStockMarketDataListener(), config);
        }

        long period = 5L;

        ScheduledExecutorService orderStreamExecutorService = Executors.newSingleThreadScheduledExecutor();
        orderStreamExecutorService.scheduleAtFixedRate(getReconnectOrderListenerRunnable(), period / 2, period, TimeUnit.MINUTES);

        ScheduledExecutorService marketDataStreamExecutorService = Executors.newSingleThreadScheduledExecutor();
        marketDataStreamExecutorService.scheduleAtFixedRate(getReconnectMarketDataListenerRunnable(), period, period, TimeUnit.MINUTES);
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
    private final Runnable reconnectMarketDataListenerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                LOG.info("Reconnecting to market stream on purpose.");
                switch (config.getMarketType()) {
                    case CRYPTO -> AlpacaApiServices.restartCryptoMarketDataListener(alpacaCryptoMarketDataListener.getCryptoMarketDataListener(), config);
                    case STOCK -> AlpacaApiServices.restartStockMarketDataListener(alpacaStockMarketDataListener.getStockMarketDataListener(), config);
                }
            } catch (Throwable t) {
                LOG.error("Error reconnecting to market data stream. {}", t.getMessage());
            }
        }
    };

    @Getter
    private final Runnable reconnectOrderListenerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                LOG.info("Reconnecting to order stream on purpose.");
                AlpacaApiServices.restartOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
            } catch (Throwable t) {
                LOG.error("Error reconnecting to order stream. {}", t.getMessage());
            }
        }
    };
}