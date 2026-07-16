package com.rdefense.core.game;

import com.rdefense.core.save.db.GameSaveManager;

/**
 * 奖励数据管理类 - 完全复刻原版 Android 版本的奖励系统
 * 包含 23 种奖励配置、动态成本递增、积分获取、效果应用等核心逻辑
 */
public final class RewardData {
    public static final int REWARD_TYPE_COUNT = 23;

    public static final int BULLET_UPGRADE = 0;
    public static final int EXPLOSIVES_UPGRADE = 1;
    public static final int ROCKET_SPEED_UPGRADE = 2;
    public static final int ANTIAIR_SPEED_UPGRADE = 3;
    public static final int ARTILLERY_SPEED_UPGRADE = 4;
    public static final int FLAME_DURATION_UPGRADE = 5;
    public static final int SLOW_DURATION_UPGRADE = 6;
    public static final int HEALTH_UPGRADE = 7;
    public static final int STARTING_CASH_UPGRADE = 8;

    public static final int TELEPORT_TOWER = 9;
    public static final int MINE_TOWER = 10;
    public static final int URANIUM_SHELLS = 11;
    public static final int NAPALM_SHELLS = 12;
    public static final int AIR_SNIPER = 13;
    public static final int CHEAP_FIREWORKS = 14;
    public static final int SLOW_BURN = 15;
    public static final int FLEA_MARKET = 16;
    public static final int SCRAMBLER = 17;
    public static final int CANNONBALL = 18;
    public static final int BONUS = 19;
    public static final int AIR_BURST = 20;
    public static final int SHOCKWAVE = 21;
    public static final int FLARE_TOWER = 22;

    private static final int TYPE_POWER = 0;
    private static final int TYPE_PERCENT_POWER = 1;
    private static final int TYPE_PERCENT_SPEED = 2;
    private static final int TYPE_PERCENT_EFFECT = 3;
    private static final int TYPE_UNLOCK = 4;

    private static final String PREFS_PREFIX = "Reward:";

    public static final long MAX_REWARD_COST = 2000000000L;
    public static final long UNLOCK_COST = 500000L;

    private static RewardProp[] reward_props;
    private static long reward_points;
    private static GameSaveManager saveManager;

