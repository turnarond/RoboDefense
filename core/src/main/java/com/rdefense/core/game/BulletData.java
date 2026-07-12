package com.rdefense.core.game;

/**
 * 子弹数据类 — 存储所有子弹类型的属性
 * 对应原版 BulletData
 * 
 * 注意：子弹类型编号必须与原版一致（0-14）
 */
public final class BulletData {

    // 子弹类型常量（匹配原版编号）
    public static final int NONE = 0;           // 无子弹
    public static final int TELEPORT = 1;       // 传送效果
    public static final int GUN = 2;            // 机枪子弹
    public static final int BIG_BULLET = 3;     // 大子弹
    public static final int SLOW = 4;           // 减速弹（无精灵图，用颜色绘制）
    public static final int ROCKET = 5;         // 火箭
    public static final int FIRE = 6;           // 火焰弹（无精灵图，用颜色绘制）
    public static final int AABULLET = 7;       // 防空弹
    public static final int SURFAIR = 8;        // 地对空导弹
    public static final int MORTAR = 9;         // 迫击炮
    public static final int ARTILLERY = 10;     // 火炮
    public static final int MINE = 11;          // 地雷（无精灵图）
    public static final int URANIUM_BULLET = 12; // 铀弹
    public static final int NAPALM_SHELL = 13;  // 凝固汽油弹
    public static final int SLOW_FIRE = 14;     // 缓慢火焰弹（无精灵图）

    // 溅射半径平方
    public static final int SPLASH_RADIUS_SQ = 2500;

    // 减速持续时间（帧）
    public static final int SLOW_DURATION = 120;

    /**
     * 获取子弹速度
     */
    public static int speed(int type) {
        switch (type) {
            case GUN: return 16;
            case BIG_BULLET: return 16;
            case SLOW: return 8;
            case ROCKET: return 4;
            case FIRE: return 12;
            case AABULLET: return 16;
            case SURFAIR: return 6;
            case MORTAR: return 9;
            case ARTILLERY: return 14;
            case URANIUM_BULLET: return 16;
            case NAPALM_SHELL: return 14;
            case SLOW_FIRE: return 12;
            default: return 10;
        }
    }

    /**
     * 获取子弹大小（像素）
     * 与原版Android一致：有精灵图时使用精灵图帧高度，无精灵图时使用硬编码值
     * 
     * 精灵图帧尺寸（原版使用 x_tile_count=0，即 width=height，每帧为正方形）：
     *   bullet_small.png: 35x35  → size=35
     *   bullet_large.png: 70x70  → size=70
     *   rocket.png:       1248x39 → 每帧39x39 → size=39
     *   missile.png:      2240x70 → 每帧70x70 → size=70
     *   shell_small.png:  35x35  → size=35
     *   shell_large.png:  70x70  → size=70
     *   uranium_bullet.png: 70x70 → size=70
     */
    public static int size(int type) {
        switch (type) {
            case GUN: return 35;            // bullet_small.png 帧高度
            case BIG_BULLET: return 70;     // bullet_large.png 帧高度
            case SLOW: return 6;            // 无精灵图，用颜色绘制
            case ROCKET: return 39;         // rocket.png 帧高度（1248/32=39）
            case FIRE: return 10;           // 无精灵图，用颜色绘制
            case AABULLET: return 70;       // bullet_large.png 帧高度
            case SURFAIR: return 70;        // missile.png 帧高度（2240/32=70）
            case MORTAR: return 35;         // shell_small.png 帧高度
            case ARTILLERY: return 70;      // shell_large.png 帧高度
            case MINE: return 10;           // 无精灵图
            case URANIUM_BULLET: return 70; // uranium_bullet.png 帧高度
            case NAPALM_SHELL: return 70;   // shell_large.png 帧高度
            case SLOW_FIRE: return 15;      // 无精灵图，用颜色绘制
            default: return 6;
        }
    }

    /**
     * 获取子弹颜色（用于无精灵图时的回退绘制）
     */
    public static int color(int type) {
        switch (type) {
            case GUN: return 0xFFFF00;
            case BIG_BULLET: return 0xFFFF00;
            case SLOW: return 0x0088FF;
            case ROCKET: return 0xFF4400;
            case FIRE: return 0xFF6600;
            case AABULLET: return 0xFFFF00;
            case SURFAIR: return 0xFF0000;
            case MORTAR: return 0x888888;
            case ARTILLERY: return 0x888888;
            case MINE: return 0x444444;
            case URANIUM_BULLET: return 0x00FF00;
            case NAPALM_SHELL: return 0x888888;
            case SLOW_FIRE: return 0xFF8800;
            default: return 0xFFFFFF;
        }
    }

    /**
     * 获取方向图像数量
     * 返回 0 表示该类型无精灵图，应使用颜色绘制
     * 返回 >1 表示多帧精灵图（用于火箭、导弹等需要方向的子弹）
     * 
     * 与原版Android一致：使用 x_tile_count=0（即 width=height，每帧为正方形）
     * 帧数 = 精灵表宽度 / 精灵表高度
     *   rocket.png:  1248/39 = 32帧
     *   missile.png: 2240/70 = 32帧
     */
    public static int getNumImages(int type) {
        switch (type) {
            case GUN:
            case BIG_BULLET:
            case AABULLET:
            case MORTAR:
            case ARTILLERY:
            case URANIUM_BULLET:
            case NAPALM_SHELL:
                return 1; // 单张精灵图
            case ROCKET:
                return 32; // rocket.png: 1248x39 → 32帧（每帧39x39）
            case SURFAIR:
                return 32; // missile.png: 2240x70 → 32帧（每帧70x70）
            case SLOW:
            case FIRE:
            case MINE:
            case SLOW_FIRE:
            case NONE:
            case TELEPORT:
                return 0; // 无精灵图，用颜色绘制
            default:
                return 0;
        }
    }

    /**
     * 获取方向图像名称
     * 返回 null 表示该类型无精灵图
     */
    public static String getDirectionImageName(int type, int direction) {
        if (getNumImages(type) == 0) {
            return null;
        }
        return null; // 由 SpriteNames.bullet(type) 处理
    }
}
