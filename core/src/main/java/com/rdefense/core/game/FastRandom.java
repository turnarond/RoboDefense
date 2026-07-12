package com.rdefense.core.game;

import java.util.Random;

/**
 * 快速随机数生成器 — 预生成随机数数组以提高性能
 * 对应原版 FastRandom
 */
public final class FastRandom {

    private static final int DEFAULT_ARRAY_SIZE = 256;
    private static final int DEFAULT_REFRESH_RATIO = 1;
    private static int index;
    private static int[] numbers;
    private static Random random;
    private static int refresh_trigger;

    /**
     * 使用默认参数初始化
     */
    public static void init() {
        init(DEFAULT_ARRAY_SIZE, DEFAULT_REFRESH_RATIO);
    }

    /**
     * 初始化随机数生成器
     * @param array_size 预生成数组大小
     * @param refresh_ratio 刷新频率
     */
    public static void init(int array_size, int refresh_ratio) {
        numbers = new int[array_size];
        index = 0;
        refresh_trigger = (array_size / refresh_ratio) + 1;
        random = new Random();

        for (int i = 0; i < numbers.length; i++) {
            numbers[i] = random.nextInt();
        }
    }

    /**
     * 获取下一个随机数
     */
    public static int nextInt() {
        int val = numbers[index % numbers.length];
        index++;

        // 定期刷新随机数
        if (index % refresh_trigger == 0) {
            numbers[index % numbers.length] = random.nextInt();
        }

        return val;
    }
}
