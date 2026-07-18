package com.rdefense.core.game;

/**
 * 敌人数据类 — 存储所有敌人类型的属性
 * 对应原版 EnemyData（简化版，移除 Android 依赖）
 */
public final class EnemyData {

    // 敌人类型常量
    public static final int SOLDIER = 0;
    public static final int HVYSOLDIER = 1;
    public static final int RUNNER = 2;
    public static final int TRUCK = 3;
    public static final int LIGHTTANK = 4;
    public static final int HEAVYTANK = 5;
    public static final int HELICOPTER = 6;
    public static final int JET = 7;
    public static final int BOMBER = 8;
    public static final int TITAN = 9;
    public static final int DROPPER = 10;
    public static final int ENEMY_TYPE_COUNT = 11;

    // 敌人属性数组
    private static EnemyProp[] enemy_props = new EnemyProp[ENEMY_TYPE_COUNT];

    /**
     * 初始化敌人数据
     */
    public static void init() {
        // 步兵 (SOLDIER)
        enemy_props[SOLDIER] = new EnemyProp();
        enemy_props[SOLDIER].health = 7;
        enemy_props[SOLDIER].armor = 0;
        enemy_props[SOLDIER].is_flyer = false;
        enemy_props[SOLDIER].speed = 20;
        enemy_props[SOLDIER].value = 1;
        enemy_props[SOLDIER].drop_type = -1;
        enemy_props[SOLDIER].drop_delay = 0;
        enemy_props[SOLDIER].first_level = 1;
        enemy_props[SOLDIER].min_difficulty = 0;
        enemy_props[SOLDIER].label = "SLDR";
        enemy_props[SOLDIER].x_tile_count = 48;
        enemy_props[SOLDIER].frame_skip = 0;
        enemy_props[SOLDIER].energy_bar_offset = 3;
        enemy_props[SOLDIER].imageName = "soldier";

        // 重步兵 (HVYSOLDIER)
        enemy_props[HVYSOLDIER] = new EnemyProp();
        enemy_props[HVYSOLDIER].health = 15;
        enemy_props[HVYSOLDIER].armor = 1;
        enemy_props[HVYSOLDIER].is_flyer = false;
        enemy_props[HVYSOLDIER].speed = 15;
        enemy_props[HVYSOLDIER].value = 2;
        enemy_props[HVYSOLDIER].drop_type = -1;
        enemy_props[HVYSOLDIER].drop_delay = 0;
        enemy_props[HVYSOLDIER].first_level = 4;
        enemy_props[HVYSOLDIER].min_difficulty = 0;
        enemy_props[HVYSOLDIER].label = "HSLD";
        enemy_props[HVYSOLDIER].x_tile_count = 64;
        enemy_props[HVYSOLDIER].frame_skip = 0;
        enemy_props[HVYSOLDIER].energy_bar_offset = 3;
        enemy_props[HVYSOLDIER].imageName = "heavy_soldier";

        // 突击兵 (RUNNER)
        enemy_props[RUNNER] = new EnemyProp();
        enemy_props[RUNNER].health = 7;
        enemy_props[RUNNER].armor = 0;
        enemy_props[RUNNER].is_flyer = false;
        enemy_props[RUNNER].speed = 28;
        enemy_props[RUNNER].value = 2;
        enemy_props[RUNNER].drop_type = -1;
        enemy_props[RUNNER].drop_delay = 0;
        enemy_props[RUNNER].first_level = 10;
        enemy_props[RUNNER].min_difficulty = 0;
        enemy_props[RUNNER].label = "RUNR";
        enemy_props[RUNNER].x_tile_count = 48;
        enemy_props[RUNNER].frame_skip = 0;
        enemy_props[RUNNER].energy_bar_offset = 3;
        enemy_props[RUNNER].imageName = "runner";

        // 卡车 (TRUCK)
        enemy_props[TRUCK] = new EnemyProp();
        enemy_props[TRUCK].health = 30;
        enemy_props[TRUCK].armor = 2;
        enemy_props[TRUCK].is_flyer = false;
        enemy_props[TRUCK].speed = 35;
        enemy_props[TRUCK].value = 3;
        enemy_props[TRUCK].drop_type = -1;
        enemy_props[TRUCK].drop_delay = 0;
        enemy_props[TRUCK].first_level = 25;
        enemy_props[TRUCK].min_difficulty = 0;
        enemy_props[TRUCK].label = "TRUK";
        enemy_props[TRUCK].x_tile_count = 12;
        enemy_props[TRUCK].frame_skip = 0;
        enemy_props[TRUCK].energy_bar_offset = 3;
        enemy_props[TRUCK].imageName = "truck";

        // 轻坦克 (LIGHTTANK) - 精灵表 840x70, 840/70 = 12 帧
        enemy_props[LIGHTTANK] = new EnemyProp();
        enemy_props[LIGHTTANK].health = 60;
        enemy_props[LIGHTTANK].armor = 6;
        enemy_props[LIGHTTANK].is_flyer = false;
        enemy_props[LIGHTTANK].speed = 10;
        enemy_props[LIGHTTANK].value = 5;
        enemy_props[LIGHTTANK].drop_type = -1;
        enemy_props[LIGHTTANK].drop_delay = 0;
        enemy_props[LIGHTTANK].first_level = 15;
        enemy_props[LIGHTTANK].min_difficulty = 0;
        enemy_props[LIGHTTANK].label = "LTNK";
        enemy_props[LIGHTTANK].x_tile_count = 12;
        enemy_props[LIGHTTANK].frame_skip = 0;
        enemy_props[LIGHTTANK].energy_bar_offset = 3;
        enemy_props[LIGHTTANK].imageName = "light_tank";

        // 重坦克 (HEAVYTANK) - 精灵表 2800x70, 2800/70 = 40 帧
        enemy_props[HEAVYTANK] = new EnemyProp();
        enemy_props[HEAVYTANK].health = 150;
        enemy_props[HEAVYTANK].armor = 14;
        enemy_props[HEAVYTANK].is_flyer = false;
        enemy_props[HEAVYTANK].speed = 8;
        enemy_props[HEAVYTANK].value = 10;
        enemy_props[HEAVYTANK].drop_type = -1;
        enemy_props[HEAVYTANK].drop_delay = 0;
        enemy_props[HEAVYTANK].first_level = 35;
        enemy_props[HEAVYTANK].min_difficulty = 0;
        enemy_props[HEAVYTANK].label = "HTNK";
        enemy_props[HEAVYTANK].x_tile_count = 40;
        enemy_props[HEAVYTANK].frame_skip = 1;
        enemy_props[HEAVYTANK].energy_bar_offset = 3;
        enemy_props[HEAVYTANK].imageName = "heavy_tank";

        // 直升机 (HELICOPTER)
        enemy_props[HELICOPTER] = new EnemyProp();
        enemy_props[HELICOPTER].health = 60;
        enemy_props[HELICOPTER].armor = 2;
        enemy_props[HELICOPTER].is_flyer = true;
        enemy_props[HELICOPTER].speed = 20;
        enemy_props[HELICOPTER].value = 8;
        enemy_props[HELICOPTER].label = "HELI";
        enemy_props[HELICOPTER].drop_type = -1;
        enemy_props[HELICOPTER].drop_delay = 0;
        enemy_props[HELICOPTER].first_level = 15;
        enemy_props[HELICOPTER].min_difficulty = 0;
        enemy_props[HELICOPTER].x_tile_count = 24;
        enemy_props[HELICOPTER].frame_skip = 0;
        enemy_props[HELICOPTER].img_center_x_ratio = 0.5f;
        enemy_props[HELICOPTER].img_center_y_ratio = 0.32f;
        enemy_props[HELICOPTER].img_visible_width_ratio = 0.666f;
        enemy_props[HELICOPTER].img_visible_height_ratio = 0.4f;
        enemy_props[HELICOPTER].energy_bar_offset = 10;
        enemy_props[HELICOPTER].imageName = "helicopter";

        // 喷气机 (JET) - 精灵表 420x175, x_tile_count=4 (4个方向，每方向1帧)
        enemy_props[JET] = new EnemyProp();
        enemy_props[JET].health = 70;
        enemy_props[JET].armor = 0;
        enemy_props[JET].is_flyer = true;
        enemy_props[JET].speed = 32;
        enemy_props[JET].value = 10;
        enemy_props[JET].label = "JET";
        enemy_props[JET].drop_type = -1;
        enemy_props[JET].drop_delay = 0;
        enemy_props[JET].first_level = 30;
        enemy_props[JET].min_difficulty = 0;
        enemy_props[JET].x_tile_count = 4;
        enemy_props[JET].frame_skip = 0;
        enemy_props[JET].img_center_x_ratio = 0.48f;
        enemy_props[JET].img_center_y_ratio = 0.27f;
        enemy_props[JET].img_visible_width_ratio = 0.666f;
        enemy_props[JET].img_visible_height_ratio = 0.5f;
        enemy_props[JET].energy_bar_offset = 12;
        enemy_props[JET].imageName = "jet";

        // 轰炸机 (BOMBER) - 精灵表 420x175, x_tile_count=4 (4个方向，每方向1帧)
        enemy_props[BOMBER] = new EnemyProp();
        enemy_props[BOMBER].health = 200;
        enemy_props[BOMBER].armor = 4;
        enemy_props[BOMBER].is_flyer = true;
        enemy_props[BOMBER].speed = 15;
        enemy_props[BOMBER].value = 20;
        enemy_props[BOMBER].label = "BOMB";
        enemy_props[BOMBER].drop_type = -1;
        enemy_props[BOMBER].drop_delay = 0;
        enemy_props[BOMBER].first_level = 40;
        enemy_props[BOMBER].min_difficulty = 0;
        enemy_props[BOMBER].x_tile_count = 4;
        enemy_props[BOMBER].frame_skip = 0;
        enemy_props[BOMBER].img_center_x_ratio = 0.575f;
        enemy_props[BOMBER].img_center_y_ratio = 0.348f;
        enemy_props[BOMBER].img_visible_width_ratio = 0.625f;
        enemy_props[BOMBER].img_visible_height_ratio = 0.348f;
        enemy_props[BOMBER].energy_bar_offset = 38;
        enemy_props[BOMBER].imageName = "bomber";

        // 泰坦 (TITAN)
        enemy_props[TITAN] = new EnemyProp();
        enemy_props[TITAN].health = 3000;
        enemy_props[TITAN].armor = 14;
        enemy_props[TITAN].is_flyer = false;
        enemy_props[TITAN].speed = 6;
        enemy_props[TITAN].value = 250;
        enemy_props[TITAN].drop_type = -1;
        enemy_props[TITAN].drop_delay = 0;
        enemy_props[TITAN].first_level = 200;
        enemy_props[TITAN].min_difficulty = 25;
        enemy_props[TITAN].label = "TITN";
        enemy_props[TITAN].x_tile_count = 40;
        enemy_props[TITAN].frame_skip = 1;
        enemy_props[TITAN].energy_bar_offset = 3;
        enemy_props[TITAN].imageName = "titan";

        // 母舰 (DROPPER) - 精灵表 420x151, x_tile_count=4 (4个方向，每方向1帧)
        enemy_props[DROPPER] = new EnemyProp();
        enemy_props[DROPPER].health = 350;
        enemy_props[DROPPER].armor = 6;
        enemy_props[DROPPER].is_flyer = true;
        enemy_props[DROPPER].speed = 10;
        enemy_props[DROPPER].value = 30;
        enemy_props[DROPPER].drop_type = 0;
        enemy_props[DROPPER].drop_delay = 100;
        enemy_props[DROPPER].first_level = 20;
        enemy_props[DROPPER].min_difficulty = 10;
        enemy_props[DROPPER].label = "DRPR";
        enemy_props[DROPPER].x_tile_count = 4;
        enemy_props[DROPPER].frame_skip = 0;
        enemy_props[DROPPER].img_center_x_ratio = 0.5f;
        enemy_props[DROPPER].img_center_y_ratio = 0.26f;
        enemy_props[DROPPER].img_visible_width_ratio = 1.0f;
        enemy_props[DROPPER].img_visible_height_ratio = 0.5f;
        enemy_props[DROPPER].energy_bar_offset = 19;
        enemy_props[DROPPER].imageName = "mothership";
    }

