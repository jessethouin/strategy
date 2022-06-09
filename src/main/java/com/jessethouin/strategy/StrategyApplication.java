package com.jessethouin.strategy;

import com.jessethouin.strategy.charts.BollingerBandsChart;
import com.jessethouin.strategy.charts.SMAChart;
import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StrategyApplication {
    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(StrategyApplication.class);
        builder.headless(false);

        try (ConfigurableApplicationContext context = builder.run(args)) {
            Config config = context.getBean(Config.class);

            if (config.isLive()) {
                context.getBean(AlpacaLiveStrategyRunner.class).run();
            } else {
                context.getBean(AlpacaTestStrategyRunner.class).run();
            }

            if (config.isChart()) {
                switch (config.getStrategy()) {
                    case BOLLINGER_BAND -> context.getBean(BollingerBandsChart.class).startChart();
                    case CCI -> {
                    }
                    case DEFAULT -> {
                    }
                    case MOVING_MOMENTUM -> {
                    }
                    case RSI2 -> {
                    }
                    case SMA -> context.getBean(SMAChart.class).startChart();
                }
            }
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }
    }
}