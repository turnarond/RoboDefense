# 星际塔防 · Robo Defense

> 经典 2D 塔防游戏 | libGDX 桌面版 | 原版 Android APK 移植重构

---

## 游戏简介

星际塔防（Robo Defense）是一款经典的 2D 塔防游戏。玩家在网格地图上策略性地放置、升级防御塔，阻止敌人突破防线。游戏支持 7 种地图、23 种防御塔、11 种敌人类型、88 项成就和 23 种奖励升级。

原作为 Android 平台游戏（MagicWach, v2.5.0），本项目将其完整迁移至桌面平台，基于 libGDX 框架重构，核心逻辑 100% 平台无关。

### 截图

| 主菜单 | 游戏界面 |
|--------|---------|
| ![主菜单](docs/images/主界面.png) | ![游戏界面](docs/images/游戏界面.png) |

| 成就 | 奖励商店 |
|------|---------|
| ![成就](docs/images/成就.png) | ![奖励](docs/images/奖励.png) |

> 运行 `java -jar desktop.jar` 体验

---

## 快速开始

### 环境要求

- **Java 8+** (推荐 JDK 17+)
- **操作系统**: Windows / macOS / Linux

### 下载与运行

```bash
# 方式一：直接运行 JAR
java -jar desktop.jar

# 方式二：从源码构建
./gradlew :desktop:dist
java -jar desktop/build/libs/desktop.jar
```

### 操作说明

| 操作 | 键位/方式 |
|------|----------|
| 放置防御塔 | 从右下角按钮拖拽到地图空格 |
| 升级/出售塔 | 点击已放置的防御塔 |
| 暂停菜单 | `ESC` |
| 快进（2x） | `空格` |
| 平移视角 | 鼠标拖拽地图空白处 |
| 缩放 | `↑` `↓` 方向键 |

---

## 游戏内容

### 地图（7 种）

| # | 地图 | 英文名 | 特点 |
|---|------|--------|------|
| 0 | 基地 | Basic Level | 标准 S 形路径 |
| 1 | 遗迹 | The Ruins | 双路径交叉 |
| 2 | 工厂 | The Factory | 含预置障碍物 |
| 3 | 庭院 | The Courtyard | 三路径开放式 |
| 4 | VR 训练场 | VR Training | 种子码随机生成 |
| 5 | 巷道 | Roadway | 窄通道，星空背景 |
| 6 | 通天塔 | Sky Tower | 垂直布局 |

### 防御塔（23 种）

```
机枪塔 Lv1 → Lv2 → Lv3 ─┬→ 防空炮 → 重型防空炮
                         ├→ 火焰塔 → 地狱之塔
                         └→ (铀弹：穿甲弹)

减速塔 Lv1 → Lv2 → Lv3 ─┬→ 传送塔 → 武装传送塔
                         └→ 地雷塔 → 触发地雷 ─→ 火炬塔

火箭塔 Lv1 → Lv2 → Lv3 ─┬→ 迫击炮 → 火炮
                         └→ 地对空导弹 → 先进 SAM
```

### 敌人（11 种）

士兵 · 重甲兵 · 跑步者 · 卡车 · 轻坦克 · 重坦克 · 直升机 · 战斗机 · 轰炸机 · 泰坦（BOSS）· 投弹者

### 奖励升级（23 种）

使用通关积分购买永久升级：弹药增强、爆炸增强、装填加速、效果延长、传送塔解锁、地雷塔解锁、铀弹、凝固汽油弹等。

### 成就系统（88 项）

涵盖难度通关、地图挑战、完美通关、特殊挑战、生存模式、VR 训练等类别。

---

## 技术架构

```
├── core/                    # 平台无关核心逻辑
│   ├── game/                # 游戏逻辑（GameState, Enemy, Tower, Bullet）
│   ├── render/              # 渲染系统（GameSceneRenderer, Camera, UiRenderer）
│   ├── scene/               # 场景管理（9 个界面）
│   ├── platform/            # 平台抽象层（5 个服务接口）
│   ├── save/                # 存档系统（SQLite）
│   ├── audio/               # 音效管理
│   ├── input/               # 输入处理
│   └── config/              # 配置数据
├── desktop/                 # 桌面启动器（LWJGL3）
├── assets/                  # 游戏资源（图片、字体、音效、本地化）
└── docs/                    # 设计文档
```

### 技术栈

| 组件 | 版本 |
|------|------|
| 游戏框架 | libGDX 1.12.1 |
| 桌面后端 | LWJGL 3 |
| 数据库 | SQLite (sqlite-jdbc 3.45.1.0) |
| 构建系统 | Gradle 9.x |
| 语言 | Java 8+ (source/target 1.8) |
| 字体 | FreeType + SimHei (黑体) |

---

## 开发

### 构建命令

```bash
./gradlew :core:compileJava     # 编译核心模块
./gradlew :desktop:dist         # 构建桌面 JAR
./gradlew :desktop:run          # 运行桌面版
./gradlew clean                 # 清理构建产物
```

### 项目结构

```
星际塔防（Robo Defense）
├── core/                 # 游戏核心模块 (63 个 Java 文件)
├── desktop/              # 桌面平台启动器
├── assets/               # 游戏资源
│   ├── images/           # 精灵图、背景图 (85 个文件)
│   ├── sounds/           # 音效 (6 个 OGG)
│   ├── fonts/            # 中文字体
│   └── i18n/             # 中文本地化 (350 条)
├── docs/                 # 文档
├── build.gradle          # 根构建脚本
└── settings.gradle       # 模块配置
```

### 调试参数

```bash
# 跳过主菜单直接进入游戏
java -Drdefense.debugStartGame=true -jar desktop.jar

# 强制触发 HD 图像回退
java -Drdefense.debugForceHdFallback=true -jar desktop.jar
```

---

## 许可证

本项目基于原版 Android APK (MagicWach, v2.5.0) 逆向工程和重构，仅供学习交流使用。原始游戏版权归原作者所有。

---

## 文档

- [CHANGELOG](CHANGELOG.md) · [白皮书](docs/01-白皮书.md) · [APK 差距分析](docs/06-APK差距分析报告.md) · [技术文章](docs/articles/2026-07-21-apk-to-desktop-port.md)

## 许可证

本项目基于原版 Android APK (MagicWach, v2.5.0) 逆向工程和重构，仅供学习交流使用。原始游戏版权归原作者所有。

---

## 致谢

- **原版作者**: MagicWach — Android 平台《星际塔防》v2.5.0
- **桌面移植**: [turnarond](https://github.com/turnarond)
- **框架**: [libGDX](https://libgdx.com) — 跨平台游戏开发框架
- **字体**: SimHei (黑体) — 中文界面支持
- **反编译工具**: [jadx](https://github.com/skylot/jadx) — APK 逆向分析
