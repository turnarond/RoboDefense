# 音效系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标:** 为桌面版移植完整的音效系统，集成到现有的 GameAudio 架构中

**架构:** 使用已有的 GameAudio 接口（libGDX Sound），创建 SoundManager 单例管理音效播放

**技术栈:** libGDX Sound, Java 单例模式

---

## 文件结构

```
core/src/main/java/com/rdefense/core/
    └── audio/
        └── SoundManager.java          ← 新建：音效管理器单例

core/assets/sounds/                      ← 已存在
    ├── gun.ogg
    ├── ice.ogg
    ├── rocket.ogg
    ├── fire.ogg
    └── mortar.ogg

修改的文件：
    core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
    core/src/main/java/com/rdefense/core/game/GameTower.java
    core/src/main/java/com/rdefense/core/game/GameState.java
    core/src/main/java/com/rdefense/core/RoboDefenseGame.java
```

---

## Task 1: 创建 SoundManager 单例

**Files:**
- Create: `core/src/main/java/com/rdefense/core/audio/SoundManager.java`

- [ ] **Step 1: 创建 audio 目录**

```bash
mkdir -p /home/ycd/develop/xingjitafang/core/src/main/java/com/rdefense/core/audio
```

- [ ] **Step 2: 创建 SoundManager.java**

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add core/src/main/java/com/rdefense/core/audio/SoundManager.java
git commit -m "feat: add SoundManager singleton for audio management"
```

---

## Task 2: 在 RoboDefenseGame 中初始化 SoundManager

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/RoboDefenseGame.java`

- [ ] **Step 1: 添加 SoundManager 字段**

在 RoboDefenseGame 类中添加：
```java
import com.rdefense.core.audio.SoundManager;
```

在字段区域添加：
```java
private SoundManager soundManager;
```

- [ ] **Step 2: 在 initializeServices 中初始化 SoundManager**

在 `initializeServices()` 方法中添加：
```java
// 初始化音效系统
soundManager = SoundManager.getInstance();
soundManager.init(services);
soundManager.preloadSounds();
```

- [ ] **Step 3: 在 applyOptions 中同步音效开关**

修改 `applyOptions()` 方法，将：
```java
if (services.getAudio() != null) {
    services.getAudio().setMasterVolume(
            optionsData.optionValue(OptionsData.ENABLE_SOUND) ? 1.0f : 0.0f);
}
```

改为：
```java
if (services.getAudio() != null) {
    services.getAudio().setMasterVolume(
            optionsData.optionValue(OptionsData.ENABLE_SOUND) ? 1.0f : 0.0f);
}

if (soundManager != null) {
    soundManager.setSoundEnabled(optionsData.optionValue(OptionsData.ENABLE_SOUND));
}
```

- [ ] **Step 4: 添加 getSoundManager 方法**

```java
public SoundManager getSoundManager() {
    return soundManager;
}
```

- [ ] **Step 5: 提交**

```bash
git add core/src/main/java/com/rdefense/core/RoboDefenseGame.java
git commit -m "feat: integrate SoundManager with RoboDefenseGame"
```

---

## Task 3: 在 GameTower 中添加发射子弹音效

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameTower.java`

- [ ] **Step 1: 添加 SoundManager 引用和 import**

在 import 区域添加：
```java
import com.rdefense.core.audio.SoundManager;
```

在 handleShot 方法开始处（发射子弹逻辑前）添加：
```java
SoundManager.getInstance().fireBullet(shot_type);
```

- [ ] **Step 2: 编译验证**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 3: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameTower.java
git commit -m "feat: add bullet fire sound in GameTower"
```

---

## Task 4: 在 GameState 中添加其他音效

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`

- [ ] **Step 1: 添加 SoundManager import 和字段**

```java
import com.rdefense.core.audio.SoundManager;
```

- [ ] **Step 2: 查找 killEnemy 方法并添加音效**

找到 `killEnemy` 方法，在方法开始处添加：
```java
SoundManager.getInstance().playEnemyDefeated();
```

- [ ] **Step 3: 查找波次相关方法并添加音效**

找到波次开始的位置（processEvent 或类似方法），添加：
```java
SoundManager.getInstance().playWaveStart();
```

找到波次结束的位置（checkWaveEnd 或类似方法），添加：
```java
SoundManager.getInstance().playWaveEnd();
```

- [ ] **Step 4: 编译验证**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 5: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "feat: add enemy defeat and wave sound effects"
```

---

## Task 5: 添加塔出售/升级音效（可选）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameTower.java`

- [ ] **Step 1: 在 sell 方法中添加音效**

找到 sell 方法，添加：
```java
SoundManager.getInstance().playTowerSold();
```

- [ ] **Step 2: 在 upgrade 方法中添加音效**

找到 upgrade 方法，添加：
```java
SoundManager.getInstance().playTowerUpgraded();
```

- [ ] **Step 3: 编译验证**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameTower.java
git commit -m "feat: add tower sell and upgrade sound effects"
```

---

## Task 6: 集成测试

**Files:**
- None (manual testing)

- [ ] **Step 1: 构建桌面版**

```bash
./gradlew :desktop:dist
```

- [ ] **Step 2: 运行并测试**

```bash
java -jar desktop/build/libs/desktop.jar
```

测试清单：
- [ ] 主菜单 → 开始游戏 → 放置炮塔 → 开火听到音效
- [ ] 选项菜单 → 关闭音效 → 确认无声音效
- [ ] 开启音效 → 确认有声音效
- [ ] 击败敌人 → 听到敌人死亡音效（如已实现）
- [ ] 出售/升级塔 → 听到相应音效（如已实现）
- [ ] 波次开始/结束 → 听到相应音效（如已实现）

---

## 验证清单

- [ ] SoundManager 单例正确实现
- [ ] 音效文件正确加载（gun.ogg, ice.ogg, rocket.ogg, fire.ogg, mortar.ogg）
- [ ] 子弹类型与音效正确映射
- [ ] 音效开关正确响应
- [ ] GameTower.handleShot() 调用音效
- [ ] GameState 相关方法调用音效（可选）
- [ ] 编译通过
- [ ] 运行时无异常

---

## 扩展性备注

如需添加背景音乐，可使用 libGDX Music 类：
```java
import com.badlogic.gdx.audio.Music;

private Map<String, Music> music = new HashMap<>();

public void loadMusic(String name, String filePath) {
    Music track = Gdx.audio.newMusic(Gdx.files.internal(filePath));
    music.put(name, track);
}

public void playMusic(String name, boolean loop) {
    Music track = music.get(name);
    if (track != null) {
        track.setLooping(loop);
        track.play();
    }
}
```
