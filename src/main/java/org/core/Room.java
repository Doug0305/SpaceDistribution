package org.core;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-22 15:58
 */
@Data //lombok注解，自动生成getter/setter、构造方法、toString等实体类通用方法
public class Room {
    @ExcelProperty(value = "index")
    private int index;
    @ExcelProperty(value = "name")
    private String name;
    @ExcelProperty(value = "number")
    private int number;
    @ExcelProperty(value = "sun")
    private double sun;
    @ExcelProperty(value = "noise")
    private double noise;
    @ExcelProperty(value = "area")
    private double area;
    @ExcelProperty(value = "length")
    private double length;
    @ExcelProperty(value = "width")
    private double width;
    @ExcelProperty(value = "height")
    private double height;
    @ExcelProperty(value = "low_scale")
    private double low_scale;
    @ExcelProperty(value = "high_scale")
    private double high_scale;
    @ExcelProperty(value = "min_length")
    private double min_length;
    @ExcelProperty(value = "min_width")
    private double min_width;
    @ExcelProperty(value = "priority")
    private double priority;
}
