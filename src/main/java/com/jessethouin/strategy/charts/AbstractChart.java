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
import org.jfree.data.time.Second;
import org.springframework.stereotype.Component;
import org.ta4j.core.*;
import reactor.core.publisher.Flux;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.List;

@Component
public abstract class AbstractChart extends ApplicationFrame {
    protected DynamicTimeSeriesCollection dynamicTimeSeriesCollection;
    protected final Flux<ChartData> alpacaChartDataSink;
    protected final BarSeries series;
    protected final Strategy strategy;
    protected final BaseTradingRecord tradingRecord;
    protected int seriesCount;
    protected boolean lastSeriesIsClose;

    public AbstractChart(Flux<ChartData> alpacaChartDataSink, BarSeries series, Strategy strategy, BaseTradingRecord tradingRecord) {
        super("Strategery");
        this.alpacaChartDataSink = alpacaChartDataSink;
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
        alpacaChartDataSink.subscribe(chartData -> addChartData(chartData.getClose().floatValue()));
    }

    protected void addChartData(float close) {
        float[] indicatorData = getIndicatorData(series.getEndIndex());
        float[] allIndicatorData = new float[seriesCount];
        for (int j = 0; j < seriesCount; j++) {
            if (j == seriesCount - 1 && lastSeriesIsClose) {
                allIndicatorData[j] = close;
            } else {
                allIndicatorData[j] = indicatorData[j];
            }
        }
        dynamicTimeSeriesCollection.advanceTime();
        dynamicTimeSeriesCollection.appendData(allIndicatorData);
    }

    protected void createDatasets() {
        List<Bar> barData = series.getBarData();
        int barCount = barData.size();
        float[][] seriesData = new float[seriesCount][barCount];

        for (int i = 0; i < barCount; i++) {
            float[] indicatorData = getIndicatorData(i);
            for (int j = 0; j < seriesCount; j++) {
                if (j == seriesCount - 1 && lastSeriesIsClose) {
                    seriesData[j][i] = barData.get(i).getClosePrice().floatValue();
                } else {
                    seriesData[j][i] = indicatorData[j];
                }
            }
        }

        dynamicTimeSeriesCollection = new DynamicTimeSeriesCollection(seriesCount, barCount, new Second());
        dynamicTimeSeriesCollection.setTimeBase(new Second(new Date()));
        for (int j = 0; j < seriesCount; j++) {
            dynamicTimeSeriesCollection.addSeries(seriesData[j], j, "Series " + (j + 1));
        }
    }

    protected abstract float[] getIndicatorData(int index);

    protected JFreeChart createChart() {
        PlotRenderer plotRenderer = getPlotRenderer();
        final JFreeChart timeSeriesChart = ChartFactory.createTimeSeriesChart(this.getClass().getSimpleName(), "hh:mm:ss", "indicators", dynamicTimeSeriesCollection, true, true, false);
        final XYPlot plot = timeSeriesChart.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        ValueAxis range = plot.getRangeAxis();
        range.setAutoRange(true);

        Shape ellipse = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);

        int closeSeriesId = dynamicTimeSeriesCollection.getSeriesCount() - 1;
        plotRenderer.setSeriesShape(closeSeriesId, ellipse);
        plotRenderer.setSeriesShapesFilled(closeSeriesId, true);
        plotRenderer.setSeriesFillPaint(closeSeriesId, Color.blue);
        plotRenderer.setUseFillPaint(true);

        plot.setDataset(0, dynamicTimeSeriesCollection);
        plot.setRenderer(0, plotRenderer);
        plot.setDomainAxis(0, domain);
        plot.setRangeAxis(0, range);
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        plot.mapDatasetToDomainAxis(0, 0);
        plot.mapDatasetToRangeAxis(0, 0);

        return timeSeriesChart;
    }

    protected PlotRenderer getPlotRenderer() {
        return new PlotRenderer(true, true) {
            @Override
            public boolean getItemShapeVisible(int chartSeries, int item) {
                if (chartSeries != (seriesCount - 1)) return false;
                int itemCount = dynamicTimeSeriesCollection.getItemCount(seriesCount - 1);
                return isEnterOrExitPosition(tradingRecord, item, itemCount, series.getEndIndex());
            }

            @Override
            public Shape getItemShape(int row, int column) {
                Shape ellipse = new Ellipse2D.Double(-5.0, -5.0, 10.0, 10.0);
                Shape square = new Rectangle2D.Double(-5.0, -5.0, 10.0, 10.0);

                List<Position> positions = tradingRecord.getPositions();
                int itemCount = dynamicTimeSeriesCollection.getItemCount(seriesCount - 1);

                for (Position position : positions) {
                    int enterIndex = position.getEntry().getIndex();
                    int positionIndexOnChart = itemCount - (series.getEndIndex() - enterIndex);
                    if (column == positionIndexOnChart) return ellipse;
                }

                for (Position position : positions) {
                    int exitIndex = position.getExit().getIndex();
                    int positionIndexOnChart = itemCount - (series.getEndIndex() - exitIndex);
                    if (column == positionIndexOnChart) return square;
                }

                Position currentPosition = tradingRecord.getCurrentPosition();
                if (currentPosition.isOpened()) {
                    int enterIndex = currentPosition.getEntry().getIndex();
                    int positionIndexOnChart = itemCount - (series.getEndIndex() - enterIndex);
                    if (column == positionIndexOnChart) return ellipse;
                }

                return super.getItemShape(row, column);
            }
        };
    }
}
