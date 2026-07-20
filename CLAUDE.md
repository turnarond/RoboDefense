# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 语言规则

- 所有交互、代码注释、文档均使用简体中文
- 专有技术名词（如 libGDX、LWJGL3、SQLite）保留英文，但需提供中文说明
- 代码符号（变量名、函数名、包名）、命令行指令、配置文件内容保持原样

## 构建与运行

```bash
# 构建桌面版独立 JAR → desktop/build/libs/desktop.jar
./gradlew :desktop:dist

# 运行桌面版
java -jar desktop/build/libs/desktop.jar

# Gradle 任务列表
./gradlew tasks --all

# 清理构建产物
./gradlew clean
```

**WSL2 注意事项**：启动器会自动检测 WSL 环境并设置 `LIBGL_ALWAYS_SOFTWARE=1`（Mesa 硬件 GL 在 WSL2 下会 SIGSEGV），Windows 原生环境无需额外设置。

## 文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| 白皮书 | `docs/01-白皮书.md` | 项目概述、快速开始、游戏内容 |
| 需求文档 | `docs/02-需求文档.md` | 完整功能需求（F-Req-001~XXX） |
| 架构设计 | `docs/03-架构设计.md` | 系统架构、模块设计、数据流、关键算法 |
| 详细方案 | `docs/04-详细方案设计.md` | 子系统详细设计、性能优化、屏幕适配 |
| 接口设计 | `docs/05-接口设计.md` | 平台服务接口、存档接口、SQLite 表结构 |
| APK 差距分析 | `docs/06-APK差距分析报告.md` | 60 类全覆盖差距分析（原版 vs 桌面版） |
| UI 设计交互评审 | `docs/07-UI设计交互评审报告.md` | 设计一致性与交互可用性评审 |
| UI 渲染架构评审 | `docs/08-UI渲染架构评审报告.md` | 渲染性能与代码架构评审 |

## 技术栈与约束

| 项目 | 版本/配置 |
|------|-----------|
| 构建系统 | Gradle（Groovy DSL） |
| 游戏框架 | libGDX 1.12.1 |
| Java 兼容性 | Java 8（source/target 1.8） |
| 桌面后端 | LWJGL3 |
| 数据库 | SQLite（sqlite-jdbc 3.45.1.0） |
| 中文字体 | FreeType（fonts/simhei.ttf） |

**关键约束**：
- **无测试代码**：项目中不存在任何单元测试或集成测试
- `gradle.properties` 中的 `android.useAndroidX=true` 和 `android.nonFinalResIds=false` 为遗留配置，不影响桌面构建
- 项目从 Android APK 反编译后重构而来，部分代码保留了原版的命名风格和设计模式

## 模块架构

```
┌──────────────────────────────────────────────┐
│  desktop（桌面启动器）                         │
│  com.rdefense.desktop                        │
│  ├── DesktopLauncher        LWJGL3 入口      │
│  └── DesktopPlatformServices  平台服务实现    │
└──────────────┬───────────────────────────────┘
               │ 依赖
┌──────────────▼───────────────────────────────┐
│  core（平台无关核心）                          │
│  com.rdefense.core                           │
│  ├── RoboDefenseGame.java   主游戏类（入口）   │
│  ├── game/    游戏逻辑与数据结构               │
│  │   ├── Starfield.java      星空粒子背景      │
│  ├── render/  渲染系统与游戏循环               │
│  │   ├── GameSceneRenderer.java  游戏场景渲染   │
│  │   ├── AchievementRenderer.java  成就弹窗     │
│  ├── scene/   场景管理（Screen 子类）           │
│  ├── platform/  平台抽象接口                   │
│  ├── save/    存档系统（SQLite）               │
│  ├── audio/   音效管理                         │
│  ├── input/   输入处理                         │
│  │   ├── GameInputController.java  键盘控制     │
│  └── config/  配置数据                         │
└──────────────────────────────────────────────┘
```

## 核心架构设计

### 平台服务抽象层（PlatformServices）

`PlatformServices` 是核心 IoC 容器，聚合了五个平台服务接口：
- **GameRenderer**：渲染抽象（核心实现为 `LibGdxRenderer`）
- **GameAudio**：音频抽象（核心实现为 `LibGdxAudio`）
- **GameStorage**：持久化存储抽象（核心实现为 `LibGdxStorage`）
- **GameInputHandler**：输入处理抽象（通过 `LibGdxInputAdapter` 桥接）
- **NetworkClient**：网络抽象（桌面版为 `NoOpNetworkClient`）

桌面版通过构造函数注入具体实现，Android 版可注入各平台特定实现。

