package com.jessethouin.strategy.conf;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.enums.MarketDataMessageType;
import net.jacobpeterson.alpaca.model.endpoint.streaming.enums.StreamingMessageType;
import net.jacobpeterson.alpaca.rest.endpoint.account.AccountEndpoint;
import net.jacobpeterson.alpaca.rest.endpoint.accountactivities.AccountActivitiesEndpoint;
import net.jacobpeterson.alpaca.rest.endpoint.marketdata.crypto.CryptoMarketDataEndpoint;
import net.jacobpeterson.alpaca.rest.endpoint.marketdata.stock.StockMarketDataEndpoint;
import net.jacobpeterson.alpaca.rest.endpoint.orders.OrdersEndpoint;
import net.jacobpeterson.alpaca.rest.endpoint.positions.PositionsEndpoint;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataWebsocketInterface;
import net.jacobpeterson.alpaca.websocket.streaming.StreamingListener;
import net.jacobpeterson.alpaca.websocket.streaming.StreamingWebsocketInterface;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AlpacaApiServices {
    public static final AlpacaAPI ALPACA_API;
    public static final AccountEndpoint ALPACA_ACCOUNT_API;
    public static final AccountActivitiesEndpoint ALPACA_ACCOUNT_ACTIVITIES_API;
    public static final CryptoMarketDataEndpoint ALPACA_CRYPTO_API;
    public static final StockMarketDataEndpoint ALPACA_STOCK_API;
    public static final OrdersEndpoint ALPACA_ORDERS_API;
    public static final PositionsEndpoint ALPACA_POSITIONS_API;
    public static final StreamingWebsocketInterface ALPACA_STREAMING_API;
    public static final MarketDataWebsocketInterface ALPACA_CRYPTO_STREAMING_API;
    public static final MarketDataWebsocketInterface ALPACA_STOCK_STREAMING_API;

    static {
        ALPACA_API = new AlpacaAPI();
        ALPACA_ACCOUNT_API = ALPACA_API.account();
        ALPACA_ACCOUNT_ACTIVITIES_API = ALPACA_API.accountActivities();
        ALPACA_CRYPTO_API = ALPACA_API.cryptoMarketData();
        ALPACA_STOCK_API = ALPACA_API.stockMarketData();
        ALPACA_ORDERS_API = ALPACA_API.orders();
        ALPACA_POSITIONS_API = ALPACA_API.positions();
        ALPACA_STREAMING_API = ALPACA_API.streaming();
        ALPACA_CRYPTO_STREAMING_API = ALPACA_API.cryptoMarketDataStreaming();
        ALPACA_STOCK_STREAMING_API = ALPACA_API.stockMarketDataStreaming();
    }

    public static void connectToCryptoStream() {
        ALPACA_CRYPTO_STREAMING_API.subscribeToControl(
                MarketDataMessageType.SUCCESS,
                MarketDataMessageType.SUBSCRIPTION,
                MarketDataMessageType.ERROR);

        ALPACA_CRYPTO_STREAMING_API.connect();
        ALPACA_CRYPTO_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_CRYPTO_STREAMING_API.isValid()) {
            System.out.println("Websocket not valid!");
        }
    }

    public static void startCryptoMarketDataListener(MarketDataListener marketDataListener, Config config) {
        AlpacaApiServices.connectToCryptoStream();
        ALPACA_CRYPTO_STREAMING_API.setListener(marketDataListener);
        ALPACA_CRYPTO_STREAMING_API.subscribe(FeedType.TRADE.equals(config.getFeed()) ? List.of(config.getCurrencyPair()) : null, null, FeedType.BAR.equals(config.getFeed()) ? List.of(config.getCurrencyPair()) : null);
    }

    public static void restartCryptoMarketDataListener(MarketDataListener marketDataListener, Config config) {
        ALPACA_CRYPTO_STREAMING_API.disconnect();
        while (ALPACA_CRYPTO_STREAMING_API.isConnected()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        startCryptoMarketDataListener(marketDataListener, config);
    }

    public static void connectToStockStream() {
        ALPACA_STOCK_STREAMING_API.subscribeToControl(
                MarketDataMessageType.SUCCESS,
                MarketDataMessageType.SUBSCRIPTION,
                MarketDataMessageType.ERROR);

        ALPACA_STOCK_STREAMING_API.connect();
        ALPACA_STOCK_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_STOCK_STREAMING_API.isValid()) {
            System.out.println("Websocket not valid!");
        }
    }

    public static void connectToUpdatesStream() {
        ALPACA_STREAMING_API.streams(StreamingMessageType.AUTHORIZATION, StreamingMessageType.LISTENING);

        ALPACA_STREAMING_API.connect();
        ALPACA_STREAMING_API.waitForAuthorization(5, TimeUnit.SECONDS);
        if (!ALPACA_API.streaming().isValid()) {
            System.out.println("Websocket not valid!");
        }
    }

    public static void startOrderUpdatesListener(StreamingListener streamingListener) {
        AlpacaApiServices.connectToUpdatesStream();
        ALPACA_STREAMING_API.setListener(streamingListener);
        ALPACA_STREAMING_API.streams(StreamingMessageType.TRADE_UPDATES);
    }

    public static void restartOrderUpdatesListener(StreamingListener streamingListener) {
        ALPACA_STREAMING_API.disconnect();
        while (ALPACA_STREAMING_API.isConnected()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        startOrderUpdatesListener(streamingListener);
    }
}
