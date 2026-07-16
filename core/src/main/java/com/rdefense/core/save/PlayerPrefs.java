package com.rdefense.core.save;

import com.rdefense.core.save.db.GameSaveManager;

/**
 * 玩家偏好设置 — 对应 Android 原版 SharedPreferences（"AndroidDefense"）
 * 移植自原版 Constants.java 中的所有 *_PREF_STR 键。
 *
 * <p>原版使用 SharedPreferences 存储玩家设置：关卡进度、地图选择、难度、生存模式、
 * 塔混合器、奖励积分、成就进度等。本类使用 GameStorage 抽象接口（libGDX Preferences），
 * 保留与原版完全相同的键名和语义。</p>
 *
 * <p>所有方法命名与原版 Constants 字段一一对应，便于在重写游戏逻辑时与 JADX 反编译
 * 输出做对照，保证行为兼容。</p>
 */
public class PlayerPrefs {

    private static final String PREFS_NAME = "AndroidDefense";

    // ===== 偏好键（与原版 Constants.java 完全一致）=====

    /** 当前选择的地图 ID（0=基础 1=遗迹 2=工厂 3=庭院 4=混合 5=道路 6=天空塔） */
    public static final String MAP_PREF_STR = "ADMapSelection";
    /** 当前选择的难度等级 */
    public static final String DIFFICULTY_PREF_STR = "ADDifficultyLevel";
    /** 最大通关关卡（每 10 关为一个台阶） */
    public static final String MAX_LEVEL_WON_STR = "ADMaxLevelWon";
    /** 是否启用生存模式 */
    public static final String SURVIVAL_MODE_STR = "AndroidDefenseSurvivalMode";
    /** 是否启用塔混合器 */
    public static final String TOWER_MIXER_ENABLED = "AndroidDefenseTowerMixerEnabled";
    /** 塔混合器选塔值 */
    public static final String TOWER_MIXER_VALUE_STR = "AndroidDefenseTowerMixerValue";
    /** 混合器选关值 */
    public static final String MIXER_VALUE_STR = "AndroidDefenseMixerValue";
    /** 调试存档标志 */
    public static final String DEBUG_SAVE_FLAG_PREF_STR = "ADDebugSaveFlag";
    /** 调试存档槽 */
    public static final String DEBUG_SAVE_SLOT_PREF_STR = "ADDebugSaveSlot";
    /** 快速存档可用标志 */
    public static final String QUICK_SAVE_AVAIL_PREF_STR = "AndroidDefenseQuickSaveValid";
    /** 启动消息（用于跨 Activity 传递） */
    public static final String STARTUP_MSG_PREF_STR = "AndroidDefenseStartupMsg";
    /** 启动错误（用于跨 Activity 传递） */
    public static final String STARTUP_ERROR_PREF_STR = "AndroidDefenseStartupError";
    /** 已请求权限标志 */
    public static final String PERMISSION_REQUESTED_PREF_STR = "AndroidDefensePermissionRequested";
    /** 启动次数 */
    public static final String TIMES_LAUNCHED_PREF_STR = "ADTimesLaunched";
    /** 游戏是否正常启动过（用于检测上次崩溃） */
    public static final String GAME_STARTED_OK_PREF_STR = "AndroidDefenseGameStartedOK";
    /** 生存者模式对话框已显示标志 */
    public static final String SURVIVOR_DIALOG_SHOWN = "AndroidDefenseSurvivorDialogShown";
    /** 缩放比例 */
    public static final String ZOOM_SCALE_PREF_STR = "AndroidDefenseZoomScale";
    /** 奖励积分 32 位 */
    public static final String REWARD_POINTS_PREF_STR_32 = "ADRewardPoints";
    /** 奖励积分 64 位（兼容旧版本溢出） */
    public static final String REWARD_POINTS_PREF_STR_64 = "ADRewardPoints64";

    private final GameSaveManager saveManager;

    public PlayerPrefs(GameSaveManager saveManager) {
        if (saveManager == null) {
            throw new IllegalArgumentException("GameSaveManager cannot be null");
        }
        this.saveManager = saveManager;
    }

    // ===== 通用读写 =====

    public int getInt(String key, int def) {
        return saveManager.getPreferenceInt(key, def);
    }

    public void putInt(String key, int value) {
        saveManager.setPreferenceInt(key, value);
    }

    public boolean getBool(String key, boolean def) {
        return saveManager.getPreferenceBool(key, def);
    }

    public void putBool(String key, boolean value) {
        saveManager.setPreferenceBool(key, value);
    }

