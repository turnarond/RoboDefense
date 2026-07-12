package com.rdefense.core.platform.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.rdefense.core.platform.GameStorage;

/**
 * libGDX 平台的存储实现
 * Android/Desktop 使用本地文件，HTML5 使用 LocalStorage
 */
public class LibGdxStorage implements GameStorage {

    private static final String PREFERENCES_NAME = "robo_defense";
    private static final String GAME_STATE_KEY = "game_state";

    private final Preferences prefs;

    public LibGdxStorage() {
        this.prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
    }

    @Override
    public void savePreference(String key, String value) {
        prefs.putString(key, value);
        prefs.flush();
    }

    @Override
    public String getPreference(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    @Override
    public int getIntPreference(String key, int defaultValue) {
        return prefs.getInteger(key, defaultValue);
    }

    @Override
    public boolean getBoolPreference(String key, boolean defaultValue) {
        return prefs.getBoolean(key, defaultValue);
    }

    @Override
    public float getFloatPreference(String key, float defaultValue) {
        return prefs.getFloat(key, defaultValue);
    }

    @Override
    public void saveGameState(byte[] data) {
        // 将字节数组转为 Base64 字符串存储（Preferences 不支持直接存字节）
        String encoded = com.badlogic.gdx.utils.Base64Coder.encodeString(new String(data));
        prefs.putString(GAME_STATE_KEY, encoded);
        prefs.flush();
    }

    @Override
    public byte[] loadGameState() {
        String encoded = prefs.getString(GAME_STATE_KEY, null);
        if (encoded == null || encoded.isEmpty()) {
            return null;
        }
        String decoded = com.badlogic.gdx.utils.Base64Coder.decodeString(encoded);
        return decoded.getBytes();
    }

    @Override
    public boolean hasGameState() {
        return prefs.contains(GAME_STATE_KEY);
    }

    @Override
    public void deleteGameState() {
        prefs.remove(GAME_STATE_KEY);
        prefs.flush();
    }

    @Override
    public void dispose() {
        prefs.flush();
    }
}
