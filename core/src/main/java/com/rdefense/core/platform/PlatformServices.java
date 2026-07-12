package com.rdefense.core.platform;

/**
 * 平台服务聚合接口
 * 各平台 Launcher 通过此类注入具体实现
 */
public class PlatformServices {

    private GameRenderer renderer;
    private final GameAudio audio;
    private final GameStorage storage;
    private final GameInputHandler inputHandler;
    private final NetworkClient network;

    public PlatformServices(GameRenderer renderer, GameAudio audio,
                            GameStorage storage, GameInputHandler inputHandler) {
        this(renderer, audio, storage, inputHandler, null);
    }

    public PlatformServices(GameRenderer renderer, GameAudio audio,
                            GameStorage storage, GameInputHandler inputHandler,
                            NetworkClient network) {
        this.renderer = renderer;
        this.audio = audio;
        this.storage = storage;
        this.inputHandler = inputHandler;
        this.network = network;
    }

    /** 设置渲染器（用于延迟初始化） */
    public void setRenderer(GameRenderer renderer) {
        this.renderer = renderer;
    }

    public GameRenderer getRenderer() {
        return renderer;
    }

    public GameAudio getAudio() {
        return audio;
    }

    public GameStorage getStorage() {
        return storage;
    }

    public GameInputHandler getInputHandler() {
        return inputHandler;
    }

    public NetworkClient getNetwork() {
        return network;
    }

    /** 是否有可用的网络服务 */
    public boolean hasNetwork() {
        return network != null;
    }

    /** 是否阻止屏幕锁定 */
    public void setScreenTimeoutLock(boolean lock) {
        // 默认平台不支持，子平台可覆盖或直接使用 no-op
    }

    /** 获取当前电量百分比；不可用时返回 -1 */
    public int getBatteryLevel() {
        return -1;
    }

    /** 释放所有服务资源 */
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (audio != null) audio.dispose();
        if (storage != null) storage.dispose();
        if (network != null) network.dispose();
    }
}
