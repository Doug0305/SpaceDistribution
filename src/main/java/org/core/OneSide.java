package org.core;

import lombok.Data;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-03-19 10:39
 */
@Data
public class OneSide {
    public double length;
    public double sunLength;
    public double silentLength;
    public double area;
    public double width;
    public int stairNum;
    private int escapeDistance = 22; //袋形走廊疏散距离22m

    public OneSide(double length, double width, double sunLength, double silentLength) {
        this.length = length;
        this.sunLength = sunLength;
        this.silentLength = silentLength;
        this.width = width;
        this.area = length * width;
        this.stairNum = (int) Math.ceil(length/(escapeDistance*2));
    }
}
