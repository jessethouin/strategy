package com.jessethouin.strategy.beans;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ta4j.core.num.Num;

@Getter
@Setter
@Builder
public class ChartData {
    public ChartData(Num close) {
        this.close = close;
    }

    private Num close;
}
