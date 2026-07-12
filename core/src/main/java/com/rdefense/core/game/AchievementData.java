package com.rdefense.core.game;

import com.rdefense.core.save.db.GameSaveManager;

import java.util.ArrayList;
import java.util.List;

public final class AchievementData {
    public static final int ACHIEVEMENT_TYPE_COUNT = 88;

    // 成就分类枚举
    public enum AchievementCategory {
        DIFFICULTY("难度成就", 0),
        MAP("地图成就", 1),
        PERFECT("完美成就", 2),
        SCORE("积分成就", 3),
        SPECIAL("特殊成就", 4),
        SURVIVAL("生存成就", 5),
        VR("训练成就", 6);

        public final String label;
        public final int iconIndex;

        AchievementCategory(String label, int iconIndex) {
            this.label = label;
            this.iconIndex = iconIndex;
        }
    }
    private static final String PREFS_PREFIX = "ADAchievement:";

    public static final int COMPLETE_DIFFICULTY_0 = 0;
    public static final int COMPLETE_DIFFICULTY_2 = 1;
    public static final int COMPLETE_DIFFICULTY_5 = 2;
    public static final int COMPLETE_DIFFICULTY_8 = 3;
    public static final int COMPLETE_DIFFICULTY_11 = 4;
    public static final int COMPLETE_DIFFICULTY_14 = 5;
    public static final int COMPLETE_DIFFICULTY_17 = 6;
    public static final int COMPLETE_DIFFICULTY_20 = 7;
    public static final int PERFECT_25 = 8;
    public static final int PERFECT_50 = 9;
    public static final int PERFECT_100 = 10;
    public static final int FAST_PACED = 11;
    public static final int ROCKETMAN = 12;
    public static final int GUNNER = 13;
    public static final int CHEAPSKATE = 14;
    public static final int BIG_SPENDER = 15;
    public static final int TWENTY_FIVE_K = 16;
    public static final int BIG_SCORE = 17;
    public static final int HUGE_SCORE = 18;
    public static final int EXPERIENCED = 19;
    public static final int POWERED_UP = 20;
    public static final int NO_SALE = 21;
    public static final int FAST_LANE = 22;
    public static final int ADDICT = 23;
    public static final int SEVENTY_FIVE_K = 24;
    public static final int RISK_TAKER = 25;
    public static final int SOLID_EFFORT = 26;
    public static final int RAINY_DAY = 27;
    public static final int BIG_SAVER = 28;
    public static final int CRAZY_SAVER = 29;
    public static final int BIG_ONE = 30;
    public static final int WHOPPER = 31;
    public static final int GOOD_GAME = 32;
    public static final int GREAT_GAME = 33;
    public static final int AMAZING_GAME = 34;
    public static final int BASIC_10 = 35;
    public static final int BASIC_25 = 36;
    public static final int BASIC_LVL_40 = 37;
    public static final int BASIC_LVL_60 = 38;
    public static final int RUINS_10 = 39;
    public static final int RUINS_25 = 40;
    public static final int RUINS_LVL_30 = 41;
    public static final int RUINS_LVL_50 = 42;
    public static final int FACTORY_10 = 43;
    public static final int FACTORY_25 = 44;
    public static final int FACTORY_LVL_20 = 45;
    public static final int FACTORY_LVL_40 = 46;
    public static final int THIRTY_THIRTY = 47;
    public static final int COURTYARD_10 = 48;
    public static final int COURTYARD_25 = 49;
    public static final int COURTYARD_LVL_15 = 50;
    public static final int COURTYARD_LVL_25 = 51;
    public static final int MIXER_A = 52;
    public static final int MIXER_B = 53;
    public static final int MIXER_C = 54;
    public static final int MIXER_D = 55;
    public static final int DEFEAT_TITAN = 56;
    public static final int CHAIN_SMOKER = 57;
    public static final int PYRO = 58;
    public static final int COMPLETE_DIFFICULTY_40 = 59;
    public static final int COMPLETE_DIFFICULTY_60 = 60;
    public static final int COMPLETE_DIFFICULTY_80 = 61;
    public static final int COMPLETE_DIFFICULTY_100 = 62;
    public static final int SUPER_POWERED = 63;
    public static final int BASIC_LVL_80 = 64;
    public static final int BASIC_LVL_100 = 65;
    public static final int RUINS_LVL_70 = 66;
    public static final int RUINS_LVL_100 = 67;
    public static final int FACTORY_LVL_70 = 68;
    public static final int FACTORY_LVL_100 = 69;
    public static final int COURTYARD_LVL_50 = 70;
    public static final int COURTYARD_LVL_100 = 71;
    public static final int ULTRA_GAME = 72;
    public static final int SURVIVAL_10 = 73;
    public static final int SURVIVAL_20 = 74;
    public static final int SURVIVAL_30 = 75;
    public static final int SURVIVAL_40 = 76;
    public static final int SURVIVAL_50 = 77;
    public static final int SURVIVAL_60 = 78;
    public static final int SURVIVAL_70 = 79;
    public static final int SURVIVAL_80 = 80;
    public static final int SURVIVAL_90 = 81;
    public static final int SURVIVAL_100 = 82;
    public static final int ROADWAY_LVL_10 = 83;
    public static final int ROADWAY_LVL_50 = 84;
    public static final int ROADWAY_LVL_100 = 85;
    public static final int MIXER_E = 86;
    public static final int SHOW_OFF = 87;

