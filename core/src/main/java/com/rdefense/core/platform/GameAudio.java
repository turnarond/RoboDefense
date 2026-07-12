package com.rdefense.core.platform;

/**
 * 音频接口 — 隔离 libGDX Sound/MediaPlayer 等平台差异
 */
public interface GameAudio {

    /**
     * 加载音效文件
     * @param name 音效名称
     * @param filePath 音效文件路径（相对于 assets 目录）
     */
    void loadSound(String name, String filePath);

    /**
     * 播放音效
     * @param name 音效名称
     * @param volume 音量 (0.0 ~ 1.0)
     */
    void playSound(String name, float volume);

    /**
     * 停止所有音效
     */
    void stopAll();

    /**
     * 设置主音量
     */
    void setMasterVolume(float volume);

    /** 释放资源 */
    void dispose();
}