    public String getString(String key, String def) {
        return saveManager.getPreference(key, def);
    }

    public void putString(String key, String value) {
        saveManager.setPreference(key, value);
    }

    public float getFloat(String key, float def) {
        return saveManager.getPreferenceFloat(key, def);
    }

    public void putFloat(String key, float value) {
        saveManager.setPreferenceFloat(key, value);
    }

    // ===== 业务便捷方法 =====

    /** 读取当前地图 ID */
    public int getMap() {
        return getInt(MAP_PREF_STR, 0);
    }

    /** 保存当前地图 ID */
    public void putMap(int mapId) {
        putInt(MAP_PREF_STR, mapId);
    }

    /** 读取当前难度等级 */
    public int getDifficulty() {
        return getInt(DIFFICULTY_PREF_STR, 0);
    }

    /** 保存当前难度等级 */
    public void putDifficulty(int level) {
        putInt(DIFFICULTY_PREF_STR, level);
    }

    /** 读取最大已通关关卡 */
    public int getMaxLevelWon() {
        return getInt(MAX_LEVEL_WON_STR, 0);
    }

    /** 保存最大已通关关卡 */
    public void putMaxLevelWon(int level) {
        putInt(MAX_LEVEL_WON_STR, level);
    }

    /** 是否启用生存模式 */
    public boolean isSurvivalMode() {
        return getBool(SURVIVAL_MODE_STR, false);
    }

    public void putSurvivalMode(boolean enabled) {
        putBool(SURVIVAL_MODE_STR, enabled);
    }

    /** 是否启用塔混合器 */
    public boolean isTowerMixerEnabled() {
        return getBool(TOWER_MIXER_ENABLED, false);
    }

    public void putTowerMixerEnabled(boolean enabled) {
        putBool(TOWER_MIXER_ENABLED, enabled);
    }

    public int getMixerValue() {
        return getInt("mixer_value", 0);
    }

    public void putMixerValue(int value) {
        putInt("mixer_value", value);
    }

    public int getTowerMixerValue() {
        return getInt("tower_mixer_value", 0);
    }

    public void putTowerMixerValue(int value) {
        putInt("tower_mixer_value", value);
    }

    /** 是否存在快速存档 */
    public boolean isQuickSaveAvailable() {
        return getBool(QUICK_SAVE_AVAIL_PREF_STR, false);
    }

    public void putQuickSaveAvailable(boolean available) {
        putBool(QUICK_SAVE_AVAIL_PREF_STR, available);
    }

    /** 奖励积分（读取时自动合并 32+64 位以兼容老版本溢出） */
    public long getRewardPoints() {
        long lo = getInt(REWARD_POINTS_PREF_STR_32, 0) & 0xFFFFFFFFL;
        long hi = getInt(REWARD_POINTS_PREF_STR_64, 0);
        return (hi << 32) | lo;
    }

    /** 保存奖励积分（拆分高低 32 位存储） */
    public void putRewardPoints(long points) {
        int lo = (int) (points & 0xFFFFFFFFL);
        long hi = points >>> 32;
        putInt(REWARD_POINTS_PREF_STR_32, lo);
        putInt(REWARD_POINTS_PREF_STR_64, (int) hi);
    }

    /** 启动消息（用于跨场景传递） */
    public String getStartupMsg() {
        return getString(STARTUP_MSG_PREF_STR, null);
    }

    public void clearStartupMsg() {
        putString(STARTUP_MSG_PREF_STR, null);
    }

    /** 启动错误 */
    public String getStartupError() {
        return getString(STARTUP_ERROR_PREF_STR, null);
    }

    public void clearStartupError() {
        putString(STARTUP_ERROR_PREF_STR, null);
    }

    /** 上次游戏是否正常启动（用于崩溃检测） */
    public boolean isGameStartedOk() {
        return getBool(GAME_STARTED_OK_PREF_STR, true);
    }

    public void putGameStartedOk(boolean ok) {
        putBool(GAME_STARTED_OK_PREF_STR, ok);
    }

    /** 启动次数 */
    public int getTimesLaunched() {
        return getInt(TIMES_LAUNCHED_PREF_STR, 0);
    }

    public void putTimesLaunched(int times) {
        putInt(TIMES_LAUNCHED_PREF_STR, times);
    }

    /** 自增并返回启动次数（与原版 commitPrefs 行为一致） */
    public int incrementTimesLaunched() {
        int t = getTimesLaunched() + 1;
        putTimesLaunched(t);
        return t;
    }
}
