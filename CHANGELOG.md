# 更新日志

本文件记录星际塔防（Robo Defense）桌面移植版的所有重要变更。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

---

## [2.5.0] — 2026-07-13

### 2026-07-15：存档系统修复

- **修复**：`GamePlayScreen(game, true)` 双参构造完全忽略布尔参数 → 继续游戏/加载存档从未生效
- **新增**：`GamePlayScreen.setResumeMode()` / `setLoadSlot(int)` 替代废弃的双参构造
- **修复**：`GameSaveScreen.loadSlot()` 加载后丢弃 GameState → 改为传递槽位 ID
- **修复**：`MainMenuScreen.resumeGame()` 改用 `setResumeMode()`

### 游戏核心

#### 新增
- 从原版 Android APK (Build 2900) 完整迁移所有游戏逻辑
- 23 种防御塔及其完整升级树（含奖励解锁分支）
- 11 种敌人类型，包含状态效果（减速、灼烧）
- 15 种子弹类型，含追踪、溅射、抛物线弹道
- 88 项成就系统，数据持久化到 SQLite
- 23 种奖励升级，效果接入游戏参数（攻击力、射速、范围等）
- 混合器模式：种子码生成随机地图布局
- 性能自适应监控器（5 级质量档位）
- 完整存档系统：10 个槽位 + 快速存档，SQLite 持久化

#### 修复
- `Enemy.applyDamage()`: 穿甲弹(type=12)无视护甲逻辑
- `Enemy.applyDamage()`: 冲击波(type=14)同时施加减速+灼烧
- `Enemy.applyDamage()`: 火焰粒子视觉效果
- `Enemy.applyDamage()`: 灼烧计数器不缩短已有灼烧
- `GameTower`: 传送塔、地雷塔（链式爆炸）、冲击波特殊行为
- `Bullet`: 迫击炮/火炮重力加速度方向
- `TowerData.shotType(20)`: 未触发地雷返回 NONE
- `CollisionGrid`: 塔碰撞范围中心偏移 +16px

#### 变更
- 子弹尺寸适配 32px 网格（原版 70px HD 网格）
- 积分计算除数改为 100 + 最低分 1 保护

### 前端 UI

#### 新增
- 科幻指挥中心风格统一设计系统（色板、面板、标题栏）
- 主菜单：居中对称布局，三级按钮分层
- 关卡选择：卡片式布局，地图缩略图预览，难度可点击滑条
- 暂停菜单：居中面板 + 保存退出/继续/不保存退出三选项
- HUD 血量条 + 状态指示灯
- ESC 暂停菜单、空格快进、键盘快捷键
- 塔按钮精灵图预览（底座 + 转头）

#### 修复
- 塔渲染预览与放置后尺寸统一为 GRID_PIXEL_SIZE
- `SpriteNames.tower()` 精灵图名称映射（types 12-22）
- 关卡选择点击检测与渲染坐标统一
- OptionsScreen 点击检测与渲染坐标统一
- 暂停菜单按钮重叠及相机变换

### 架构

#### 新增
- `MixerLevelGenerator` — 混合器随机地图生成器
- `PerformanceMonitor` — 自适应性能监控
- `MovementGrid.checkObstacleLayout()` — 障碍物布局验证
- `LevelData` MIXER_LEVEL 集成
- `RewardData.rewardLevel()` 辅助方法

### 文档
- 新增 `README.md` — 项目白皮书
- 新增 `CLAUDE.md` — AI 辅助开发指南
- 新增 `VERSION` — 版本文件
- 新增 `CHANGELOG.md` — 本文件
- 更新 `docs/Phase3迁移完成.md`、`docs/重构进度.md`

---

## 版本说明

本项目的版本号 2.5.0 继承自原版 Android APK 的版本号，表示这是原版 v2.5.0 (Build 2900) 的桌面移植版。

后续开发版本号将从此基线递增。
