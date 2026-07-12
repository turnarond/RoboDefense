package com.rdefense.core.game;

/**
 * 游戏积分计算器 - 根据游戏结果计算奖励积分
 * 完全复刻原版积分获取逻辑
 */
public final class GameRewardCalculator {

    // 地图系数
    public static final int MAP_BASIC = 0;
    public static final int MAP_COURTYARD = 1;
    public static final int MAP_ICE = 2;
    public static final int MAP_LAVA = 3;
    public static final int MAP_EXTREME = 4;

    private static final float[] MAP_COEFFICIENTS = {
        1.0f,   // BASIC
        1.2f,   // COURTYARD
        1.3f,   // ICE
        1.5f,   // LAVA
        2.0f    // EXTREME
    };

    /**
     * 计算游戏奖励积分
     *
     * @param difficultyLevel 难度等级
     * @param mapId 地图ID
     * @param remainingHealth 剩余生命
     * @param maxHealth 最大生命
     * @param gameTimeMs 游戏时长（毫秒）
     * @param standardTimeMs 标准时长（毫秒）
     * @return 计算得到的积分
     */
    public static long calculateRewardPoints(
            int difficultyLevel,
            int mapId,
            int remainingHealth,
            int maxHealth,
            long gameTimeMs,
            long standardTimeMs) {

        // 基础积分 = 难度等级 × 地图系数 × 100
        float mapCoeff = getMapCoefficient(mapId);
        long basePoints = (long) (difficultyLevel * mapCoeff * 100);

        // 生命加成 = 剩余生命 / 最大生命 × 0.5
        float healthBonus = 0.0f;
        if (maxHealth > 0) {
            healthBonus = ((float) remainingHealth / maxHealth) * 0.5f;
        }

        // 效率加成 = min(1.0, 标准时间 / 实际时间) × 0.3
        float efficiencyBonus = 0.0f;
        if (gameTimeMs > 0 && standardTimeMs > 0) {
            efficiencyBonus = Math.min(1.0f, (float) standardTimeMs / gameTimeMs) * 0.3f;
        }

        // 最终积分 = 基础积分 × (1 + 生命加成 + 效率加成)
        // 注：原版公式中击杀加成已简化，主要依赖难度等级
        long finalPoints = (long) (basePoints * (1.0f + healthBonus + efficiencyBonus));

        return Math.max(0, finalPoints);
    }

    /**
     * 简化版计算（仅使用难度等级和地图）
     * 用于当前 GameState 已有数据的场景
     */
    public static long calculateSimple(int difficultyLevel, int mapId) {
        float mapCoeff = getMapCoefficient(mapId);
        return (long) (difficultyLevel * mapCoeff * 100);
    }

    private static float getMapCoefficient(int mapId) {
        if (mapId >= 0 && mapId < MAP_COEFFICIENTS.length) {
            return MAP_COEFFICIENTS[mapId];
        }
        return 1.0f;
    }
}
