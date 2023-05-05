package org.utils;

import org.locationtech.jts.algorithm.MinimumDiameter;
import org.locationtech.jts.geom.*;
import wblut.geom.*;
import wblut.hemesh.HEC_FromPolygons;
import wblut.hemesh.HES_Planar;
import wblut.hemesh.HE_Mesh;
import wblut.hemesh.HE_Vertex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: CountrySewage-FX
 * @author: Donggeng
 * @create: 2021-03-06 14:54
 */
public class PolygonTools {
    public static WB_GeometryFactory gf = WB_GeometryFactory.instance();
    public static GeometryFactory JTSgf = new GeometryFactory();
    private static final double epsilon = 0.0000001;


    public static Coordinate[] addFirst2Last(Coordinate... coords) {
        Coordinate[] cs = new Coordinate[coords.length + 1];
        int i = 0;
        for (; i < coords.length; i++) {
            cs[i] = coords[i];
        }
        cs[i] = coords[0];
        return cs;
    }

    /**
     * Polygon.getCoordinates包括首尾重合的点 ，用此方法去重
     *
     * @param coords
     * @return
     */
    public static Coordinate[] subLast(Coordinate... coords) {
        Coordinate[] cs = new Coordinate[coords.length - 1];
        int i = 0;
        for (; i < coords.length - 1; i++) {
            cs[i] = coords[i];
        }
        return cs;
    }

    public static Coordinate toJTScoord(WB_Point p) {
        return new Coordinate(p.xd(), p.yd(), p.zd());
    }

    public static Point toJTSpoint(WB_Point p) {
        Coordinate coord = toJTScoord(p);
        Point g = JTSgf.createPoint(coord);
        return g;
    }

    public static WB_Point tpWBPoint(Coordinate p) {
        return new WB_Point(p.x, p.y, 0);
    }


    public static Polygon toJTSPolygon(WB_Polygon poly) {
        Coordinate[] coord = new Coordinate[poly.getNumberOfPoints()];
        for (int i = 0; i < poly.getNumberOfPoints(); i++) {
            WB_Point p = poly.getPoint(i);
            Coordinate c = new Coordinate(p.xd(), p.yd(), p.zd());
            coord[i] = c;
        }
        LinearRing ring = JTSgf.createLinearRing(addFirst2Last(coord));
        return JTSgf.createPolygon(ring);
    }

    public static LineString toJTSPolyline(WB_Segment s) {
        Coordinate[] coord = new Coordinate[]{toJTScoord((WB_Point) s.getOrigin()), toJTScoord((WB_Point) s.getEndpoint())};
        return JTSgf.createLineString(coord);
    }

    public static LineString toJTSPolyline(WB_PolyLine poly) {
        Coordinate[] coord = new Coordinate[poly.getNumberOfPoints()];
        for (int i = 0; i < poly.getNumberOfPoints(); i++) {
            WB_Point p = poly.getPoint(i);
            Coordinate c = new Coordinate(p.xd(), p.yd(), p.zd());
            coord[i] = c;
        }
        return JTSgf.createLineString(coord);
    }

    /**
     * 将JTS_Polygon转化为WB_Polygon
     *
     * @param line JTS多段线
     * @return HeMesh多段线
     */
    public static WB_PolyLine toWB_PolyLine(LineString line) {
        Coordinate[] coords = line.getCoordinates();
        List<WB_Coord> list = new ArrayList<>();
        for (Coordinate pt : coords) {
            double z = pt.z;
            if (Double.isNaN(z))
                z = 0;
            WB_Coord coordinate = new WB_Point(pt.x, pt.y, z);
            list.add(coordinate);
        }
        return new WB_PolyLine(list);
    }