### 游戏循环（GameLoop）

固定 30fps 时间步长，使用纳秒累加器：
- 逻辑帧以 30fps 固定速率推进
- 渲染帧以显示器刷新率运行（通常 60fps）
- 低帧率模式：`lowerFpsMode` 将逻辑步长翻倍（60fps 到 30fps）
- 快进模式：每帧执行 3 次逻辑更新

`GameLoop.tick(LogicUpdater)` → `GamePlayScreen` 传入 `gameState::nextState` 作为回调。

### 对象池系统

`GameState` 中大量使用链表实现的对象池，避免 GC：
- **Enemy**：`enemy_list`（活跃）→ `enemy_graveyard`（死亡但播放动画中）→ `enemy_pool`（空闲）
- **GameTower**：`tower_list`（活跃）↔ `tower_pool`（空闲）
- **Bullet**：`bullet_list`（活跃）↔ `bullet_pool`（空闲）
- **GameEvent**：`event_list[]`（按类型分桶）↔ `event_pool`（空闲）

分配时从 pool 取出，回收时放回 pool。分配上限为 100 个敌人。

### 场景管理

所有场景继承 `GameScreen`（实现了 libGDX 的 `Screen` 接口），生命周期：
`show()` → `init()` → `render()` → `update()` / `draw()` → `dispose()`

场景切换通过 `RoboDefenseGame.setScreen()` 完成。

### 存档/加载系统

### 存档流程

```
主菜单
 ├─ 新游戏 → 清除快速存档 → 关卡选择 → GamePlayScreen(新游戏)
 ├─ 继续游戏 → quickLoad() → GamePlayScreen(resumeMode=true)
 └─ 载入存档 → GameSaveScreen(加载模式) → 选槽位 → GamePlayScreen(loadSlotId=N)
         └─ 保存模式 → 选槽位 → createSave(state, slotId)

游戏内 ESC
 ├─ 继续 → 关闭菜单
 ├─ 保存并退出 → quickSave(state) → 主菜单
 └─ 不保存退出 → 主菜单
```

### GamePlayScreen 三种启动模式（重构后）

`GamePlayScreen` 经重构后缩减至 ~550 行协调角色。渲染批处理移至 `GameSceneRenderer`，按键处理移至 `GameInputController`。绘制流程统一为相机变换→世界层(begin/end)→屏幕层(begin/end)→条件覆盖层。

| 模式 | 触发方式 | 行为 |
|------|---------|------|
| 新游戏 | 默认构造 | `initGame(level, diff)` |
| 继续游戏 | `setResumeMode()` | `initGame()` → `quickLoad()` |
| 加载槽位 | `setLoadSlot(N)` | `initGame()` → `loadSave(N)` |

### 关键 API

```java
GamePlayScreen gps = new GamePlayScreen(game);
gps.setResumeMode();          // 从快速存档恢复
gps.setLoadSlot(slotId);      // 从指定槽位加载
gps.configureLevel(map, diff); // 新游戏（关卡选择）
```

## 存档系统

使用 SQLite 数据库（`saves/game_saves.db`），管理以下数据：
- **save_slots**：存档元数据（关卡、分数、金钱、时间戳）
- **save_states**：存档二进制数据（GameState 序列化）
- **player_progress**：玩家进度（偏好设置、最高关卡、奖励积分）
- **achievements**：成就数据

存档序列化使用魔数 `1094993222` + 版本号验证，通过 `GameSaveWriter`/`GameSaveReader` 接口读写。

### 网格与路径系统

- **CollisionGrid**：碰撞检测网格，每帧重置，按位置添加敌人
- **MovementGrid**：寻路网格，用于敌人移动和塔放置验证（放置塔后需 `calcPaths` 重算路径）
- **GridObjectOrder**：维护 Y 排序链表，确保渲染顺序正确（上方对象后渲染）

### 事件系统

`GameEvent` 按类型分桶存储（`EVENT_ENEMY_DEFEATED`、`EVENT_MONEY_CHANGED` 等），渲染系统遍历事件列表播放动画和更新 UI。

## 重要注意事项

1. **无测试**：修改游戏逻辑后需手动运行验证。关键调试属性：
   - `-Drdefense.debugForceHdFallback=true`：强制触发 HD 回退
   - `-Drdefense.debugStartGame=true`：跳过主菜单直接进入游戏

2. **Java 8 限制**：不可使用 Java 9+ API（如 `List.of()`、`var`、模块系统等），lambda 和 try-with-resources 可用。

3. **资源文件**：`assets/` 目录为游戏运行时工作目录（desktop 模块的 `workingDir` 指向 `../assets`），资源加载使用 `Gdx.files.internal()` 相对路径。

