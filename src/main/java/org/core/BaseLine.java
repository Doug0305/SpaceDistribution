package org.core;

import org.apache.commons.collections4.CollectionUtils;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.DD;
import org.locationtech.jts.math.Vector2D;
import org.utils.JtsRender;
import org.utils.PolygonTools;
import processing.core.PApplet;
import wblut.geom.WB_Polygon;
import wblut.processing.WB_Render3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-22 15:57
 */
public class BaseLine {
    LineSegment line; //走廊基线
    int index; //该线段在整个Group中的序号
    LineString lineString; //绘制走廊基线
    Coordinate p0; //线段起点
    Coordinate p1; //线段终点
    int floor; //走廊所处层数
    boolean isRoom01; //绕c0逆时针转90°方向一侧是否布局房间
    boolean isRoom10; //绕c1逆时针转90°方向一侧是否布局房间
    Vector2D v01;
    Vector2D v10;
    double corridorWidth; //走廊宽度
    double height; //走廊高度
    double elevation; //走廊水平标高
    double roomWidth01;
    double roomWidth10; //房间进深
    double precision; //分析精度
    double sunLength01; //01侧满足日照长度
    double sunLength10; //10侧满足日照长度
    double silentLength01; //01侧安静走廊长度
    double silentLength10; //10侧安静走廊长度
    Probe[][] probes = new Probe[2][]; //探针,01一侧占据probes[0]，10一侧占据probes[1]

    Polygon corridor; //走廊多边形
    Polygon room01; //房间多边形
    Polygon room10; //房间多边形
    WB_Polygon[] shell = new WB_Polygon[5];
    GeometryFactory gf = new GeometryFactory();

    public BaseLine(int floor, LineSegment line, double corridorWidth, double height, double roomWidth01, double roomWidth10, double precision, int index) {
        this.floor = floor;
        this.height = height;
        this.elevation = floor * height;
        this.p0 = new Coordinate(line.p0);
        this.p1 = new Coordinate(line.p1);
        this.p0.z = elevation;
        this.p1.z = elevation;
        this.line = new LineSegment(p0, p1);
        this.lineString = this.line.toGeometry(gf);
        this.precision = precision;
        this.index = index;

        this.corridorWidth = corridorWidth;
        this.roomWidth01 = roomWidth01;
        this.roomWidth10 = roomWidth10;
        silentLength01 = line.getLength();
        silentLength10 = line.getLength();
        isRoom01 = roomWidth01 > 0;
        isRoom10 = roomWidth10 > 0;
        Vector2D v = new Vector2D(p0, p1).normalize();
        v01 = v.rotate(DD.PI.doubleValue() / 2);
        v10 = v.rotate(-DD.PI.doubleValue() / 2);
        createPolygons();
        createProbes();
    }

    private void createPolygons() {
        Vector2D v0 = v01.multiply(corridorWidth / 2 + roomWidth01);
        Vector2D v1 = v01.multiply(corridorWidth / 2);
        Vector2D v2 = v10.multiply(corridorWidth / 2);
        Vector2D v3 = v10.multiply(corridorWidth / 2 + roomWidth10);
        Coordinate c0 = new Coordinate(p0.x + v0.getX(), p0.y + v0.getY(), elevation);
        Coordinate c1 = new Coordinate(p1.x + v0.getX(), p1.y + v0.getY(), elevation);
        Coordinate c2 = new Coordinate(p1.x + v1.getX(), p1.y + v1.getY(), elevation);
        Coordinate c3 = new Coordinate(p0.x + v1.getX(), p0.y + v1.getY(), elevation);
        Coordinate c4 = new Coordinate(p0.x + v2.getX(), p0.y + v2.getY(), elevation);
        Coordinate c5 = new Coordinate(p1.x + v2.getX(), p1.y + v2.getY(), elevation);
        Coordinate c6 = new Coordinate(p1.x + v3.getX(), p1.y + v3.getY(), elevation);
        Coordinate c7 = new Coordinate(p0.x + v3.getX(), p0.y + v3.getY(), elevation);
        corridor = gf.createPolygon(new Coordinate[]{c3, c2, c5, c4, c3});
        room01 = gf.createPolygon(new Coordinate[]{c0, c1, c2, c3, c0});
        room10 = gf.createPolygon(new Coordinate[]{c4, c5, c6, c7, c4});

        shell = new WB_Polygon[5]; //每根线段对应五个面，上、前后、左右，用于日照计算
        Vector2D v = new Vector2D(p0, p1).normalize();
        Vector2D v01 = v.rotate(DD.PI.doubleValue() / 2).multiply(corridorWidth / 2 + roomWidth01);
        Vector2D v10 = v.rotate(-DD.PI.doubleValue() / 2).multiply(corridorWidth / 2 + roomWidth10);
        //构建8个顶点坐标
        Coordinate cc0 = new Coordinate(p0.x + v01.getX(), p0.y + v01.getY(), elevation);
        Coordinate cc1 = new Coordinate(p1.x + v01.getX(), p1.y + v01.getY(), elevation);
        Coordinate cc2 = new Coordinate(p1.x + v10.getX(), p1.y + v10.getY(), elevation);
        Coordinate cc3 = new Coordinate(p0.x + v10.getX(), p0.y + v10.getY(), elevation);
        Coordinate cc4 = new Coordinate(p0.x + v01.getX(), p0.y + v01.getY(), elevation + height);
        Coordinate cc5 = new Coordinate(p1.x + v01.getX(), p1.y + v01.getY(), elevation + height);
        Coordinate cc6 = new Coordinate(p1.x + v10.getX(), p1.y + v10.getY(), elevation + height);
        Coordinate cc7 = new Coordinate(p0.x + v10.getX(), p0.y + v10.getY(), elevation + height);
        //构建五个面,按照日照遮挡概率，顺序为前后、上、左右
        shell[0] = PolygonTools.toWB_Polygon(gf.createPolygon(new Coordinate[]{cc0, cc1, cc5, cc4, cc0}));
        shell[1] = PolygonTools.toWB_Polygon(gf.createPolygon(new Coordinate[]{cc3, cc2, cc6, cc7, cc3}));
        shell[2] = PolygonTools.toWB_Polygon(gf.createPolygon(new Coordinate[]{cc4, cc5, cc6, cc7, cc4}));
        shell[3] = PolygonTools.toWB_Polygon(gf.createPolygon(new Coordinate[]{cc0, cc3, cc7, cc4, cc0}));
        shell[4] = PolygonTools.toWB_Polygon(gf.createPolygon(new Coordinate[]{cc1, cc2, cc6, cc5, cc1}));
    }

