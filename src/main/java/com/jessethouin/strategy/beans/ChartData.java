package com.jessethouin.strategy.beans;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChartData {
    private float open;
    private float close;
    private float high;
    private float low;
    private float volume;
}