4. **存档兼容性**：存档魔数和版本号在 `SQLiteSaveManager` 中定义，修改存档格式时必须同步更新版本号。

5. **中文字体**：使用 simhei.ttf + FreeType 动态生成位图字体，`CHINESE_CHARS` 常量硬编码了所有界面汉字。添加新中文文本时需同步更新该常量。

6. **HD 资源回退**：`LibGdxRenderer` 在 OOM 时自动触发回退，调用 `HdFallbackListener` 禁用 HQ 模式并持久化设置。

## APK 反编译对比验证

原版 APK：`xingjitafngv2.5.0_bvev_7273.com.apk`（Build 2900）

使用 `jadx -d /tmp/apk_decompiled --no-res` 反编译可得 362 个 Java 类，其中游戏核心在 `com.magicwach.rdefense` 包下（60 个类）。

### 已验证与修复的关键逻辑

| 文件 | 对比结果 | 备注 |
|------|---------|------|
| `MovementGrid.java` | ✅ BFS 寻路与原版一致 | `calcPaths()` 和 `findPathGreedy()` 等效 |
| `Bullet.java` | ✅ 追踪、抛物线、溅射正确 | 烟雾粒子限值硬编码 50（原版自适配） |
| `Enemy.java` | ✅ 已修复 | `applyDamage()` 3 处偏差已修正 |
| `GameTower.java` | ✅ 已修复 | 传送塔/地雷塔/冲击波行为已接入 |
| `LevelDataGenerator.java` | ✅ 编码格式一致 | `bits[24-30]=delay, [15-23]=initDelay, [10-14]=type, [3-9]=count, [0-2]=path` |

### `applyDamage` 伤害类型速查

```
4  = 减速弹（仅施加减速，无伤害）
6  = 火焰弹（仅灼烧，无直接伤害）
12 = 穿甲弹/铀弹（无视护甲！）
13 = 凝固汽油弹（部分灼烧 + 常规伤害）
14 = 冲击波（灼烧 + 减速，无直接伤害）
```

## 奖励升级系统

23 种奖励升级通过 `RewardData` 管理，效果已接入游戏参数：

| 奖励 | 效果 | 接入点 |
|------|------|-------|
| BULLET_UPGRADE (0) | 机枪/防空塔威力↑ | `TowerData.power()` |
| EXPLOSIVES_UPGRADE (1) | 火箭/导弹/迫击炮威力↑ | `TowerData.power()` |
| ROCKET_SPEED (2) | 火箭装填速度↑ | `TowerData.shotDelay()` |
| ANTIAIR_SPEED (3) | 防空导弹装填↑ | `TowerData.shotDelay()` |
| ARTILLERY_SPEED (4) | 迫击炮装填↑ | `TowerData.shotDelay()` |
| FLAME_DURATION (5) | 火焰威力↑ | `TowerData.power()` |
| SLOW_DURATION (6) | 减速持续时间↑ | `TowerData.power()` |
| URANIUM_SHELLS (11) | 重型机枪→穿甲弹 | `TowerData.shotType()` |
| NAPALM_SHELLS (12) | 火炮→凝固汽油弹 | `TowerData.shotType()` |
| SLOW_BURN (15) | 地狱之塔→冲击波 | `TowerData.shotType()` |
| AIR_SNIPER (13) | 重型防空范围×2 | `TowerData.attackRadiusSq()` |
| FLEA_MARKET (16) | 出售价值×1.5 | `TowerData.sellValue()` (`value += value/2`) |
| STARTING_CASH (8) | 初始金钱↑ | `GameState.initGame()` |
| HEALTH_UPGRADE (7) | 初始生命↑ | `GameState.initGame()` |

## 成就系统

88 项成就通过 `AchievementData.increaseLevel()` 触发，检测点分布在：
- `GameState.setMoney()` — 金钱里程碑
- `GameState.enemyDefeated()` — 击杀/得分里程碑
- `GameState.nextLevel()` — 完美通关/快进/生存模式
- `GameState.gameWonAchievements()` — 胜利时全部检测（难度/地图/特殊成就）
- `GameState.endGame()` → `gameWonAchievements()`
- `GameTower.handleMine()` — 地雷链成就

## APK 保真修复 + UI 重构记录（2026-07-17〜20）

共 83 次提交，覆盖 6 个 P0、~40 条 P1、UI 全线重设计。