    private void createProbes() {
        int num = (int) Math.ceil(line.getLength() / precision);
        probes = new Probe[2][num];
        for (int i = 0; i < num; i++) {
            Coordinate c0 = line.pointAlong((double) i / (double) num);
            Coordinate c1 = line.pointAlong((double) (i + 1) / (double) num);
            LineSegment lineSegment = new LineSegment(c0, c1);
            Coordinate c = lineSegment.midPoint();
            c.z = elevation + height / 2; //探测点水平高度位于楼层中央
            Vector2D v01width = v01.multiply(roomWidth01 + corridorWidth / 2); //探测点向外移动0.1m，保证在建筑外
            Vector2D v10width = v10.multiply(roomWidth10 + corridorWidth / 2);
            Coordinate c01 = new Coordinate(c.x + v01width.getX(), c.y + v01width.getY(), c.z);
            Coordinate c10 = new Coordinate(c.x + v10width.getX(), c.y + v10width.getY(), c.z);
            probes[0][i] = new Probe(this, lineSegment, roomWidth01, c01, true, (double) i / (double) num, (double) (i + 1) / (double) num);
            probes[1][i] = new Probe(this, lineSegment, roomWidth10, c10, false, (double) i / (double) num, (double) (i + 1) / (double) num);
        }

    }

    public Probe[][] getProbes() {
        return probes;
    }

    public WB_Polygon[] getShell() {
        return shell;
    }

    public void show(PApplet app, JtsRender render, WB_Render3D wbRender) {
        app.pushStyle();
        app.stroke(200, 50, 50, 200);
        render.drawGeometry3D(lineString);
        app.noStroke();
        app.fill(200, 50, 30, 100);
        render.drawGeometry3D(corridor);
        app.fill(50, 200, 30, 100);
        render.drawGeometry3D(room01);
        app.fill(50, 50, 200, 100);
        render.drawGeometry3D(room10);

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < probes[0].length; j++) {
                probes[i][j].show(app, render, wbRender);
            }
        }
        app.popStyle();
    }

    public void showRoomName(PApplet app, JtsRender render, WB_Render3D wbRender) {
        app.pushStyle();

        app.popStyle();
    }

    public void showLight(PApplet app, JtsRender render, WB_Render3D wbRender) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < probes[0].length; j++) {
                probes[i][j].showLight(app, render, wbRender);
            }
        }
    }

    public void showNoise(PApplet app, JtsRender render, WB_Render3D wbRender) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < probes[0].length; j++) {
                probes[i][j].showNoise(app, render, wbRender);
            }
        }
    }

    public void sun(WB_Polygon[][] shell, Coordinate sunPosition) {
        for (int i = 0; i < probes.length; i++) {
            Probe[] probes_ = probes[i];
            for (Probe probe : probes_) {
                probe.sun(shell, sunPosition);
            }
        }
    }

    public void noise(WB_Polygon[][] shell, Map<Geometry, Integer> noiseObject) {
        for (int i = 0; i < probes.length; i++) {
            Probe[] probes_ = probes[i];
            for (Probe probe : probes_) {
                probe.noise(shell, noiseObject);
                if (probe.noise != 0) {
                    if (i == 0) silentLength01 -= probe.length;
                    if (i == 1) silentLength10 -= probe.length;
                }
            }
        }
    }
}
