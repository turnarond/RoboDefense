package com.rdefense.core.save.db;

/**
 * 存档槽位信息 - 用于存档列表显示
 */
public class SaveSlotInfo {
    /** 槽位ID (1-10) */
    public int slotId;
    /** 自定义存档名 */
    public String saveName;
    /** 地图类型 */
    public int levelType;
    /** 关卡号/波次 */
    public int levelNum;
    /** 难度等级 */
    public int difficulty;
    /** 生存模式 */
    public boolean survivalMode;
    /** 塔混合器 */
    public boolean towerMixer;
    /** 当前金币 */
    public int money;
    /** 当前积分 */
    public int score;
    /** 生命值 */
    public int health;
    /** 游戏时长（秒） */
    public int playTimeSecs;
    /** 创建时间戳 */
    public long createdAt;
    /** 更新时间戳 */
    public long updatedAt;
    /** 存档有效性 */
    public boolean isValid;
    /** 缩略图数据 */
    public byte[] thumbnail;

    public SaveSlotInfo() {}

    public SaveSlotInfo(int slotId) {
        this.slotId = slotId;
        this.isValid = false;
    }

    /**
     * 获取显示名称
     */
    public String getDisplayName() {
        if (saveName != null && !saveName.isEmpty()) {
            return saveName;
        }
        return "存档 " + slotId;
    }

    /**
     * 获取地图名称
     */
    public String getMapName() {
        String[] mapNames = {"基础", "遗迹", "工厂", "庭院", "混合", "道路", "天空塔"};
        if (levelType >= 0 && levelType < mapNames.length) {
            return mapNames[levelType];
        }
        return "未知";
    }

    /**
     * 格式化游戏时长
     */
    public String getPlayTimeFormatted() {
        int hours = playTimeSecs / 3600;
        int mins = (playTimeSecs % 3600) / 60;
        int secs = playTimeSecs % 60;
        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, mins, secs);
        }
        return String.format("%02d:%02d", mins, secs);
    }

    /**
     * 格式化保存时间
     */
    public String getUpdateTimeFormatted() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(updatedAt));
    }
}
