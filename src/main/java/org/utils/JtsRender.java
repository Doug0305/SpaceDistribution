package org.utils;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.math.Vector2D;
import org.locationtech.jts.triangulate.ConformingDelaunayTriangulationBuilder;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import processing.core.PApplet;

import java.io.Serializable;

/**
 * draw Jts Geometry
 *
 * @author Li Biao, ZHANG Bai-zhou zhangbz
 * @project shopping_mall
 * @date 2020/10/10
 * @time 15:13
 */
public class JtsRender implements Serializable {
    private static transient final GeometryFactory gf = new GeometryFactory();
    private transient final PApplet app;

    public JtsRender(PApplet app) {
        this.app = app;
    }

    /**
     * draw jts Geometry
     *
     * @param geo input geometry
     * @return void
     */
    public void drawGeometry(Geometry geo) {
        String type = geo.getGeometryType();
        switch (type) {
            case "Point":
                drawPoint(geo);
                break;
            case "LineString":
                drawLineString(geo);
                break;
            case "LinearRing":
                drawLinearRing(geo);
                break;
            case "Polygon":
                drawPolygon(geo);
                break;
            default:
                for (int i = 0; i < geo.getNumGeometries(); i++) {
                    drawGeometry(geo.getGeometryN(i));
                }
                break;
        }
    }


    /**
     * draw jts Geometry in 3D
     *
     * @param geo input geometry
     * @return void
     */
    public void drawGeometry3D(Geometry geo) {
        String type = geo.getGeometryType();
        switch (type) {
            case "Point":
                drawPoint3D((Point) geo);
                break;
            case "LineString":
                drawLineString3D((LineString) geo);
                break;
            case "Polygon":
                drawPolygon3D((Polygon) geo);
                break;
            default:
                for (int i = 0; i < geo.getNumGeometries(); i++) {
                    drawGeometry3D(geo.getGeometryN(i));
                }
                break;
        }
    }

    public void drawGeometry3D(Geometry geo, float z) {
        String type = geo.getGeometryType();
        switch (type) {
            case "Point":
                drawPoint3D((Point) geo);
                break;
            case "LineString":
                drawLineString3D((LineString) geo);
                break;
            case "Polygon":
                drawPolygon3D((Polygon) geo, z);
                break;
            default:
                for (int i = 0; i < geo.getNumGeometries(); i++) {
                    drawGeometry3D(geo.getGeometryN(i));
                }
                break;
        }
    }

    public void drawVector2D(Vector2D v) {
    }

    /**
     * draw Point as a circle
     *
     * @param geo input geometry
     * @return void
     */
    private void drawPoint(Geometry geo) {
        Point point = (Point) geo;
        app.ellipse((float) point.getX(), (float) point.getY(), 5f, 5f);
    }

    public void drawPoint(Point point, float r) {
        app.ellipse((float) point.getX(), (float) point.getY(), r, r);
    }

    public void drawPoint3D(Coordinate p, float r) {
        app.pushMatrix();
        app.translate((float) p.x, (float) p.y, (float) p.z);
        app.sphere(r);
        app.popMatrix();
    }

    public void drawPoint(Coordinate point, float r) {
        app.ellipse((float) point.x, (float) point.y, r, r);
    }

    public void drawPoint(Coordinate point, float r, float r2) {
        app.ellipse((float) point.x, (float) point.y, r, r2);
    }

    /**
     * draw 3D Point as a sphere
     *
     * @param p input Point
     */
    private void drawPoint3D(Point p) {
        app.pushMatrix();
        app.translate((float) p.getX(), (float) p.getY(), (float) p.getCoordinate().z);
        app.sphere(0.3f);
        app.popMatrix();
    }

    /**
     * draw LineString as multiple lines
     *
     * @param geo input geometry
     * @return void
     */
    private void drawLineString(Geometry geo) {
        LineString ls = (LineString) geo;
        for (int i = 0; i < ls.getCoordinates().length - 1; i++) {
            app.line((float) ls.getCoordinateN(i).x, (float) ls.getCoordinateN(i).y, (float) ls.getCoordinateN(i + 1).x, (float) ls.getCoordinateN(i + 1).y);
        }
    }

    /**
     * draw LineString as multiple lines
     *
     * @param ls input LineString
     * @return void
     */
    private void drawLineString3D(LineString ls) {
        for (int i = 0; i < ls.getCoordinates().length - 1; i++) {
            app.line(
                    (float) ls.getCoordinateN(i).x, (float) ls.getCoordinateN(i).y, (float) ls.getCoordinateN(i).z,
                    (float) ls.getCoordinateN(i + 1).x, (float) ls.getCoordinateN(i + 1).y, (float) ls.getCoordinateN(i + 1).z
            );
        }
    }

