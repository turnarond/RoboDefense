package com.rdefense.core.render;

import com.rdefense.core.game.BulletData;
import com.rdefense.core.game.EnemyData;
import com.rdefense.core.game.LevelData;
import com.rdefense.core.game.TowerData;

/**
 * 精灵图名称映射工具类
 * 将游戏内部类型常量映射到 assets/images/ 下的文件名（不含扩展名）
 * LibGdxRenderer 会自动尝试 .png 和 .jpg 后缀
 */
public final class SpriteNames {

    private SpriteNames() {}

    // ========== 塔 ==========

    /**
     * 获取塔的精灵图名称
     * @param type 塔类型（TowerData 常量）
     * @param upgradeLevel 升级级别（1=基础，2=升级1，3=升级2）
     */
    public static String tower(int type, int upgradeLevel) {
        switch (type) {
            case 1: return upgradeLevel >= 3 ? "gun_tower3" : (upgradeLevel >= 2 ? "gun_tower2" : "gun_tower");
            case 2: return upgradeLevel >= 3 ? "gun_tower3" : (upgradeLevel >= 2 ? "gun_tower2" : "gun_tower");
            case 3: return "gun_tower3";
            case 4: return upgradeLevel >= 3 ? "ice_tower3" : (upgradeLevel >= 2 ? "ice_tower2" : "ice_tower");
            case 5: return upgradeLevel >= 3 ? "ice_tower3" : (upgradeLevel >= 2 ? "ice_tower2" : "ice_tower");
            case 6: return "ice_tower3";
            case 7: return upgradeLevel >= 3 ? "rocket_tower3" : (upgradeLevel >= 2 ? "rocket_tower2" : "rocket_tower");
            case 8: return upgradeLevel >= 3 ? "rocket_tower3" : (upgradeLevel >= 2 ? "rocket_tower2" : "rocket_tower");
            case 9: return "rocket_tower3";
            case 10: return upgradeLevel >= 2 ? "flame_tower2" : "flame_tower";
            case 11: return "flame_tower2";
            case 12: return "aa_tower";
            case 13: return "aa_tower2";
            case 14: return "sam_tower";
            case 15: return "sam_tower2";
            case 16: return "mortar";
            case 17: return "artillery";
            case 18: return "teleport_tower_unarmed";
            case 19: return "teleport_tower_armed";
            case 20: return "mine_unarmed";
            case 21: return "mine_armed";
            case 22: return "flare";
            default: return "gun_tower";
        }
    }

    /** 获取塔的精灵图名称（根据类型自动判断级别，名称必须与 assets/images/ 中文件名一致） */
    public static String tower(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: return "gun_tower";
            case 2: return "gun_tower2";
            case 3: return "gun_tower3";
            // 减速塔系列
            case 4: return "ice_tower";
            case 5: return "ice_tower2";
            case 6: return "ice_tower3";
            // 火箭塔系列
            case 7: return "rocket_tower";
            case 8: return "rocket_tower2";
            case 9: return "rocket_tower3";
            // 火焰塔系列
            case 10: return "flame_tower";
            case 11: return "flame_tower2";
            // 防空塔系列
            case 12: return "aa_tower";
            case 13: return "aa_tower2";
            // 地对空导弹系列
            case 14: return "sam_tower";
            case 15: return "sam_tower2";
            // 迫击炮系列
            case 16: return "mortar";
            case 17: return "artillery";
            // 传送塔系列
            case 18: return "teleport_tower_unarmed";
            case 19: return "teleport_tower_armed";
            // 地雷系列
            case 20: return "mine_unarmed";
            case 21: return "mine_armed";
            // 火炬塔
            case 22: return "flare";
            default: return "gun_tower";
        }
    }

    // ========== 敌人 ==========

    /**
     * 获取敌人的精灵图名称（不含方向/帧，assets 中每种敌人只有一张静态图）
     * @param type 敌人类型（EnemyData 常量）
     */
    public static String enemy(int type) {
        switch (type) {
            case EnemyData.SOLDIER:    return "soldier";
            case EnemyData.HVYSOLDIER: return "heavy_soldier";
            case EnemyData.RUNNER:     return "runner";
            case EnemyData.TRUCK:      return "truck";
            case EnemyData.LIGHTTANK:  return "light_tank";
            case EnemyData.HEAVYTANK:  return "heavy_tank";
            case EnemyData.HELICOPTER: return "helicopter";
            case EnemyData.JET:        return "jet";
            case EnemyData.BOMBER:     return "bomber";
            case EnemyData.TITAN:      return "titan";
            case EnemyData.DROPPER:    return "mothership";
            default:                   return "soldier";
        }
    }

    // ========== 子弹 ==========

    /**
     * 获取子弹精灵图名称
     * @param shotType 子弹类型（BulletData 常量）
     */
    public static String bullet(int shotType) {
        switch (shotType) {
            case BulletData.GUN:             return "bullet_small";
            case BulletData.BIG_BULLET:      return "bullet_large";
            case BulletData.ROCKET:          return "rocket";
            case BulletData.AABULLET:        return "bullet_large";
            case BulletData.SURFAIR:         return "missile";
            case BulletData.MORTAR:          return "shell_small";
            case BulletData.ARTILLERY:       return "shell_large";
            case BulletData.URANIUM_BULLET:  return "uranium_bullet";
            case BulletData.NAPALM_SHELL:    return "shell_large";
            // 以下类型无精灵图，返回 null（回退到彩色矩形）
            case BulletData.SLOW:
            case BulletData.FIRE:
            case BulletData.MINE:
            case BulletData.SLOW_FIRE:
            case BulletData.NONE:
            case BulletData.TELEPORT:
            default:                         return null;
        }
    }

    // ========== 爆炸特效 ==========

    /** 获取爆炸精灵图名称 */
    public static String explosion() {
        return "explosion";
    }

    /** 获取传送爆炸精灵图名称 */
    public static String teleportExplosion() {
        return "teleport_explosion";
    }

    // ========== 地图背景 ==========

    /**
     * 获取关卡背景图名称
     * @param levelType 关卡类型（LevelData 常量）
     */
    public static String levelBackground(int levelType) {
        switch (levelType) {
            case LevelData.BASIC_LEVEL:    return "basic_level";
            case LevelData.RUINS_LEVEL:    return "cross_level";
            case LevelData.FACTORY_LEVEL:  return "threeway_level";
            case LevelData.COURTYARD_LEVEL: return "courtyard_level";
            case LevelData.MIXER_LEVEL:    return "mixer_level";
            case LevelData.ROADWAY_LEVEL:  return "roadway_level";
            case LevelData.SKYTOWER_LEVEL: return "skytower_level";
            default:                       return "basic_level";
        }
    }

    public static String levelBackgroundClassic(int levelType) {
        switch (levelType) {
            case LevelData.BASIC_LEVEL:    return "basic_level_classic";
            default:                       return levelBackground(levelType);
        }
    }

    // ========== UI ==========

    /** 心形图标（生命值） */
    public static String heart() { return "heart"; }

    /** 成就已获得图标 */
    public static String achievementEarned() { return "achievement_earned"; }

    /** 成就未获得图标 */
    public static String achievementPending() { return "achievement_pending"; }

    /** 向上箭头 */
    public static String arrowUp() { return "up_arrow"; }

    /** 向下箭头 */
    public static String arrowDown() { return "down_arrow"; }

    /** 主菜单标题背景（1-9 随机选） */
    public static String titleBackground(int index) {
        int n = Math.max(1, Math.min(9, index));
        return "title" + n;
    }
}
