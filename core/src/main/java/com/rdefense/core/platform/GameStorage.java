package com.rdefense.core.platform;

import java.util.Map;

/**
 * 存储接口 — 隔离 SharedPreferences / LocalStorage / 文件系统等平台差异
 */
public interface GameStorage {

    /** 保存键值对偏好设置 */
    void savePreference(String key, String value);

    /** 读取偏好设置，返回默认值如果不存在 */
    String getPreference(String key, String defaultValue);

    /** 读取整型偏好设置 */
    int getIntPreference(String key, int defaultValue);

    /** 读取布尔型偏好设置 */
    boolean getBoolPreference(String key, boolean defaultValue);

    /** 读取浮点型偏好设置 */
    float getFloatPreference(String key, float defaultValue);

    /** 保存完整游戏状态（JSON 字节数组） */
    void saveGameState(byte[] data);

    /** 加载完整游戏状态 */
    byte[] loadGameState();

    /** 检查是否存在游戏存档 */
    boolean hasGameState();

    /** 删除游戏存档 */
    void deleteGameState();

    /** 释放资源 */
    void dispose();
}
