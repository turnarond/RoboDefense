package com.rdefense.core.save;

/**
 * 游戏存档数据类 — JSON 格式，带版本号
 * 当前版本: 2
 *
 * 版本历史:
 * v1 - 初始版本（旧 Java 序列化格式迁移）
 * v2 - 增加混合器模式字段、累计统计数据
 */
public class GameSaveData {

    /** 存档格式版本号 */
    public int version = 2;

    // === 游戏状态 ===
    public int difficultyLevel;
    public int money;
    public int health;
    public long score;
    public int bonusMultiplier;
    public int levelNum;
    public int stateIndex;

    // === 关卡数据 ===
    public int levelType;
    public long levelSeed;
    public boolean survivalMode;
    public boolean mixerActive;
    public int mixerSeed;

    // === 关卡波次进度 ===
    public int waveIndex;
    public int waveSubIndex;
    public int waveFrameIndex;
    public int enemiesRemaining;

    // === 成就统计 ===
    public int achievementsEarned;
    public boolean noSlowTowersCreated;
    public boolean cheapskate;
    public boolean onlyOneTowerCreated;

    // === 防御塔列表 ===
    public TowerSaveData[] towers;

    // === 活跃敌人列表 ===
    public EnemySaveData[] enemies;

    // === 元数据 ===
    public long timestamp;
    public String platform;

    public static class TowerSaveData {
        public int gridx;
        public int gridy;
        public int type;
        public int shotDelay;
        public int lastDirection;
    }

    public static class EnemySaveData {
        public int gridx;
        public int gridy;
        public float xOffset;
        public float yOffset;
        public int orientation;
        public int type;
        public int health;
        public int pathNum;
        public int slowCounter;
        public int fireCounter;
        public boolean atExit;
    }
}
