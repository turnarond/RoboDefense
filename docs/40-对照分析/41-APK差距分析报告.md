---
类型: 快照
冻结日期: 2026-07-17
结论去向: 大部分已由 v2.6.0 实施，残余见 docs/03-需求文档.md §5
---

# 06 - APK 差距分析报告（原版 Android Build 2900 vs 桌面版）

- 分析日期：2026-07-17
- 设计依据：`docs/03-需求文档.md`、`docs/10-架构设计.md`
- 基准：`xingjitafngv2.5.0_bvev_7273.com.apk`（Build 2900）jadx 反编译产物（60 类）
- 方法：5 个分域只读代理并行对比 + 主线程逐条复核全部 P0/P1
- 分级：P0 缺失功能 / P1 行为偏差 / P2 平台不适用 / P3 有意差异

## 一、总览

| 域 | 类数 | P0 | P1 | 复核说明 |
|----|------|----|----|---------|
| ① 核心玩法数值 | 18 | 0 | 13 | 13 条全部确认（1 条比代理所报更严重） |
| ② 游戏状态与事件 | 7 | 2 | 15 | 驳回 2 条误报，主线程新增 2 条 |
| ③ UI 界面与控件 | 24 | 4 | 9 | 多条动画类重分级为 P3 |
| ④ 平台服务与工具 | 7 | 0 | 3 | 驳回 1 条、降级 3 条 |
| ⑤ 存档与持久化 | 3 | （并入②） | 1 | 4 条 P0 与域②独立收敛合并、1 条驳回、2 条降 P3 |
| 排除 | 1（R） | — | — | Android 资源生成类 |
| **合计** | **60** | **6** | **41** | |

**重要新事实**：原版共 **7 种地图**（0=basic, 1=ruins, 2=factory, 3=courtyard, 4=mixer, 5=roadway, 6=skytower），此前项目文档均记为 6 种。

## 二、P0 缺失功能（已复核确认）

### P0-1 积分结算与难度递增体系缺失【域②，影响最大】

原版 `endGame()` 流程（原版 `GameState.java:155-180,906-931`）：
1. `saveScore()`：输/赢/退出三种结局都按 `score` 结算奖励积分；胜利额外 +20% 胜利奖金、+score×health×1% 生命奖金、满血 +20% 完美奖金、+money×难度×2 金钱奖金
2. 产生 `EVENT_SCORE_SAVED`(7) 事件驱动结算动画（配合 ScoreOverlay，见 P0-4）
3. 胜利时 `RewardData.gameWon()`（原版 `RewardData.java:243-251`）：**当前难度 +1、更新最高通关难度**——这是原版核心进度机制
4. `QuickSave.clearSave()` 清快速存档
5. `AchievementData.trySaveProgress()` 持久化成就

