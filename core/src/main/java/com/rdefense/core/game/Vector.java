package com.rdefense.core.game;

/**
 * 向量类 — 用于计算方向和距离
 * 对应原版 Vector
 */
public final class Vector {

    public int x;
    public int y;

    /**
     * 计算向量角度（弧度转度）
     */
    public static int arctan(Vector v) {
        return arctan(-v.x, -v.y);
    }

    /**
     * 计算角度
     * @return 0-359 度的角度值
     */
    public static int arctan(int x, int y) {
        return (((int) Math.toDegrees(Math.atan2(x, -y))) + 180) % 360;
    }
}
