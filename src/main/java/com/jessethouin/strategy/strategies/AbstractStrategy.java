package com.jessethouin.strategy.strategies;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.ta4j.core.Indicator;
import org.ta4j.core.Rule;
import org.ta4j.core.num.Num;
import org.ta4j.core.rules.CrossedDownIndicatorRule;
import org.ta4j.core.rules.CrossedUpIndicatorRule;
import org.ta4j.core.rules.OverIndicatorRule;
import org.ta4j.core.rules.UnderIndicatorRule;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractStrategy {
    @Getter
    final static List<Indicator<?>> indicators = new ArrayList<>();

    @NotNull
    static Rules getRules(int crossDownOscillator, int crossUpOscillator, Indicator<Num> shortEma, Indicator<Num> longEma, Indicator<Num> stochasticOscillatorK, Indicator<Num> macD, Indicator<Num> emaMacD) {
        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedDownIndicatorRule(stochasticOscillatorK, crossDownOscillator)) // Signal 1
                .and(new OverIndicatorRule(macD, emaMacD)); // Signal 2
        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
                .and(new CrossedUpIndicatorRule(stochasticOscillatorK, crossUpOscillator)) // Signal 1
                .and(new UnderIndicatorRule(macD, emaMacD)); // Signal 2
        return new Rules(entryRule, exitRule);
    }

    record Rules(Rule entryRule, Rule exitRule) {
    }
}