    public static void init(GameSaveManager manager) {
        saveManager = manager;
        reward_points = saveManager.getRewardPoints();
        // 初始积分保持为 0（原版行为）
        reward_props = new RewardProp[REWARD_TYPE_COUNT];

        // 前 9 种：属性升级类（baseCost > 0, multiplier 不同，最大等级10）
        reward_props[BULLET_UPGRADE] = new RewardProp("Stronger Bullets", "更强力的弹药", "增加弹药威力到 %d", 50000, TYPE_POWER, 1, 10);
        reward_props[EXPLOSIVES_UPGRADE] = new RewardProp("Stronger Explosives", "更强力的炸药", "增加 火箭/导弹/迫击炮 威力到 %d%%", 75000, TYPE_PERCENT_POWER, 10, 10);
        reward_props[ROCKET_SPEED_UPGRADE] = new RewardProp("Faster Rocket Reload", "更快的火箭弹填弹速度", "重新加载火箭 %d%% 更快", 40000, TYPE_PERCENT_SPEED, 10, 10);
        reward_props[ANTIAIR_SPEED_UPGRADE] = new RewardProp("Faster Antiair Reload", "更快的防空炮填弹速度", "重新加载防空导弹 %d%% 更快", 20000, TYPE_PERCENT_SPEED, 10, 10);
        reward_props[ARTILLERY_SPEED_UPGRADE] = new RewardProp("Faster Artillery Reload", "更快的炮弹填弹速度", "重新加载迫击炮/火炮 %d%% 更快", 30000, TYPE_PERCENT_SPEED, 10, 10);
        reward_props[FLAME_DURATION_UPGRADE] = new RewardProp("Longer Flame Burn", "火焰燃烧时间增长", "增加火焰持续伤害到 %d%%", 15000, TYPE_PERCENT_EFFECT, 10, 10);
        reward_props[SLOW_DURATION_UPGRADE] = new RewardProp("Longer Slowdown", "减速时间增长", "增加缓速塔作用时间到 %d%%", 30000, TYPE_PERCENT_EFFECT, 10, 10);
        reward_props[HEALTH_UPGRADE] = new RewardProp("Health Reward", "奖励生命值", "增加基础生命值到 %d", 10000, TYPE_POWER, 1, 10);
        reward_props[STARTING_CASH_UPGRADE] = new RewardProp("Starting Cash Reward", "启动现金奖励", "增加初始金钱到 %d", 25000, TYPE_POWER, 5, 10);

        // 后 14 种：解锁类（baseCost=0, type=TYPE_UNLOCK, multiplier=1, 最大等级1）
        reward_props[TELEPORT_TOWER] = new RewardProp("Unlock Teleport Tower", "解锁传送塔", "传送塔将敌人传送回到起始位置", 0, TYPE_UNLOCK, 1, 1);
        reward_props[MINE_TOWER] = new RewardProp("Unlock Mine Tower", "解锁地雷塔", "地雷被踩到后悔引发大量的伤害", 0, TYPE_UNLOCK, 1, 1);
        reward_props[URANIUM_SHELLS] = new RewardProp("Uranium Shells", "铀弹", "最大等级机枪无视护甲", 0, TYPE_UNLOCK, 1, 1);
        reward_props[NAPALM_SHELLS] = new RewardProp("Napalm Shells", "凝固汽油弹", "炮弹攻击单位", 0, TYPE_UNLOCK, 1, 1);
        reward_props[AIR_SNIPER] = new RewardProp("Air Sniper", "空中狙击者", "升级防空炮可以增加2倍的范围", 0, TYPE_UNLOCK, 1, 1);
        reward_props[CHEAP_FIREWORKS] = new RewardProp("Cheap Fireworks", "廉价的花炮", "升级防空导弹成本更少", 0, TYPE_UNLOCK, 1, 1);
        reward_props[SLOW_BURN] = new RewardProp("Slow Burn", "缓慢燃烧", "升级火塔也可以减速敌人", 0, TYPE_UNLOCK, 1, 1);
        reward_props[FLEA_MARKET] = new RewardProp("Flea Market", "跳骚市场", "出售防御塔可以获得金钱", 0, TYPE_UNLOCK, 1, 1);
        reward_props[SCRAMBLER] = new RewardProp("Scrambler", "扰频器", "被传送的敌人受到伤害", 0, TYPE_UNLOCK, 1, 1);
        reward_props[CANNONBALL] = new RewardProp("Cannonball", "加农炮", "增加 迫击炮/火炮 伤害", 0, TYPE_UNLOCK, 1, 1);
        reward_props[BONUS] = new RewardProp("Bonus!", "奖励!", "杀敌奖励翻倍", 0, TYPE_UNLOCK, 1, 1);
        reward_props[AIR_BURST] = new RewardProp("Air Burst", "空中打击者", "地雷还可以攻击空中单位", 0, TYPE_UNLOCK, 1, 1);
        reward_props[SHOCKWAVE] = new RewardProp("Shockwave", "冲击波", "地雷链可以对所有敌人进行减速和火焰伤害", 0, TYPE_UNLOCK, 1, 1);
        reward_props[FLARE_TOWER] = new RewardProp("FLARE_TOWER_UPGRADED", "解锁火炬塔", "火炬对路过的敌人造成减速和燃烧伤害", 0, TYPE_UNLOCK, 1, 1);
    }

    public static long getRewardPoints() {
        return reward_points;
    }

    public static void addRewardPoints(long points) {
        reward_points += points;
        if (saveManager != null) {
            saveManager.setRewardPoints(reward_points);
        }
    }

    public static int getLevel(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return 0;
        return reward_props[type].level;
    }

    /**
     * 计算下一级奖励成本（动态递增）
     * 普通奖励：base_cost × 1.2^level
     * 解锁类奖励：500000 × (已解锁数量 + 1)
     */
    public static long nextLevelRewardCost(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return Long.MAX_VALUE;
        RewardProp rp = reward_props[type];

        // 解锁类奖励：500000 × (已解锁数量 + 1)
        if (rp.type == TYPE_UNLOCK) {
            if (rp.level > 0) return Long.MAX_VALUE; // 已解锁
            int unlocked_count = 0;
            for (int i = 0; i < reward_props.length; i++) {
                if (reward_props[i].type == TYPE_UNLOCK && reward_props[i].level > 0) {
                    unlocked_count++;
                }
            }
            return UNLOCK_COST * (unlocked_count + 1);
        }

        // 普通奖励：base_cost × 1.2^level
        return rp.calculateCost();
    }

    /**
     * 计算奖励因子（等级 × 乘数）
     */
    public static int rewardFactor(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return 0;
        return reward_props[type].rewardFactor();
    }

    /**
     * 生成动态描述（包含下一级数值）
     */
    public static String rewardString(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return "";
        return reward_props[type].rewardString();
    }