    /**
     * @param g
     * @return
     */
    public static WB_Polygon toWB_Polygon(Geometry g) {
        if (g.getGeometryType().equalsIgnoreCase("Polygon")) {
            Polygon p = (Polygon) g;
            Coordinate[] coordOut = p.getExteriorRing().getCoordinates();
            coordOut = PolygonTools.subLast(coordOut);
            WB_Point[] outPt = new WB_Point[coordOut.length];
            for (int i = 0; i < coordOut.length; i++) {
                if (Double.isNaN(coordOut[i].z)) coordOut[i].z = 0;
                outPt[i] = new WB_Point(coordOut[i].x, coordOut[i].y, coordOut[i].z);
            }
            int num = p.getNumInteriorRing();
            if (num == 0) {
                return new WB_Polygon(outPt);
            } else {
                WB_Point[][] ptsIn = new WB_Point[num][];
                for (int i = 0; i < num; i++) {
                    Coordinate[] coords = p.getInteriorRingN(i).getCoordinates();
                    /**
                     * LineString 也需sublast
                     */
                    // System.out.println(coords[0]+" &&
                    // "+coords[coords.length-1]);/
                    WB_Point[] pts = new WB_Point[coords.length];
                    for (int j = 0; j < coords.length; j++) {
                        if (Double.isNaN(coordOut[i].z)) coordOut[i].z = 0;
                        pts[j] = new WB_Point(coords[j].x, coords[j].y, coords[i].z);
                    }
                    ptsIn[i] = pts;
                }
                return new WB_Polygon(outPt, ptsIn);
            }
        } else {
            System.out.println("type is : " + g.getGeometryType());
            System.out.println("this Geometry is not a Polygon!");
            return null;
        }
    }

    /**
     * @return wblut.geom.WB_Polygon
     * @description WB_Polygon 点序反向 支持带洞
     */
    public static WB_Polygon reversePolygon(final WB_Polygon original) {
        if (original.getNumberOfHoles() == 0) {
            WB_Point[] newPoints = new WB_Point[original.getNumberOfPoints()];
            for (int i = 0; i < newPoints.length; i++) {
                newPoints[i] = original.getPoint(newPoints.length - 1 - i);
            }
            return new WB_Polygon(newPoints);
        } else {
            WB_Point[] newExteriorPoints = new WB_Point[original.getNumberOfShellPoints()];
            for (int i = 0; i < original.getNumberOfShellPoints(); i++) {
                newExteriorPoints[i] = original.getPoint(original.getNumberOfShellPoints() - 1 - i);
            }

            int[] cpt = original.getNumberOfPointsPerContour();
            int index = cpt[0];
            WB_Point[][] newInteriorPoints = new WB_Point[original.getNumberOfHoles()][];

            for (int i = 0; i < original.getNumberOfHoles(); i++) {
                WB_Point[] newHole = new WB_Point[cpt[i + 1]];
                for (int j = 0; j < newHole.length; j++) {
                    newHole[j] = new WB_Point(original.getPoint(newHole.length - 1 - j + index));
                }
                newInteriorPoints[i] = newHole;
                index = index + cpt[i + 1];
            }

            return new WB_Polygon(newExteriorPoints, newInteriorPoints);
        }
    }

    /**
     * @return wblut.geom.WB_Polygon
     * @description 让WB_Polygon法向量朝向Z轴正向  支持带洞
     */
    public static WB_Polygon polygonFaceUp(final WB_Polygon polygon) {
        if (polygon.getNormal().zd() < 0) {
            return reversePolygon(polygon);
        } else {
            return polygon;
        }
    }

    /**
     * @return wblut.geom.WB_Polygon
     * @description 让WB_Polygon法向量朝向Z轴负向  支持带洞
     */
    public static WB_Polygon polygonFaceDown(final WB_Polygon polygon) {
        if (polygon.getNormal().zd() > 0) {
            return reversePolygon(polygon);
        } else {
            return polygon;
        }
    }

    /**
     * @return wblut.geom.WB_Polygon
     * @description make first point coincide with last point
     */
    public static WB_Polygon validateWB_Polygon(final WB_Polygon polygon) {
        if (polygon.getNumberOfHoles() == 0) {
            if (polygon.getPoint(0).getDistance2D(polygon.getPoint(polygon.getNumberOfPoints() - 1)) < epsilon) {
                return polygon;
            } else {
                List<WB_Coord> points = polygon.getPoints().toList();
                points.add(polygon.getPoint(0));
                return gf.createSimplePolygon(points);
            }
        } else {
            boolean flag = true;
            List<WB_Point> exterior = new ArrayList<>();
            for (int i = 0; i < polygon.getNumberOfShellPoints(); i++) {
                exterior.add(polygon.getPoint(i));
            }
            if (exterior.get(0).getDistance2D(exterior.get(exterior.size() - 1)) >= epsilon) {
                flag = false;
                exterior.add(exterior.get(0));
            }

            WB_Point[][] interior = new WB_Point[polygon.getNumberOfHoles()][];
            int[] npc = polygon.getNumberOfPointsPerContour();
            int index = npc[0];
            for (int i = 0; i < polygon.getNumberOfHoles(); i++) {
                List<WB_Point> contour = new ArrayList<>();
                for (int j = 0; j < npc[i + 1]; j++) {
                    contour.add(polygon.getPoint(index));
                    index = index + 1;
                }
                if (contour.get(0).getDistance2D(contour.get(contour.size() - 1)) >= epsilon) {
                    flag = false;
                    contour.add(contour.get(0));
                }
                interior[i] = contour.toArray(new WB_Point[0]);
            }
            if (flag) {
                return polygon;
            } else {
                return gf.createPolygonWithHoles(exterior.toArray(new WB_Point[0]), interior);
            }
        }
    }

