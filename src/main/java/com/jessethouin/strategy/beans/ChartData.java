package com.jessethouin.strategy.beans;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ta4j.core.num.DecimalNum;

@Getter
@Setter
@Builder
public class ChartData {
    public ChartData(DecimalNum close) {
        this.close = close;
    }

    private DecimalNum close;
}
