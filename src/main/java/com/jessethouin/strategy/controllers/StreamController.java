package com.jessethouin.strategy.controllers;

import com.jessethouin.strategy.beans.ChartData;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class StreamController {
    final Flux<ChartData> alpacaChartDataFlux;

    public StreamController(Flux<ChartData> alpacaChartDataFlux) {
        this.alpacaChartDataFlux = alpacaChartDataFlux;
    }

    @MessageMapping("streamChartData")
    Flux<ChartData> streamChartData() {
        return alpacaChartDataFlux;
    }
}
