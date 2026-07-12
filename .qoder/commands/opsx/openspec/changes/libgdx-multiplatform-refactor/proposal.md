## Why

当前项目是从 APK 反编译的 Android 专属代码，核心游戏逻辑与 Android 平台 API（Canvas、MediaPlayer、SharedPreferences）深度耦合，无法复用到其他平台。业务目标要求：保留 Android 版、新增 Web 版、后续支持联网功能（排行榜、云存档）。使用 libGDX 重构可以一套核心代码同时输出 Android + Desktop + HTML5，并为联网功能预留统一的服务层接口。

## What Changes

- **BREAKING** 整体项目结构从单模块 Android 工程重构为 libGDX 多平台工程（core / android / desktop / html）
- 抽取约 30 个纯游戏逻辑类到 `core` 模块，零平台依赖
- 定义平台抽象层接口（Renderer、Audio、Storage、Network），隔离 Android API
- 渲染层从 Android Canvas 迁移到 libGDX SpriteBatch + ShapeRenderer
- 音频层从 MediaPlayer 迁移到 libGDX Sound/Music
- 存档系统从 Java 序列化 + SharedPreferences 迁移到 JSON 格式 + 版本号
- 新增联网层：REST API 客户端，支持排行榜提交/查询、云存档同步
- 新增 Desktop Launcher（窗口管理、键盘快捷键、鼠标滚轮缩放）
- 新增 HTML5 Launcher（GWT 编译、中文字体支持）
- 资源格式转换：Android res → libGDX assets（TextureAtlas + BitmapFont + I18NBundle）
- 修复反编译遗留问题：删除内嵌 androidx 源码、统一 R 类引用、重命名混淆类

## Capabilities

### New Capabilities
- `platform-abstraction`: 平台抽象层接口定义（Renderer、Audio、Storage、Input、Network），使游戏核心与具体平台解耦
- `libgdx-rendering`: 基于 libGDX SpriteBatch 的渲染实现，替换 Android Canvas，支持 TextureAtlas、精灵动画、粒子效果
- `desktop-platform`: Desktop 平台启动器与适配（窗口管理、键盘/鼠标输入、HiDPI、全屏）
- `web-platform`: HTML5/GWT 平台启动器与适配（中文字体、触摸/鼠标兼容、资源异步加载）
- `network-service`: 联网服务层（REST 客户端、排行榜 API、云存档同步、登录认证）
- `save-system-v2`: 新版存档系统（JSON 格式、版本兼容、跨平台统一、云端同步接口）
- `asset-pipeline`: 资源管线（Android res → TextureAtlas + BitmapFont + I18NBundle 转换工具与规范）
- `code-cleanup`: 反编译代码清理（删除内嵌 androidx、统一 R 类、混淆类重命名、包结构重组）

### Modified Capabilities
<!-- 无已有 spec，此为全新项目重构 -->

## Impact

- **项目结构**：从单模块变为 4 模块（core/android/desktop/html），构建系统从纯 Android Gradle 变为 libGDX Gradle 多项目
- **依赖变更**：移除内嵌 androidx 源码 (~400 文件)；新增 libGDX 1.12+、libGDX GWT、Gson/Jackson
- **构建工具**：保持 Gradle，但需 libGDX 项目模板的多项目配置
- **API 影响**：所有 Activity 重写为 libGDX Screen；所有 Canvas 绘制重写为 SpriteBatch；所有 MediaPlayer 替换为 libGDX Sound
- **存档兼容**：新存档格式不兼容旧 Java 序列化存档，需提供一次性迁移工具
- **联网新增**：需后端服务支持（排行榜 API、用户认证、云存档存储），客户端新增 HTTP 客户端依赖
- **资源**：所有精灵图需重新打包为 TextureAtlas；字体需转为 BitmapFont/.fnt；strings.xml 需转为 .properties
