package com.jessethouin.strategy.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SMAChartData extends ChartData {
    private float shortMA;
    private float longMA;

    @Builder @SuppressWarnings("unused")
    public SMAChartData(float open, float close, float high, float low, float volume, float shortMA, float longMA) {
        super(open, close, high, low, volume);
        this.shortMA = shortMA;
        this.longMA = longMA;
    }
}
