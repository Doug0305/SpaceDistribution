package org.core;

import gurobi.*;

import java.util.Arrays;

/**
 * @program: CellGrid.java
 * @author: Donggeng
 * @create: 2023-03-16 17:24
 */
public class Arrangement {
    OneSide[][][] oneSides;
    int maxRoom;
    Room[] roomList;
    int toiletId = -1;
    int stairId = -1;
    int classroomId = -1;
    int teachingOfficeId = -1;
    int officeId = -1;
    int spacialClassRoomId1 = -1;
    int spacialClassRoomId2 = -1;
    int assistantRoom = -1;
    int dormId = -1;


    GRBEnv env = new GRBEnv();
    GRBModel model = new GRBModel(env);
    GRBVar[][][][] vars;

    public Arrangement(BaseLineGroup baseLineGroup) throws GRBException {
        roomList = baseLineGroup.roomList.toArray(new Room[0]);
        oneSides = new OneSide[baseLineGroup.allBaseLine.length][baseLineGroup.allBaseLine[0].length][2];
        double corridorArea = 0;
        for (int i = 0; i < baseLineGroup.allBaseLine.length; i++) {
            for (int j = 0; j < baseLineGroup.allBaseLine[i].length; j++) {
                BaseLine l = baseLineGroup.allBaseLine[i][j];
                //创建每条走廊两侧容纳房间的容器
                OneSide s0 = new OneSide(l.line.getLength(), l.roomWidth01, l.sunLength01, l.silentLength01);
                OneSide s1 = new OneSide(l.line.getLength(), l.roomWidth10, l.sunLength10, l.silentLength10);
                oneSides[i][j][0] = s0;
                oneSides[i][j][1] = s1;
                corridorArea += s0.area;
                corridorArea += s1.area;
            }
        }

        for (int i = 0; i < roomList.length; i++) {
            Room room = roomList[i];
            maxRoom = Math.max(maxRoom, room.getNumber());
            //记录房间种类id，方便后续直接solve调用
            if (room.getName().equals("楼梯间")) stairId = i;
            if (room.getName().equals("卫生间")) toiletId = i;
            if (room.getName().equals("普通教室")) classroomId = i;
            if (room.getName().equals("教学办公室")) teachingOfficeId = i;
            if (room.getName().equals("其他办公室")) officeId = i;
            if (room.getName().equals("专用教室1")) spacialClassRoomId1 = i;
            if (room.getName().equals("专用教室2")) spacialClassRoomId2 = i;
            if (room.getName().equals("宿舍")) dormId = i;
            if (room.getName().equals("教学辅助用房")) assistantRoom = i;
        }

        //GRB运算
        try {
            initialGRB();
            setConstrains();
            setObjects();
        } catch (GRBException e) {
            e.printStackTrace();
        }
    }

    public void initialGRB() throws GRBException {
        env = new GRBEnv();
        model = new GRBModel(env);
        vars = new GRBVar[oneSides.length][oneSides[0].length][oneSides[0][0].length][roomList.length];
        for (int i = 0; i < oneSides.length; i++) {
            for (int j = 0; j < oneSides[i].length; j++) {
                for (int k = 0; k < oneSides[i][j].length; k++) {
                    for (int l = 0; l < roomList.length; l++) {
                        //厕所和楼梯间不能根据最大房间数限定，因其根据楼栋数和层数需要灵活调整
                        if (l == toiletId || l == stairId)
                            vars[i][j][k][l] = model.addVar(0, 9999, 0, GRB.INTEGER, i + "-" + j + "-" + k + "-" + l);
                        else
                            vars[i][j][k][l] = model.addVar(0, maxRoom, 0, GRB.INTEGER, i + "-" + j + "-" + k + "-" + l);
                    }
                }
            }
        }
    }

