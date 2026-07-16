package com.rdefense.core.game;

/**
 * 性能自适应监控器 — 根据帧丢失情况动态调整渲染质量
 * 对应原版 PerformanceMonitor
 *
 * <p>skip_level 范围 0-40，映射到 5 个质量档位：
 * <table>
 *   <tr><th>档位</th><th>skip_level</th><th>烟雾粒子</th><th>火焰粒子</th><th>爆炸帧率</th></tr>
 *   <tr><td>0</td><td>0-1</td><td>30</td><td>5</td><td>100%</td></tr>
 *   <tr><td>1</td><td>2-3</td><td>20</td><td>4</td><td>100%</td></tr>
 *   <tr><td>2</td><td>4-7</td><td>10</td><td>2</td><td>75%</td></tr>
 *   <tr><td>3</td><td>8-15</td><td>0</td><td>1</td><td>50%</td></tr>
 *   <tr><td>4</td><td>16-31</td><td>0</td><td>1</td><td>0%</td></tr>
 *   <tr><td>5</td><td>32-40</td><td>0</td><td>1</td><td>0%</td></tr>
 * </table>
 *
 * <p>恢复机制：帧丢失后等待 recovery_counter 帧，如果期间没有进一步丢帧，skip_level 逐渐降低。
 */
public final class PerformanceMonitor {

    private static final int GRACE_PERIOD = 15;
    private static final int INITIAL_RECOVERY_COUNTER = 15;
    private static final int MAX_RECOVERY_COUNTER = 60;
    private static final int MAX_STATE = 40;
    private static final int RECOVERY_DECREMENT = 1;
    private static final int RECOVERY_INCREMENT = 2;

    private static int skipLevel = 0;
    private static int skipIndex = 0;
    private static int recoveryInitValue = INITIAL_RECOVERY_COUNTER;
    private static int recoveryCounter = 0;
    private static int graceCounter = 0;

    private static final int[] SKIP_LEVELS = {2, 4, 8, 16, 32};
    private static final int[] SMOKE_MAX_PARTICLES = {30, 20, 10, 0, 0};
    private static final int[] MAX_FLAMELETS = {5, 4, 2, 1, 1};
    private static final int[] EXPLOSION_FRAME_RATIO = {256, 256, 192, 128, 0};

    /** 当前允许的最大烟雾粒子数 */
    public static int maxSmokeParticles() {
        return SMOKE_MAX_PARTICLES[skipIndex];
    }

    /** 当前允许的最大火焰粒子数 */
    public static int maxFlameCount() {
        return MAX_FLAMELETS[skipIndex];
    }

    /** 当前爆炸帧率（256=100%） */
    public static int explosionFrameRatio() {
        return EXPLOSION_FRAME_RATIO[skipIndex];
    }

    /** 当前跳帧级别 */
    public static int skipLevel() {
        return skipLevel;
    }

    /** 恢复计数器 */
    public static int recoveryCounter() {
        return recoveryCounter;
    }

    /** 恢复初始值 */
    public static int recoveryInitValue() {
        return recoveryInitValue;
    }

    /** 宽限计数器（丢帧后跳过N帧不检测） */
    public static int graceCounter() {
        return graceCounter;
    }

    /**
     * 每帧调用，更新性能自适应状态
     * @param frameLost 当前帧是否丢失（渲染耗时超标）
     */
    public static void nextState(boolean frameLost) {
        // 丢帧后宽限期，避免连续波动
        if (graceCounter > 0) {
            graceCounter--;
            return;
        }

        int oldSkipLevel = skipLevel;

        if (frameLost) {
            // 帧丢失：提高跳帧级别
            if (skipLevel < MAX_STATE) {
                skipLevel++;
            }
            graceCounter = GRACE_PERIOD;
            recoveryCounter = recoveryInitValue;
            // 提高恢复门槛（让系统更难恢复，避免抖动）
            if (skipLevel < MAX_STATE) {
                recoveryInitValue += RECOVERY_INCREMENT;
                if (recoveryInitValue > MAX_RECOVERY_COUNTER) {
                    recoveryInitValue = MAX_RECOVERY_COUNTER;
                }
            }
        } else if (recoveryCounter > 0) {
            // 恢复计数中
            recoveryCounter--;
        } else {
            // 恢复：降低跳帧级别
            if (skipLevel > 0) {
                skipLevel--;
            }
            recoveryCounter = recoveryInitValue;
            recoveryInitValue -= RECOVERY_DECREMENT;
            if (recoveryInitValue < INITIAL_RECOVERY_COUNTER) {
                recoveryInitValue = INITIAL_RECOVERY_COUNTER;
            }
        }

        // 更新质量档位索引
        if (skipLevel != oldSkipLevel) {
            skipIndex = 0;
            while (skipIndex < SKIP_LEVELS.length - 1 && skipLevel >= SKIP_LEVELS[skipIndex]) {
                skipIndex++;
            }
        }
    }

    /** 重置到初始状态 */
    public static void reset() {
        skipLevel = 0;
        skipIndex = 0;
        recoveryInitValue = INITIAL_RECOVERY_COUNTER;
        recoveryCounter = 0;
        graceCounter = 0;
    }
}
