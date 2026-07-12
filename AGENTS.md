# AGENTS.md

This file provides guidance to the AI agent when working with code in this repository.

# 语言规则
- 所有回答都使用简体中文。
- 代码注释、错误信息和提示都使用中文。
- 当涉及专有技术术语时，可以保留英文原文，但请提供中文解释。
- 代码本身（如变量名、函数名）、命令行指令、配置文件内容，则根据实际需要保持原样，不要翻译。

## Project Overview

星际塔防（Robo Defense）是一款经典的 2D 塔防游戏，基于 libGDX 框架开发，支持桌面平台运行。项目从 Android APK 反编译后进行了全面重构，核心逻辑已迁移到平台无关的 `core` 模块。

## 架构说明

### 模块结构

| 模块 | 包名 | 说明 |
|------|------|------|
| `core` | `com.rdefense.core` | 游戏核心逻辑，平台无关，包含游戏状态、渲染、输入、存档等子系统 |
| `desktop` | `com.rdefense.desktop` | 桌面版启动器（LWJGL3），提供平台服务实现 |

### 核心子系统

- **平台服务抽象** (`PlatformServices`)：统一封装渲染、音频、存储、输入、网络，各平台注入具体实现
- **场景管理** (`GameScreen`)：基类提供生命周期管理，子类包括主菜单、关卡选择、游戏界面、设置、成就、奖励等
- **游戏核心** (`GameState`)：对象池管理、网格系统、事件系统、碰撞检测
- **游戏循环** (`GameLoop`)：固定 30fps 时间步长，支持快进模式
- **存档系统**：SQLite 数据库存储游戏进度、成就、奖励积分

## Critical Gotchas

- **无测试代码**：项目中没有单元测试或集成测试
- **Java 8 兼容性**：项目使用 Java 8 source/target，确保代码兼容性

## Build

```bash
./gradlew :desktop:dist    # 构建桌面版独立 JAR → desktop/build/libs/desktop.jar
```

**运行桌面版：**
```bash
java -jar desktop/build/libs/desktop.jar
```

WSL2 环境下启动器会自动检测并设置 `LIBGL_ALWAYS_SOFTWARE=1`（Mesa 硬件 GL 在 WSL2 下会 SIGSEGV）。Windows 侧直接运行 JAR 无需额外设置。

- libGDX 1.12.1，Java 8 source/target 兼容性
- `gradle.properties` 中 `android.useAndroidX=true` 和 `android.nonFinalResIds=false` 为遗留配置，目前不影响桌面构建

## Linux 环境配置

`local.properties` 和 `gradle.properties` 可能包含本地路径：

1. **`local.properties`**：若需要 Android 构建，需设置 `sdk.dir` 路径
2. **`gradle.properties`**：`org.gradle.jvmargs` 配置 Gradle JVM 参数

## Localization

所有用户界面文本为简体中文，存储在 `assets/i18n/` 目录下的属性文件中。添加或修改字符串时，请使用中文。

## 目录结构

```
├── core/                    # 游戏核心模块
│   └── src/main/java/com/rdefense/core/
│       ├── game/            # 游戏逻辑（GameState, Enemy, Tower, Bullet）
│       ├── render/          # 渲染系统（GameWorldRenderer, HudRenderer）
│       ├── scene/           # 场景管理（MainMenu, GamePlay, LevelSelect）
│       ├── platform/        # 平台抽象层（PlatformServices, GameRenderer）
│       ├── save/            # 存档系统（SQLite, PlayerPrefs）
│       ├── audio/           # 音效管理
│       ├── input/           # 输入处理
│       └── config/          # 配置数据
├── desktop/                 # 桌面版启动器
├── assets/                  # 游戏资源（图片、字体、音效）
├── docs/                    # 文档（设计文档、需求文档）
└── gradle/                  # Gradle 构建脚本
```
