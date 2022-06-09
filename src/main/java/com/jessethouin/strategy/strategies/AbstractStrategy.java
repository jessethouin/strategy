package com.jessethouin.strategy.strategies;

import lombok.Getter;
import org.ta4j.core.Indicator;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStrategy {
    @Getter
    final static List<Indicator<?>> indicators = new ArrayList<>();
}
