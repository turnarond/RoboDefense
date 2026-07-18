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
            // 廉价花炮（奖励14）解锁后高级 SAM 降为 50（原版 applyTowerAwards）
            case 15: return RewardData.getLevel(RewardData.CHEAP_FIREWORKS) > 0 ? 50 : 90;
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
        int base;
        int rewardType = -1;

        switch (type) {
            // 机枪塔系列
            case 1: case 2: case 3: base = 6; break;
            // 减速塔系列
            case 4: base = 30; break;
            case 5: base = 20; break;
            case 6: base = 15; break;
            // 火箭塔系列 → ROCKET_SPEED_UPGRADE
            case 7: base = 70; rewardType = RewardData.ROCKET_SPEED_UPGRADE; break;
            case 8: base = 55; rewardType = RewardData.ROCKET_SPEED_UPGRADE; break;
            case 9: base = 50; rewardType = RewardData.ROCKET_SPEED_UPGRADE; break;
            // 火焰塔系列
            case 10: case 11: base = 3; break;
            // 防空塔系列
            case 12: case 13: base = 6; break;
            // 导弹塔系列 → ANTIAIR_SPEED_UPGRADE
            case 14: base = 50; rewardType = RewardData.ANTIAIR_SPEED_UPGRADE; break;
            case 15: base = 50; rewardType = RewardData.ANTIAIR_SPEED_UPGRADE; break;
            // 迫击炮系列 → ARTILLERY_SPEED_UPGRADE
            case 16: base = 90; rewardType = RewardData.ARTILLERY_SPEED_UPGRADE; break;
            case 17: base = 90; rewardType = RewardData.ARTILLERY_SPEED_UPGRADE; break;
            // 地雷系列
            case 20: return 0;  // 特殊处理
            case 21: return 1;
            // 照明弹
            case 22: base = 3; break;
            default: return 1000;
        }

        if (rewardType >= 0) {
            return RewardData.applyReward(base, rewardType);
        }
        return base;
    }

    /**
     * 获取子弹类型
     */
    public static int shotType(int type) {
        switch (type) {
            // 机枪塔系列
            case 1: case 2: return BulletData.GUN;
            case 3:
                // 铀弹奖励：重型机枪 → 穿甲弹
                if (RewardData.rewardLevel(RewardData.URANIUM_SHELLS) > 0) {
                    return BulletData.URANIUM_BULLET;
                }
                return BulletData.GUN;
            // 减速塔系列
            case 4: case 5: case 6: return BulletData.SLOW;
            // 火箭塔系列
            case 7: case 8: case 9: return BulletData.ROCKET;
            // 火焰塔系列
            case 10: return BulletData.FIRE;
            case 11:
                // 缓慢燃烧奖励：地狱之塔 → 冲击波（灼烧+减速）
                if (RewardData.rewardLevel(RewardData.SLOW_BURN) > 0) {
                    return BulletData.SLOW_FIRE;
                }
                return BulletData.FIRE;
            // 防空塔系列
            case 12: case 13: return BulletData.AABULLET;
            // 导弹塔系列
            case 14: case 15: return BulletData.SURFAIR;
            // 迫击炮系列
            case 16: return BulletData.MORTAR;
            case 17:
                // 凝固汽油弹奖励：火炮 → 凝固汽油弹
                if (RewardData.rewardLevel(RewardData.NAPALM_SHELLS) > 0) {
                    return BulletData.NAPALM_SHELL;
                }
                return BulletData.ARTILLERY;
            // 地雷系列
            case 20: return BulletData.NONE;  // 未触发地雷：无需子弹
            case 21: return BulletData.MINE;  // 已触发地雷
            // 照明弹
            case 22: return BulletData.TELEPORT;
            default: return BulletData.NONE;
        }
    }

    /**
     * 获取塔攻击力（含奖励升级加成）
     */
    public static int power(int type) {
        int base;
        int rewardType = -1;

        switch (type) {
            // 机枪塔系列 → BULLET_UPGRADE
            case 1: base = 5; rewardType = RewardData.BULLET_UPGRADE; break;
            case 2: base = 6; rewardType = RewardData.BULLET_UPGRADE; break;
            case 3: base = 8; rewardType = RewardData.BULLET_UPGRADE; break;
            // 减速塔系列 → SLOW_DURATION_UPGRADE
            case 4: base = 30; rewardType = RewardData.SLOW_DURATION_UPGRADE; break;
            case 5: base = 45; rewardType = RewardData.SLOW_DURATION_UPGRADE; break;
            case 6: base = 60; rewardType = RewardData.SLOW_DURATION_UPGRADE; break;
            // 火箭塔系列 → EXPLOSIVES_UPGRADE
            case 7: base = 15; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            case 8: base = 25; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            case 9: base = 40; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            // 火焰塔系列 → FLAME_DURATION_UPGRADE
            case 10: base = 10; rewardType = RewardData.FLAME_DURATION_UPGRADE; break;
            case 11: base = 15; rewardType = RewardData.FLAME_DURATION_UPGRADE; break;
            // 防空塔系列 → BULLET_UPGRADE
            case 12: base = 10; rewardType = RewardData.BULLET_UPGRADE; break;
            case 13: base = 20; rewardType = RewardData.BULLET_UPGRADE; break;
            // 导弹塔系列 → EXPLOSIVES_UPGRADE
            case 14: base = 30; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            case 15: base = 50; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            // 迫击炮系列 → EXPLOSIVES_UPGRADE + CANNONBALL
            case 16: base = 20; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            case 17: base = 50; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            // 地雷系列 → EXPLOSIVES_UPGRADE
            case 20: base = 25; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            case 21: base = 400; rewardType = RewardData.EXPLOSIVES_UPGRADE; break;
            // 照明弹 → FLAME_DURATION_UPGRADE
            case 22: base = 40; rewardType = RewardData.FLAME_DURATION_UPGRADE; break;
            default: return 5;
        }

        if (rewardType >= 0) {
            return RewardData.applyReward(base, rewardType);
        }
        return base;
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
        // 空中狙击者：重型防空塔攻击范围翻倍
        if (type == 13 && RewardData.rewardLevel(RewardData.AIR_SNIPER) > 0) {
            radius *= 2;
        }
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
     * 获取升级类型（精确匹配原版 upgrade_type[] 数组，索引3为出售=0）
     * @param current_type 当前塔类型
     * @param upgrade_idx 升级选项索引（0=第一个，1=第二个，2=第三个）
     * @return 升级后的塔类型，-1 表示无此升级选项
     */
    public static int upgradeType(int current_type, int upgrade_idx) {
        // 索引 3 总是出售
        if (upgrade_idx == 3) return 0;

        switch (current_type) {
            // 机枪塔系列
            case 1: // gun_tower → gun_tower2 / flame_tower / aa_tower
                switch (upgrade_idx) {
                    case 0: return 2;
                    case 1: return 10;
                    case 2: return 12;
                    default: return -1;
                }
            case 2: // gun_tower2 → gun_tower3 / flame_tower / aa_tower
                switch (upgrade_idx) {
                    case 0: return 3;
                    case 1: return 10;
                    case 2: return 12;
                    default: return -1;
                }
            case 3: // gun_tower3 → heavy_aa_tower（铀弹奖励后升级）
                if (RewardData.rewardLevel(RewardData.URANIUM_SHELLS) > 0) {
                    return upgrade_idx == 0 ? 13 : -1;
                }
                return -1;

            // 减速塔系列
            case 4: // ice_tower → ice_tower2 / teleport* / mine*
                switch (upgrade_idx) {
                    case 0: return 5;
                    case 1: return RewardData.rewardLevel(RewardData.TELEPORT_TOWER) > 0 ? 18 : -1;
                    case 2: return RewardData.rewardLevel(RewardData.MINE_TOWER) > 0 ? 20 : -1;
                    default: return -1;
                }
            case 5: // ice_tower2 → ice_tower3
                return upgrade_idx == 0 ? 6 : -1;

            // 火箭塔系列
            case 7: // rocket_tower → rocket_tower2 / mortar / sam_tower
                switch (upgrade_idx) {
                    case 0: return 8;
                    case 1: return 16;
                    case 2: return 14;
                    default: return -1;
                }
            case 8: // rocket_tower2 → rocket_tower3
                return upgrade_idx == 0 ? 9 : -1;

            // 火焰塔系列
            case 10: // flame_tower → flame_tower2 (inferno)
                return upgrade_idx == 0 ? 11 : -1;

            // 防空塔系列
            case 12: // aa_tower → aa_tower2
                return upgrade_idx == 0 ? 13 : -1;

            // 导弹塔系列
            case 14: // sam_tower → sam_tower2
                return upgrade_idx == 0 ? 15 : -1;

            // 迫击炮系列
            case 16: // mortar → artillery
                return upgrade_idx == 0 ? 17 : -1;

            // 传送塔系列
            case 18: // teleport_unarmed → teleport_armed
                return upgrade_idx == 0 ? 19 : -1;

            // 地雷系列
            case 20: // mine_unarmed → mine_armed / flare*
                switch (upgrade_idx) {
                    case 0: return 21;
                    case 1: return RewardData.rewardLevel(RewardData.FLARE_TOWER) > 0 ? 22 : -1;
                    default: return -1;
                }

            // 终端塔（无进一步升级）
            case 6:  case 9:  case 11: case 13:
            case 15: case 17: case 19: case 21:
            case 22:
                return -1;
        }
        return -1;
    }

    /**
     * 获取降级类型（出售回退到的塔类型，0 = 直接出售）
     * 精确匹配原版 downgrade_type 字段
     */
    public static int downgradeType(int type) {
        switch (type) {
            case 2: return 1;   // 中型机枪 → 基础机枪
            case 3: return 2;   // 重型机枪 → 中型机枪
            case 5: return 4;   // 升级减速 → 基础减速
            case 6: return 5;   // 先进减速 → 升级减速
            case 8: return 7;   // 中型火箭 → 轻型火箭
            case 9: return 8;   // 重型火箭 → 中型火箭
            case 10: return 2;  // 火焰塔 → 中型机枪
            case 11: return 10; // 地狱之塔 → 火焰塔
            case 12: return 2;  // 防空塔 → 中型机枪
            case 13: return 12; // 重型防空 → 防空塔
            case 14: return 7;  // SAM → 轻型火箭
            case 15: return 14; // 先进SAM → SAM
            case 16: return 7;  // 迫击炮 → 轻型火箭
            case 17: return 16; // 火炮 → 迫击炮
            case 18: return 4;  // 传送塔 → 基础减速
            case 19: return 18; // 激活传送 → 未激活
            case 20: return 4;  // 地雷 → 基础减速
            case 21: return 20; // 触发地雷 → 未触发
            case 22: return 20; // 火炬塔 → 未触发地雷
            default: return -1;  // 基础塔 → 无降级（出售链终止）
        }
    }

    /**
     * 获取出售价值（含所有降级路径的累计成本）
     */
    public static int sellValue(int type) {
        int value = 0;
        int current = type;
        while (current > 0) {
            value += cost(current) / 2;
            current = downgradeType(current);
        }
        // 跳蚤市场：出售价值 ×1.5（原版 value + value/2）
        if (RewardData.rewardLevel(RewardData.FLEA_MARKET) > 0) {
            value += value / 2;
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