    private static AchievementProp[] achievement_props;
    private static int[] new_achievements;
    private static int new_head;
    private static int new_tail;
    private static int total_count;
    private static GameSaveManager saveManager;

    static {
        total_count = 0;
        achievement_props = new AchievementProp[ACHIEVEMENT_TYPE_COUNT];
        new_achievements = new int[ACHIEVEMENT_TYPE_COUNT];
        new_tail = 0;
        new_head = 0;
    }

    public static void init(GameSaveManager manager) {
        saveManager = manager;
        total_count = 0;
        achievement_props = new AchievementProp[ACHIEVEMENT_TYPE_COUNT];
        new_achievements = new int[ACHIEVEMENT_TYPE_COUNT];
        new_tail = 0;
        new_head = 0;

        // 难度成就
        achievement_props[COMPLETE_DIFFICULTY_0] = new AchievementProp(
            "Complete Any Map", "通关任意地图", "通关任意地图",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_2] = new AchievementProp(
            "Complete Difficulty 2", "通关难度 2", "通关一个地图难度等级达到 2 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_5] = new AchievementProp(
            "Complete Difficulty 5", "通关难度 5", "通关一个地图难度等级达到 5 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_8] = new AchievementProp(
            "Complete Difficulty 8", "通关难度 8", "通关一个地图难度等级达到 8 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_11] = new AchievementProp(
            "Complete Difficulty 11", "通关难度 11", "通关一个地图难度等级达到 11 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_14] = new AchievementProp(
            "Complete Difficulty 14", "通关难度 14", "通关一个地图难度等级达到 14 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_17] = new AchievementProp(
            "Complete Difficulty 17", "通关难度 17", "通关一个地图难度等级达到 17 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_20] = new AchievementProp(
            "Complete Difficulty 20", "通关难度 20", "通关一个地图难度等级达到 20",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_40] = new AchievementProp(
            "Complete Difficulty 40", "通关难度 40", "通关一个地图难度等级达到 40 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_60] = new AchievementProp(
            "Complete Difficulty 60", "通关难度 60", "通关一个地图难度等级达到 60 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_80] = new AchievementProp(
            "Complete Difficulty 80", "通关难度 80", "通关一个地图难度等级达到 80 或更大",
            AchievementCategory.DIFFICULTY, 1);
        achievement_props[COMPLETE_DIFFICULTY_100] = new AchievementProp(
            "Complete Difficulty 100", "通关难度 100", "通关一个地图难度等级达到 100 或更大",
            AchievementCategory.DIFFICULTY, 1);

        // 完美成就
        achievement_props[PERFECT_25] = new AchievementProp(
            "Perfect 25", "完美通关 25 次", "在一个地图前25关不损失生命值",
            AchievementCategory.PERFECT, 1);
        achievement_props[PERFECT_50] = new AchievementProp(
            "Perfect 50", "完美通关 50 次", "在一个地图前50关不损失生命值",
            AchievementCategory.PERFECT, 1);
        achievement_props[PERFECT_100] = new AchievementProp(
            "Perfect 100", "完美通关 100 次", "通关一个地图不损失生命值",
            AchievementCategory.PERFECT, 1);

        // 积分成就
        achievement_props[TWENTY_FIVE_K] = new AchievementProp(
            "25K Club", "25K 俱乐部", "击败2.5万敌军",
            AchievementCategory.SCORE, 25000);
        achievement_props[BIG_SCORE] = new AchievementProp(
            "Big Score", "大积分", "获得 1,000,000 生命值积分",
            AchievementCategory.SCORE, 1000000);
        achievement_props[HUGE_SCORE] = new AchievementProp(
            "Huge Score", "历史高分", "获得 50,000,000 生命值积分",
            AchievementCategory.SCORE, 50000000);
        achievement_props[EXPERIENCED] = new AchievementProp(
            "Experienced", "老手", "通关 1,000 关",
            AchievementCategory.SCORE, 1000);
        achievement_props[ADDICT] = new AchievementProp(
            "Addict", "痴迷者", "通关 5,000 关",
            AchievementCategory.SCORE, 5000);
        achievement_props[SEVENTY_FIVE_K] = new AchievementProp(
            "75K Club", "75K 俱乐部", "击败7.5万敌军",
            AchievementCategory.SCORE, 75000);

        // 特殊成就
        achievement_props[FAST_PACED] = new AchievementProp(
            "Fast-Paced", "快节奏", "通关一个地图不使用减速塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[ROCKETMAN] = new AchievementProp(
            "Rocketman", "火箭人", "通关一个地图不使用枪塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[GUNNER] = new AchievementProp(
            "Gunner", "枪手", "通关一个地图不使用火箭塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[CHEAPSKATE] = new AchievementProp(
            "Cheapskate", "吝啬鬼", "通关一个地图不升级任何防御塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[BIG_SPENDER] = new AchievementProp(
            "Big Spender", "大富豪", "在通关一个地图后所有的防御塔都升级到最大等级",
            AchievementCategory.SPECIAL, 1);
        achievement_props[POWERED_UP] = new AchievementProp(
            "Powered up", "动力升级", "在通关一个地图后剩余12个或者更少的防御塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[NO_SALE] = new AchievementProp(
            "No Sale", "非卖品", "通关一个地图不出售防御塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[FAST_LANE] = new AchievementProp(
            "Fast Lane", "快车道", "在一场游戏中使用快进功能通关至少75关",
            AchievementCategory.SPECIAL, 1);
        achievement_props[RISK_TAKER] = new AchievementProp(
            "Risk Taker", "冒险者", "在通关一个地图后剩余1点生命值",
            AchievementCategory.SPECIAL, 1);
        achievement_props[SOLID_EFFORT] = new AchievementProp(
            "Solid Effort", "坚实的努力", "在通关一个地图后剩余至少18生命值",
            AchievementCategory.SPECIAL, 1);
        achievement_props[RAINY_DAY] = new AchievementProp(
            "Rainy Day", "下雨天", "在一场游戏中获得 $1,000",
            AchievementCategory.SPECIAL, 1);
        achievement_props[BIG_SAVER] = new AchievementProp(
            "Big Saver", "大储蓄", "在一场游戏中获得 $2,500",
            AchievementCategory.SPECIAL, 1);
        achievement_props[CRAZY_SAVER] = new AchievementProp(
            "Crazy Saver", "疯狂的储蓄者", "在一场游戏中获得 $5,000",
            AchievementCategory.SPECIAL, 1);
        achievement_props[BIG_ONE] = new AchievementProp(
            "Big One", "大收获", "从一个敌人那获得 500+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[WHOPPER] = new AchievementProp(
            "Whopper", "特大之物", "从一个敌人那获得 1,000+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[GOOD_GAME] = new AchievementProp(
            "Good Game", "干得不错", "在一场游戏中获得 250,000+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[GREAT_GAME] = new AchievementProp(
            "Great Game", "干得真棒", "在一场游戏中获得 500,000+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[AMAZING_GAME] = new AchievementProp(
            "Amazing Game", "太棒了", "在一场游戏中获得 1,000,000+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[THIRTY_THIRTY] = new AchievementProp(
            "30/30", "30/30", "通关一场游戏时等级 >= 30 生命值 >= 30",
            AchievementCategory.SPECIAL, 1);
        achievement_props[DEFEAT_TITAN] = new AchievementProp(
            "Titan Defeated", "泰坦被打败", "找到并干掉泰坦",
            AchievementCategory.SPECIAL, 1);
        achievement_props[CHAIN_SMOKER] = new AchievementProp(
            "Chain Smoker", "大烟枪", "引爆 10 个连锁爆炸",
            AchievementCategory.SPECIAL, 1);
        achievement_props[PYRO] = new AchievementProp(
            "Pyro", "喷火兵", "通关一场比赛使用25个或更多火焰塔",
            AchievementCategory.SPECIAL, 1);
        achievement_props[SUPER_POWERED] = new AchievementProp(
            "Super Powered", "超级动力", "用12个或更少的防御塔完美通关地图",
            AchievementCategory.SPECIAL, 1);
        achievement_props[ULTRA_GAME] = new AchievementProp(
            "Ultra Game", "最强的比赛", "在一场游戏中获得 5,000,000+ 积分",
            AchievementCategory.SPECIAL, 1);
        achievement_props[SHOW_OFF] = new AchievementProp(
            "Show Off", "炫耀", "用一个防御塔通关地图",
            AchievementCategory.SPECIAL, 1);

        // 地图成就 - 基地
        achievement_props[BASIC_10] = new AchievementProp(
            "Basic 10", "基地×10", "通关基地地图 10 次",
            AchievementCategory.MAP, 10);
        achievement_props[BASIC_25] = new AchievementProp(
            "Basic 25", "基地×25", "通关基地地图 25 次",
            AchievementCategory.MAP, 25);
        achievement_props[BASIC_LVL_40] = new AchievementProp(
            "Basic level 40", "基地×难度等级 40", "在难度 40通关基地地图",
            AchievementCategory.MAP, 1);
        achievement_props[BASIC_LVL_60] = new AchievementProp(
            "Basic level 60", "基地×难度等级 60", "在难度 60通关基地地图",
            AchievementCategory.MAP, 1);
        achievement_props[BASIC_LVL_80] = new AchievementProp(
            "Basic level 80", "基地×难度等级 80", "在难度 80通关基地地图",
            AchievementCategory.MAP, 1);
        achievement_props[BASIC_LVL_100] = new AchievementProp(
            "Basic level 100", "基地×难度等级 100", "在难度 100通关基地地图",
            AchievementCategory.MAP, 1);

        // 地图成就 - 遗迹
        achievement_props[RUINS_10] = new AchievementProp(
            "Ruins 10", "遗迹×10", "通关遗迹地图 10 次",
            AchievementCategory.MAP, 10);
        achievement_props[RUINS_25] = new AchievementProp(
            "Ruins 25", "遗迹×25", "通关遗迹地图 25 次",
            AchievementCategory.MAP, 25);
        achievement_props[RUINS_LVL_30] = new AchievementProp(
            "The Ruins level 30", "遗迹×难度等级 30", "在难度等级 30通关遗迹地图",
            AchievementCategory.MAP, 1);
        achievement_props[RUINS_LVL_50] = new AchievementProp(
            "The Ruins level 50", "遗迹×难度等级 50", "在难度等级 50通关遗迹地图",
            AchievementCategory.MAP, 1);
        achievement_props[RUINS_LVL_70] = new AchievementProp(
            "The Ruins level 70", "遗迹×难度等级 70", "在难度等级 70通关遗迹地图",
            AchievementCategory.MAP, 1);
        achievement_props[RUINS_LVL_100] = new AchievementProp(
            "The Ruins level 100", "遗迹×难度等级 100", "在难度等级 100通关遗迹地图",
            AchievementCategory.MAP, 1);

        // 地图成就 - 工厂
        achievement_props[FACTORY_10] = new AchievementProp(
            "Factory 10", "工厂×10", "通关工厂地图 10 次",
            AchievementCategory.MAP, 10);
        achievement_props[FACTORY_25] = new AchievementProp(
            "Factory 25", "工厂×25", "通关工厂地图 25 次",
            AchievementCategory.MAP, 25);
        achievement_props[FACTORY_LVL_20] = new AchievementProp(
            "The Factory level 20", "工厂×难度等级 20", "在难度等级 20通关工厂地图",
            AchievementCategory.MAP, 1);
        achievement_props[FACTORY_LVL_40] = new AchievementProp(
            "The Factory level 40", "工厂×难度等级 40", "在难度等级 40通关工厂地图",
            AchievementCategory.MAP, 1);
        achievement_props[FACTORY_LVL_70] = new AchievementProp(
            "The Factory level 70", "工厂×难度等级 70", "在难度等级 70通关工厂地图",
            AchievementCategory.MAP, 1);
        achievement_props[FACTORY_LVL_100] = new AchievementProp(
            "The Factory level 100", "工厂×难度等级 100", "在难度等级 100通关工厂地图",
            AchievementCategory.MAP, 1);

        // 地图成就 - 庭院
        achievement_props[COURTYARD_10] = new AchievementProp(
            "Courtyard 10", "庭院×10", "通关庭院地图 10 次",
            AchievementCategory.MAP, 10);
        achievement_props[COURTYARD_25] = new AchievementProp(
            "Courtyard 25", "庭院×25", "通关庭院地图 25 次",
            AchievementCategory.MAP, 25);
        achievement_props[COURTYARD_LVL_15] = new AchievementProp(
            "The Courtyard level 15", "庭院×难度等级 15", "在难度等级 15通关庭院地图",
            AchievementCategory.MAP, 1);
        achievement_props[COURTYARD_LVL_25] = new AchievementProp(
            "The Courtyard level 25", "庭院×难度等级 25", "在难度等级 25通关庭院地图",
            AchievementCategory.MAP, 1);
        achievement_props[COURTYARD_LVL_50] = new AchievementProp(
            "The Courtyard level 50", "庭院×难度等级 50", "在难度等级 50通关庭院地图",
            AchievementCategory.MAP, 1);
        achievement_props[COURTYARD_LVL_100] = new AchievementProp(
            "The Courtyard level 100", "庭院×难度等级 100", "在难度等级 100通关庭院地图",
            AchievementCategory.MAP, 1);

        // 地图成就 - 巷道
        achievement_props[ROADWAY_LVL_10] = new AchievementProp(
            "Roadway level 10", "巷道×难度等级 10", "在难度等级 10通关巷道地图",
            AchievementCategory.MAP, 1);
        achievement_props[ROADWAY_LVL_50] = new AchievementProp(
            "Roadway level 50", "巷道×难度等级 50", "在难度等级 50通关巷道地图",
            AchievementCategory.MAP, 1);
        achievement_props[ROADWAY_LVL_100] = new AchievementProp(
            "Roadway level 100", "巷道×难度等级 100", "在难度等级 100通关巷道地图",
            AchievementCategory.MAP, 1);

        // 生存成就
        achievement_props[SURVIVAL_10] = new AchievementProp(
            "Cub scout", "幼童军", "在生存模式中通关 10 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_20] = new AchievementProp(
            "Boy scout", "童子军", "在生存模式中通关 20 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_30] = new AchievementProp(
            "Eagle scout", "鹰级童军", "在生存模式中通关 30 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_40] = new AchievementProp(
            "Bear Grylls", "贝尔·格里尔斯", "在生存模式中通关 40 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_50] = new AchievementProp(
            "Les Stroud", "莱斯史特劳", "在生存模式中通关 50 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_60] = new AchievementProp(
            "Scott O'Grady", "斯科特·O·格雷迪", "在生存模式中通关 60 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_70] = new AchievementProp(
            "Aron Ralston", "艾伦·洛斯顿", "在生存模式中通关 70 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_80] = new AchievementProp(
            "Uruguayan Air Force Flight 571", "乌拉圭空军571号", "在生存模式中通关 80 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_90] = new AchievementProp(
            "Cornelius Rost", "极地重生", "在生存模式中通关 90 关",
            AchievementCategory.SURVIVAL, 1);
        achievement_props[SURVIVAL_100] = new AchievementProp(
            "What Robot Apocalypse?", "机器人启示录", "在生存模式中通关 100 关 (解锁新的游戏模式!)",
            AchievementCategory.SURVIVAL, 1);

        // 训练成就
        achievement_props[MIXER_A] = new AchievementProp(
            "VR-A", "VR-A", "通关 5 A级训练任务",
            AchievementCategory.VR, 5);
        achievement_props[MIXER_B] = new AchievementProp(
            "VR-B", "VR-B", "通关 5 B级训练任务",
            AchievementCategory.VR, 5);
        achievement_props[MIXER_C] = new AchievementProp(
            "VR-C", "VR-C", "通关 5 C级训练任务",
            AchievementCategory.VR, 5);
        achievement_props[MIXER_D] = new AchievementProp(
            "VR-D", "VR-D", "通关 5 D级训练任务",
            AchievementCategory.VR, 5);
        achievement_props[MIXER_E] = new AchievementProp(
            "VR-E", "VR-E", "通关 5 E级训练任务",
            AchievementCategory.VR, 5);
    }

