package com.jessethouin.strategy.beans;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ta4j.core.num.DecimalNum;

@Getter
@Setter
@Builder
public class MarketData {
    private boolean newBar;
    private DecimalNum close;

    public MarketData(boolean newBar, DecimalNum close) {
        this.newBar = newBar;
        this.close = close;
    }
}
