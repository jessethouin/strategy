package com.jessethouin.strategy.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CCIChartData extends ChartData {
    private float longCCIIndicatorValue;
    private float plus100IndicatorValue;
    private float shortCCIIndicatorValue;
    private float minus100IndicatorValue;

    @Builder
    public CCIChartData(float open, float close, float high, float low, float volume, float longCCIIndicatorValue, float plus100IndicatorValue, float shortCCIIndicatorValue, float minus100IndicatorValue) {
        super(open, close, high, low, volume);
        this.longCCIIndicatorValue = longCCIIndicatorValue;
        this.plus100IndicatorValue = plus100IndicatorValue;
        this.shortCCIIndicatorValue = shortCCIIndicatorValue;
        this.minus100IndicatorValue = minus100IndicatorValue;
    }
}