    public static String getName(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return "Unknown";
        }
        return achievement_props[type].name;
    }

    public static String getNameZh(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return "未知";
        }
        return achievement_props[type].nameZh;
    }

    public static AchievementCategory getCategory(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return AchievementCategory.SPECIAL;
        }
        return achievement_props[type].category;
    }

    public static String getDescription(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return "";
        }
        AchievementProp prop = achievement_props[type];
        // 如果有描述，返回描述 + 进度
        if (prop.description != null && !prop.description.isEmpty()) {
            String progress = prop.getDescription();
            if (progress.isEmpty()) {
                return prop.description;
            }
            return prop.description + " " + progress;
        }
        return prop.getDescription();
    }

    public static boolean isAchieved(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return false;
        }
        return achievement_props[type].isAchieved();
    }

    public static int totalCount() {
        return total_count;
    }

    public static int getLevel(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return 0;
        }
        return achievement_props[type].level;
    }

    public static int getTriggerLevel(int type) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return 0;
        }
        return achievement_props[type].trigger_level;
    }

    public static boolean increaseLevel(int type, int amount) {
        if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
            return false;
        }
        if (achievement_props[type].increaseLevel(amount)) {
            int[] iArr = new_achievements;
            int i = new_tail;
            new_tail = i + 1;
            iArr[i] = type;
            return true;
        }
        return false;
    }

    public static int dequeueEarned() {
        if (new_tail <= new_head) {
            return -1;
        }
        int[] iArr = new_achievements;
        int i = new_head;
        new_head = i + 1;
        return iArr[i];
    }

    public static boolean hasNewAchievements() {
        return new_tail > new_head;
    }

    public static void trySaveProgress() {
        int first_pending = 0;
        while (first_pending < ACHIEVEMENT_TYPE_COUNT && !achievement_props[first_pending].saveIsPending()) {
            first_pending++;
        }
        if (first_pending < ACHIEVEMENT_TYPE_COUNT && saveManager != null) {
            StringBuilder sb = new StringBuilder();
            for (int type = 0; type < ACHIEVEMENT_TYPE_COUNT; type++) {
                achievement_props[type].trySaveLevel(sb);
            }
            if (sb.length() > 0) {
                saveManager.saveAchievements(sb.toString());
            }
        }
    }

    private static class AchievementProp {
        private String cached_description;
        public String description;      // 成就描述
        public String key;
        private int level;
        public String name;
        public String nameZh;            // 中文名称
        public AchievementCategory category;  // 成就分类
        private int saved_level;
        private int trigger_level;

        public AchievementProp(String key, int trigger_level) {
            this(key, key, "", AchievementCategory.SPECIAL, trigger_level);
        }

        public AchievementProp(String key, String nameZh, String description,
                               AchievementCategory category, int trigger_level) {
            this.key = key;
            this.name = key;
            this.nameZh = nameZh;
            this.description = description;
            this.category = category;
            this.trigger_level = trigger_level;
            this.level = 0;

            if (saveManager != null && key.length() > 0) {
                String saved = saveManager.getAchievement(key);
                if (saved != null) {
                    try {
                        this.level = Integer.parseInt(saved);
                    } catch (NumberFormatException e) {
                        this.level = 0;
                    }
                }
            }
            this.saved_level = this.level;
            if (this.level >= trigger_level) {
                total_count++;
            }
        }

        public String getDescription() {
            if (this.cached_description == null) {
                if (this.trigger_level <= 1) {
                    this.cached_description = "";
                } else {
                    StringBuilder buff = new StringBuilder(64);
                    buff.append("(");
                    buff.append(this.level);
                    buff.append(" / ");
                    buff.append(this.trigger_level);
                    buff.append(")");
                    this.cached_description = buff.toString();
                }
            }
            return this.cached_description;
        }

        public boolean increaseLevel(int amount) {
            if (this.level >= this.trigger_level || amount <= 0) {
                return false;
            }
            this.cached_description = null;
            this.level += amount;
            if (this.level < this.trigger_level) {
                return false;
            }
            this.level = this.trigger_level;
            total_count++;
            trySaveProgress();
            return true;
        }

        public void trySaveLevel(StringBuilder sb) {
            if (this.saved_level != this.level) {
                if (this.key.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append(this.key);
                    sb.append("=");
                    sb.append(this.level);
                }
                this.saved_level = this.level;
            }
        }

        public boolean saveIsPending() {
            return this.saved_level != this.level;
        }

        public boolean isAchieved() {
            return this.level >= this.trigger_level;
        }
    }
}