    private void setConstrains() throws GRBException {
        //房间总个数限制
        //公式(5-5)
        for (int n = 0; n < roomList.length; n++) {
            if (n == stairId || n == toiletId) continue;
            GRBLinExpr linExpr = new GRBLinExpr();
            for (int i = 0; i < vars.length; i++) {
                for (int j = 0; j < vars[i].length; j++) {
                    for (int k = 0; k < vars[i][j].length; k++) {
                        linExpr.addTerm(1, vars[i][j][k][n]);
                    }
                }
            }
            model.addConstr(linExpr, GRB.LESS_EQUAL, roomList[n].getNumber(), "Number");
        }

        //公式(5-6)
        // 对于第i个建筑的第j层走廊一侧的总面积Area >= 该侧房间的总面积
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                for (int k = 0; k < vars[i][j].length; k++) {
                    GRBLinExpr linExpr = new GRBLinExpr();
                    for (int l = 0; l < vars[i][j][k].length; l++) {
                        linExpr.addTerm(roomList[l].getArea(), vars[i][j][k][l]);
                    }
                    model.addConstr(linExpr, GRB.LESS_EQUAL, oneSides[i][j][k].area, "totalArea");
                }
            }
        }

        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                for (int k = 0; k < vars[i][j].length; k++) {
                    GRBLinExpr linExpr0 = new GRBLinExpr();
                    GRBLinExpr linExpr1 = new GRBLinExpr();
                    for (int l = 0; l < vars[i][j][k].length; l++) {
                        //统计日照和噪音房间个数*最小面宽
                        if (roomList[l].getSun() > 0)
                            linExpr0.addTerm(roomList[l].getMin_length(), vars[i][j][k][l]);
                        if (roomList[l].getNoise() > 0)
                            linExpr1.addTerm(roomList[l].getMin_length(), vars[i][j][k][l]);

                        GRBVar x = model.addVar(0, 1, 0, GRB.BINARY, "trans"); ////(5-7),中间变量,用来判断类型l的房间是否存在
                        GRBLinExpr temp = new GRBLinExpr();
                        GRBLinExpr linExpr = new GRBLinExpr();
                        temp.addTerm(1, vars[i][j][k][l]);
                        model.addGenConstrIndicator(x, 0, temp, GRB.EQUAL, 0, "temp");//temp=0时不存在，x为0；存在为1，x=1
                        //(5-8)房间最小宽度<=走廊宽度
                        linExpr.addTerm(roomList[l].getMin_width(), x);
                        model.addConstr(linExpr, GRB.LESS_EQUAL, oneSides[i][j][k].width, "width");
                    }
                    //(5-9/10)
                    // 需要日照/安静的房间最小长度相加 <= 该走廊的日照/安静长度
                    model.addConstr(linExpr0, GRB.LESS_EQUAL, oneSides[i][j][k].sunLength, "sun");
                    model.addConstr(linExpr1, GRB.LESS_EQUAL, oneSides[i][j][k].silentLength, "noise");
                }
            }
        }

        //(5-11/12)
        // 每一层最少楼梯间和卫生间个数为：走廊长度/（2*袋形走廊疏散距离）向上取整
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                GRBLinExpr linExpr0 = new GRBLinExpr();
                GRBLinExpr linExpr1 = new GRBLinExpr();
                for (int k = 0; k < vars[i][j].length; k++) {
                    if (toiletId >= 0)
                        linExpr0.addTerm(1, vars[i][j][k][toiletId]);
                    if (stairId >= 0)
                        linExpr1.addTerm(1, vars[i][j][k][stairId]);
                }
                model.addConstr(linExpr0, GRB.GREATER_EQUAL, oneSides[i][j][0].getStairNum(), "toiletNumMin");
                model.addConstr(linExpr1, GRB.GREATER_EQUAL, oneSides[i][j][0].getStairNum(), "stairNumMin");
            }
        }

        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                if (classroomId >= 0 && teachingOfficeId >= 0) {
                    GRBLinExpr linExpr5_14 = new GRBLinExpr();
                    //(5-13) 中间变量
                    GRBVar e = model.addVar(0, 1, 0, GRB.BINARY, "trans"); //中间变量,用来判断该侧走廊类型l的房间是否存在
                    GRBLinExpr temp = new GRBLinExpr();
                    temp.addTerm(1, vars[i][j][0][classroomId]);
                    temp.addTerm(1, vars[i][j][1][classroomId]);
                    model.addGenConstrIndicator(e, 0, temp, GRB.EQUAL, 0, "width");//temp=0时,没有普通教室，e=0；temp!=0时，e=1
                    //(5-14) 有普通教室的楼层必须有至少1个教学办公室
                    linExpr5_14.addTerm(1, vars[i][j][0][teachingOfficeId]);
                    linExpr5_14.addTerm(1, vars[i][j][1][teachingOfficeId]);
                    model.addConstr(linExpr5_14, GRB.GREATER_EQUAL, e, "classroom");
                }

                //(5-15) 每一层内，教学辅助用房数=两种专用教室数之和
                if (spacialClassRoomId1 >= 0 && assistantRoom >= 0) {
                    GRBLinExpr linExpr5_15 = new GRBLinExpr();
                    linExpr5_15.addTerm(1, vars[i][j][0][spacialClassRoomId1]);
                    linExpr5_15.addTerm(1, vars[i][j][1][spacialClassRoomId1]);
                    linExpr5_15.addTerm(1, vars[i][j][0][spacialClassRoomId2]);
                    linExpr5_15.addTerm(1, vars[i][j][1][spacialClassRoomId2]);
                    GRBLinExpr linExpr5_15R = new GRBLinExpr();
                    linExpr5_15R.addTerm(1, vars[i][j][0][assistantRoom]);
                    linExpr5_15R.addTerm(1, vars[i][j][1][assistantRoom]);
                    model.addConstr(linExpr5_15, GRB.EQUAL, linExpr5_15R, "assistant");
                }

                //(5-16/17) 每一层的楼梯数和厕所数量等（与0层保持一致）
                for (int k = 0; k < vars[i][j].length; k++) {
                    if (stairId >= 0) {
                        GRBLinExpr linExpr = new GRBLinExpr();
                        linExpr.addTerm(1, vars[i][j][k][stairId]);
                        linExpr.addTerm(-1, vars[i][0][k][stairId]);
                        model.addConstr(linExpr, GRB.EQUAL, 0, "stair");
                    }
                    if (toiletId >= 0) {
                        GRBLinExpr linExpr2 = new GRBLinExpr();
                        linExpr2.addTerm(1, vars[i][j][k][toiletId]);
                        linExpr2.addTerm(-1, vars[i][0][k][toiletId]);
                        model.addConstr(linExpr2, GRB.EQUAL, 0, "toilet");
                    }
                }
            }
        }
    }

    private void setObjects() throws GRBException {
        //(5-1)
        //数量差最小
        GRBLinExpr obj = new GRBLinExpr();
        for (int l = 0; l < roomList.length; l++) {
            if (l == stairId || l == toiletId) continue;
            obj.addConstant(roomList[l].getNumber());
            GRBLinExpr obj_ = new GRBLinExpr();
            GRBLinExpr num = new GRBLinExpr();
            for (int i = 0; i < vars.length; i++) {
                for (int j = 0; j < vars[i].length; j++) {
                    for (int k = 0; k < vars[i][j].length; k++) {
                        num.addTerm(1, vars[i][j][k][l]);
                    }
                }
            }
            //与目标的差值，越小越接近
            obj_.multAdd(-1, num);
            //差值与优先度挂钩，优先度越高，对最终结果影响越大
            obj.multAdd(roomList[l].getPriority() * 10, obj_);
        }
        model.setObjectiveN(obj, 0, 2, 1, 0, 0, "num");

        //(5-2) 普通教室尽量布置在楼栋编号小的楼栋
        // 可以通过传入房间的列表控制优先普通教室的线段
        if (classroomId >= 0) {
            GRBLinExpr linExpr = new GRBLinExpr();
            for (int i = 0; i < vars.length; i++) {
                for (int j = 0; j < vars[i].length; j++) {
                    linExpr.addTerm(i * 10 + j, vars[i][j][0][classroomId]);
                    linExpr.addTerm(i * 10 + j, vars[i][j][1][classroomId]);
                }
            }
            model.setObjectiveN(linExpr, 2, 1, 1, 0, 0, "classroom");
        }
        //5-3)宿舍尽量布置在楼栋编号大的楼栋
//        if (dormId >= 0) {
//            GRBLinExpr linExpr2 = new GRBLinExpr();
//            for (int i = 0; i < vars.length; i++) {
//                for (int j = 0; j < vars[i].length; j++) {
//                    linExpr2.addTerm(-(i * 10 + j), vars[i][j][0][dormId]);
//                    linExpr2.addTerm(-(i * 10 + j), vars[i][j][1][dormId]);
//                }
//            }
//            model.setObjectiveN(linExpr2, 3, 1, 1, 0, 0, "dorm");
//        }

        //(5-4) 控制厕所和楼梯间面积之和最小
        GRBLinExpr obj1 = new GRBLinExpr();
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                for (int k = 0; k < vars[i][j].length; k++) {
                    if (toiletId >= 0)
                        obj1.addTerm(roomList[toiletId].getArea(), vars[i][j][k][toiletId]);
                    if (stairId >= 0)
                        obj1.addTerm(roomList[stairId].getArea(), vars[i][j][k][stairId]);
                }
            }
        }
        model.setObjectiveN(obj1, 4, 1, 1, 0, 0, "toiletAndStair");
    }

    public int[][][][] solve(double timeLimit) throws GRBException {
        model.set(GRB.DoubleParam.TimeLimit, timeLimit);
        model.set(GRB.IntParam.NonConvex, 2);
        model.optimize();
        int[][][][] result = new int[vars.length][vars[0].length][vars[0][0].length][vars[0][0][0].length];
        int[] num = new int[vars[0][0][0].length];
        double areas = 0;
        for (int i = 0; i < vars.length; i++) {
            for (int j = 0; j < vars[i].length; j++) {
                for (int k = 0; k < vars[i][j].length; k++) {
                    double area = 0;
                    for (int l = 0; l < vars[i][j][k].length; l++) {
                        result[i][j][k][l] = (int) Math.round(Math.abs(vars[i][j][k][l].get(GRB.DoubleAttr.X)));
                        area += roomList[l].getArea() * result[i][j][k][l];
                        num[l] += result[i][j][k][l];
                        areas += roomList[l].getArea() * result[i][j][k][l];
                    }
                    System.out.println(i + "-" + j + "-" + k + ":" + Arrays.toString(result[i][j][k]) + "--------房间面积：" + area + "，最大容量：" + oneSides[i][j][k].area);
                }
            }
        }
        System.out.println("总共：" + Arrays.toString(num) + "---" + areas + " ㎡");
        model.dispose();
        env.dispose();
        return result;
    }
}