    /*
  构造带洞多边形
   */
    public static WB_Polygon createPolygonWithHoles(WB_Polygon polygon, List<WB_Polygon> innerPolys) {
        polygon = polygonFaceUp(polygon);
        List<WB_Coord>[] ps = new List[innerPolys.size()];
        for (int i = 0; i < innerPolys.size(); i++) {
            ps[i] = polygonFaceDown(innerPolys.get(i)).getPoints().toList();
        }
        return gf.createPolygonWithHoles(polygon.getPoints().toList(), ps);
    }

    /*
    合并相距过近的polygon,d: 距离阈值
     */
    public static List<WB_Polygon> unionClosePolygons(List<WB_Polygon> polygons, double d) {
        List<WB_Polygon> biggerPolygons = gf.createBufferedPolygons(polygons, d / 2, 0);
        List<WB_Polygon> smallerPolygons = gf.createBufferedPolygons(biggerPolygons, -d / 2, 0);
        List<WB_Polygon> polys = new ArrayList<>();
        for (WB_Polygon poly : smallerPolygons) {
            List<WB_Point> ps = new ArrayList<>();
            for (int i = 0; i < poly.getNumberOfShellPoints(); i++) {
                ps.add(poly.getPoint(i));
            }
            polys.add(new WB_Polygon(ps));
        }
        return polys;
    }


   /*
    生成WB_Polygon的集合生成各自的buffer，
     */

    public static List<WB_Polygon> createBufferedPolygons(List<WB_Polygon> polygons, double d) {
        List<WB_Polygon> newPolygons = new ArrayList<>();
        for (WB_Polygon polygon : polygons) {
            newPolygons.addAll(gf.createBufferedPolygons(polygonFaceUp(polygon), d, 0));
        }
        return newPolygons;
    }

    /*
在多个多边形包裹一个点的时候，寻找多边形上离该点最近的点
 */
    public static WB_Point getClosestPointOnPolygons(WB_Point point, List<WB_Polygon> polygons) {
        List<WB_Segment> segments = new ArrayList<>();
        for (WB_Polygon poly : polygons) {
            segments.addAll(poly.toSegments());
        }
        return getClosestPointOnSegments(point, segments);
    }

    public static WB_Point getClosestPointOnSegments(WB_Point point, List<WB_Segment> segs) {
        double min = Double.MAX_VALUE;
        WB_Point p = point;
        for (WB_Segment seg : segs) {
            if (WB_GeometryOp.getDistance3D(point, seg) < min) {
                p = WB_GeometryOp.getClosestPoint3D(seg, point);
                min = WB_GeometryOp.getDistance3D(point, seg);
            }
        }
        return p;
    }

    public static WB_Segment getClosestSegmentOfPoint(WB_Point point, List<WB_Segment> segs) {
        double min = Double.MAX_VALUE;
        WB_Segment s = null;
        for (WB_Segment seg : segs) {
            if (WB_GeometryOp.getDistance3D(point, seg) < min) {
                min = WB_GeometryOp.getDistance3D(point, seg);
                s = seg;
            }
        }
        return s;
    }

    public static WB_Polygon getClosestPolygonOfPoint(WB_Point point, List<WB_Polygon> polygons) {
        double min = Double.MAX_VALUE;
        WB_Polygon s = null;
        for (WB_Polygon seg : polygons) {
            if (WB_GeometryOp.getDistance3D(point, seg) < min) {
                min = WB_GeometryOp.getDistance3D(point, seg);
                s = seg;
            }
        }
        return s;
    }

