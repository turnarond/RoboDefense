package com.rdefense.core.game;

import java.util.Random;

/**
 * 关卡数据生成器 — 根据种子和难度生成关卡
 * 对应原版 LevelDataGenerator，使用基于健康值的动态分组算法
 */
public final class LevelDataGenerator {

    private static final int DELAY_RANGE = 20;
    private static final int GROUPS_MASK = 3;
    private static final int HEALTH_ACCEL = 130;
    private static final int HEALTH_SWING_PCT = 10;
    private static final int HEALTH_TOUGH_PCT = 25;
    private static final int INTRA_LEVEL_DELAY = 120;
    private static final int MAX_MASK = 127;
    private static final int MAX_UNIT_COUNT = 20;
    private static int NUM_LEVELS = 100;
    private static final int QUICK_DELAY = 10;
    private static final int SLOW_DELAY = 30;
    private static final int TOUGH_MASK = 28;
    private static final int TYPE_MASK = 96;

    private static int difficulty_level;
    private static int[] level_data;
    private static int level_idx;
    private static int level_number;
    private static int max_unit_count;
    private static int num_paths;
    private static Random random;
    private static int starting_health;

    /**
     * 生成关卡数据
     * @param seed 随机种子
     * @param _difficulty_level 难度等级
     * @param num_paths2 路径数量
     * @param survival_mode 生存模式
     * @return 关卡数据数组
     */
    public static int[] generate(int seed, int _difficulty_level, int num_paths2, boolean survival_mode) {
        NUM_LEVELS = 100;
        random = new Random(seed);
        level_data = new int[256];
        level_idx = 0;
        num_paths = num_paths2;
        max_unit_count = 20;
        level_number = 1;
        difficulty_level = _difficulty_level;

        while (level_number <= NUM_LEVELS) {
            if (survival_mode) {
                difficulty_level = getSurvivalDifficultyLevel(level_number);
            }
            starting_health = (EnemyData.baseHealth(0, 1, difficulty_level) * 5) / 4;
            int health_accel = (difficulty_level * 20) + HEALTH_ACCEL;
            int l_zero = level_number - 1;
            int health = starting_health + (starting_health * l_zero) + (((l_zero * l_zero) * (health_accel / 100)) / 2);
            createLevel(adjustHealth(health));
            level_number++;
        }

        trimLevelData();
        return level_data;
    }

    /**
     * 获取生存模式难度等级
     */
    public static int getSurvivalDifficultyLevel(int level_num) {
        return (int) Math.pow(level_num, 1.5d);
    }

    /**
     * 创建关卡，尝试不同的 features 值直到成功
     */
    private static void createLevel(int health) {
        int level_idx_save = level_idx;
        int features = random.nextInt(MAX_MASK); // 0-127

        while (!tryCreateLevel(health, features)) {
            features++;
            level_idx = level_idx_save;
            if (features > 254) {
                health = (health * 9) / 10;
                if (health < starting_health) {
                    health = starting_health;
                }
                max_unit_count++;
            }
        }
    }

    /**
     * 尝试创建关卡
     * @return true 如果成功创建
     */
    private static boolean tryCreateLevel(int health, int features) {
        boolean ok = true;
        boolean flying_allowed = false;
        boolean ground_allowed = true;
        boolean first_group = true;

        // 分组数量：1-4 组
        int num_groups = (features & GROUPS_MASK) + 1;
        int health_per_group = health / num_groups;

        // 原版此分支为死代码（计算结果赋给未使用的局部变量），保真起见不生效。
        // 保留注释以说明 TOUGH_MASK 在原版即无实际作用。

        // TYPE_MASK (bit 5,6): 控制飞行单位
        if ((features & TYPE_MASK) == 0) {
            flying_allowed = true;
            ground_allowed = true;
        } else if ((features & TYPE_MASK) == TYPE_MASK) {
            flying_allowed = true;
        }

        // 每组生命值不能低于起始生命值
        if (health_per_group < starting_health) {
            ok = false;
        }

        // 尝试添加每个组
        int group_idx = 0;
        while (group_idx < num_groups) {
            if (!tryAddGroup(health_per_group, flying_allowed, ground_allowed, first_group)) {
                ok = false;
                break;
            }
            first_group = false;
            group_idx++;
        }

        // 成功后添加关卡终止符
        if (ok) {
            appendIntToLevel(0);
        }
        return ok;
    }

