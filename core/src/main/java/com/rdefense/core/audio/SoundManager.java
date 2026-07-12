package com.rdefense.core.audio;

import com.rdefense.core.platform.GameAudio;
import com.rdefense.core.platform.PlatformServices;

/**
 * 音效管理器单例
 * 封装 GameAudio，提供更高级的音效播放接口
 */
public final class SoundManager {

    // 音效名称常量
    public static final String SOUND_GUN = "gun";
    public static final String SOUND_ICE = "ice";
    public static final String SOUND_ROCKET = "rocket";
    public static final String SOUND_FIRE = "fire";
    public static final String SOUND_MORTAR = "mortar";

    private static SoundManager instance;
    private GameAudio audio;
    private boolean soundEnabled = true;

    private SoundManager() {
    }

    public static synchronized SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    /**
     * 初始化音效系统
     * @param services PlatformServices 实例
     */
    public void init(PlatformServices services) {
        if (services != null) {
            this.audio = services.getAudio();
        }
    }

    /**
     * 预加载所有音效
     */
    public void preloadSounds() {
        if (audio == null) return;
        audio.loadSound(SOUND_GUN, "sounds/gun.ogg");
        audio.loadSound(SOUND_ICE, "sounds/ice.ogg");
        audio.loadSound(SOUND_ROCKET, "sounds/rocket.ogg");
        audio.loadSound(SOUND_FIRE, "sounds/fire.ogg");
        audio.loadSound(SOUND_MORTAR, "sounds/mortar.ogg");
    }

    /**
     * 根据子弹类型播放对应音效
     * @param shotType 子弹类型（对应 BulletData 常量）
     */
    public void fireBullet(int shotType) {
        if (!soundEnabled || audio == null) return;

        String soundName = getSoundForBulletType(shotType);
        if (soundName != null) {
            audio.playSound(soundName, 1.0f);
        }
    }

    /**
     * 播放敌人击败音效
     */
    public void playEnemyDefeated() {
        if (!soundEnabled || audio == null) return;
        // 原版无此音效，可选实现
    }

    /**
     * 播放塔出售音效
     */
    public void playTowerSold() {
        if (!soundEnabled || audio == null) return;
        // 原版无此音效，可选实现
    }

    /**
     * 播放塔升级音效
     */
    public void playTowerUpgraded() {
        if (!soundEnabled || audio == null) return;
        // 原版无此音效，可选实现
    }

    /**
     * 播放波次开始音效
     */
    public void playWaveStart() {
        if (!soundEnabled || audio == null) return;
        // 原版无此音效，可选实现
    }

    /**
     * 播放波次结束音效
     */
    public void playWaveEnd() {
        if (!soundEnabled || audio == null) return;
        // 原版无此音效，可选实现
    }

    /**
     * 根据子弹类型获取对应音效名称
     * @return 音效名称，或 null 如果无对应音效
     */
    private String getSoundForBulletType(int shotType) {
        switch (shotType) {
            case 2: case 3: case 7: case 12:
                return SOUND_GUN;
            case 4:
                return SOUND_ICE;
            case 5: case 8:
                return SOUND_ROCKET;
            case 6: case 14:
                return SOUND_FIRE;
            case 9: case 10: case 13:
                return SOUND_MORTAR;
            default:
                return null;
        }
    }

    /**
     * 设置音效开关
     */
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    /**
     * 获取音效开关状态
     */
    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * 释放资源
     */
    public void dispose() {
        if (audio != null) {
            audio.stopAll();
        }
    }
}