### APK 保真（计划①〜④）——游戏逻辑
- **结算体系**：`saveScore` 四奖金公式（20%/1%/20%/金钱×难度×2）、`RewardData.gameWon` 难度递增、`/500` 除数、逐图分数倍率
- **数值保真**：溅射半径 256/2048、出售 1.5×、SAM 减免、死亡帧 `(value<<1)+10`、速度 `×32/40`、冲击波类型10
- **健壮性**：事件 500 上限、敌人 100 上限、读档校验+强制暂停、每10关自动存档、消息槽位
- **存档补全**：10 字段（8 成就追踪 + at_exit/exiting_grid）
- **新功能**：混合器面板、Starfield、ScoreOverlay、控制按钮接入
- **死代码清理**：GameRewardCalculator、HudRenderer、tower_pool

### UI 全线重设计——视觉/交互/架构
- **主菜单「指挥中心」**：深空黑底+金色信标+全息青副标题+扫描线动画
- **战斗 HUD「战术覆盖层」**：紧凑一行(地图/难/$/杀/HP/PTS)、HP≤3脉冲呼吸、状态入脚注
- **升级弹窗「数据卡片」**：细分隔线、出售锈红、半透明玻璃面板
- **商城**：纯图标(底座+转头)、钢板蓝/暗深红/全息青边框状态编码
- **成就界面**：56px行高、描述文字+进度条、分类标签
- **架构重构**：提取 GameSceneRenderer(446行)+GameInputController(67行)、GamePlayScreen 931→553行、标题栏/返回按钮统一到 GameScreen 基类、begin/end 10→2 对
- **Y 排序**：塔+敌人统一经 GridObjectOrder 链表绘制
- **字库补全**：10+ 缺失字符（？！▲▼←→×草混迹院）

### 升级树修复
- 二级机枪：删除错误的火焰塔(10)/防空塔(12)分支（原版仅升级到三级）
- 升级弹窗点击坐标与渲染布局同步

### 新增文件
| 文件 | 用途 |
|------|------|
| `docs/06-APK差距分析报告.md` | 60 类全覆盖差距分析 |
| `docs/07-UI设计交互评审报告.md` | 设计一致性与交互可用性 |
| `docs/08-UI渲染架构评审报告.md` | 渲染性能与代码架构 |
| `core/.../render/GameSceneRenderer.java` | 游戏场景渲染层(446行) |
| `core/.../input/GameInputController.java` | 键盘输入控制(67行) |
| `core/.../game/Starfield.java` | 星空粒子背景(47行) |

### 已修复的关键偏差
- 恢复 `saveScore` 四奖金结算（20% 胜利奖金 + 1% 生命奖金 + 20% 完美奖金 + 金钱×难度×2）
- 恢复 `RewardData.gameWon` 难度递增体系
- 恢复成就弹窗链路（dequeueEarned → showAchievement，经 GamePlayScreen 轮询）
- 实现混合器 5 位数码选择面板（LevelSelectScreen 内嵌，解除 mixerValue 死锁）
- 实现 ScoreOverlay 结算动画、Starfield 星空粒子、控制按钮接入
- 击杀得分除数 500、逐图分数倍率保真（含 skytower 第 7 种地图）
- 数值保真 10 项（溅射半径 2500→256、出售倍率 2×→1.5×、死亡帧 10→(value<<1)+10 等）
- 存档补全 10 个字段（8 个成就追踪 + at_exit/exiting_grid）
- 健壮性（事件/敌人上限安全网、读档强制暂停、自动存档请求、消息槽位、HUD 千位分隔）
- 死代码清理（HudRenderer、tower_pool、GameRewardCalculator）

### 新发现（此前文档需订正）
- 原版共 **7 种地图**（0-6，含 skytower），非此前记载的 6 种
- `HudRenderer` 为死代码——HUD 实际由 `GamePlayScreen` 直接渲染
- `GameRewardCalculator` 系数表（ICE/LAVA/EXTREME）为臆造，已删除
- 保真决策点：Z_ACCEL 保留当前（更物理正确的弹道）、TOUGH_MASK 恢复原版死代码、敌人拒放检查恢复、灼烧封顶恢复原版

### 关键架构变更
- `GameSceneRenderer`（446行）+ `GameInputController`（67行）+ `Starfield`（47行）— 新增类
- `GamePlayScreen`：931→553行（-41%）— 纯协调角色
- 标题栏/返回按钮：7+6 Screen 统一到 `GameScreen` 基类
- 塔+敌人 Y 排序：`drawSortedObjects` 经 `GridObjectOrder` 链表单次遍历
- 升级树修复：二级机枪仅升级到三级（删除错误的火焰/防空分支）
- CHINESE_CHARS：新增 10+ 缺失字符（？！▲▼←→×草混迹院等）
