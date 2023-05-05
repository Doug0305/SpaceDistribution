package org.core;

import gurobi.GRBException;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.DD;
import org.locationtech.jts.math.Vector2D;
import org.utils.JtsRender;
import org.utils.PolygonTools;
import org.utils.SunAngle;
import processing.core.PApplet;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render3D;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-22 15:58
 */
public class BaseLineGroup {
    BaseLine[][] allBaseLine; //所有的走廊-[层数][本层序号]

    List<Room> roomList; //需要布置的房间类型

    //*********参数设定区**********
    int num_floor = 5; //建筑的层数
    double precision = 8; //分析精度
    double heightPerFloor = 3; //单层层高
    double corridorWidth = 2; //走廊宽度
    double roomWidth01 = 8; //一侧房间宽度
    double roomWidth10 = 4; //另一侧房间宽度
    double latitude = 39; //当地纬度
    int month = 12; //日照模拟月份
    int day = 21; //日照模拟日期

    //*********参数设定区**********

    //*********性能计算区**********
    WB_Polygon[][] shell; //用于日照阴影计算的外表面多边形、也用来计算噪音的遮挡情况
    Map<Geometry, Integer> noiseObject; //噪音源，<噪音源物体，推荐远离距离>
    GeometryFactory gf = new GeometryFactory();

    //*********性能计算区**********

    LineSegment[] baseSegments; //传入所有的线段

    public BaseLineGroup(LineSegment[] baseSegments, int num_floor, double heightPerFloor, double corridorWidth, double roomWidth01, double roomWidth10) {
        this.num_floor = num_floor;
        this.heightPerFloor = heightPerFloor;
        this.corridorWidth = corridorWidth;
        this.roomWidth01 = roomWidth01;
        this.roomWidth10 = roomWidth10;
        this.baseSegments = baseSegments;
        initialGroup(precision);
        calculateShell();
    }

    public BaseLineGroup(LineSegment[] baseSegments, double minArea, double maxArea, double voidage, double heightPerFloor, double corridorWidth, double roomWidth01, double roomWidth10) {
        this.heightPerFloor = heightPerFloor;
        this.corridorWidth = corridorWidth;
        this.roomWidth01 = roomWidth01;
        this.roomWidth10 = roomWidth10;
        this.baseSegments = baseSegments;
        calFloor(baseSegments, minArea, maxArea, voidage);
        calculateShell();
    }

    /**
     * @Description 根据孔洞率创建建筑体量
     * @author Donggeng
     * @date 2023/2/23 15:32
     */
    private void calFloor(LineSegment[] baseSegments, double minArea, double maxArea, double voidage) {
        double area = 0;
        int floor = 0;
        List<BaseLine>[] baseLines = new List[baseSegments.length];
        for (int i = 0; i < baseLines.length; i++) {
            baseLines[i] = new ArrayList<>();
        }

        do {
            for (int i = 0; i < baseSegments.length; i++) {
                BaseLine line;
                double northEmpty = Math.random();
                double southEmpty = Math.random();
                if (northEmpty < voidage && southEmpty < voidage) {
                    line = new BaseLine(floor, baseSegments[i], corridorWidth, heightPerFloor, 0, 0, precision, i);
                } else if (northEmpty < voidage && southEmpty >= voidage) {
                    line = new BaseLine(floor, baseSegments[i], corridorWidth, heightPerFloor, 0, roomWidth10, precision, i);
                    area += baseSegments[i].getLength() * roomWidth10;
                } else if (northEmpty >= voidage && southEmpty < voidage) {
                    line = new BaseLine(floor, baseSegments[i], corridorWidth, heightPerFloor, roomWidth01, 0, precision, i);
                    area += baseSegments[i].getLength() * roomWidth01;
                } else {
                    line = new BaseLine(floor, baseSegments[i], corridorWidth, heightPerFloor, roomWidth01, roomWidth10, precision, i);
                    area += baseSegments[i].getLength() * (roomWidth01 + roomWidth10);
                }

                baseLines[i].add(line);
            }
            floor++;
        } while (area < maxArea);  //此处将建筑面积最大化，以便能够更好地进行空间分配运算
        System.out.println("生成了面积容量为" + area + "的走廊空间");
        num_floor = floor;
        allBaseLine = new BaseLine[baseSegments.length][floor];
        for (int i = 0; i < baseSegments.length; i++) {
            for (int j = 0; j < floor; j++) {
                allBaseLine[i][j] = baseLines[i].get(j);
            }
        }
    }

