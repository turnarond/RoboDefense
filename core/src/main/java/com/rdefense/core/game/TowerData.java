package com.rdefense.core.game;

/**
 * 塔数据类 — 存储所有塔类型的属性
 * 对应原版 TowerData
 * 
 * 注意：塔类型索引必须与原版 tower_props 数组索引一致：
 * 1-3: 机枪塔系列, 4-6: 减速塔系列, 7-9: 火箭塔系列
 * 10-11: 火焰塔系列, 12-13: 防空塔系列, 14-15: 导弹塔系列
 * 16-17: 迫击炮系列, 18-19: 传送塔系列, 20-21: 地雷系列, 22: 照明弹
 */
public final class TowerData {

    // 塔类型常量（匹配原版 tower_props 索引）
    public static final int GUN_TOWER = 1;
    public static final int SLOW_TOWER = 4;
    public static final int ROCKET_TOWER = 7;
    public static final int FLAME_TOWER = 10;     // 原版 flame_tower
    public static final int AA_TOWER = 12;        // 原版 aa_tower
    public static final int MORTAR_TOWER = 16;    // 原版 mortar
    public static final int MINE_TOWER = 20;      // 原版 mine_unarmed

    // 塔的精灵表配置
    private static class TowerConfig {
        String imageName;
        int directions;    // 方向数量（不含占位帧）
        boolean animated;

        TowerConfig(String imageName, int directions, boolean animated) {
            this.imageName = imageName;
            this.directions = directions;
            this.animated = animated;
        }
    }

    private static final TowerConfig[] towerConfigs = new TowerConfig[23];

    static {
        // 机枪塔系列
        towerConfigs[1] = new TowerConfig("gun_tower", 32, false);
        towerConfigs[2] = new TowerConfig("gun_tower2", 32, false);
        towerConfigs[3] = new TowerConfig("gun_tower3", 32, false);
        // 减速塔系列（动画）
        towerConfigs[4] = new TowerConfig("ice_tower", 8, true);
        towerConfigs[5] = new TowerConfig("ice_tower2", 8, true);
        towerConfigs[6] = new TowerConfig("ice_tower3", 8, true);
        // 火箭塔系列
        towerConfigs[7] = new TowerConfig("rocket_tower", 32, false);
        towerConfigs[8] = new TowerConfig("rocket_tower2", 32, false);
        towerConfigs[9] = new TowerConfig("rocket_tower3", 32, false);
        // 火焰塔系列
        towerConfigs[10] = new TowerConfig("flame_tower", 32, false);
        towerConfigs[11] = new TowerConfig("flame_tower2", 32, false);
        // 防空塔系列
        towerConfigs[12] = new TowerConfig("aa_tower", 32, false);
        towerConfigs[13] = new TowerConfig("aa_tower2", 32, false);
        // 导弹塔系列
        towerConfigs[14] = new TowerConfig("sam_tower", 32, false);
        towerConfigs[15] = new TowerConfig("sam_tower2", 32, false);
        // 迫击炮系列
        towerConfigs[16] = new TowerConfig("mortar", 1, false);
        towerConfigs[17] = new TowerConfig("artillery", 1, false);
        // 传送塔系列
        towerConfigs[18] = new TowerConfig("teleport_tower_unarmed", 0, false);
        towerConfigs[19] = new TowerConfig("teleport_tower_armed", 16, true);
        // 地雷系列
        towerConfigs[20] = new TowerConfig("mine_unarmed", 0, false);
        towerConfigs[21] = new TowerConfig("mine_armed", 6, true);
        // 照明弹
        towerConfigs[22] = new TowerConfig("flare", 0, false);
    }

