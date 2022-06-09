package com.jessethouin.strategy.conf;


import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.ChartData;
import lombok.Getter;
import lombok.Setter;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.*;
import org.ta4j.core.num.DecimalNum;
import org.ta4j.core.num.Num;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Configuration
@ConfigurationProperties(prefix = "quant")
@Getter
@Setter
public class Config {
    public FeedType feed;
    public DecimalNum cash;
    public int maxBars;
    public boolean live;
    public boolean chart;
    public StrategyType strategy;
    public BacktestType backtest;
    public String backtestStart;
    public String backtestEnd;
    public String currencyPair;

    @Bean
    public Sinks.Many<ChartData> alpacaChartSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<ChartData> alpacaChartFlux(Sinks.Many<ChartData> alpacaChartSink){
        return alpacaChartSink.asFlux();
    }

    @Bean
    public Sinks.Many<Order> alpacaOrderSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<Order> alpacaOrderFlux(Sinks.Many<Order> alpacaOrderSink){
        return alpacaOrderSink.asFlux();
    }

    @Bean
    public BarSeries series() {
        return new BaseBarSeriesBuilder().withName("BTCUSD_Crypto").build();
    }

    @Bean
    public Strategy strategy() {
        return StrategyRunnerUtil.chooseStrategy(getStrategy(), series());
    }

    @Bean
    public BaseTradingRecord tradingRecord() {
        return new BaseTradingRecord();
    }

    @Bean
    public Num cash() {
        return getCash();
    }
}