    /**
     * 获取敌人首次出现的关卡
     */
    public static int firstLevel(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.first_level : 1;
    }

    /**
     * 获取敌人生命值
     */
    public static int baseHealth(int type, int level_num, int difficulty) {
        EnemyProp prop = enemy_props[type];
        if (prop == null) return 10;
        return (prop.health * ((difficulty * 8) + 9 + level_num)) / 10;
    }

    /**
     * 获取敌人速度
     */
    public static int speed(int type) {
        EnemyProp prop = enemy_props[type];
        // 原版：(speed * GRID_PIXEL_SIZE) / 40，本项目网格 32px
        return prop != null ? (prop.speed * 32) / 40 : 10;
    }

    /**
     * 获取敌人价值（击杀奖励）
     */
    public static int value(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.value : 1;
    }

    /**
     * 是否飞行器
     */
    public static boolean isFlyer(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null && prop.is_flyer;
    }

    /**
     * 血条偏移
     */
    public static int energyBarOffset(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.energy_bar_offset : 3;
    }

    /**
     * 绘制X偏移
     */
    public static int drawShiftX(int type) {
        return 0;
    }

    /**
     * 绘制Y偏移
     */
    public static int drawShiftY(int type) {
        return 0;
    }

    /**
     * 图像中心X
     */
    public static int imageCenterX(int type) {
        return 16; // 默认一半
    }