    public static WB_Point getClosestPointOfPoints(WB_Point point, List<WB_Point> points) {
        double min = Double.MAX_VALUE;
        WB_Point p = point;
        for (int i = 0; i < points.size(); i++) {
            if (WB_GeometryOp.getDistance3D(point, points.get(i)) < min) {
                p = points.get(i);
                min = WB_GeometryOp.getDistance3D(point, points.get(i));
            }
        }
        return p;
    }

    public static double getClosetDistance(List<WB_Point> cs, WB_Point p) {
        double distance = Double.MAX_VALUE;
        if (cs.size() > 0)
            distance = WB_GeometryOp.getDistanceToPoint3D(cs.get(0), p);
        for (int i = 1; i < cs.size(); i++) {
            if (WB_GeometryOp.getDistanceToPoint3D(cs.get(i), p) < distance)
                distance = WB_GeometryOp.getDistanceToPoint3D(cs.get(i), p);
        }
        return distance;
    }

    /*
    判断多边形相交
     */
    public static boolean checkIntersection(WB_Point a, WB_Polygon polygon) {
        return toJTSPolygon(polygon).intersects(toJTSpoint(a));
    }

    public static boolean checkIntersection(WB_Polygon polygon1, WB_Polygon polygon2) {
        return toJTSPolygon(polygon1).intersects(toJTSPolygon(polygon2));
    }

    public static boolean checkIntersection(WB_PolyLine polygon1, WB_Polygon polygon2) {
        return toJTSPolyline(polygon1).intersects(toJTSPolygon(polygon2));
    }

