package com.jessethouin.strategy;

import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.conf.MarketOperation;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTrade;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTradesResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarFeed;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.trade.StockTrade;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.trade.StockTradesResponse;
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

import static com.jessethouin.strategy.conf.AlpacaApiServices.*;

public class AlpacaStrategyRunnerUtil {
    private static final Logger LOG = LogManager.getLogger();

    public static void preloadSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, Config config) throws AlpacaClientException {
        LOG.info("Preloading series data from {} to {}", start, end);
        switch (config.getFeed()) {
            case BAR: {
                switch (config.getMarketType()) {
                    case CRYPTO -> preloadCryptoBarSeries(barSeries, start, config.getMaxBars(), config.getSymbol());
                    case STOCK -> preloadStockBarSeries(barSeries, start, end, config.getMaxBars(), config.getSymbol());
                }
            }
            case TRADE: {
                switch (config.getMarketType()) {
                    case CRYPTO -> AlpacaStrategyRunnerUtil.preloadCryptoTradeSeries(barSeries, start, end, config.getSymbol());
                    case STOCK -> AlpacaStrategyRunnerUtil.preloadStockTradeSeries(barSeries, start, end, config.getSymbol());
                }
            }
        }
    }

    public static void preloadCryptoTradeSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, String symbol) throws AlpacaClientException {
        CryptoTradesResponse cryptoTradesResponse = ALPACA_CRYPTO_API.getTrades(symbol, List.of(Exchange.COINBASE), start, end, 10000, null);
        String nextPageToken = cryptoTradesResponse.getNextPageToken();
        ArrayList<CryptoTrade> trades = cryptoTradesResponse.getTrades();

        while (nextPageToken != null) {
            cryptoTradesResponse = ALPACA_CRYPTO_API.getTrades(symbol, List.of(Exchange.COINBASE), start, end, 10000, nextPageToken);
            nextPageToken = cryptoTradesResponse.getNextPageToken();
            trades.addAll(cryptoTradesResponse.getTrades());
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

    public static void preloadCryptoBarSeries(BarSeries barSeries, ZonedDateTime start, int maxBars, String symbol) throws AlpacaClientException {
        CryptoBarsResponse cryptoBarsResponse = ALPACA_CRYPTO_API.getBars(symbol, List.of(Exchange.COINBASE), start.minus(maxBars, ChronoUnit.MINUTES), maxBars - 1, null, 1, BarTimePeriod.MINUTE);
        cryptoBarsResponse.getBars().forEach(cryptoBar -> barSeries.addBar(
                cryptoBar.getTimestamp(),
                DecimalNum.valueOf(cryptoBar.getOpen()),
                DecimalNum.valueOf(cryptoBar.getHigh()),
                DecimalNum.valueOf(cryptoBar.getLow()),
                DecimalNum.valueOf(cryptoBar.getClose()),
                DecimalNum.valueOf(cryptoBar.getVolume())));
    }

    public static void preloadStockTradeSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, String symbol) throws AlpacaClientException {
        StockTradesResponse stockTradesResponse = ALPACA_STOCK_API.getTrades(symbol, start, end, 10000, null);
        String nextPageToken = stockTradesResponse.getNextPageToken();
        ArrayList<StockTrade> trades = stockTradesResponse.getTrades();

        while (nextPageToken != null) {
            stockTradesResponse = ALPACA_STOCK_API.getTrades(symbol, start, end, 10000, nextPageToken);
            nextPageToken = stockTradesResponse.getNextPageToken();
            trades.addAll(stockTradesResponse.getTrades());
        }

        trades.forEach(stockTrade -> {
            if (barSeries.isEmpty()) {
                BaseBar bar = BaseBar.builder()
                        .volume(DecimalNum.valueOf(0))
                        .amount(DecimalNum.valueOf(0))
                        .endTime(stockTrade.getTimestamp().plusSeconds(1).withNano(0))
                        .timePeriod(Duration.ofSeconds(1)).build();
                barSeries.addBar(bar);
            }
            StrategyRunnerUtil.addTradeToBar(barSeries, stockTrade.getTimestamp(), stockTrade.getSize().doubleValue(), stockTrade.getPrice());
        });
    }

    public static void preloadStockBarSeries(BarSeries barSeries, ZonedDateTime start, ZonedDateTime end, int maxBars, String symbol) throws AlpacaClientException {
        StockBarsResponse cryptoBarsResponse = ALPACA_STOCK_API.getBars(symbol, start, end, maxBars - 1, null, 1, BarTimePeriod.MINUTE, BarAdjustment.RAW, BarFeed.IEX);
        cryptoBarsResponse.getBars().forEach(cryptoBar -> barSeries.addBar(
                cryptoBar.getTimestamp(),
                DecimalNum.valueOf(cryptoBar.getOpen()),
                DecimalNum.valueOf(cryptoBar.getHigh()),
                DecimalNum.valueOf(cryptoBar.getLow()),
                DecimalNum.valueOf(cryptoBar.getClose()),
                DecimalNum.valueOf(cryptoBar.getVolume())));
    }

    public static void exerciseAlpacaStrategy(MarketOperation marketOperation, DecimalNum close, DecimalNum cash, String symbol) {
        switch (marketOperation) {
            case ENTER -> alpacaBuy(symbol, StrategyRunnerUtil.get90PercentBuyBudget(close, cash));
            case EXIT -> alpacaSell(symbol);
        }
    }

    public static void alpacaBuy(String symbol, DecimalNum budget) {
        LOG.info("Alpaca BUY: {}", budget);
        try {
            ALPACA_ORDERS_API.requestFractionalMarketOrder(symbol, budget.getDelegate().setScale(4, RoundingMode.FLOOR).doubleValue(), OrderSide.BUY);
        } catch (AlpacaClientException e) {
            LOG.error("AlpacaClientException {}", e.getMessage());
        }
    }

    public static void alpacaSell(String symbol) {
        try {
            ALPACA_POSITIONS_API.close(symbol, null, 100.0);
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
