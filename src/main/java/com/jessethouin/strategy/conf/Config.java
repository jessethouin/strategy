package com.jessethouin.strategy.conf;


import com.jessethouin.strategy.StrategyRunnerUtil;
import com.jessethouin.strategy.beans.ChartData;
import com.jessethouin.strategy.beans.MarketData;
import lombok.Getter;
import lombok.Setter;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
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
    public MarketType marketType;
    public String symbol;

    @Bean
    public Sinks.Many<ChartData> alpacaChartDataSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<ChartData> alpacaChartDataFlux(Sinks.Many<ChartData> alpacaChartDataSink){
        return alpacaChartDataSink.asFlux();
    }

    @Bean
    public Sinks.Many<MarketData> alpacaMarketDataSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<MarketData> alpacaMarketDataFlux(Sinks.Many<MarketData> alpacaMarketDataSink){
        return alpacaMarketDataSink.asFlux();
    }

    @Bean
    public Sinks.Many<MarketOperation> alpacaMarketOperationSink(){
        return Sinks.many().replay().latest();
    }

    @Bean
    public Flux<MarketOperation> alpacaMarketOperationFlux(Sinks.Many<MarketOperation> alpacaMarketOperationSink){
        return alpacaMarketOperationSink.asFlux();
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
        return new BaseBarSeriesBuilder().withName(getSymbol()).build();
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
