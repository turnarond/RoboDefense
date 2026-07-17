# APK 差距分析设计（原版 Android vs 桌面版）

- 日期：2026-07-17
- 状态：已批准（brainstorming 会话中用户逐项确认）
- 基准：`xingjitafngv2.5.0_bvev_7273.com.apk`（Build 2900），jadx 反编译产物位于
  `/tmp/apk_decompiled/sources/com/magicwach/rdefense/`，共 60 个类
- 对比对象：当前仓库 `core` 模块（63 个 Java 文件）与 `desktop` 模块

## 1. 目标与范围（用户决策记录）

| 决策点 | 结论 |
|--------|------|
| 最终目标 | 产出完整差距报告，并对需修复项走设计→计划→实现流程 |
| 对比范围 | 60 类全部纳入；Android 特有功能标注「不适用」并给等价替代建议 |
| 验证深度 | 混合深度：已验证类抽查复核、未验证类逐方法、纯 UI 类功能级 |
| 执行方式 | 先建映射总表，再按功能域并行只读代理对比，主线程复核汇总 |

## 2. 分域与深度策略

### 深度定义

- **抽查复核**：每类抽 2-3 个关键方法或常量表核对（优先此前修复过的点与数值表）；
  发现任一偏差则该类升级为逐方法全查。
- **逐方法**：类中每个非平凡方法核对逻辑等价性与常量值一致性；
  getter/setter 与纯样板代码可跳过。
- **功能级**：确认原版类承担的每项用户可见功能在当前实现中有对应物且行为一致
  （交互结果、数据展示）；不比逐行实现，不比视觉样式（UI 已有意重设计为科幻主题）。

### 分域清单（合计 60 类）

**域① 核心玩法数值（18 类）**
Enemy、EnemyData、GameTower、TowerData、ActiveTower、Bullet、BulletData、
MovementGrid、CollisionGrid、LevelData、LevelDataGenerator、MixerLevelGenerator、
ExplosionData、Vector、VectorLookup、FastRandom、GridObject、GridObjectOrder

- 其中已验证类（抽查复核即可）：MovementGrid、Bullet、Enemy、GameTower、TowerData、
  LevelDataGenerator（见 CLAUDE.md「APK 反编译对比验证」）
- 其余 12 类未做过系统验证：逐方法对比

**域② 游戏状态与事件（7 类，全部逐方法）**
GameState、GameEvent、RewardData、AchievementData、PerformanceMonitor、C、G

- GameState 是原版最大的类且从未全量验证，为本次风险最高项
- C、G 为常量/全局类：逐常量核对数值

**域③ UI 界面与控件（24 类，功能级）**
GameActivity、TitleActivity、LevelSelectActivity、MixerSelectActivity、
AchievementActivity、RewardActivity、OptionsActivity、CreditsActivity、DebugActivity、
GameHud、HudEntry、TowerButton、UpgradeButton、UpgradeDialog、PauseButton、
FastFwdButton、MenuButton、LevelOverlay、ScoreOverlay、AchievementAlert、
Starfield、ConcurrentBackground、DisplayScaleUI、Display

- 视觉风格差异一律 P3（有意差异）；功能缺失（按钮/交互/展示无对应物）按 P0/P1 记
- 重点确认对应物存疑的类：MixerSelectActivity、DebugActivity、Starfield、
  ConcurrentBackground、DisplayScaleUI

**域④ 平台服务与工具（7 类）**
SoundManager、GameInput、ImageLoader、BatteryLevel、NumberFormatter、
Profiler、UserProfiler

- SoundManager、GameInput：逐方法
- ImageLoader、NumberFormatter：功能级
- BatteryLevel、Profiler、UserProfiler：预期属 P2/P3，需给出分级结论与替代建议

**域⑤ 存档与持久化（3 类）**
QuickSave、SDBackup、OptionsData

- QuickSave：逐字段核对序列化格式与当前 SQLite 存档系统的字段覆盖
- SDBackup：确认缺失情况并给桌面等价替代建议（如存档导出/导入）
- OptionsData：逐选项核对（已有 module-comparison-report.md 结论可复用）

**排除（1 类）**：R（Android 资源生成类，无对比意义）

## 3. 差异分级

| 级别 | 定义 | 处置 |
|------|------|------|
| P0 缺失功能 | 原版有、桌面版没有的玩家可感知功能 | 进入修复计划 |
| P1 行为偏差 | 功能存在但数值/逻辑/时序与原版不一致 | 进入修复计划 |
| P2 不适用 | Android 平台特有（电量、锁屏、SD 卡等），桌面无意义 | 标注 + 可选等价替代建议 |
| P3 有意差异 | UI 重设计、SQLite 存储等已接受的实现差异 | 记录，不修复 |

分级判断在分析执行时依据证据作出，本设计不预设任何类的结论。

## 4. 证据与复核标准

- 每条 P0/P1 必须附双边证据：`原版文件:行号` ↔ `当前文件:行号`（或「无对应实现」）
- 代理产出不直接采信：主线程逐条复核 P0/P1 的原始源码后才写入报告
- P2/P3 需一句话说明归类理由

## 5. 产出物与后续流程

1. **差距报告**：`docs/06-APK差距分析报告.md`（沿用 docs 编号惯例），结构：
   - 类映射总表（60 行，每类一行：原版类 → 当前对应物 → 分级结论）
   - 分级差异清单（按 P0→P3 排列，附证据）
   - 修复建议（按域组织）
   - 并入现有 `docs/module-comparison-report.md` 的结论（平台服务缺口等）；
     原报告保留不删除，作为历史记录
2. **修复计划**：报告经用户确认后，对 P0/P1 项调用 writing-plans 技能生成实施计划。
   修复范围以报告实际结果为准，不在本设计中预设。

## 6. 成功标准

- 映射总表覆盖全部 60 类，每类有明确分级结论
- 所有 P0/P1 附双边证据且经主线程复核
- 报告提交 git；P0/P1 清单可直接作为 writing-plans 的输入