    /**
     * 尝试添加一个敌人组
     */
    private static boolean tryAddGroup(int health, boolean flying_allowed, boolean ground_allowed, boolean first_group) {
        int type_num = 0;
        int enemy_skip = random.nextInt(11); // 跳过 0-10 个候选类型

        // 遍历候选类型，找到第一个有效的
        while (type_num < 121) {
            if (unitTypeValid(type_num % 11, health, flying_allowed, ground_allowed)) {
                if (enemy_skip <= 0) {
                    break;
                }
                enemy_skip--;
            }
            type_num++;
        }

        boolean ok = type_num < 121;
        if (ok) {
            addGroup(type_num % 11, health, first_group);
        }
        return ok;
    }

    /**
     * 添加一个敌人组到关卡数据
     * 事件编码格式（匹配原版 LevelData.nextState() 解析）：
     *   bits 10-14: 敌人类型 (unit_type)
     *   bits 3-9:   敌人数量 (unit_count)
     *   bits 24-30: 波次间隔 (unit_delay)
     *   bits 15-23: 首波间隔（仅首个事件）
     *   bits 0-2:   路径号 (path_num)
     */
    private static void addGroup(int type, int health, boolean first_group) {
        int initial_delay;
        if (first_group) {
            initial_delay = INTRA_LEVEL_DELAY; // 120
        } else {
            initial_delay = random.nextInt(DELAY_RANGE) + QUICK_DELAY; // 10-29
        }

        // 第一个关卡的首波延迟固定为 240
        if (level_idx == 0) {
            initial_delay = 240;
        }

        int inner_delay = random.nextInt(DELAY_RANGE) + QUICK_DELAY; // 10-29
        int count = getUnitCount(health, type);
        int path_num = num_paths > 1 ? random.nextInt(num_paths) : 0;

        appendIntToLevel((inner_delay << 24) | (initial_delay << 15) | (type << 10) | (count << 3) | path_num);
    }

    /**
     * 计算敌人数量
     */
    private static int getUnitCount(int health, int type) {
        int base_health = EnemyData.baseHealth(type, level_number, difficulty_level);
        return ((base_health >> 1) + health) / base_health;
    }

    /**
     * 检查敌人类型是否有效
     */
    private static boolean unitTypeValid(int type, int health, boolean flying_allowed, boolean ground_allowed) {
        if (level_number < EnemyData.firstLevel(type)) {
            return false;
        }
        if (difficulty_level < EnemyData.minDifficulty(type)) {
            return false;
        }
        int unit_count = getUnitCount(health, type);
        if (unit_count == 0 || unit_count > max_unit_count) {
            return false;
        }
        if (flying_allowed || !EnemyData.isFlyer(type)) {
            return ground_allowed || EnemyData.isFlyer(type);
        }
        return false;
    }

    /**
     * 调整健康值（±10% 随机浮动）
     */
    private static int adjustHealth(int health) {
        int number = random.nextInt(0x7FFFFFFF); // ViewCompat.MEASURED_SIZE_MASK
        boolean increase = (number & 1) == 1;
        int number2 = number >> 1;
        int max_swing = ((health * HEALTH_SWING_PCT) / 100) + 1;
        return increase ? health + (number2 % max_swing) : health - (number2 % max_swing);
    }

    /**
     * 追加整数到关卡数据
     */
    private static void appendIntToLevel(int val) {
        if (level_idx == level_data.length) {
            int[] new_data = new int[level_data.length * 2];
            System.arraycopy(level_data, 0, new_data, 0, level_data.length);
            level_data = new_data;
        }
        level_data[level_idx] = val;
        level_idx++;
    }

    /**
     * 裁剪关卡数据到实际大小
     */
    private static void trimLevelData() {
        int[] new_data = new int[level_idx];
        System.arraycopy(level_data, 0, new_data, 0, new_data.length);
        level_data = new_data;
    }
}
