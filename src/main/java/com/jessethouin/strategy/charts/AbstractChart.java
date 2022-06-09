package com.jessethouin.strategy.charts;

import com.jessethouin.strategy.beans.ChartData;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.chart.ui.UIUtils;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.springframework.stereotype.Component;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Strategy;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.awt.geom.Ellipse2D;

@Component
public abstract class AbstractChart extends ApplicationFrame {
    protected DynamicTimeSeriesCollection dynamicTimeSeriesCollection;
    protected final Flux<ChartData> alpacaChartFlux;
    protected final BarSeries series;
    protected final Strategy strategy;
    protected final BaseTradingRecord tradingRecord;

    public AbstractChart(Flux<ChartData> alpacaChartFlux, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super("Strategery");
        this.alpacaChartFlux = alpacaChartFlux;
        this.strategy = strategy;
        this.series = series;
        this.tradingRecord = tradingRecord;
    }

    public void startChart() {
        this.createDatasets();

        JFreeChart chart = createChart();
        this.add(new ChartPanel(chart) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(2560, 1440);
            }
        }, BorderLayout.CENTER);

        this.pack();
        UIUtils.centerFrameOnScreen(this);
        this.setVisible(true);
        alpacaChartFlux.subscribe(chartData -> addChartData(chartData.getClose().floatValue()));
    }

    protected abstract void addChartData(float floatValue);

    protected abstract void createDatasets();

    protected abstract float[] getIndicatorData(int index);

    protected JFreeChart createChart() {
        PlotRenderer plotRenderer = getPlotRenderer();
        final JFreeChart smaChart = ChartFactory.createTimeSeriesChart("SMA", "hh:mm:ss", "indicators", dynamicTimeSeriesCollection, true, true, false);
        final XYPlot plot = smaChart.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        ValueAxis range = plot.getRangeAxis();
        range.setAutoRange(true);

        Shape shape = new Ellipse2D.Double(-4.0, -4.0, 8.0, 8.0);

        int closeSeriesId = dynamicTimeSeriesCollection.getSeriesCount() - 1;
        plotRenderer.setSeriesShape(closeSeriesId, shape);
        plotRenderer.setSeriesShapesFilled(closeSeriesId, true);
        plotRenderer.setSeriesFillPaint(closeSeriesId, Color.black);
        plotRenderer.setUseFillPaint(true);

        plot.setDataset(0, dynamicTimeSeriesCollection);
        plot.setRenderer(0, plotRenderer);
        plot.setDomainAxis(0, domain);
        plot.setRangeAxis(0, range);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        return smaChart;
    }

    protected PlotRenderer getPlotRenderer() {
        return new PlotRenderer(true, true) {
            @Override
            public boolean getItemShapeVisible(int chartSeries, int item) {
                return false;
            }
        };
    }
}
