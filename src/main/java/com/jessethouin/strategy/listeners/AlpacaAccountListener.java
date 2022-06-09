package com.jessethouin.strategy.listeners;

import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.streaming.enums.StreamingMessageType;
import net.jacobpeterson.alpaca.model.endpoint.streaming.trade.TradeUpdateMessage;
import net.jacobpeterson.alpaca.websocket.streaming.StreamingListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Component
public class AlpacaAccountListener {
    private static final Logger LOG = LogManager.getLogger();
    final Sinks.Many<Order> alpacaOrderSink;

    public AlpacaAccountListener(Sinks.Many<Order> alpacaOrderSink) {
        this.alpacaOrderSink = alpacaOrderSink;
    }

    public StreamingListener getStreamingListener() {
        return (messageType, message) -> {
            if (messageType.equals(StreamingMessageType.TRADE_UPDATES)) {
                TradeUpdateMessage tradeUpdateMessage = (TradeUpdateMessage) message;
                Order order = tradeUpdateMessage.getData().getOrder();
                alpacaOrderSink.tryEmitNext(order);
            } else {
                LOG.info(message.toString());
            }
        };
    }
}
