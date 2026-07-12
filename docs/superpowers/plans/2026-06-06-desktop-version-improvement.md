# 星际塔防桌面版开发计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成桌面版相比安卓版缺失的功能，实现完整的游戏体验

**Architecture:** 基于 libGDX 跨平台框架，采用模块化设计，已完成成就系统，需继续实现奖励商店、混音器关卡、调试模式等功能

**Tech Stack:** Java, libGDX, SQLite, Gradle

---

## 已完成功能

- [x] 成就系统完整实现 (88个成就)
- [x] 成就列表界面 (搜索/筛选)
- [x] 成就达成弹窗动画

---

## 待开发功能

### 优先级1：奖励商店系统

**参考文件：** `app/src/main/java/com/magicwach/rdefense/RewardData.java`

| 子任务 | 文件 | 描述 |
|--------|------|------|
| 1.1 | Create: `core/src/main/java/com/rdefense/core/game/RewardData.java` | 奖励物品数据模型（体力加成、护盾等） |
| 1.2 | Modify: `core/src/main/java/com/rdefense/core/scene/RewardScreen.java` | 升级奖励商店界面 |
| 1.3 | Create: `core/src/main/java/com/rdefense/core/render/RewardRenderer.java` | 奖励购买弹窗渲染器 |
| 1.4 | Modify: `core/src/main/java/com/rdefense/core/GameState.java` | 集成奖励效果到游戏逻辑 |

**验收标准：**
- 显示可购买的奖励物品列表
- 正确扣除/增加奖励积分
- 购买后效果生效（体力加成、护盾等）

---

### 优先级2：混音器关卡系统

**参考文件：** `app/src/main/java/com/magicwach/rdefense/MixerSelectActivity.java`

| 子任务 | 文件 | 描述 |
|--------|------|------|
| 2.1 | Create: `core/src/main/java/com/rdefense/core/game/MixerLevelGenerator.java` | 混音器关卡生成器 |
| 2.2 | Modify: `core/src/main/java/com/rdefense/core/scene/MixerSelectScreen.java` | 升级混音器选择界面 |
| 2.3 | Modify: `core/src/main/java/com/rdefense/core/GameState.java` | 支持混音器关卡初始化 |

**验收标准：**
- 用户可选择地图元素组合生成关卡
- 生成有效的关卡路径
- 成功进入自定义关卡

---

### 优先级3：调试模式

**参考文件：** `app/src/main/java/com/magicwach/rdefense/DebugActivity.java`

| 子任务 | 文件 | 描述 |
|--------|------|------|
| 3.1 | Create: `core/src/main/java/com/rdefense/core/debug/DebugMenu.java` | 调试菜单界面 |
| 3.2 | Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java` | 添加调试功能入口 |
| 3.3 | Create: `core/src/main/java/com/rdefense/core/debug/DebugCommands.java` | 调试命令实现 |

**验收标准：**
- 显示 FPS/内存监控
- 支持关卡跳转
- 支持无敌模式开关

---

### 优先级4：存档导出/导入

**参考文件：** `app/src/main/java/com/magicwach/rdefense/SDBackup.java`

| 子任务 | 文件 | 描述 |
|--------|------|------|
| 4.1 | Modify: `core/src/main/java/com/rdefense/core/save/db/GameSaveManager.java` | 添加导出/导入接口 |
| 4.2 | Create: `core/src/main/java/com/rdefense/core/save/SaveExportManager.java` | 存档导出管理器 |
| 4.3 | Modify: `core/src/main/java/com/rdefense/core/scene/OptionsScreen.java` | 添加导出/导入按钮 |

**验收标准：**
- 可将存档导出为 JSON 文件
- 可从 JSON 文件导入存档
- 导出文件可跨平台使用

---

## 测试计划

每个功能完成后需验证：

1. **编译验证**：`./gradlew :core:compileJava`
2. **运行验证**：`./gradlew :desktop:dist && java -jar desktop/build/libs/desktop.jar`
3. **功能验证**：
   - 奖励商店：购买物品，检查积分变化和效果
   - 混音器：生成并进入自定义关卡
   - 调试模式：打开调试菜单，测试各项功能

---

## 文件路径参考

### 核心文件
- `core/src/main/java/com/rdefense/core/GameState.java` - 游戏状态管理
- `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java` - 游戏主场景
- `core/src/main/java/com/rdefense/core/scene/MainMenuScreen.java` - 主菜单

### 参考原版
- `app/src/main/java/com/magicwach/rdefense/RewardData.java` - 奖励数据
- `app/src/main/java/com/magicwach/rdefense/MixerSelectActivity.java` - 混音器
- `app/src/main/java/com/magicwach/rdefense/DebugActivity.java` - 调试模式
- `app/src/main/java/com/magicwach/rdefense/SDBackup.java` - 存档备份

---

## 下一步

**建议执行顺序：**
1. 奖励商店系统（优先级最高）
2. 混音器关卡系统
3. 调试模式
4. 存档导出/导入

**执行方式选择：**
1. Subagent-Driven (推荐) - 每任务派发独立子代理
2. Inline Execution - 当前会话批量执行
