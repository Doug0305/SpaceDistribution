package org.utils;

import java.io.Serializable;

/**
 * @program: SchoolProject
 * @author: Donggeng
 * @create: 2022-10-17 20:51
 */
public class SunAngle implements Serializable {
    //当前经纬度
    double latitude;
    //太阳直射维度
    double chiwei;
    //太阳高度角，方位角
    double heightAngle;
    double positionAngle;
    //昼长
    int dayHour;

    public static int[] months = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    public SunAngle(double latitude, int month, int day) {
        this.latitude = latitude;
        int N = day;
        for (int i = 0; i < month - 1; i++) {
            N += months[i];
        }
        chiwei = -23.44 * Math.cos(2 * 3.1415926 / 365 * (N + 10));
        System.out.println("赤纬角：" + chiwei);
        dayHour = (int) (12 + (2 * Math.asin(Math.tan(latitude / 180 * 3.1415926) * Math.tan(chiwei / 180 * 3.1415926)) / 3.1415926 * 180) / 15);
        System.out.println("昼长：" + dayHour);
    }

    public double[] calHeightAndPositionAngle(double hour) {
        double h = 15.0 * (12 - hour); //时角
        heightAngle = Math.asin(Math.cos(h / 180 * 3.1415926) * Math.cos(chiwei / 180 * 3.1415926) * Math.cos(latitude / 180 * 3.1415926) + Math.sin(chiwei / 180 * 3.1415926) * Math.sin(latitude / 180 * 3.1415926));
        positionAngle = Math.acos((Math.sin(chiwei / 180 * 3.1415926) - Math.sin(heightAngle) * Math.sin(latitude / 180 * 3.1415926)) / (Math.cos(heightAngle) * Math.cos(latitude / 180 * 3.1415926)));

        if (hour > 12)
            positionAngle *= -1;
        heightAngle = heightAngle * 180 / 3.1415926;
        positionAngle = positionAngle * 180 / 3.1415926;

        return new double[]{heightAngle, positionAngle};
    }

    public double getHeightAngle() {
        return heightAngle;
    }

    public double getPositionAngle() {
        return positionAngle;
    }

    public int getDayHour() {
        return dayHour;
    }

    public static void main(String[] args) {
        SunAngle test = new SunAngle(51,  6, 22);
        test.calHeightAndPositionAngle(12);
        System.out.println(test.getHeightAngle());
        System.out.println(test.getPositionAngle());
    }
}
