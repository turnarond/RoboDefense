package com.rdefense.core.render;

/**
 * 固定时间步长游戏循环
 * 对应原版 GameState.nextState() 的 30fps 固定步长逻辑
 *
 * 设计思路：
 * - 游戏逻辑以固定 30fps 更新（每帧 33.33ms）
 * - 渲染以显示器刷新率运行（通常 60fps）
 * - 使用累加器确保逻辑帧和渲染帧正确同步
 */
public class GameLoop {

    // 固定时间步长：30fps = 33.333ms/帧
    private static final long FIXED_DELTA = 1_000_000_000L / 30; // 纳秒

    // 累加器（纳秒）
    private long accumulator = 0;

    // 上一帧时间戳（纳秒）
    private long previousTime = 0;

    // 当前状态索引（帧计数）
    private int stateIndex = 0;

    // 固定时间步长（纳秒），支持低帧率模式
    private long fixedDelta = FIXED_DELTA;

    // 快进模式：每帧执行 3 次逻辑更新
    private boolean fastFwdMode = false;

    // 性能统计
    private int frameCount = 0;
    private long lastFpsTime = 0;
    private int currentFps = 0;

    /**
     * 初始化游戏循环
     */
    public void init() {
        this.previousTime = System.nanoTime();
        this.accumulator = 0;
        this.stateIndex = 0;
    }

    /**
     * 更新游戏循环（每帧调用一次）
     * @param logicUpdater 逻辑更新回调
     * @return 是否需要渲染
     */
    public boolean tick(LogicUpdater logicUpdater) {
        long currentTime = System.nanoTime();
        long frameTime = currentTime - previousTime;
        previousTime = currentTime;

        // 限制最大帧时间，防止大跳跃后逻辑更新爆炸
        if (frameTime > 250_000_000L) { // 250ms
            frameTime = 250_000_000L;
        }

        if (fastFwdMode) {
            // 原版快进模式每帧以 3 倍速度推进逻辑状态
            frameTime *= 3;
        }

        accumulator += frameTime;

        int updates = 0;
        int maxUpdates = fastFwdMode ? 3 : 1;
        long effectiveFixedDelta = fastFwdMode ? FIXED_DELTA : fixedDelta;

        // 固定时间步长更新逻辑
        boolean shouldRender = false;
        while (accumulator >= effectiveFixedDelta && updates < maxUpdates) {
            logicUpdater.update(stateIndex);
            stateIndex++;
            accumulator -= effectiveFixedDelta;
            updates++;
            shouldRender = true;
        }

        // 计算 FPS
        frameCount++;
        long now = System.currentTimeMillis();
        if (now - lastFpsTime >= 1000) {
            currentFps = frameCount;
            frameCount = 0;
            lastFpsTime = now;
        }

        return shouldRender;
    }

    /**
     * 获取当前状态索引
     */
    public int getStateIndex() {
        return stateIndex;
    }

    /**
     * 设置状态索引（加载存档时调用）
     */
    public void setStateIndex(int index) {
        this.stateIndex = index;
    }

    /**
     * 设置快进模式
     */
    public void setFastFwdMode(boolean enabled) {
        this.fastFwdMode = enabled;
    }

    /**
     * 是否处于快进模式
     */
    public boolean isFastFwdMode() {
        return fastFwdMode;
    }

    /**
     * 获取当前 FPS
     */
    public int getCurrentFps() {
        return currentFps;
    }

    /**
     * 获取累加器剩余时间（ms）
     */
    public long getAccumulatorMs() {
        return accumulator / 1_000_000L;
    }

    /**
     * 获取固定时间步长（ms）
     */
    public long getFixedDeltaMs() {
        return (int) (fixedDelta / 1_000_000L);
    }

    public void setLowerFpsMode(boolean enabled) {
        this.fixedDelta = enabled ? (FIXED_DELTA * 2) : FIXED_DELTA;
    }

    /**
     * 逻辑更新回调接口
     */
    public interface LogicUpdater {
        /**
         * 更新游戏逻辑
         * @param stateIndex 当前状态索引
         */
        void update(int stateIndex);
    }
}