    /**
     * 图像中心Y
     */
    public static int imageCenterY(int type) {
        return 16;
    }

    /**
     * 图像可见宽度
     */
    public static int imageVisibleWidth(int type) {
        return 32;
    }

    /**
     * 图像可见高度
     */
    public static int imageVisibleHeight(int type) {
        return 32;
    }

    /**
     * 死亡动画帧数
     */
    public static int deathFrames(int type) {
        // 原版：(价值 << 1) + 10，高价值敌人死亡动画更长（泰坦 510 帧）
        EnemyProp prop = enemy_props[type];
        return prop != null ? (prop.value << 1) + 10 : 10;
    }

    /**
     * 掉落延迟
     */
    public static int dropDelay(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.drop_delay : 0;
    }

    /**
     * 最低难度
     */
    public static int minDifficulty(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.min_difficulty : 0;
    }

    /**
     * 获取掉落单位类型（-1 表示不掉落）
     */
    public static int dropType(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.drop_type : -1;
    }

    /**
     * 获取护甲值
     */
    public static int armor(int type) {
        EnemyProp prop = enemy_props[type];
        return prop != null ? prop.armor : 0;
    }

    /**
     * 获取敌人图像名称
     */
    public static String getImageName(int type, int orientation, int frame) {
        EnemyProp prop = enemy_props[type];
        if (prop == null) return "unknown";
        return "enemy_" + prop.label + "_" + orientation + "_" + frame;
    }

