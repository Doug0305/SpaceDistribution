package org.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-02-24 21:44
 * <p>
 * <dependency>
 * <groupId>com.alibaba</groupId>
 * <artifactId>easyexcel</artifactId>
 * <version>3.2.1</version>
 * </dependency>
 * <dependency>
 * <groupId>org.projectlombok</groupId>
 * <artifactId>lombok</artifactId>
 * <version>1.18.20</version>
 * </dependency>
 * <dependency>
 * <groupId>org.slf4j</groupId>
 * <artifactId>slf4j-nop</artifactId>
 * <version>1.7.32</version>
 * <type>jar</type>
 * </dependency>
 */
public class RoomLoader extends AnalysisEventListener<Room> {
    List<Room> list = new ArrayList<>();
    public double minArea,maxArea;
    public RoomLoader(String path, int n) {
        EasyExcel.read(path, Room.class, this).sheet(n).doRead();
        int num = 0;
        for (Room room : list) {
            num += room.getNumber();
            minArea += room.getArea() * room.getLow_scale() * room.getNumber();
            maxArea += room.getArea() * room.getHigh_scale() * room.getNumber();
            System.out.println(room.getName() + ": " + room.getNumber() + " 个--" + room);
        }
        System.out.println("第"+n+"个sheet中，总共读取到了" + list.size() + "种房间，共计" + num + "个");
        System.out.println("房间最少需" + minArea + "，最多需" + maxArea);
    }

    @Override
    public void invoke(Room room, AnalysisContext analysisContext) {
        list.add(room);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

    }

    public List<Room> getList() {
        return list;
    }
}