    /**
     * @Description 初始化基线，并传入细分精度
     * @author Donggeng
     * @date 2023/2/23 15:32
     */
    private void initialGroup(double precision) {
        allBaseLine = new BaseLine[baseSegments.length][num_floor];
        for (int i = 0; i < baseSegments.length; i++) {
            for (int j = 0; j < num_floor; j++) {
                allBaseLine[i][j] = new BaseLine(j, baseSegments[i], corridorWidth, heightPerFloor, roomWidth01, roomWidth10, precision, i);
            }
        }
    }

    /**
     * @Description 分层评估每段BaseLine，两侧房间的日照时长、噪音指数
     * @author Donggeng
     * @date 2023/2/23 15:26
     */
    public void evaluate() {
        sunAnalysis();
        noiseAnalysis();
        sortProbes();
    }

    List<Probe> orderedProbes = new ArrayList<>();

    private void sortProbes() {
        List<Probe> probes = new ArrayList<>();
        for (int i = 0; i < baseSegments.length; i++) {
            for (int j = 0; j < num_floor; j++) {
                Arrays.stream(allBaseLine[i][j].getProbes()).forEach(e -> probes.addAll(Arrays.stream(e).collect(Collectors.toList())));
            }
        }
        //多重排序，优先将日照良好的区域放置列表前端
        Comparator<Probe> bySun = (o1, o2) -> Integer.compare(o2.getSunHour(), o1.getSunHour());
        Comparator<Probe> byNoise = Comparator.comparingDouble(Probe::getNoise);
        orderedProbes = probes.stream().sorted(bySun.thenComparing(byNoise)).collect(Collectors.toList());
    }

    List<double[]>[][][] sun; //满足日照的区间，double[i][j][s]  第i栋，第j层，s边，k组[start, end]区间满足日照
    List<double[]>[][][] silent; //安静的区间，double[i][j][s] 第i栋，第j层，s边，k组[start, end]区间满足安静

    private void sunAnalysis() {
        SunAngle sunAngle = new SunAngle(latitude, month, day);
        double dayHour = sunAngle.getDayHour();
        for (double i = 12 - dayHour / 2; i <= 12 + dayHour / 2; i++) {
            //早上八点之前的太阳和下午四点之后的太阳不算
            if (i < 9 || i > 15) continue;
            double[] heightAndPositionAngle = sunAngle.calHeightAndPositionAngle(i);
            double m = Math.sin(heightAndPositionAngle[1] / 180 * Math.PI);
            double n = Math.cos(heightAndPositionAngle[1] / 180 * Math.PI);
            double p = Math.sin(heightAndPositionAngle[0] / 180 * Math.PI);
            double sunDistance = 1495978700; //日地距离149597870km，但无法取值到149597870000
            Coordinate sunPosition = new Coordinate(m * sunDistance, n * sunDistance, p * sunDistance);
            for (BaseLine[] baseSegments : allBaseLine) {
                for (BaseLine baseSegment : baseSegments) {
                    baseSegment.sun(shell, sunPosition);
                }
            }
        }

        sun = new List[allBaseLine.length][allBaseLine[0].length][2];
        for (int i = 0; i < allBaseLine.length; i++) {
            for (int j = 0; j < allBaseLine[i].length; j++) {
                for (int m = 0; m < allBaseLine[i][j].probes.length; m++) {
                    sun[i][j][m] = new ArrayList<>();
                    for (Probe probe : allBaseLine[i][j].probes[m]) {
                        if (probe.sunHour >= 2) {
                            sun[i][j][m].add(new double[]{probe.startPart, probe.endPart});
                            if (m == 0) {
                                allBaseLine[i][j].sunLength01 += probe.length;
                            }
                            if (m == 1) {
                                allBaseLine[i][j].sunLength10 += probe.length;
                            }
                        }
                    }
                }
            }
        }
    }

    public void setNoiseObject(Map<Geometry, Integer> noiseObject) {
        this.noiseObject = noiseObject;
    }

