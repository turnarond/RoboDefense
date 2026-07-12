package com.rdefense.core.game;

/**
 * 向量查找表 — 快速计算归一化向量
 * 对应原版 VectorLookup
 */
public final class VectorLookup {

    private static final int BASE_SPEED = 1024;
    private static final int BASE_SPEED_SHIFT = 10;
    private static final int GRID_SHIFT = 4;
    private static final int GRID_SIZE = 16;
    private static int[] gridx;
    private static int[] gridy;

    /**
     * 初始化查找表（游戏启动时调用一次）
     */
    public static void init() {
        gridx = new int[256];
        gridy = new int[256];
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                float vect_x = x;
                float vect_y = y;
                float length = (float) Math.sqrt((vect_x * vect_x) + (vect_y * vect_y));
                gridx[(y << GRID_SHIFT) | x] = (int) ((vect_x / length) * BASE_SPEED);
                gridy[(y << GRID_SHIFT) | x] = (int) ((vect_y / length) * BASE_SPEED);
            }
        }
    }

    /**
     * 缩放向量到指定速度
     */
    public static void scaleVector(Vector v, int speed) {
        int x_direction = 1;
        int y_direction = 1;
        int x = v.x;
        int y = v.y;

        if (x < 0) {
            x = -x;
            x_direction = -1;
        }
        if (y < 0) {
            y = -y;
            y_direction = -1;
        }

        // 缩放到查找表范围内
        while (x >= GRID_SIZE || y >= GRID_SIZE) {
            x >>= 1;
            y >>= 1;
        }

        v.x = ((gridx[(y << GRID_SHIFT) | x] * speed) >> BASE_SPEED_SHIFT) * x_direction;
        v.y = ((gridy[(y << GRID_SHIFT) | x] * speed) >> BASE_SPEED_SHIFT) * y_direction;
    }
}