    /**
     * 获取精灵表图像名称（不含扩展名）
     */
    public static String getImageSheetName(int type) {
        EnemyProp prop = enemy_props[type];
        if (prop == null || prop.imageName == null) return "soldier";
        return prop.imageName;
    }

    /**
     * 获取精灵表总帧数（x_tile_count）
     */
    public static int getTotalFrames(int type) {
        EnemyProp prop = enemy_props[type];
        if (prop == null) return 48; // 默认值
        return prop.x_tile_count;
    }

    /**
     * 获取每个方向的帧数（总帧数 / 4）
     */
    public static int getFramesPerDirection(int type) {
        EnemyProp prop = enemy_props[type];
        if (prop == null || prop.x_tile_count <= 0) return 12;
        return prop.x_tile_count / 4;
    }

    /**
     * 计算动画帧在精灵表中的索引
     * 原版逻辑：frames[((orientation - 1) * frames_per_dir) + ((frame / (frame_skip + 1)) % frames_per_dir)]
     * 
     * @param type 敌人类型
     * @param orientation 朝向（1-4）
     * @param animationFrame 动画帧计数
     * @return 精灵表中的帧索引
     */
    public static int getAnimationFrameIndex(int type, int orientation, int animationFrame) {
        EnemyProp prop = enemy_props[type];
        if (prop == null || prop.x_tile_count <= 0) return 0;

        int framesPerDir = prop.x_tile_count / 4;
        if (framesPerDir <= 0) return 0;

        int frameSkip = prop.frame_skip;
        
        int dirIndex;
        // 只对地面单位使用特殊方向映射（右、下、左、上）
        // 飞行单位（JET、HELICOPTER、BOMBER、DROPPER）使用原版方向（右、上、左、下）
        if (type == SOLDIER || type == HVYSOLDIER || type == RUNNER || 
            type == TRUCK || type == LIGHTTANK || type == HEAVYTANK || type == TITAN) {
            switch (orientation) {
                case 1: dirIndex = 0 * framesPerDir; break;
                case 2: dirIndex = 3 * framesPerDir; break;
                case 3: dirIndex = 2 * framesPerDir; break;
                case 4: dirIndex = 1 * framesPerDir; break;
                default: dirIndex = 0;
            }
        } else {
            dirIndex = (orientation - 1) * framesPerDir;
        }
        
        int frameIndex = (animationFrame / (frameSkip + 1)) % framesPerDir;

        int result = dirIndex + frameIndex;
        // 边界检查
        if (result >= prop.x_tile_count) {
            result = result % prop.x_tile_count;
        }
        return result;
    }

    /**
     * 敌人属性内部类
     */
    private static class EnemyProp {
        int health;
        int armor;
        boolean is_flyer;
        int speed;
        int value;
        int drop_type;
        int drop_delay;
        int first_level;
        int min_difficulty;
        String label;
        int x_tile_count;
        int frame_skip;
        int energy_bar_offset;
        float img_center_x_ratio;
        float img_center_y_ratio;
        float img_visible_width_ratio;
        float img_visible_height_ratio;
        String imageName;
    }
}
