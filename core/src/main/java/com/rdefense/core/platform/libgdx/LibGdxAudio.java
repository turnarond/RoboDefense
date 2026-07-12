package com.rdefense.core.platform.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.rdefense.core.platform.GameAudio;

import java.util.HashMap;
import java.util.Map;

/**
 * libGDX 平台的音频实现
 * 使用 Gdx.audio.newSound() 加载和播放音效
 */
public class LibGdxAudio implements GameAudio {

    private final Map<String, Sound> sounds = new HashMap<String, Sound>();
    private float masterVolume = 1.0f;

    @Override
    public void loadSound(String name, String filePath) {
        if (sounds.containsKey(name)) {
            return;
        }
        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal(filePath));
            sounds.put(name, sound);
        } catch (Exception e) {
            // 音效加载失败时静默处理
        }
    }

    @Override
    public void playSound(String name, float volume) {
        Sound sound = sounds.get(name);
        if (sound == null) {
            // 延迟加载
            loadSound(name, "sounds/" + name + ".ogg");
            sound = sounds.get(name);
        }
        if (sound != null) {
            sound.play(masterVolume * volume);
        }
    }

    @Override
    public void stopAll() {
        for (Sound sound : sounds.values()) {
            sound.stop();
        }
    }

    @Override
    public void setMasterVolume(float volume) {
        this.masterVolume = volume;
    }

    @Override
    public void dispose() {
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();
    }
}
