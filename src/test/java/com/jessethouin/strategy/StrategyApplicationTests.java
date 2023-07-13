package com.jessethouin.strategy;

import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.LatestCryptoBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.orderbook.LatestCryptoOrderbooksResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.quote.LatestCryptoQuotesResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.CryptoTradesResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.trade.LatestCryptoTradesResponse;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static com.jessethouin.strategy.conf.AlpacaApiServices.ALPACA_CRYPTO_API;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class StrategyApplicationTests {
    private static final Logger LOG = LogManager.getLogger();
    private static final String SYMBOLS = "BTC/USD,LTC/USD";

    @Test
    void contextLoads() {
    }

    @Test
    void testGetBars() {
        try {
            CryptoBarsResponse cryptoBarsResponse = ALPACA_CRYPTO_API.getBars(List.of(SYMBOLS),
                    ZonedDateTime.now().minus(60, ChronoUnit.MINUTES),
                    ZonedDateTime.now(),
                    1441,
                    null,
                    1,
                    BarTimePeriod.MINUTE);
            cryptoBarsResponse.getBars().forEach((s, cryptoBars) -> {
                assertNotNull(s);
                LOG.info("bars for " + s);
                cryptoBars.forEach(cryptoBar -> {
                    assertTrue(cryptoBar.getOpen() > 0);
                    LOG.info("bar open price for " + s + " : " + cryptoBar.getOpen());
                    assertTrue(cryptoBar.getClose() > 0);
                    LOG.info("bar close price for " + s + " : " + cryptoBar.getClose());
                    assertTrue(cryptoBar.getHigh() > 0);
                    LOG.info("bar high price for " + s + " : " + cryptoBar.getHigh());
                    assertTrue(cryptoBar.getLow() > 0);
                    LOG.info("bar low price for " + s + " : " + cryptoBar.getLow());
                    assertTrue(cryptoBar.getVwap() > 0);
                    LOG.info("bar vwap for " + s + " : " + cryptoBar.getVwap());
                    assertTrue(cryptoBar.getVolume() > 0);
                    LOG.info("bar volume for " + s + " : " + cryptoBar.getVolume());
                    assertTrue(cryptoBar.getTradeCount() > 0);
                    LOG.info("bar trade count for " + s + " : " + cryptoBar.getTradeCount());
                    assertNotNull(cryptoBar.getTimestamp());
                    LOG.info("bar timestamp for " + s + " : " + cryptoBar.getTimestamp());
                });
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetTrades() {
        try {
            CryptoTradesResponse cryptoTradesResponse = ALPACA_CRYPTO_API.getTrades(List.of(SYMBOLS),
                    ZonedDateTime.now().minus(60, ChronoUnit.MINUTES),
                    ZonedDateTime.now(),
                    1441,
                    null
                    );
            cryptoTradesResponse.getTrades().forEach((s, cryptoTrades) -> {
                assertNotNull(s);
                LOG.info("trades for " + s);
                cryptoTrades.forEach(cryptoTrade -> {
                    assertTrue(cryptoTrade.getTradeID() > 0);
                    LOG.info("trade ID for " + s + " : " + cryptoTrade.getTradeID());
                    assertTrue(cryptoTrade.getPrice() > 0);
                    LOG.info("trade price for " + s + " : " + cryptoTrade.getPrice());
                    assertTrue(cryptoTrade.getSize() > 0);
                    LOG.info("trade size for " + s + " : " + cryptoTrade.getSize());
                    assertNotNull(cryptoTrade.getTakerSide());
                    LOG.info("trade takerside for " + s + " : " + cryptoTrade.getTakerSide());
                });
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetLatestQuotes() {
        try {
            LatestCryptoQuotesResponse latestCryptoQuotesResponse = ALPACA_CRYPTO_API.getLatestQuotes(List.of(SYMBOLS));
            latestCryptoQuotesResponse.getQuotes().forEach((s, cryptoQuote) -> {
                assertNotNull(s);
                LOG.info("latest quote for " + s);
                assertTrue(cryptoQuote.getAskPrice() > 0);
                LOG.info("latest quote ask price for " + s + " : " + cryptoQuote.getAskPrice());
                assertTrue(cryptoQuote.getAskSize() > 0);
                LOG.info("latest quote ask size for " + s + " : " + cryptoQuote.getAskPrice());
                assertTrue(cryptoQuote.getBidPrice() > 0);
                LOG.info("latest quote bid price for " + s + " : " + cryptoQuote.getBidPrice());
                assertTrue(cryptoQuote.getBidSize() > 0);
                LOG.info("latest quote bid size for " + s + " : " + cryptoQuote.getBidPrice());
                assertNotNull(cryptoQuote.getTimestamp());
                LOG.info("latest quote timestamp for " + s + " : " + cryptoQuote.getTimestamp());
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetLatestBars() {
        try {
            LatestCryptoBarsResponse latestCryptoBarsResponse = ALPACA_CRYPTO_API.getLatestBars(List.of(SYMBOLS));
            latestCryptoBarsResponse.getBars().forEach((s, cryptoBar) -> {
                assertNotNull(s);
                LOG.info("latest bar for " + s);
                assertTrue(cryptoBar.getOpen() > 0);
                LOG.info("latest bar open price for " + s + " : " + cryptoBar.getOpen());
                assertTrue(cryptoBar.getClose() > 0);
                LOG.info("latest bar close price for " + s + " : " + cryptoBar.getClose());
                assertTrue(cryptoBar.getHigh() > 0);
                LOG.info("latest bar high price for " + s + " : " + cryptoBar.getHigh());
                assertTrue(cryptoBar.getLow() > 0);
                LOG.info("latest bar low price for " + s + " : " + cryptoBar.getLow());
                assertTrue(cryptoBar.getVwap() > 0);
                LOG.info("latest bar vwap for " + s + " : " + cryptoBar.getVwap());
                assertTrue(cryptoBar.getVolume() > 0);
                LOG.info("latest bar volume for " + s + " : " + cryptoBar.getVolume());
                assertTrue(cryptoBar.getTradeCount() > 0);
                LOG.info("latest bar trade count for " + s + " : " + cryptoBar.getTradeCount());
                assertNotNull(cryptoBar.getTimestamp());
                LOG.info("latest bar timestamp for " + s + " : " + cryptoBar.getTimestamp());
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetLatestTrades() {
        try {
            LatestCryptoTradesResponse latestCryptoTradesResponse = ALPACA_CRYPTO_API.getLatestTrades(List.of(SYMBOLS));
            latestCryptoTradesResponse.getTrades().forEach((s, cryptoTrade) -> {
                assertNotNull(s);
                LOG.info("latest trade for " + s);
                assertTrue(cryptoTrade.getTradeID() > 0);
                LOG.info("latest trade ID for " + s + " : " + cryptoTrade.getTradeID());
                assertTrue(cryptoTrade.getPrice() > 0);
                LOG.info("latest trade price for " + s + " : " + cryptoTrade.getPrice());
                assertTrue(cryptoTrade.getSize() > 0);
                LOG.info("latest trade size for " + s + " : " + cryptoTrade.getSize());
                assertNotNull(cryptoTrade.getTakerSide());
                LOG.info("latest trade takerside for " + s + " : " + cryptoTrade.getTakerSide());
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetLatestOrderbooks() {
        try {
            LatestCryptoOrderbooksResponse latestCryptoOrderbooksResponse = ALPACA_CRYPTO_API.getLatestOrderbooks(List.of(SYMBOLS));
            latestCryptoOrderbooksResponse.getOrderbooks().forEach((s, cryptoOrderbook) -> {
                assertNotNull(s);
                LOG.info("orderbook for " + s);
                assertNotNull(cryptoOrderbook.getAskSide());
                cryptoOrderbook.getAskSide().forEach(cryptoOrderbookEntry -> {
                    assertTrue(cryptoOrderbookEntry.getPrice() > 0);
                    LOG.info(s + " orderbook ask : " + cryptoOrderbookEntry.getPrice());
                    assertTrue(cryptoOrderbookEntry.getSize() > 0);
                    LOG.info(s + " orderbook ask size : " + cryptoOrderbookEntry.getSize());
                });
                assertNotNull(cryptoOrderbook.getBidSide());
                cryptoOrderbook.getBidSide().forEach(cryptoOrderbookEntry -> {
                    assertTrue(cryptoOrderbookEntry.getPrice() > 0);
                    LOG.info(s + " orderbook bid : " + cryptoOrderbookEntry.getPrice());
                    assertTrue(cryptoOrderbookEntry.getSize() > 0);
                    LOG.info(s + " orderbook bid size : " + cryptoOrderbookEntry.getSize());
                });
            });
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }
}
