package com.jessethouin.strategy.beans;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ta4j.core.num.DecimalNum;

@Getter
@Setter
@Builder
public class MarketData {
    public MarketData(DecimalNum close) {
        this.close = close;
    }

    private DecimalNum close;
}