    public static boolean checkIntersection(WB_Point a, List<WB_Polygon> polygons) {
        for (WB_Polygon polygon : polygons) {
            if (toJTSPolygon(polygon).intersects(toJTSpoint(a))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIntersection(WB_Segment s, List<WB_Polygon> polygons) {
        for (WB_Polygon polygon : polygons) {
            if (toJTSPolygon(polygon).intersects(toJTSPolyline(s))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkIntersection(WB_Segment s, WB_Polygon polygon) {
        return toJTSPolygon(polygon).intersects(toJTSPolyline(s));
    }

    public static boolean checkIntersection(WB_Segment s1, WB_Segment s2) {
        return toJTSPolyline(s2).intersects(toJTSPolyline(s1));
    }

    public static boolean checkIntersection(List<WB_Segment> ss, List<WB_Polygon> polygons) {
        for (WB_Polygon polygon : polygons) {
            for (WB_Segment s : ss) {
                if (toJTSPolygon(polygon).intersects(toJTSPolyline(s))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean checkIntersection(List<WB_Segment> ss, WB_Polygon polygon) {
        for (WB_Segment s : ss) {
            if (toJTSPolygon(polygon).intersects(toJTSPolyline(s))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkCover(List<WB_Segment> ss, WB_Polygon polygon) {
        for (WB_Segment s : ss) {
            if (!toJTSPolygon(polygon).covers(toJTSPolyline(s))) {
                return false;
            }
        }
        return true;
    }


    /*
     * aabb转wbWB_Polygon
     */
    public static WB_Polygon aabbToWBPolygon(WB_AABB2D aabb) {
        WB_Point[] ps = aabb.getCorners();
        WB_Point p = ps[2];
        ps[2] = ps[3];
        ps[3] = p;
        return gf.createSimplePolygon(ps);
    }

    /*
求多边形的最小外接矩形
 */
    public static WB_Polygon getMinimumRectangle(WB_Polygon poly) {
        Polygon toJTS = toJTSPolygon(poly);
        Polygon obbrect = (Polygon) (new MinimumDiameter(toJTS)).getMinimumRectangle();
        return toWB_Polygon(obbrect);
    }

    public static List<WB_Point> getAllPointsOfSegment(List<WB_Segment> segs) {
        List<WB_Point> points = new ArrayList<>();
        for (WB_Segment seg : segs) {
            points.add((WB_Point) seg.getOrigin());
            points.add((WB_Point) seg.getEndpoint());
        }
        return points;
    }

    public static List<WB_Segment> getAllSegmentsOfPolygons(List<WB_Polygon> polygons) {
        List<WB_Segment> segs = new ArrayList<>();
        for (WB_Polygon polygon : polygons) {
            segs.addAll(polygon.toSegments());
        }
        return segs;
    }

    public static List<WB_Segment> getAllSegmentsOfPolyLines(List<WB_PolyLine> polylines) {
        List<WB_Segment> segs = new ArrayList<>();
        for (WB_PolyLine polygon : polylines) {
            for (int i = 0; i < polygon.getNumberSegments(); i++) {
                segs.add(polygon.getSegment(i));
            }
        }
        return segs;
    }


    public static List<WB_Point> getAllPointsOfPolyline(List<WB_PolyLine> polylines) {
        List<WB_Point> points = new ArrayList<>();
        for (WB_PolyLine poly : polylines) {
            List<WB_Point> pointsForOne = new ArrayList<>();
            for (int i = 0; i < poly.getNumberOfPoints(); i++)
                pointsForOne.add(poly.getPoint(i));
            points.addAll(pointsForOne);
        }
        return points;
    }

    public static List<WB_Point> getAllPointsOfPolygon(List<WB_Polygon> polygons) {
        List<WB_Point> points = new ArrayList<>();
        for (WB_Polygon poly : polygons) {
            List<WB_Point> pointsForOne = new ArrayList<>();
            for (int i = 0; i < poly.getNumberOfPoints(); i++)
                pointsForOne.add(poly.getPoint(i));
            points.addAll(pointsForOne);
        }
        return points;
    }

    public static double getPolygonsArea(List<WB_Polygon> polygons) {
        double sum = 0;
        for (WB_Polygon wp : polygons) {
            sum += Math.abs(wp.getSignedArea());
        }
        return sum;
    }

    public static List<WB_Segment> getSegmentsWithCoord(List<WB_Segment> segments, WB_Coord c) {
        List<WB_Segment> result = new ArrayList<>();
        for (WB_Segment s : segments) {
            if (s.getOrigin().equals(c) || s.getEndpoint().equals(c))
                result.add(s);
        }
        return result;
    }

    public static List<WB_Segment> getSegmentsWithCoord2D(final List<WB_Segment> segments, WB_Coord c) {
        List<WB_Segment> result = new ArrayList<>();
        for (WB_Segment s : segments) {
            if (Math.abs(s.getOrigin().xd() - c.xd()) < epsilon && Math.abs(s.getOrigin().yd() - c.yd()) < epsilon)
                result.add(s);
            else if (Math.abs(s.getEndpoint().xd() - c.xd()) < epsilon && Math.abs(s.getEndpoint().yd() - c.yd()) < epsilon)
                result.add(s);
        }
        return result;
    }

    public static HE_Mesh polygonsToHemesh(List<WB_Polygon> polys) {
        HE_Mesh mesh;
        HEC_FromPolygons creator = new HEC_FromPolygons().setPolygons(polys);
        mesh = new HE_Mesh(creator);
        return mesh;
    }

    public static WB_Polygon getPolygonWithPoint(WB_Point4D point, List<WB_Polygon> polygons) {
        WB_Polygon polygon = null;
        for (WB_Polygon poly : polygons) {
            if (toJTSPolygon(poly).contains(PolygonTools.toJTSpoint(new WB_Point(point))))
                polygon = poly;
        }
        return polygon;
    }

    public static List<WB_PolyLine> splitPolygonByPoints(WB_Polygon polygon, List<WB_Point> points) {
        List<WB_Segment> segments = polygon.toSegments();
        for (WB_Point p : points) {
            WB_Segment s = getClosestSegmentOfPoint(p, segments);
            WB_Point closetPoint = getClosestPointOnSegments(p, segments);
            segments.remove(s);
            segments.add(new WB_Segment(s.getEndpoint(), closetPoint));
            segments.add(new WB_Segment(s.getOrigin(), closetPoint));
        }

        List<WB_PolyLine> pls = new ArrayList<>();

        for (int i = 0; i < segments.size(); i++) {
            for (int j = 0; j < segments.size(); j++) {
                if (segments.get(i).getOrigin().equals(segments.get(j).getOrigin()) ||
                        segments.get(i).getOrigin().equals(segments.get(j).getEndpoint()) ||
                        segments.get(i).getEndpoint().equals(segments.get(j).getOrigin()) ||
                        segments.get(i).getEndpoint().equals(segments.get(j).getEndpoint())) {
                }
            }
        }

        return pls;
    }

    public static List<WB_PolyLine> createPolyline(List<WB_Segment> segments) {
        List<WB_Segment> doneSegments = new ArrayList<>();
        List<WB_PolyLine> finalPolylines = new ArrayList<>();
        for (WB_Segment s1 : segments) {
            WB_PolyLine pls = new WB_PolyLine();
            doneSegments.add(s1);
            for (WB_Segment s2 : segments) {
                if (s1.getOrigin().equals(s2.getOrigin())) {
                    doneSegments.add(s2);
                    finalPolylines.add(gf.createPolyLine(new WB_Coord[]{s1.getEndpoint(), s1.getOrigin(), s2.getEndpoint()}));
                }
            }
        }
        return finalPolylines;
    }


    public static double getDistance(WB_Polygon p1, WB_Polygon p2) {
        double distance = Double.MAX_VALUE;
        if (checkIntersection(p1, p2))
            distance = 0;
        else {
            for (WB_Coord c : p1.getPoints().toList()) {
                if (WB_GeometryOp.getDistance3D(c, p2) < distance)
                    distance = WB_GeometryOp.getDistance3D(c, p2);
            }
            for (WB_Coord c : p2.getPoints().toList()) {
                if (WB_GeometryOp.getDistance3D(c, p1) < distance)
                    distance = WB_GeometryOp.getDistance3D(c, p1);
            }
        }
        return distance;
    }

    public static double getDistance(WB_PolyLine p1, WB_Polygon p2) {
        double distance = Double.MAX_VALUE;
        if (checkIntersection(p1, p2))
            distance = 0;
        else {
            for (WB_Coord c : p1.getPoints().toList()) {
                if (WB_GeometryOp.getDistance3D(c, p2) < distance)
                    distance = WB_GeometryOp.getDistance3D(c, p2);
            }
            for (WB_Coord c : p2.getPoints().toList()) {
                if (WB_GeometryOp.getDistance3D(c, p1) < distance)
                    distance = WB_GeometryOp.getDistance3D(c, p1);
            }
        }
        return distance;
    }

    public static List<WB_Point> getPointsByDistanceOfPolygon(WB_Polygon polygon, double distance) {
        List<WB_Point> ps = new ArrayList<>();
        for (int i = 0; i < polygon.getNumberSegments() + 1; i++) {
            WB_Segment s = polygon.getSegment(i);
            for (double j = 0; j < 1; j += distance / s.getLength()) {
                ps.add(s.getPointOnCurve(j));
            }
            ps.add(new WB_Point(s.getEndpoint()));
        }
        return ps;
    }

    public static List<WB_Point> getPointsByDistanceInPolygon(WB_Polygon polygon, double distance) {
        double lengthMax = Collections.max(polygon.toSegments().stream().map(WB_Segment::getLength).collect(Collectors.toList()));
        int times = (int) (Math.log(lengthMax / distance) / Math.log(2)) + 1;
        List<WB_Point> ps = new ArrayList<>();
        HEC_FromPolygons creator = new HEC_FromPolygons().setPolygons(Collections.singletonList(polygon));
        HE_Mesh mesh = new HE_Mesh(creator);
        mesh.triangulate();
        HES_Planar subdividor = new HES_Planar();
        mesh.subdivide(subdividor, times);
        for (HE_Vertex v : mesh.getVertices())
            ps.add(v.getPosition());

        return ps;


    }

    public static List<WB_Point> getPointsByDistanceOfPolyline(WB_PolyLine polygon, double distance) {
        List<WB_Point> ps = new ArrayList<>();
        for (int i = 0; i < polygon.getNumberSegments(); i++) {
            WB_Segment s = polygon.getSegment(i);
            for (double j = 0; j < 1; j += distance / s.getLength()) {
                ps.add(s.getPointOnCurve(j));
            }
            ps.add(new WB_Point(s.getEndpoint()));
        }
        return ps;
    }

    public static List<WB_Segment> breakSelf(List<WB_Segment> segments) {
        List<WB_Segment> segs = new ArrayList<>();
        List lineStrings = segments.stream().map(PolygonTools::toJTSPolyline).collect(Collectors.toList());
        Geometry nodedLineStrings = (LineString) lineStrings.get(0);
        for (int i = 1; i < lineStrings.size(); i++) {
            nodedLineStrings = nodedLineStrings.union((LineString) lineStrings.get(i));
        }
        List<WB_PolyLine> pl = new ArrayList<>();
        for (int i = 0; i < nodedLineStrings.getNumGeometries(); i++) {
            try {
                pl.add(toWB_PolyLine((LineString) nodedLineStrings.getGeometryN(i)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return getAllSegmentsOfPolyLines(pl);
    }

}
