package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.conf.FeedType;
import com.jessethouin.strategy.conf.MarketOperation;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTrade;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTradesResponse;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;

import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_ACCOUNT_API;

public class AlpacaStrategyRunnerUtil {
    private static final AlpacaAPI ALPACA_API = new AlpacaAPI();
    private static final Logger LOG = LogManager.getLogger();

    public static void preloadSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, FeedType feedType, int maxBars, String currencyPair) throws AlpacaClientException {
        LOG.info("Preloading series data from {} to {}", start, end);
        switch (feedType) {
            case BAR: AlpacaStrategyRunnerUtil.preloadBarSeries(barSeries, start, maxBars, currencyPair);
            case TRADE: AlpacaStrategyRunnerUtil.preloadTradeSeries(barSeries, start, end, currencyPair);
        }
    }

    public static void preloadTradeSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, String currencyPair) throws AlpacaClientException {
        CryptoTradesResponse btcTradeResponse = ALPACA_API.cryptoMarketData().getTrades(currencyPair, List.of(Exchange.COINBASE), start, end, 10000, null);
        String nextPageToken = btcTradeResponse.getNextPageToken();
        ArrayList<CryptoTrade> trades = btcTradeResponse.getTrades();

        while (nextPageToken != null) {
            btcTradeResponse = ALPACA_API.cryptoMarketData().getTrades(currencyPair, List.of(Exchange.COINBASE), start, end, 10000, nextPageToken);
            nextPageToken = btcTradeResponse.getNextPageToken();
            trades.addAll(btcTradeResponse.getTrades());
        }

        trades.forEach(cryptoTrade -> {
            if (barSeries.isEmpty()) {
                BaseBar bar = BaseBar.builder()
                        .volume(DecimalNum.valueOf(0))
                        .amount(DecimalNum.valueOf(0))
                        .endTime(cryptoTrade.getTimestamp().plusSeconds(1).withNano(0))
                        .timePeriod(Duration.ofSeconds(1)).build();
                barSeries.addBar(bar);
            }
            StrategyRunnerUtil.addTradeToBar(barSeries, cryptoTrade.getTimestamp(), cryptoTrade.getSize(), cryptoTrade.getPrice());
        });
    }

    public static void preloadBarSeries(BarSeries barSeries, ZonedDateTime start, int maxBars, String currencyPair) throws AlpacaClientException {
        CryptoBarsResponse btcBarsResponse = ALPACA_API.cryptoMarketData().getBars(currencyPair, List.of(Exchange.COINBASE), start.minus(maxBars, ChronoUnit.MINUTES), maxBars - 1, null, 1, BarTimePeriod.MINUTE);
        btcBarsResponse.getBars().forEach(cryptoBar -> barSeries.addBar(
                cryptoBar.getTimestamp(),
                DecimalNum.valueOf(cryptoBar.getOpen()),
                DecimalNum.valueOf(cryptoBar.getHigh()),
                DecimalNum.valueOf(cryptoBar.getLow()),
                DecimalNum.valueOf(cryptoBar.getClose()),
                DecimalNum.valueOf(cryptoBar.getVolume())));
    }

    public static void exerciseAlpacaStrategy(MarketOperation marketOperation, DecimalNum close, DecimalNum cash, String currencyPair) {
        switch (marketOperation) {
            case ENTER -> alpacaBuy(currencyPair, StrategyRunnerUtil.get95PercentBuyBudget(close, cash));
            case EXIT -> alpacaSell(currencyPair);
        }
    }

    public static void alpacaBuy(String symbol, DecimalNum budget) {
        LOG.info("Alpaca BUY: {}", budget);
        try {
            ALPACA_API.orders().requestFractionalMarketOrder(symbol, budget.getDelegate().setScale(4, RoundingMode.FLOOR).doubleValue(), OrderSide.BUY);
        } catch (AlpacaClientException e) {
            LOG.error("AlpacaClientException {}", e.getMessage());
        }
    }

    public static void alpacaSell(String symbol) {
        try {
            ALPACA_API.positions().close(symbol, null, 100.0);
        } catch (AlpacaClientException e) {
            LOG.error("AlpacaClientException {}", e.getMessage());
        }
    }

    public static Num updateCash(Config config) {
        try {
            config.setCash(DecimalNum.valueOf(ALPACA_ACCOUNT_API.get().getCash()));
        } catch (AlpacaClientException e) {
            LOG.error("Error updating cash from Alpaca account. {}", e.getMessage());
        }
        return config.getCash();
    }
}
