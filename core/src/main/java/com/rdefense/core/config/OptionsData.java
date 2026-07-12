package com.rdefense.core.config;

import com.rdefense.core.platform.GameStorage;

/**
 * 通用选项系统
 * 在 core 层负责读取/保存用户选项，并提供统一访问接口。
 */
public final class OptionsData {

    public static final int ENABLE_SOUND = 0;
    public static final int FAST_FORWARD_LEVEL_PAUSE = 1;
    public static final int FAST_FORWARD_LIFE_PAUSE = 2;
    public static final int SCREEN_TIMEOUT_LOCK = 3;
    public static final int SHOW_TURRET_RANGE = 4;
    public static final int SHOW_BATTERY_GAUGE = 5;
    public static final int SHOW_ZOOM_LEVEL = 6;
    public static final int SHOW_DRAW_PERFORMANCE = 7;
    public static final int HQ_GRAPHICS_MODE = 8;
    public static final int BITMAP_FILTERING = 9;
    public static final int BACKGROUND_16BIT = 10;
    public static final int CLASSIC_BACKGROUNDS = 11;
    public static final int LOWER_FPS = 12;

    public static final int OPTION_TYPE_COUNT = 13;

    private static final String OPTIONS_PREFIX = "ADOptions:";

    private static final String[] OPTION_KEYS = {
        "Enable Sound",
        "Deactivate Fast Forward Between Levels",
        "Fast Forward Pause After Life Loss",
        "Prevent Screen From Locking",
        "Show Turret Range While Placing/Upgrading",
        "Show Battery Gauge",
        "Show Zoom Control At Top",
        "Show Drawing Performance During Pause",
        "High Quality Graphics Mode",
        "Bitmap Filtering",
        "16-bit Color Background",
        "Use Classic Graphics",
        "Reduce Framerate"
    };

    private static final String[] OPTION_NAMES = {
        "开启音效",
        "关卡间停止快进",
        "生命损失时暂停快进",
        "禁止屏幕锁定",
        "放置/升级时显示塔范围",
        "显示电量指示",
        "顶部显示缩放控件",
        "暂停时显示性能",
        "高品质图像模式",
        "位图过滤",
        "16 位背景",
        "经典背景",
        "降低帧率"
    };

    private static final boolean[] DEFAULT_VALUES = {
        true,  // ENABLE_SOUND
        true,  // FAST_FORWARD_LEVEL_PAUSE
        true,  // FAST_FORWARD_LIFE_PAUSE
        true,  // SCREEN_TIMEOUT_LOCK
        true,  // SHOW_TURRET_RANGE
        true,  // SHOW_BATTERY_GAUGE
        true,  // SHOW_ZOOM_LEVEL
        false, // SHOW_DRAW_PERFORMANCE
        true,  // HQ_GRAPHICS_MODE
        true,  // BITMAP_FILTERING
        false, // BACKGROUND_16BIT
        false, // CLASSIC_BACKGROUNDS
        false  // LOWER_FPS
    };

    private final GameStorage storage;
    private final boolean[] values;

    public OptionsData(GameStorage storage) {
        if (storage == null) {
            throw new IllegalArgumentException("GameStorage cannot be null");
        }
        this.storage = storage;
        this.values = new boolean[OPTION_TYPE_COUNT];
        init();
    }

    public void init() {
        for (int i = 0; i < OPTION_TYPE_COUNT; i++) {
            this.values[i] = this.storage.getBoolPreference(getPreferenceKey(i), DEFAULT_VALUES[i]);
        }
    }

    public boolean optionValue(int type) {
        validateOptionType(type);
        return this.values[type];
    }

    public boolean setOptionValue(int type, boolean value) {
        validateOptionType(type);
        if (this.values[type] == value) {
            return false;
        }
        this.values[type] = value;
        this.storage.savePreference(getPreferenceKey(type), Boolean.toString(value));
        return true;
    }

    public String optionName(int type) {
        validateOptionType(type);
        return OPTION_NAMES[type];
    }

    private String getPreferenceKey(int type) {
        return OPTIONS_PREFIX + OPTION_KEYS[type];
    }

    private void validateOptionType(int type) {
        if (type < 0 || type >= OPTION_TYPE_COUNT) {
            throw new IllegalArgumentException("Invalid option type: " + type);
        }
    }
}
