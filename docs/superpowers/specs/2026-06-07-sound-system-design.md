# 音效系统设计文档

> **日期**: 2026-06-07
> **项目**: 星际塔防桌面版音效系统

## 目标

为桌面版移植完整的音效系统，与原版安卓保持一致。

## 音效资源

已存在的音效文件（`assets/sounds/`）：

| 文件名 | 用途 |
|--------|------|
| gun.ogg | 机枪子弹射击声 |
| ice.ogg | 冰冻效果声 |
| rocket.ogg | 火箭发射声 |
| fire.ogg | 火焰效果声 |
| mortar.ogg | 迫击炮弹声 |

## 子弹类型与音效映射

| 子弹类型 | 音效文件 | 说明 |
|---------|---------|------|
| 2, 3, 7, 12 | gun.ogg | 机枪、重机枪、减速机枪、防空机枪 |
| 4 | ice.ogg | 冰冻子弹 |
| 5, 8 | rocket.ogg | 火箭弹、空对地导弹 |
| 6, 14 | fire.ogg | 火焰弹、地雷 |
| 9, 10, 13 | mortar.ogg | 迫击炮、炮弹、原子弹 |
| 其他 | 无 | 无音效 |

## 音效触发点

| 事件 | 触发位置 | 说明 |
|------|---------|------|
| 发射子弹 | `GameTower.handleShot()` | 炮塔开火时 |
| 敌人被击败 | `GameState.killEnemy()` | 敌人死亡时 |
| 塔出售 | `GameTower.sell()` | 出售炮塔时 |
| 塔升级 | `GameTower.upgrade()` | 升级炮塔时 |
| 波次开始 | `GameState.processEvent()` | 新波次开始时 |
| 波次结束 | `GameState.checkWaveEnd()` | 波次结束时 |

## 技术架构

### 文件结构

```
core/src/main/java/com/rdefense/core/
    └── audio/
        └── SoundManager.java    ← 音效管理器（单例）

core/assets/sounds/              ← 音效文件（已存在）
    ├── gun.ogg
    ├── ice.ogg
    ├── rocket.ogg
    ├── fire.ogg
    └── mortar.ogg
```

### SoundManager 类设计

**职责**：
1. 加载和管理音效资源
2. 根据子弹类型播放对应音效
3. 提供音效开关控制
4. 管理音效实例池（避免重复加载）

**公共接口**：

```java
public class SoundManager {
    // 初始化音效系统
    public static void init();

    // 播放子弹音效
    public static void fireBullet(int shotType);

    // 播放敌人击败音效
    public static void playEnemyDefeated();

    // 播放塔出售音效
    public static void playTowerSold();

    // 播放塔升级音效
    public static void playTowerUpgraded();

    // 播放波次开始音效
    public static void playWaveStart();

    // 播放波次结束音效
    public static void playWaveEnd();

    // 设置音效开关
    public static void setSoundEnabled(boolean enabled);

    // 获取音效开关状态
    public static boolean isSoundEnabled();

    // 释放资源
    public static void dispose();
}
```

### 配置扩展

在 `OptionsData` 中新增音效开关配置项：

| 配置键 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| sound_enabled | boolean | true | 音效开关 |

## 实现步骤

1. 创建 `audio/SoundManager.java`
2. 在 `BulletData` 中定义子弹类型常量
3. 在 `OptionsData` 中添加音效开关配置
4. 在 `GameTower.handleShot()` 中调用音效
5. 在 `GameState` 中添加敌人击败/波次音效
6. 在游戏暂停和继续时处理音效
7. 测试所有音效触发点

## 与原版对比

| 功能 | 原版安卓 | 桌面版 |
|------|---------|--------|
| 音效引擎 | Android MediaPlayer | libGDX Sound |
| 音效格式 | OGG | OGG |
| 子弹音效 | 支持 | 待实现 |
| 敌人击败音效 | 不支持 | 待实现 |
| 塔出售/升级音效 | 不支持 | 待实现 |
| 波次音效 | 不支持 | 待实现 |
| 音效开关 | 系统音量 | 配置项 |

## 扩展性

未来可轻松添加：
- 背景音乐（使用 libGDX Music 类）
- 更多音效类型
- 音量滑块控制