    /**
     * 获取塔造价
     */
    public static int cost(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: return 5;
            case 2: return 4;
            case 3: return 6;
            // 减速塔系列
            case 4: return 10;
            case 5: return 10;
            case 6: return 10;
            // 火箭塔系列
            case 7: return 20;
            case 8: return 30;
            case 9: return 50;
            // 火焰塔系列
            case 10: return 70;
            case 11: return 50;
            // 防空塔系列
            case 12: return 20;
            case 13: return 40;
            // 导弹塔系列
            case 14: return 70;
            case 15: return 90;
            // 迫击炮系列
            case 16: return 50;
            case 17: return 90;
            // 地雷系列
            case 20: return 20;
            case 21: return 3;
            // 照明弹
            case 22: return 3;
            default: return 10;
        }
    }

    /**
     * 获取塔射击延迟（帧）
     * 匹配原版 applyTowerAwards 后的基础值
     */
    public static int shotDelay(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: case 2: case 3: return 6;
            // 减速塔系列
            case 4: return 30;
            case 5: return 20;
            case 6: return 15;
            // 火箭塔系列
            case 7: return 70;
            case 8: return 55;
            case 9: return 50;
            // 火焰塔系列
            case 10: case 11: return 3;
            // 防空塔系列
            case 12: case 13: return 6;
            // 导弹塔系列
            case 14: case 15: return 50;
            // 迫击炮系列
            case 16: case 17: return 90;
            // 地雷系列
            case 20: return 0;  // 特殊处理
            case 21: return 1;
            // 照明弹
            case 22: return 3;
            default: return 1000;  // 原版默认值
        }
    }

    /**
     * 获取子弹类型
     */
    public static int shotType(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: case 2: case 3: return BulletData.GUN;
            // 减速塔系列
            case 4: case 5: case 6: return BulletData.SLOW;
            // 火箭塔系列
            case 7: case 8: case 9: return BulletData.ROCKET;
            // 火焰塔系列
            case 10: case 11: return BulletData.FIRE;
            // 防空塔系列
            case 12: case 13: return BulletData.AABULLET;
            // 导弹塔系列
            case 14: case 15: return BulletData.SURFAIR;
            // 迫击炮系列
            case 16: return BulletData.MORTAR;
            case 17: return BulletData.ARTILLERY;
            // 地雷系列
            case 20: case 21: return BulletData.MINE;
            // 照明弹
            case 22: return BulletData.TELEPORT;
            default: return BulletData.NONE;
        }
    }

    /**
     * 获取塔攻击力
     */
    public static int power(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: return 5;
            case 2: return 6;
            case 3: return 8;
            // 减速塔系列（power 值 = slow 持续时间，单位：帧）
            case 4: return 30;  // 原版 RewardData.applyAward(30, 6)
            case 5: return 45;  // 原版 RewardData.applyAward(45, 6)
            case 6: return 60;  // 原版 RewardData.applyAward(60, 6)
            // 火箭塔系列
            case 7: return 15;
            case 8: return 25;
            case 9: return 40;
            // 火焰塔系列
            case 10: return 10;
            case 11: return 15;
            // 防空塔系列
            case 12: return 10;
            case 13: return 20;
            // 导弹塔系列
            case 14: return 30;
            case 15: return 50;
            // 迫击炮系列
            case 16: return 20;
            case 17: return 50;
            // 地雷系列
            case 20: return 25;
            case 21: return 400;
            // 照明弹
            case 22: return 40;
            default: return 5;
        }
    }

    /**
     * 获取枪口高度（基于塔的 towerHeight + 7，对齐原版）
     */
    public static int gunHeight(int type) {
        return towerHeight(type) + 7;
    }

    /**
     * 获取枪口半径
     */
    public static int gunRadius(int type) {
        // 原版默认 gun_radius = 25，但不同塔类型有不同值
        switch (type) {
            case 4: case 5: case 6: return 1;   // 减速塔
            case 12: case 13: return 5;         // 防空塔
            case 16: case 17: return 1;         // 迫击炮
            case 18: case 19: return 1;         // 传送塔
            case 22: return 5;                  // 照明弹
            default: return 25;                 // 默认值
        }
    }

    /**
     * 获取攻击半径（单位：世界像素）
     * 匹配原版 setAttackRadius 计算：attack_radius = (GRID_PIXEL_SIZE * decigrids) / 10
     */
    public static int attackRadius(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: case 2: case 3:
                return (32 * 18) / 10;  // 57 像素
            // 减速塔系列
            case 4: case 5: case 6:
                return (32 * 18) / 10;  // 57 像素
            // 火箭塔系列
            case 7: case 8: case 9:
                return (32 * 35) / 10;  // 112 像素
            // 火焰塔系列
            case 10: case 11:
                return (32 * 18) / 10;  // 57 像素
            // 防空塔系列
            case 12: case 13:
                return (32 * 18) / 10;  // 57 像素
            // 导弹塔系列
            case 14: case 15:
                return (32 * 45) / 10;  // 144 像素
            // 迫击炮系列
            case 16: return (32 * 45) / 10;   // 144 像素
            case 17: return (32 * 55) / 10;   // 176 像素
            // 地雷系列
            case 20: case 21:
                return (32 * 18) / 10;  // 57 像素
            // 照明弹
            case 22:
                return (32 * 5) / 10;   // 16 像素
            default:
                return (32 * 3) / 10;   // 原版默认 9 像素
        }
    }

    /**
     * 获取攻击半径的平方（单位：世界像素平方）
     * 用于距离比较，避免开方运算
     */
    public static int attackRadiusSq(int type) {
        int radius = attackRadius(type);
        return radius * radius;
    }

    /**
     * 获取塔高度
     */
    public static int towerHeight(int type) {
        // 默认高度（与原版一致）
        int defaultHeight = (32 * 3) / 8;
        switch (type) {
            // 传送塔、地雷、照明弹等无转头高度
            case 18: case 19: case 20: case 21: case 22:
                return 0;
            default:
                return defaultHeight;
        }
    }

    /**
     * 是否阻挡敌人移动
     */
    public static boolean isBlocking(int type) {
        return true;
    }

    /**
     * 获取升级类型
     * @param current_type 当前塔类型
     * @param upgrade_idx 升级选项索引（0=第一个，1=第二个，2=第三个）
     * @return 升级后的塔类型，-1 表示无此升级选项
     */
    public static int upgradeType(int current_type, int upgrade_idx) {
        switch (current_type) {
            // 机枪塔系列
            case 1: // gun_tower -> gun_tower2, flame_tower, aa_tower
                switch (upgrade_idx) {
                    case 0: return 2;   // 升级到机枪塔2
                    case 1: return 10;  // 转换为火焰塔
                    case 2: return 12;  // 转换为防空塔
                }
                break;
            case 2: // gun_tower2 -> gun_tower3
                return upgrade_idx == 0 ? 3 : -1;
            // 减速塔系列
            case 4: // ice_tower -> ice_tower2
                return upgrade_idx == 0 ? 5 : -1;
            case 5: // ice_tower2 -> ice_tower3
                return upgrade_idx == 0 ? 6 : -1;
            // 火箭塔系列
            case 7: // rocket_tower -> rocket_tower2, mortar, sam_tower
                switch (upgrade_idx) {
                    case 0: return 8;   // 升级到火箭塔2
                    case 1: return 16;  // 转换为迫击炮
                    case 2: return 14;  // 转换为导弹塔
                }
                break;
            case 8: // rocket_tower2 -> rocket_tower3
                return upgrade_idx == 0 ? 9 : -1;
            // 火焰塔系列
            case 10: // flame_tower -> flame_tower2
                return upgrade_idx == 0 ? 11 : -1;
            // 防空塔系列
            case 12: // aa_tower -> aa_tower2
                return upgrade_idx == 0 ? 13 : -1;
            // 导弹塔系列
            case 14: // sam_tower -> sam_tower2
                return upgrade_idx == 0 ? 15 : -1;
            // 迫击炮系列
            case 16: // mortar -> artillery
                return upgrade_idx == 0 ? 17 : -1;
            // 地雷系列
            case 20: // mine_unarmed -> mine_armed
                return upgrade_idx == 0 ? 21 : -1;
            // 传送塔系列
            case 18: // teleport_unarmed -> teleport_armed
                return upgrade_idx == 0 ? 19 : -1;
        }
        return -1;
    }

    /**
     * 获取降级类型（用于计算出售价值）
     * @return 降级后的塔类型，-1 表示无降级
     */
    public static int downgradeType(int type) {
        switch (type) {
            case 3: return 2;
            case 2: return 1;
            case 6: return 5;
            case 5: return 4;
            case 9: return 8;
            case 8: return 7;
            case 11: return 10;
            case 13: return 12;
            case 15: return 14;
            case 17: return 16;
            case 19: return 18; // 传送塔
            case 21: return 20; // 地雷
            default: return -1;
        }
    }

    /**
     * 获取出售价值（含所有降级路径的累计成本）
     */
    public static int sellValue(int type) {
        int value = 0;
        int current = type;
        while (current >= 0) {
            value += cost(current) / 2;
            current = downgradeType(current);
        }
        return value;
    }

    /**
     * 获取塔图像名称
     */
    public static String getImageName(int type) {
        switch (type) {
            case 1: case 2: case 3: return "tower_gun";
            case 4: case 5: case 6: return "tower_slow";
            case 7: case 8: case 9: return "tower_rocket";
            case 10: case 11: return "tower_flame";
            case 12: case 13: return "tower_aa";
            case 14: case 15: return "tower_sam";
            case 16: case 17: return "tower_mortar";
            case 18: return "teleport_tower_unarmed";
        case 19: return "teleport_tower_armed";
        case 20: case 21: return "tower_mine";
        case 22: return "flare";
            default: return "tower_unknown";
        }
    }

    /**
     * 获取方向图像名称
     */
    public static String getDirectionImageName(int type, int direction, int frameIndex) {
        return getImageName(type) + "_" + direction;
    }

    /**
     * 获取精灵表图像名称（不含扩展名）
     */
    public static String getImageSheetName(int type) {
        if (type >= 0 && type < towerConfigs.length && towerConfigs[type] != null) {
            return towerConfigs[type].imageName;
        }
        return "gun_tower";
    }

    /**
     * 返回用于调整炮塔转头在桌面端的像素偏移（相对于原版 y = gridY*GRID - towerHeight）
     * 这是为了解决图集帧内透明边距与 Android Canvas/LibGDX 绘制锚点差异造成的偏移。
     * 值为正时表示在计算出的基线之上再向下移动该像素数。
     */
    public static int turretYOffset(int type) {
        switch (type) {
            // 机枪塔系列（不同升级变种使用不同图）
            case 1: return 3;  // gun_tower
            case 2: return 6;  // gun_tower2
            case 3: return 6;  // gun_tower3
            // 减速塔（ice）
            case 4: case 5: case 6: return 28;
            // 火箭塔系列
            case 7: return 5;
            case 8: return 10;
            case 9: return 17;
            // 火焰塔系列
            case 10: return 2;
            case 11: return 10;
            // 防空塔
            case 12: case 13: return 11;
            // 导弹塔
            case 14: case 15: return 10;
            // 迫击炮
            case 16: case 17: return 13;
            // 传送塔、地雷、照明弹等（多为不需偏移或特殊）
            case 19: return 18; // teleport armed
            case 20: case 21: return 0; // mine
            case 22: return 0; // flare
            default: return 0;
        }
    }

    /**
     * 获取精灵表总帧数（含占位帧 = directions + 1）
     */
    public static int getTotalFrames(int type) {
        if (type >= 0 && type < towerConfigs.length && towerConfigs[type] != null) {
            return towerConfigs[type].directions + 1;
        }
        return 33; // 默认 32 方向 + 1
    }

    /**
     * 获取塔的精灵表帧索引
     * 原版逻辑：
     * - animated: images[(frame % directions) + 1]
     * - 非动画: getDirectionImage(angle) -> images[(directions * angle) / 360 + 1]
     * 
     * @param type 塔类型
     * @param direction 朝向角度（0-359）
     * @param animFrame 动画帧计数
     * @return 精灵表中的帧索引（从 0 开始）
     */
    public static int getDirectionFrameIndex(int type, int direction, int animFrame) {
        if (type < 0 || type >= towerConfigs.length || towerConfigs[type] == null) {
            return 1; // 默认第一帧
        }
        TowerConfig config = towerConfigs[type];
        if (config.directions == 0) {
            return 0; // 无方向变化
        }

        if (config.animated) {
            // 动画塔：帧循环
            return (animFrame % config.directions) + 1;
        } else {
            // 非动画塔：根据角度选择方向
            int dirIndex = (config.directions * direction) / 360;
            return dirIndex + 1;
        }
    }
}
