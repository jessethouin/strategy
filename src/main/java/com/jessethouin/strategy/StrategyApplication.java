package com.jessethouin.strategy;

import com.jessethouin.strategy.charts.BollingerBandsChart;
import com.jessethouin.strategy.charts.CCIChart;
import com.jessethouin.strategy.charts.SMAChart;
import com.jessethouin.strategy.conf.Config;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class StrategyApplication {
    private static final Logger LOG = LogManager.getLogger();

    public static void main(String[] args) {
        SpringApplicationBuilder builder = new SpringApplicationBuilder(StrategyApplication.class);
        builder.headless(false);

        ConfigurableApplicationContext context = builder.run(args);
        try {
            Config config = context.getBean(Config.class);

            if (config.isLive()) {
                context.getBean(AlpacaLiveStrategyRunner.class).run();
            } else {
                switch (config.getBacktest()) {
                    case SINGLE, BATCH -> context.getBean(AlpacaTestStrategyRunner.class).run();
                    case DYNAMIC -> context.getBean(AlpacaDynamicStrategyRunner.class).run();
                }
            }

            if (config.isChart()) {
                switch (config.getStrategy()) {
                    case BOLLINGER_BAND -> context.getBean(BollingerBandsChart.class).startChart();
                    case CCI -> context.getBean(CCIChart.class).startChart();
                    case SMA, DEFAULT -> context.getBean(SMAChart.class).startChart();
                }
            }
        } catch (AlpacaClientException e) {
            LOG.error("Caught AlpacaClientException. {}", e.getMessage());
        }
    }
}