    /**
     * 生成等级信息字符串（如"威力 3 (+30)"）
     */
    public static String towerString(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return "";
        RewardProp rp = reward_props[type];
        int factor = rp.rewardFactor();

        switch (rp.type) {
            case TYPE_POWER:
                return "威力 " + rp.level + " (+" + factor + ")";
            case TYPE_PERCENT_POWER:
                return "威力 " + rp.level + " (+" + factor + "%)";
            case TYPE_PERCENT_SPEED:
                return "速度 " + rp.level + " (+" + factor + "%)";
            case TYPE_PERCENT_EFFECT:
                return "作用 " + rp.level + " (+" + factor + "%)";
            case TYPE_UNLOCK:
                return "";
            default:
                return "";
        }
    }

    /**
     * 检查奖励是否被锁定
     * 闪光塔需要先解锁地雷塔
     */
    public static String isBlocked(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return null;
        // 闪光塔需要先解锁地雷塔
        if (type == FLARE_TOWER) {
            if (getLevel(MINE_TOWER) == 0 && getLevel(FLARE_TOWER) == 0) {
                return "需要先解锁地雷塔";
            }
        }
        return null;
    }

    public static boolean tryUpgrade(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return false;

        // 检查是否被锁定
        if (isBlocked(type) != null) return false;

        long cost = nextLevelRewardCost(type);
        if (cost > reward_points) return false;

        reward_points -= cost;
        reward_props[type].increaseLevel();
        saveProgress();
        return true;
    }

    /**
     * 应用奖励效果到基础数值
     */
    public static int applyReward(int baseValue, int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return baseValue;
        RewardProp rp = reward_props[type];
        int factor = rp.rewardFactor();
        long result = baseValue;

        switch (rp.type) {
            case TYPE_POWER:
                result += factor;
                break;
            case TYPE_PERCENT_POWER:
            case TYPE_PERCENT_EFFECT:
                result = ((factor + 100) * result) / 100;
                break;
            case TYPE_PERCENT_SPEED:
                result = (result * 100) / (factor + 100);
                break;
            case TYPE_UNLOCK:
                result = factor;
                break;
        }

        return (int) result;
    }

    public static String getName(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return "Unknown";
        return reward_props[type].name;
    }

    public static String getDescription(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return "";
        return reward_props[type].description;
    }

    /**
     * 获取奖励等级（等价于原版 RewardData.rewardLevel）
     * @return 奖励等级，0 表示未解锁/未升级
     */
    public static int rewardLevel(int type) {
        return getLevel(type);
    }

    public static boolean isUnlockable(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return false;
        return reward_props[type].type == TYPE_UNLOCK;
    }

    public static boolean isMaxLevel(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return false;
        RewardProp rp = reward_props[type];
        return rp.level >= rp.maxLevel;
    }

    public static int getMaxLevel(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return 1;
        return reward_props[type].maxLevel;
    }

    public static boolean canAfford(int type) {
        if (type < 0 || type >= REWARD_TYPE_COUNT) return false;
        return nextLevelRewardCost(type) <= reward_points;
    }

    private static void saveProgress() {
        if (saveManager != null) {
            for (int i = 0; i < REWARD_TYPE_COUNT; i++) {
                saveManager.setPreferenceInt(PREFS_PREFIX + reward_props[i].key, reward_props[i].level);
            }
        }
    }

    /**
     * 奖励属性内部类
     */
    private static class RewardProp {
        String key;
        String name;
        String description;  // 包含 %d 占位符的动态描述模板
        int baseCost;
        int type;
        int level;
        int multiplier = 1;
        int maxLevel = 10;  // 默认最大等级
        String cached_reward_string;  // 缓存的动态描述

        RewardProp(String key, String name, String description, int baseCost, int type, int multiplier, int maxLevel) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.baseCost = baseCost;
            this.type = type;
            this.multiplier = multiplier;
            this.maxLevel = maxLevel;

            if (saveManager != null) {
                this.level = saveManager.getPreferenceInt(PREFS_PREFIX + key, 0);
            }
        }

        /**
         * 计算当前等级的成本（base_cost × 1.2^level）
         */
        long calculateCost() {
            if (type == TYPE_UNLOCK) {
                return 0; // 解锁类成本在 RewardData 中动态计算
            }
            long cost = baseCost;
            for (int i = 0; i < level; i++) {
                cost = (cost * 6) / 5;
                if (cost > MAX_REWARD_COST) return MAX_REWARD_COST;
            }
            return cost;
        }

        void increaseLevel() {
            level++;
            cached_reward_string = null; // 清除缓存
        }

        int rewardFactor() {
            return level * multiplier;
        }

        String rewardString() {
            if (cached_reward_string != null) {
                return cached_reward_string;
            }
            if (type == TYPE_UNLOCK) {
                cached_reward_string = description;
            } else {
                int value = (level + 1) * multiplier;
                cached_reward_string = String.format(description, value);
            }
            return cached_reward_string;
        }
    }
}
