package com.rdefense.core.save.db;

import com.rdefense.core.game.GameState;
import java.util.List;

/**
 * 统一存档管理接口 - 支持多存档槽位和快速存档
 */
public interface GameSaveManager {

    // ========== 存档槽位管理 ==========

    /**
     * 获取所有存档槽位信息
     */
    List<SaveSlotInfo> getAllSlots();

    /**
     * 获取指定槽位信息
     */
    SaveSlotInfo getSlot(int slotId);

    /**
     * 创建/覆盖存档
     * @param slotId 槽位ID (1-10)
     * @param state 游戏状态
     * @param name 存档名称（可选）
     * @return 是否成功
     */
    boolean createSave(int slotId, GameState state, String name);

    /**
     * 加载存档
     * @param slotId 槽位ID
     * @param state 目标游戏状态
     * @return 是否成功
     */
    boolean loadSave(int slotId, GameState state);

    /**
     * 删除存档
     */
    boolean deleteSave(int slotId);

    /**
     * 检查存档是否存在
     */
    boolean hasSave(int slotId);

    /**
     * 获取下一个空闲槽位
     */
    int getNextFreeSlot();

    /**
     * 获取存档数量
     */
    int getSaveCount();

    // ========== 快速存档（兼容原版） ==========

    /**
     * 快速存档（槽位0）
     */
    boolean quickSave(GameState state);

    /**
     * 快速读档
     */
    boolean quickLoad(GameState state);

    /**
     * 是否有快速存档
     */
    boolean hasQuickSave();

    /**
     * 清除快速存档
     */
    void clearQuickSave();

    // ========== 玩家进度 ==========

    /**
     * 获取最大通关关卡
     */
    int getMaxLevelWon();

    /**
     * 设置最大通关关卡
     */
    void setMaxLevelWon(int level);

    /**
     * 获取奖励积分
     */
    long getRewardPoints();

    /**
     * 设置奖励积分
     */
    void setRewardPoints(long points);

    // ========== 偏好设置（兼容原版 SharedPreferences） ==========

    /**
     * 获取字符串偏好
     */
    String getPreference(String key, String defaultValue);

    /**
     * 设置字符串偏好
     */
    void setPreference(String key, String value);

    /**
     * 获取整数偏好
     */
    int getPreferenceInt(String key, int defaultValue);

    /**
     * 设置整数偏好
     */
    void setPreferenceInt(String key, int value);

    /**
     * 获取布尔偏好
     */
    boolean getPreferenceBool(String key, boolean defaultValue);

    /**
     * 设置布尔偏好
     */
    void setPreferenceBool(String key, boolean value);

    /**
     * 获取浮点偏好
     */
    float getPreferenceFloat(String key, float defaultValue);

    /**
     * 设置浮点偏好
     */
    void setPreferenceFloat(String key, float value);

    // ========== 成就系统 ==========

    /**
     * 获取成就进度
     * @param key 成就键名
     * @return 成就等级，不存在返回 null
     */
    String getAchievement(String key);

    /**
     * 保存成就进度（批量格式：key1=value1;key2=value2;...）
     */
    void saveAchievements(String achievements);

    // ========== 工具方法 ==========

    /**
     * 关闭数据库连接
     */
    void close();

    /**
     * 获取数据库路径
     */
    String getDatabasePath();
}
