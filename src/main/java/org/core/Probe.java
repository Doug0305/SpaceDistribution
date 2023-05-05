package org.core;

import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.utils.JtsRender;
import processing.core.PApplet;
import processing.core.PConstants;
import wblut.geom.*;
import wblut.processing.WB_Render3D;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-23 15:37
 */
@Data
public class Probe {
    BaseLine baseLine; //探针所处的基线
    LineSegment lineSegment; //探针所代表线段
    double width; //探针所代表线段进深
    double length; //探针所代表线段长度
    double area; //探针所代表线段面积
    Coordinate position; //探针位置，位于建筑外表面，投影至lineSegment中点
    boolean is01; //是否在线段绕0点逆时针旋转90°一侧
    int sunHour = 0; //日照时长
    Map<Geometry, Integer> noiseObject; //噪音源，<噪音源物体，推荐远离距离>
    double noise; //噪音大小

    double startPart;
    double endPart;

    public Probe(BaseLine baseLine, LineSegment lineSegment, double width, Coordinate position, boolean is01, double startPart, double endPart) {
        this.baseLine = baseLine;
        this.lineSegment = lineSegment;
        this.length = lineSegment.getLength();
        this.position = position;
        this.is01 = is01;
        this.width = width;
        this.area = width * length;
        this.startPart = startPart;
        this.endPart = endPart;
    }

    public void show(PApplet app, JtsRender render, WB_Render3D wbRender) {
        if(width==0) return;
        app.pushStyle();
        app.colorMode(PConstants.HSB);
        app.fill((float) (180 - sunHour * 15), 255, 255);
        if (sunHour >= 2) app.fill(100, 255, 200);
        else app.fill(0, 255, 200);
        render.drawPoint3D(position, 0.8f);
        app.popStyle();
    }

    public void showLight(PApplet app, JtsRender render, WB_Render3D wbRender) {
        app.pushStyle();
        app.colorMode(PConstants.RGB);
        app.stroke(255, 238, 48, 30);
        for (WB_Segment wb_segment : sunlight) {
            wbRender.drawSegment(wb_segment);
        }
        app.popStyle();
    }

    public void showNoise(PApplet app, JtsRender render, WB_Render3D wbRender) {
        if (noiseObject != null) {
            app.pushStyle();
            app.stroke(0, 50);
            app.noFill();
            for (WB_Segment wb_segment : noiseLines) {
                wbRender.drawSegment(wb_segment);
            }
            app.popStyle();
        }
    }

    List<WB_Segment> sunlight = new ArrayList<>(); //能够照射到该探针的光线

    public void sun(WB_Polygon[][] shell, Coordinate sunPosition) {
        WB_Segment light = new WB_Segment(position.x, position.y, position.z, sunPosition.x, sunPosition.y, sunPosition.z);
        for (int i = 0; i < shell.length; i++) {
            for (int j = 0; j < shell[i].length; j++) {
                WB_IntersectionResult result = WB_GeometryOp.getIntersection3D(light, shell[i][j]);
                if (result.intersection) {
                    if (result.getObject().getClass().equals(WB_Point.class)) {
                        WB_Point p = (WB_Point) (result.getObject());
                        if (p.getDistance3D(new WB_Point(position.x, position.y, position.z)) > 0.01)
                            return;
                    }
                }
            }
        }
        //若都无遮挡，则日照时长+1
        sunlight.add(light);
        sunHour++;
    }

    List<WB_Segment> noiseLines = new ArrayList<>(); //能够照射到该探针的光线

    public void noise(WB_Polygon[][] shell, Map<Geometry, Integer> noiseObject) {
        if (this.noiseObject == null || !this.noiseObject.equals(noiseObject)) this.noiseObject = noiseObject;
        GeometryFactory gf = new GeometryFactory();
        OUT:
        for (Geometry geometry : noiseObject.keySet()) {
            int distance = noiseObject.get(geometry);
            Coordinate[] cs = DistanceOp.nearestPoints(gf.createPoint(position), geometry);
            WB_Segment noiseLine = new WB_Segment(cs[0].x, cs[0].y, cs[0].z, cs[1].x, cs[1].y, cs[1].z);
            if (noiseLine.getLength() < distance) {
                for (int i = 0; i < shell.length; i++) {
                    for (int j = 0; j < shell[i].length; j++) {
                        WB_IntersectionResult result = WB_GeometryOp.getIntersection3D(noiseLine, shell[i][j]);
                        if (result.intersection) {
                            if (result.getObject().getClass().equals(WB_Point.class)) {
                                WB_Point p = (WB_Point) (result.getObject());
                                if (p.getDistance3D(new WB_Point(position.x, position.y, position.z)) > 0.01)
                                    continue OUT;
                            }
                        }
                    }
                }
                noiseLines.add(noiseLine);
                noise++;
            }
        }
    }

    @Override
    public String toString() {
        return "Probe{" +
                "position=" + position +
                ", is01=" + is01 +
                ", sunHour=" + sunHour +
                ", noise=" + noise +
                '}';
    }
}