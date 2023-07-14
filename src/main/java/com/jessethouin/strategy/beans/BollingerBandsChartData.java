package com.jessethouin.strategy.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BollingerBandsChartData extends ChartData {
    private float upperIndicatorValue;
    private float middleIndicatorValue;
    private float lowerIndicatorValue;

    @Builder @SuppressWarnings("unused")
    public BollingerBandsChartData(float open, float close, float high, float low, float volume, float upperIndicatorValue, float middleIndicatorValue, float lowerIndicatorValue) {
        super(open, close, high, low, volume);
        this.upperIndicatorValue = upperIndicatorValue;
        this.middleIndicatorValue = middleIndicatorValue;
        this.lowerIndicatorValue = lowerIndicatorValue;
    }
}