当前 `GameState.java:669-688`：仅胜利时 `GameRewardCalculator.calculateSimple(难度, 地图)` = `难度×系数×100`，输了 0 积分；无难度递增、无结算事件、无清存档。
且 `GameRewardCalculator.java:10-22` 的系数表是臆造的（BASIC/COURTYARD/**ICE/LAVA/EXTREME**），与本游戏 6 种地图（BASIC/RUINS/FACTORY/COURTYARD/MIXER/ROADWAY）对不上。

**影响**：玩家永远停留在所选难度，无进度累积；输局得不到任何积分（原版按得分给）；积分数值体系与原版完全不同。

### P0-2 成就获得弹窗链路断裂【域②③】

- 原版：`GameState.nextState()` → `dequeueAchievements()`（原版 `GameState.java:202,1137-1144`）→ `EVENT_ACHIEVEMENT_EARNED`(10) → HUD 弹出 AchievementAlert
- 当前：弹窗渲染器 `AchievementRenderer` 已完整实现（动画/音效/中文文案），但 **`showAchievement()`（`AchievementRenderer.java:34`）与 `AchievementData.dequeueEarned()`（`AchievementData.java:506`）均为零调用者**；`GameEvent.EVENT_ACHIEVEMENT_EARNED` 只有常量定义（`GameEvent.java:34`）
- **影响**：88 项成就静默达成，游戏内毫无提示

### P0-3 塔混合器（Mixer）选择器缺失且解锁死锁【域③】

- 原版 `MixerSelectActivity.java:28-203`：5 位数码选择界面（每位上下箭头 + 随机按钮），产出 mixer 种子
- 当前：无对应界面；`LevelSelectScreen.java:196-197` 直接 `configureLevel()` 跳过选择
- **更严重**：`PlayerPrefs.putMixerValue()`（`PlayerPrefs.java:158`）**零调用者**，`mixerValue` 永远为 0，而解锁条件 `isMixerUnlocked() = mixerValue>0 || towerMixerValue>0`（`LevelSelectScreen.java:183-184`）**永远为 false → 塔混合器整个功能死锁不可达**

### P0-4 关卡结算分数动画（ScoreOverlay）缺失【域③，与 P0-1 同源】

- 原版 `ScoreOverlay.java:54-228`：战后逐项弹出得分细项（基础分→生命奖励→金钱奖励→完美奖励）→ 倒计数合计 → 停留 → 淡出
- 当前 `GamePlayScreen.java:280-286`：仅一行文字「胜利！/ 点击继续」
- 依赖 P0-1 的 `EVENT_SCORE_SAVED` 事件与奖金细项数据，需一并修复

### P0-5 游戏内可见控制按钮未接入【域③】

- 原版：PauseButton / FastFwdButton / MenuButton 屏幕可见可点击
- 当前：`UiRenderer.renderControlButtons()`（`UiRenderer.java:233`）已实现但**零调用者**；仅键盘快捷键（空格/F/ESC，`GamePlayScreen.java:539`）
- **影响**：纯鼠标玩家没有暂停/快进/菜单的可视入口

### P0-6 宇宙关卡星空背景（Starfield）缺失【域③】

- 原版 `Starfield` 类 + `Display.java:582-588` 视差星空粒子
- 当前：全代码库 `Starfield` 零匹配
- 影响：宇宙背景关卡视觉内容缺失（玩法无影响，按定义仍属可感知缺失）

## 三、P1 行为偏差（已复核确认）

### 域② 游戏状态与事件（13 条 + 主线程新增 2 条）

| # | 差异 | 证据（原版 ↔ 当前） |
|---|------|----------------------|
| 1 | **击杀得分 5 倍膨胀**：除数 `/500`（`C.EVENT_ALLOCATION_SANITY_LIMIT=500`，已核实 `C.java:14`）→ `/100`，且加了原版没有的 `Math.max(1,·)` 下限 | `GameState.java:661` ↔ `GameState.java:225`；影响全部得分里程碑成就时机 |
| 2 | `getEnemyKillBonus` 缺 `+AchievementData.totalCount()` 与奖励 19 的 ×2 | `GameState.java:293-299` ↔ `GameState.java:983` |
| 3 | `initGame` 缺 `EVENT_TOWERS_CHANGED`/`EVENT_GAME_INIT` 初始事件（当前 3/7/11 号事件通道整体未接线，UI 也无监听——修复需连同监听端） | `GameState.java:84,88` ↔ `GameState.java:89-129` |
| 4 | 消息系统：缺 `showMessage()`（45 帧短消息）、缺 10 槽位管理、缺 +30 淡入帧 | `GameState.java:496-570` ↔ `GameState.java:968-972` |
| 5 | 事件分配缺 500 上限自动清理粒子/爆炸队列（防 OOM） | `GameState.java:509-511` ↔ `GameState.java:552-565` |
| 6 | 敌人分配缺 100 上限报错+endGame(5) 安全网 | `GameState.java:855-858` ↔ `GameState.java:596-608` |
| 7 | 出售塔未递减 `tower_powup_counter`（≤12 时），影响 POWERED_UP 成就 | `GameState.java:217-222` ↔ `GameState.java:786-803` |
| 8 | **[新增]** `cheapskate` 在扣钱成功**前**置位——钱不够的失败升级也毁掉"吝啬鬼"成就（原版仅成功升级后置位） | `GameState.java:233` ↔ `GameState.java:792` |
| 9 | `loadState` 无魔数眼标（game/tower/enemy/eof eye）、无字段范围校验（health 1-500、money≥0 等逐项） | `GameState.java:360-494` ↔ `GameState.java:1086-1216` |
| 10 | 读档后未强制 `GAME_PAUSED`（当前恢复存档时刻的 run_state，可能直接开跑）、无 `EVENT_GAME_LOAD_SUCCESS` | `GameState.java:485-487` ↔ `GameState.java:1206-1215` |
| 11 | 存档漏 8 个字段：6 个成就追踪布尔 + `tower_powup_counter` + `fast_fwd_counter`——读档后这些成就条件全部失真 | `GameState.java:330-337` ↔ `GameState.java:1025-1063` |
| 12 | 每 10 关自动快速存档被注释掉 | `GameState.java:815-816` ↔ `GameState.java:395-398` |
| 13 | PerformanceMonitor 档位判定 `>` → `>=`：skip_level 恰为 2/4/8/16 时提前降质一档 | `PerformanceMonitor.java:81` ↔ `PerformanceMonitor.java:120` |
| 14 | **[新增]** 生存模式首关难度未按生存曲线设定（原版 `initGame` 末尾重设；当前仅 `nextLevel` 后调整，首波敌人血量按玩家所选难度） | `GameState.java:101-103` ↔ `GameState.java:89-129`（待域①佐证 LevelDataGenerator 内部路径） |
| 15 | `initGame` 将 `events_allocated` 直接清零而原版重算 event_list 残留（配合 #5 上限恢复时会造成计数漂移） | `GameState.java:78-83` ↔ `GameState.java:112` |

**域②驳回**（2 条，代理误报）：
- ~~initGame 未重置 bullet_pool/tower_pool~~：原版同样不重置 bullet_pool，且原版无塔池（`allocateGameTower` 每次 new，原版 `GameState.java:868-874`）
- ~~成就持久化时机（批量 vs 立即）~~：当前达标瞬间即 `trySaveProgress()`（`AchievementData.java:605`），与原版行为等价 → P3

### 域③ UI（复核后保留 P1）

| # | 差异 | 证据 |
|---|------|------|
| 1 | HUD 无千位分隔符、无渐进计数动画、无「+N」金钱飘字（击杀飘字已有 `GamePlayScreen.java:813-821`）；注：`HudRenderer` 为**零调用废弃代码**，实际 HUD 在 `GamePlayScreen.java:462-492` | 原版 `HudEntry.java:71-77,115-167,221-246` ↔ `GamePlayScreen.java:462-492` |
| 2 | 新游戏时快速存档覆盖无确认对话框（原版 AlertDialog 确认） | `TitleActivity.java:109-129` ↔ `MainMenuScreen` 无对应 |
| 3 | 生存模式通关彩蛋提示缺失 | `TitleActivity.java`（同上）↔ 无对应 |
| 4 | DisplayScaleUI 顶部缩放滑块缺失（滚轮缩放已有，选项 6 控制的滑块 UI 无） | `DisplayScaleUI.java:66-75` ↔ `UiRenderer` 无对应 |
| 5 | 关卡选择界面缺声音开关与 Options 入口 | `LevelSelectActivity` ↔ `LevelSelectScreen` |
| 6 | LevelOverlay/Underlay 地图盖层渲染待确认（域①/存疑） | `LevelOverlay.java` ↔ 待查 |
| 7 | 粒子/爆炸/消息闪烁等事件渲染简化（与域② #3 事件通道相关） | 待逐项确认 |
| 8 | 游戏开始前指引画面简化 | `GameActivity` ↔ `GamePlayScreen.java:286` |
| 9 | 塔按钮防误触状态机简化（原版拖出按钮区取消） | `GameInput.java:44,75-79` ↔ `GameInputHandlerImpl.java:54-57` |

### 域④ 平台服务与工具（复核后保留 P1）

| # | 差异 | 证据 |
|---|------|------|
| 1 | 音效缺 8 帧同类去重延迟（高射速时音效重叠/毛刺） | `SoundManager.java:78-85` ↔ `SoundManager.java:59-66` |
| 2 | 拖动判定阈值 15px → 20px | `GameInput.java:13` ↔ `GameInputHandlerImpl.java:28` |
| 3 | ESC/返回键状态机简化：原版按序取消升级对话框→清除塔选择→退出确认；当前 ESC 仅清 activeTower + 开菜单 | `GameInput.java:163-182` ↔ `GameInputHandlerImpl.java:239-243` |

**域④驳回/降级**：
- ~~NumberFormatter 仅处理 ≥1000~~：<1000 本就无需分隔符——但实际 HUD 根本无分隔（并入域③ #1，d4 引证的 `HudRenderer.formatNumber` 是废弃代码）
- 返回键 P0 → P1（ESC 暂停菜单已存在，属行为简化非缺失）
- pinch zoom P1 → P2（桌面无触屏，滚轮等价替代）
- UserProfiler 帧时间彩色显示 P1 → P3（调试辅助，FPS 显示已有）

## 四、P2 平台不适用（摘要）

| 项 | 理由 / 替代建议 |
|----|----------------|
| BatteryLevel 电量条 | 桌面已有 `🔋 X%` 文本（`GamePlayScreen.java:529-534`）；原版彩色电池图标不必要 |
| 双指缩放 | 桌面无触屏；滚轮缩放已实现 |
| 系统音量流（AudioManager）| 桌面交由系统混音器；音量选项已有 |
| WakeLock/存储权限/Android 广播/DPI 体系 | 平台机制，无桌面对应物 |
| Profiler | 原版即空实现，无运行时行为 |
| C/G 常量类的 Bitmap 配置与全局尺寸 | Android 位图/测量体系专用 |

## 五、P3 有意差异（摘要，不修复）

- 全部 UI 科幻主题重设计（布局/配色/字体/卡片化）
- SharedPreferences → SQLite 存档体系（含成就/进度/选项存储层）
- HD 资源：density scaling → `_2x/_large/_hd` 后缀查找
- MediaPlayer → libGDX Sound 音频架构
- ConcurrentBackground 异步菜单背景 → 静态科幻背景（重设计替代）
- 升级对话框 8 帧展开动画、塔按钮拖拽放置 → 桌面点击交互适配
- 成就达标即批量保存（行为等价）；`GameEvent.init()` 清零 var[]（安全增强）
- 新增功能（原版无）：GameSaveScreen 存档槽位界面、击杀飘字、FPS 显示
- 待清理死代码：`HudRenderer`（零调用）、`tower_pool`（不回收，永远为空池）

## 六、域① 核心玩法数值 P1（13 条，全部经双边源码复核确认）

| # | 差异 | 证据（原版 ↔ 当前） |
|---|------|----------------------|
| 1 | 冲击波缺类型 10 直接火焰伤害（`applyDamage((power×len)/4, 10)` 整行缺失，冲击波伤害减半） | `GameTower.java:193-199` ↔ `GameTower.java:245-253` |
| 2 | 跳蚤市场出售倍率 1.5×→2×（收益高 33%；早期开发指南中「出售价值×2」的描述本身与原版不符，现依据代码订正为 1.5×） | `TowerData.java:615-625` ↔ `TowerData.java:464-476` |
| 3 | 高级 SAM 缺 CHEAP_FIREWORKS(奖励14) 成本减免 90→50 | `TowerData.java:182-184` ↔ 当前 `cost()` 无此逻辑 |
| 4 | 溅射/地雷半径硬编码 2500：原版 `GRID²/4=256`（溅射面积放大 9.8 倍）、`GRID×2×GRID=2048`（地雷 1.22 倍） | `BulletData.java:33-34` ↔ `BulletData.java:29-32` |
| 5 | TOUGH_MASK 死代码被修活：约 1/8 关卡敌人生命额外 +25%，难度高于原版【决策点1】 | `LevelDataGenerator.java:82-84` ↔ `LevelDataGenerator.java:110-112` |
| 6 | 敌人速度缺 `×GRID/40` 缩放：32px 网格下全体敌人快 25% | `EnemyData.java:231-233` ↔ `EnemyData.java:254-257` |
| 7 | 分数倍率公式化 `100+type×25`：**mixer 200（应 180）、roadway 225（应 100）、skytower 250（应 125）**——比代理所报多出 mixer 一处；与域② #1 的 /500→/100 叠加后部分关卡得分膨胀至原版 11 倍 | `LevelData.java:354-415` ↔ `LevelData.java:260` |
| 8 | 放塔缺「敌人占空格拒放」检查：阻挡塔可困住路径上的敌人【决策点3】 | `MovementGrid.java:45-64` ↔ `MovementGrid.java:228-231` |
| 9 | Z_ACCEL 符号相反（-4→+4）：弹道形状不同（当前为更真实的抛物线）【决策点2】 | `Bullet.java:12` ↔ `Bullet.java:15` |
| 10 | 地雷缺 shot_type 11 飞行豁免：无 Air Burst(奖励20) 时飞行单位也会触雷浪费 | `GameTower.java:149-153` ↔ 当前 `validTarget` 无 case 11 |
| 11 | 火焰灼烧语义：原版 `+4` 后封顶到 amount（可缩短已有值）；当前 `Math.max` 不缩短（利玩家）【决策点4】。注：早期开发指南称 applyDamage 已全部修正，此处仍与原版不同 | `Enemy.java:193-197` ↔ `Enemy.java:204-209` |
| 12 | imageCenter/drawShift 固定 16/0 vs 原版按精灵尺寸动态计算（命中点分布偏差，低影响，抽查级复核） | `EnemyData.java:251-273` ↔ `EnemyData.java:286-323` |
| 13 | deathFrames 恒 10 vs `(value<<1)+10`：泰坦死亡动画 510 帧 → 10 帧 | `EnemyData.java:247-249` ↔ `EnemyData.java:328-330` |

域① 其余结论：CollisionGrid / Vector / VectorLookup / FastRandom / MixerLevelGenerator 逐方法匹配；ActiveTower 为 Android 拖拽 UI（P2，桌面已重构交互）；GridObject / GridObjectOrder / ExplosionData 为等价重构（P3）。

## 七、域⑤ 存档与持久化（复核后）

- **P0 收敛**：域⑤ 报出的 4 条 P0（saveScore 奖金体系 / 难度递增 / 结算事件 / trySaveProgress）与域② P0-1 为同一体系，两域独立发现互为佐证，已合并
- **P1 确认**：`at_exit`/`exiting_grid` 漏存——敌人抵达出口瞬间存档，读档后该敌人不扣血继续走（原版 `Enemy.java:241-242,257-258` ↔ 当前 `GameState.java:1192-1193` 硬编码 false）
- 8 个成就追踪字段漏存与域② #11 为同一条（字段对照表见域⑤代理产出，已核实）
- **驳回**：「endGame 缺 trySaveProgress」单列 P0——当前成就达标瞬间即持久化（`AchievementData.java:605`），行为等价
- **降级 P3**：ENABLE_SOUND 默认 false→true（桌面合理默认【决策点5】）；RewardData 键前缀 `ADReward:`→`Reward:`（无跨平台存档迁移需求）；敌人序列化字段顺序不同（存档格式有意重构，由魔数/版本隔离）；PlayerPrefs mixer 键硬编码但 get/put 自洽（代码卫生问题）

## 八、保真决策点（修复前需决定）

| # | 项 | 现状 | 建议 |
|---|----|------|------|
| 1 | TOUGH_MASK 死代码修活（域① #5） | 当前难度高于原版 | 待决策：A 保真恢复 / B 保留修复 |
| 2 | Z_ACCEL 符号（域① #9） | 当前抛物线更物理正确 | 建议保留当前，记录差异 |
| 3 | 敌人占格拒放检查（域① #8） | 当前可困住敌人 | 建议恢复原版检查 |
| 4 | 火焰灼烧不缩短（域① #11） | 当前利玩家 | 建议保真恢复 |
| 5 | ENABLE_SOUND 默认开（域⑤） | 桌面用户习惯 | 建议保留当前 |

## 九、修复建议（按依赖关系分组）

1. **结算与进度组**（P0-1 + P0-4 + 域② #1/#2 + 域① #7）：恢复 `saveScore` 四奖金公式、`/500` 除数与各图分数倍率（含 skytower）→ 恢复 `RewardData.gameWon` 难度递增 → 产生 `EVENT_SCORE_SAVED` → 实现 ScoreOverlay 结算动画 → 补 `QuickSave.clearSave`。删除臆造的 `GameRewardCalculator` 系数表
2. **成就完整性组**（P0-2 + 域② #7/#8 + #11 存档字段 + 域⑤ at_exit）：接通 dequeue→showAchievement 链路；修 powup/cheapskate 时机；补存档字段
3. **数值保真组**（域① #1-#6/#10/#12/#13 + 决策点 1-4）：冲击波/半径/速度/SAM 减免/出售倍率/触雷豁免/死亡帧数等逐项校正
4. **混合器组**（P0-3）：实现 5 位数码选择界面 + mixerValue 写入路径
5. **游戏内 UI 组**（P0-5/P0-6 + 域③ P1）：接入控制按钮、Starfield、HUD 增强、对话框
6. **健壮性组**（域② #5/#6/#9/#10/#12/#15）：上限安全网、存档校验、读档暂停、自动存档
7. **微调与清理组**（域④ P1 + PerformanceMonitor 差一 + 死代码 HudRenderer/tower_pool/mixer 键常量）

## 十、附录：60 类映射总表

| 原版类 | 当前对应物 | 结论 |
|--------|-----------|------|
| Enemy | game/Enemy.java | P1×2（灼烧语义、中心点） |
| EnemyData | game/EnemyData.java | P1×3（速度、中心点、死亡帧） |
| GameTower | game/GameTower.java | P1×2（冲击波、触雷豁免） |
| TowerData | game/TowerData.java | P1×2（出售倍率、SAM 减免） |
| ActiveTower | 并入 GamePlayScreen/UiRenderer | P2（Android 拖拽 UI） |
| Bullet | game/Bullet.java | P1（Z_ACCEL）+P3（烟雾限值） |
| BulletData | game/BulletData.java | P1（半径硬编码） |
| MovementGrid | game/MovementGrid.java | P1（敌人拒放检查） |
| CollisionGrid | game/CollisionGrid.java | ✅ 匹配 |
| LevelData | game/LevelData.java | P1（分数倍率） |
| LevelDataGenerator | game/LevelDataGenerator.java | P1（TOUGH_MASK） |
| MixerLevelGenerator | game/MixerLevelGenerator.java | ✅ 匹配 |
| ExplosionData | game/ExplosionData.java | P3（接口简化） |
| Vector / VectorLookup / FastRandom | game/ 同名 | ✅ 匹配 |
| GridObject / GridObjectOrder | game/ 同名 | P3（OOP 重构/防御检查） |
| GameState | game/GameState.java | P0×1 + P1×12 |
| GameEvent | game/GameEvent.java | ✅ 结构一致（3/7/10/11 号通道未接线见 P0/P1） |
| RewardData | game/RewardData.java | P0（gameWon 缺失）+P3（键前缀） |
| AchievementData | game/AchievementData.java | P0（弹窗链路）+P3（批量保存） |
| PerformanceMonitor | game/PerformanceMonitor.java | P1（差一） |
| C | 分散内联 | P3；常量值已核（500/100） |
| G | 硬编码 32/16 | P3 |
| GameActivity | scene/GamePlayScreen.java | P1（指引画面等） |
| TitleActivity | scene/MainMenuScreen.java | P1（对话框×2）+P3（重设计） |
| LevelSelectActivity | scene/LevelSelectScreen.java | P1（声音开关/Options 入口） |
| MixerSelectActivity | **无** | **P0-3** |
| AchievementActivity | scene/AchievementScreen.java | P3（重设计） |
| RewardActivity | scene/RewardScreen.java | P3（重设计） |
| OptionsActivity | scene/OptionsScreen.java | P1（保存行为）+P3 |
| CreditsActivity | scene/CreditsScreen.java | P3（重设计） |
| DebugActivity | JVM 调试属性替代 | P2/P3 |
| GameHud / HudEntry | GamePlayScreen 内联（HudRenderer 为死代码） | P1（千位分隔/渐进动画） |
| TowerButton | render/UiRenderer.java | P3（点击代替拖拽）+P1（防误触） |
| UpgradeButton / UpgradeDialog | render/UiRenderer.java | P3（动画） |
| PauseButton / FastFwdButton / MenuButton | UiRenderer.renderControlButtons（未接入） | **P0-5** |
| LevelOverlay | 待确认渲染 | P1（存疑遗留） |
| ScoreOverlay | **无** | **P0-4** |
| AchievementAlert | render/AchievementRenderer.java（未接入） | **P0-2** |
| Starfield | **无** | **P0-6** |
| ConcurrentBackground | 静态背景替代 | P3（重设计） |
| Display | GamePlayScreen+CameraManager+渲染器 | P1（分散重构，个别功能缺失） |
| DisplayScaleUI | CameraManager（滚轮） | P1（滑块 UI 缺失） |
| GameInput | input/GameInputHandlerImpl.java | P1×3（阈值/状态机/ESC） |
| ImageLoader | platform/libgdx/LibGdxRenderer.java | P3（HD 策略等价） |
| SoundManager | audio/SoundManager.java | P1（8 帧去重） |
| NumberFormatter | GamePlayScreen 内联 | P1（并入 HUD 项） |
| BatteryLevel | GamePlayScreen 文本显示 | P2 |
| Profiler | 无（原版即空实现） | P2 |
| UserProfiler | FPS 显示替代 | P3 |
| QuickSave | save/db/SQLiteSaveManager.java | P1（字段漏存，见域⑤） |
| SDBackup | 无 | P2（建议：存档导出/导入） |
| OptionsData | config/OptionsData.java | P3（默认音效）；余 12 项一致 |
| R | — | 排除 |
