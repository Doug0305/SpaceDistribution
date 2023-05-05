package org.example;

import guo_cam.CameraController;
import org.core.BaseLineGroup;
import org.core.Room;
import org.core.RoomLoader;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.utils.JtsRender;
import processing.core.PApplet;
import processing.core.PFont;
import wblut.processing.WB_Render3D;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-22 21:08
 */
public class Test01 extends PApplet {
    public static void main(String[] args) {
        PApplet.main("org.example.Test01");
    }

    CameraController cam;
    LineSegment[] baseline;
    BaseLineGroup baseLineGroup;

    public void settings() {
        size(800, 600, P3D);
    }

    JtsRender render;
    WB_Render3D wbRender;

    public void setup() {
        cam = new CameraController(this, 50);
        render = new JtsRender(this);
        wbRender = new WB_Render3D(this);
        PFont font = createFont("src/main/resources/黑体简.ttf", 20);
        textFont(font);

        String path = "src/main/java/org/core/test2.xlsx";
        RoomLoader loader = new RoomLoader(path, 0);
        List<Room> list1 = loader.getList();
        //建筑线段自定义初始化
        Coordinate c0 = new Coordinate(0, 0, 0);
        Coordinate c1 = new Coordinate(60, 0, 0);
        Coordinate c2 = new Coordinate(60, 80, 0);
        Coordinate c3 = new Coordinate(-10, 60, 0);
        Coordinate c4 = new Coordinate(-30, 0, 0);
        Coordinate c5 = new Coordinate(4, -30, 0);
        Coordinate c6 = new Coordinate(60, -40, 0);
        Coordinate c7 = new Coordinate(90, 0, 0);
        Coordinate c8 = new Coordinate(90, 90, 0);
        Coordinate c9 = new Coordinate(60, 120, 0);
        Coordinate c10 = new Coordinate(-20, 100, 0);
        Coordinate c11 = new Coordinate(-40, 60, 0);
        baseline = new LineSegment[12];
        baseline[0] = new LineSegment(c0,c1);
        baseline[1] = new LineSegment(c1,c2);
        baseline[2] = new LineSegment(c2,c3);
        baseline[3] = new LineSegment(c3,c0);
        baseline[4] = new LineSegment(c0,c4);
        baseline[5] = new LineSegment(c0,c5);
        baseline[6] = new LineSegment(c1,c6);
        baseline[7] = new LineSegment(c1,c7);
        baseline[8] = new LineSegment(c2,c8);
        baseline[9] = new LineSegment(c2,c9);
        baseline[10] = new LineSegment(c3,c10);
        baseline[11] = new LineSegment(c3,c11);

        baseLineGroup = new BaseLineGroup(baseline, loader.minArea, loader.maxArea, 0.3, 4, 2, 8, 8);

        //设置噪音源及推荐远离距离
        Map<Geometry, Integer> noiseObject = new HashMap<>();
        noiseObject.put(new GeometryFactory().createPoint(new Coordinate(-30, -20, 0)), 50);
        noiseObject.put(new GeometryFactory().createPoint(new Coordinate(-120, 150, 0)), 150);
//        baseLineGroup.setNoiseObject(noiseObject);

        //评估
        baseLineGroup.evaluate();
        //塞房间
        baseLineGroup.arrangeRoom(list1);
    }

    boolean showLight = false;
    boolean showNoise = false;
    boolean showBuilding = true;

    public void draw() {
        background(255);
        cam.begin3d();
        cam.drawSystem(100);
        if (frameCount % 30 == 0) {
            thread("post");
        }

        if (showBuilding)
            baseLineGroup.show(this, render, wbRender);
        else
            baseLineGroup.showClassRoom(this, render, wbRender);
        if (showLight) baseLineGroup.showLight(this, render, wbRender);
        if (showNoise) baseLineGroup.showNoise(this, render, wbRender);

        cam.begin2d();
        fill(0);
        textSize(15);
        textMode(5);
        text(time, 10, 30);
    }

    String time = "";

    public void post() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        time = formatter.format(new Date(System.currentTimeMillis()));
    }
    @Override
    public void keyReleased() {
        if (key == 's' || key == 'S') showLight = !showLight;
        if (key == 'a' || key == 'A') showNoise = !showNoise;
        if (key == 'd' || key == 'D') showBuilding = !showBuilding;
    }
}