    /**
     * draw LinearRing as a closed shape
     *
     * @param geo input geometry
     * @return void
     */
    private void drawLinearRing(Geometry geo) {
        LinearRing lr = (LinearRing) geo;
        Coordinate[] vs = lr.getCoordinates();
        app.beginShape();
        for (Coordinate v : vs) {
            app.vertex((float) v.x, (float) v.y);
        }
        app.endShape(app.CLOSE);
    }

    /**
     * draw Polygon as a closed shape
     *
     * @param geo input geometry
     * @return void
     */
    private void drawPolygon(Geometry geo) {
        Polygon poly = (Polygon) geo;
        // outer boundary
        app.beginShape();
        LineString shell = poly.getExteriorRing();
        Coordinate[] coord_shell = shell.getCoordinates();
        for (Coordinate c_s : coord_shell) {
            app.vertex((float) c_s.x, (float) c_s.y);
        }
        // inner holes
        if (poly.getNumInteriorRing() > 0) {
            int interNum = poly.getNumInteriorRing();
            for (int i = 0; i < interNum; i++) {
                LineString in_poly = poly.getInteriorRingN(i);
                Coordinate[] in_coord = in_poly.getCoordinates();
                app.beginContour();
                for (int j = 0; j < in_coord.length; j++) {
                    app.vertex((float) in_coord[j].x, (float) in_coord[j].y);
                }
                app.endContour();
            }
        }
        app.endShape();
    }

    /**
     * draw Polygon as a closed shape
     *
     * @param poly input Polygon
     * @return void
     */
    public void drawPolygon3D(Polygon poly) {
        // outer boundary
        app.beginShape();
        LineString shell = poly.getExteriorRing();
        Coordinate[] coord_shell = shell.getCoordinates();
        for (Coordinate c_s : coord_shell) {
            app.vertex((float) c_s.x, (float) c_s.y, (float) c_s.z);
        }
        // inner holes
        if (poly.getNumInteriorRing() > 0) {
            int interNum = poly.getNumInteriorRing();
            for (int i = 0; i < interNum; i++) {
                LineString in_poly = poly.getInteriorRingN(i);
                Coordinate[] in_coord = in_poly.getCoordinates();
                app.beginContour();
                for (int j = 0; j < in_coord.length; j++) {
                    app.vertex((float) in_coord[j].x, (float) in_coord[j].y, (float) in_coord[j].z);
                }
                app.endContour();
            }
        }
        app.endShape();
    }

    public void drawPolygon3D(Polygon poly, float z) {
        // outer boundary
        app.beginShape();
        LineString shell = poly.getExteriorRing();
        Coordinate[] coord_shell = shell.getCoordinates();
        for (Coordinate c_s : coord_shell) {
            app.vertex((float) c_s.x, (float) c_s.y, z);
        }
        // inner holes
        if (poly.getNumInteriorRing() > 0) {
            int interNum = poly.getNumInteriorRing();
            for (int i = 0; i < interNum; i++) {
                LineString in_poly = poly.getInteriorRingN(i);
                Coordinate[] in_coord = in_poly.getCoordinates();
                app.beginContour();
                for (int j = 0; j < in_coord.length; j++) {
                    app.vertex((float) in_coord[j].x, (float) in_coord[j].y, z);
                }
                app.endContour();
            }
        }
        app.endShape();
    }


    /**
     * draw delaunay triangles
     *
     * @param delaunayBuilder
     * @return void
     */
    @Deprecated
    public void drawDelaunayTriangle(ConformingDelaunayTriangulationBuilder delaunayBuilder) {
        Geometry triangles = delaunayBuilder.getTriangles(JtsRender.gf);
        int num = triangles.getNumGeometries();
        for (int i = 0; i < num; i++) {
            this.drawGeometry(triangles.getGeometryN(i));
        }
    }

    /**
     * draw voronoi polygons
     *
     * @param voronoiBuilder
     * @return void
     */
    @Deprecated
    public void drawVoronoi(VoronoiDiagramBuilder voronoiBuilder) {
        Geometry voronois = voronoiBuilder.getDiagram(JtsRender.gf);
        int num = voronois.getNumGeometries();
        for (int i = 0; i < num; i++) {
            this.drawGeometry(voronois.getGeometryN(i));
        }
    }

    /**
     * @Description draw Segment from two point
     * @author Donggeng
     * @date 2022/5/20 16:12
     */

    public void drawLine(Coordinate p1, Coordinate p2) {
        app.line((float) p1.x, (float) p1.y, (float) p2.x, (float) p2.y);
    }
    public void drawLine3D(Coordinate p1, Coordinate p2) {
        app.line((float) p1.x, (float) p1.y,(float) p1.z,(float) p2.x, (float) p2.y,(float) p2.z);
    }
}
