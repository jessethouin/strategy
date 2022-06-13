package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.AlpacaApiServices;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.listeners.AlpacaAccountListener;
import com.jessethouin.strategy.listeners.AlpacaMarketDataListener;
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

    public AlpacaLiveStrategyRunner(Config config, BarSeries barSeries, BaseTradingRecord tradingRecord, AlpacaMarketDataListener alpacaMarketDataListener, AlpacaAccountListener alpacaAccountListener, OrderDataSubscription orderDataSubscription) {
        this.config = config;
        this.barSeries = barSeries;
        this.tradingRecord = tradingRecord;
        this.alpacaMarketDataListener = alpacaMarketDataListener;
        this.alpacaAccountListener = alpacaAccountListener;
        this.orderDataSubscription = orderDataSubscription;
    }

    public void run() throws AlpacaClientException {
        int seriesBarCount = preloadLiveSeries();
        barSeries.setMaximumBarCount(seriesBarCount);
        LOG.info("Processing {} bars using the {} strategy.", seriesBarCount, config.getStrategy());

        config.setCash(DecimalNum.valueOf(ALPACA_ACCOUNT_API.get().getCash()));
        LOG.info("Starting cash: {}", config.getCash());

        ALPACA_POSITIONS_API.get().stream().filter(position -> position.getSymbol().equals(config.getCurrencyPair())).forEach(position -> {
            DecimalNum qty = DecimalNum.valueOf(position.getQuantity());
            DecimalNum price = DecimalNum.valueOf(position.getAverageEntryPrice());
            tradingRecord.enter(barSeries.getEndIndex(), price, qty);
        });

        orderDataSubscription.subscribe();
        AlpacaApiServices.startOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
        AlpacaApiServices.startCryptoMarketDataListener(alpacaMarketDataListener.getCryptoMarketDataListener(), config);
    }

    private int preloadLiveSeries() throws AlpacaClientException {
        ZonedDateTime start = ZonedDateTime.now().minusMinutes(60);
        ZonedDateTime end = ZonedDateTime.now();

        AlpacaStrategyRunnerUtil.preloadSeries(barSeries, start, end, config.getFeed(), config.getMaxBars(), config.getCurrencyPair());
        return barSeries.getBarCount();
    }

    @Getter
    private final TimerTask reconnect = new TimerTask() {
        public void run() {
            LOG.info("Reconnecting to order and crypto market streams on purpose.");
            AlpacaApiServices.restartOrderUpdatesListener(alpacaAccountListener.getStreamingListener());
            AlpacaApiServices.restartCryptoMarketDataListener(alpacaMarketDataListener.getCryptoMarketDataListener(), config);
        }
    };
}