package com.jessethouin.strategy.subscriptions;

import com.jessethouin.strategy.AlpacaStrategyRunnerUtil;
import com.jessethouin.strategy.conf.Config;
import com.jessethouin.strategy.conf.MarketOperation;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.num.DecimalNum;
import reactor.core.publisher.Flux;

@Component
public class MarketOperationSubscription {
    private final Config config;
    private final BarSeries barSeries;
    private final Flux<MarketOperation> alpacaMarketOperationFlux;

    public MarketOperationSubscription(Config config, BarSeries barSeries, Flux<MarketOperation> alpacaMarketOperationFlux) {
        this.config = config;
        this.barSeries = barSeries;
        this.alpacaMarketOperationFlux = alpacaMarketOperationFlux;
    }

    public void subscribe() {
        alpacaMarketOperationFlux.subscribe(marketOperation -> {
            OrderTimeInForce orderTimeInForce = OrderTimeInForce.IMMEDIATE_OR_CANCEL;
            switch (config.getMarketType()) {
                case STOCK -> orderTimeInForce = OrderTimeInForce.DAY;
                case CRYPTO -> orderTimeInForce = OrderTimeInForce.GOOD_UNTIL_CANCELLED;
            }
            AlpacaStrategyRunnerUtil.exerciseAlpacaStrategy(marketOperation, (DecimalNum) barSeries.getLastBar().getClosePrice(), config.getCash(), config.getSymbol(), orderTimeInForce);
        });
    }
}