    private void noiseAnalysis() {
        if (noiseObject != null) {
            silent = new List[allBaseLine.length][allBaseLine[0].length][2];
            for (int i = 0; i < allBaseLine.length; i++) {
                for (int j = 0; j < allBaseLine[i].length; j++) {
                    allBaseLine[i][j].noise(shell, noiseObject);

                    //记录安静区间
                    for (int m = 0; m < allBaseLine[i][j].probes.length; m++) {
                        Probe[] probes_ = allBaseLine[i][j].probes[m];
                        silent[i][j][m] = new ArrayList<>();
                        for (Probe probe : probes_) {
                            if (probe.noise == 0) {
                                silent[i][j][m].add(new double[]{probe.startPart, probe.endPart});
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @Description 计算建筑包围盒Polygon, 用于射线检测
     * @author Donggeng
     * @date 2023/2/26 15:59
     */
    private void calculateShell() {
        int num = allBaseLine.length * allBaseLine[0].length;
        shell = new WB_Polygon[num][5]; //每根线段对应五个面，上、前后、左右，用于日照计算
        int id = 0;
        for (int i = 0; i < allBaseLine.length; i++) {
            for (int j = 0; j < allBaseLine[i].length; j++) {
                shell[id] = allBaseLine[i][j].getShell();
                id++;
            }
        }
    }

    public void show(PApplet app, JtsRender render, WB_Render3D wbRender) {
        for (int i = 0; i < allBaseLine.length; i++) {
            for (int j = 0; j < allBaseLine[i].length; j++) {
                allBaseLine[i][j].show(app, render, wbRender);
            }
        }

        app.fill(240, 20);
        for (WB_Polygon[] polygons : shell) {
            for (WB_Polygon polygon : polygons) {
                wbRender.drawPolygonEdges(polygon);
            }
        }
    }

    public void showLight(PApplet app, JtsRender render, WB_Render3D wbRender) {
        for (int i = 0; i < allBaseLine.length; i++) {
            for (int j = 0; j < allBaseLine[i].length; j++) {
                allBaseLine[i][j].showLight(app, render, wbRender);
            }
        }
    }

    public void showNoise(PApplet app, JtsRender render, WB_Render3D wbRender) {
        for (int i = 0; i < allBaseLine.length; i++) {
            for (int j = 0; j < allBaseLine[i].length; j++) {
                allBaseLine[i][j].showNoise(app, render, wbRender);
            }
        }

        if (noiseObject != null)
            for (Geometry geometry : noiseObject.keySet()) {
                render.drawGeometry3D(geometry);
            }
    }

    public void addOtherBuildings(Polygon[] otherBuildingShell) {
        WB_Polygon[] otherShell = new WB_Polygon[otherBuildingShell.length]; //用于日照阴影计算其他建筑多边形、也用来计算噪音的遮挡情况
        for (int i = 0; i < otherBuildingShell.length; i++) {
            otherShell[i] = PolygonTools.toWB_Polygon(otherBuildingShell[i]);
        }
        WB_Polygon[][] newShell = new WB_Polygon[shell.length + 1][];
        for (int i = 0; i < newShell.length - 1; i++) {
            newShell[i] = shell[i];
        }
        newShell[newShell.length - 1] = otherShell;
        this.shell = newShell;
    }

    int[][][][] solve;

    public void arrangeRoom(List<Room> rooms) {
        this.roomList = rooms;
        try {
            Arrangement arrangement = new Arrangement(this);
            //设置最长求解时间，单位 s
            solve = arrangement.solve(5);
        } catch (GRBException e) {
            throw new RuntimeException(e);
        }
        System.out.println("整数规划运算完毕");
    }

    public void showClassRoom(PApplet app, JtsRender render, WB_Render3D wbRender) {
        if (solve != null) {
//            app.colorMode(PConstants.HSB);
            int id = 0;
            app.textSize(1);
            app.textAlign(PApplet.LEFT, PApplet.CENTER);
            for (int i = 0; i < allBaseLine.length; i++) {
                for (int j = 0; j < allBaseLine[i].length; j++) {
                    app.pushMatrix();
                    app.rotateY(PApplet.PI);
                    app.rotateZ(PApplet.PI);
                    app.translate((float) allBaseLine[i][j].p1.x, -(float) allBaseLine[i][j].p1.y, -(float) allBaseLine[i][j].p1.z);
                    StringBuilder result2 = new StringBuilder();
                    result2.append("    ");
                    for (int k = 0; k < solve[i][j][1].length; k++) {
                        if (solve[i][j][1][k] > 0) {
                            result2.append(roomList.get(k).getName()).append(":").append(solve[i][j][1][k]).append(" ");
                        }
                    }
                    app.fill(80, 200, 90);
                    app.text(String.valueOf(result2), 0, 0);

                    StringBuilder result = new StringBuilder();
                    result.append("    ");
                    for (int k = 0; k < solve[i][j][0].length; k++) {
                        if (solve[i][j][0][k] > 0) {
                            result.append(roomList.get(k).getName()).append(":").append(solve[i][j][0][k]).append(" ");
                        }
                    }
                    app.fill(200, 80, 90);
                    app.text(String.valueOf(result), 0, -2);
                    app.popMatrix();

                    app.fill((int) solve[i][j][0][id] * 30);
                    render.drawGeometry3D(allBaseLine[i][j].room01);
                    app.fill((int) solve[i][j][1][id] * 30);
                    render.drawGeometry3D(allBaseLine[i][j].room10);
                }
            }
//            app.colorMode(PConstants.RGB);
        }
    }
}
