package com.jessethouin.strategy.subscriptions;

import com.jessethouin.strategy.AlpacaStrategyRunnerUtil;
import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class OrderDataSubscription {
    private static final Logger LOG = LogManager.getLogger();
    final Flux<Order> alpacaOrderFlux;
    final Config config;

    public OrderDataSubscription(Flux<Order> alpacaOrderFlux, Config config) {
        this.alpacaOrderFlux = alpacaOrderFlux;
        this.config = config;
    }

    public void subscribe() {
        alpacaOrderFlux.subscribe(order -> {
            LOG.info("Incoming order {} status {}", order.getId(), order.getStatus());
            LOG.info("Cash updated to {}", AlpacaStrategyRunnerUtil.updateCash(config));
        });
    }
}
