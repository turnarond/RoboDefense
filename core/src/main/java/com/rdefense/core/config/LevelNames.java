package com.rdefense.core.config;

/**
 * 关卡名称映射 — 对应原版 res/values/strings.xml 中的 level_data_* 字符串
 * 原版 7 种地图的中文名称集中在此，方便所有场景使用。
 */
public final class LevelNames {

    private LevelNames() {}

    /** 获取地图 ID 对应的中文名（对应原版 level_data_basic_level 等资源） */
    public static String getName(int levelType) {
        switch (levelType) {
            case 0: return "基础关卡";
            case 1: return "遗迹关卡";
            case 2: return "工厂关卡";
            case 3: return "庭院关卡";
            case 4: return "混合关卡";
            case 5: return "道路关卡";
            case 6: return "天空塔关卡";
            default: return "未知关卡";
        }
    }

    /** 获取地图 ID 对应的英文名（与原版 Robo Defense 地图 ID 命名一致） */
    public static String getEnglishName(int levelType) {
        switch (levelType) {
            case 0: return "Basic";
            case 1: return "The Ruins";
            case 2: return "The Factory";
            case 3: return "The Courtyard";
            case 4: return "VR Training";
            case 5: return "The Roadway";
            case 6: return "Skytower";
            default: return "Unknown";
        }
    }
}
