package com.jessethouin.strategy.charts;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.ta4j.core.BaseTradingRecord;
import org.ta4j.core.Position;

import java.util.List;

public abstract class PlotRenderer extends XYLineAndShapeRenderer {
    public PlotRenderer(boolean lines, boolean shapes) {
        super(lines, shapes);
    }

    @Override
    public abstract boolean getItemShapeVisible(int chartSeries, int item);

    public boolean isEnterOrExitPosition(BaseTradingRecord tradingRecord, int item, int itemCount, int endIndex) {
        List<Position> positions = tradingRecord.getPositions();

        for (Position position : positions) {
            int enterIndex = position.getEntry().getIndex();
            int positionIndexOnChart = itemCount - (endIndex - enterIndex);
            if (item == positionIndexOnChart) return true;
        }

        for (Position position : positions) {
            int exitIndex = position.getExit().getIndex();
            int positionIndexOnChart = itemCount - (endIndex - exitIndex);
            if (item == positionIndexOnChart) return true;
        }

        Position currentPosition = tradingRecord.getCurrentPosition();
        if (currentPosition.isOpened()) {
            int enterIndex = currentPosition.getEntry().getIndex();
            int positionIndexOnChart = itemCount - (endIndex - enterIndex);
            return item == positionIndexOnChart;
        }

        return false;
    }

}
