# APK 保真修复计划④：微调清理 / 死代码 / 文档更新

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended). Steps use checkbox (`- [ ]`) syntax.

**Goal:** 清理死代码、修复剩余微调偏差、更新 CLAUDE.md 文档。

**Architecture:** 纯维护性变更 — 删除未使用代码、修正偏移一处、统一文档。

**Tech Stack:** Java 8 / libGDX 1.12.1 / Gradle。

**依据：** `docs/06-APK差距分析报告.md`（域② #13 PerformanceMonitor 差一、P3 死代码项、文档更新需求）。

## Global Constraints

- Java 8 语法
- 编译：`./gradlew :core:compileJava`，期望 `BUILD SUCCESSFUL`
- 每提交末尾 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`

---

### Task 1: PerformanceMonitor 差一修复（域② #13）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/PerformanceMonitor.java:120`

- [ ] **Step 1: 将 `>=` 改回 `>`**

搜索 `skipLevel >= SKIP_LEVELS[skipIndex]`，改为：
```java
            while (skipIndex < SKIP_LEVELS.length - 1 && skipLevel > SKIP_LEVELS[skipIndex]) {
```

- [ ] **Step 2: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/game/PerformanceMonitor.java
git commit -m "fix: PerformanceMonitor 档位判定 > 恢复原版（域② #13）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: 死代码清理 — HudRenderer + tower_pool + mixer 键常量

**Files:**
- Delete: `core/src/main/java/com/rdefense/core/render/HudRenderer.java`（零调用者，经验证全代码库无引用）
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`（移除 `tower_pool` 相关死代码）
- Modify: `core/src/main/java/com/rdefense/core/save/PlayerPrefs.java:154-168`（mixer 键从字符串改为类常量）

- [ ] **Step 1: 删除 HudRenderer.java**
```bash
rm core/src/main/java/com/rdefense/core/render/HudRenderer.java
grep -rn "HudRenderer" core/src/ || echo "无残留引用"
```

- [ ] **Step 2: 删除 tower_pool 字段和使用**

`GameState.java` 中搜索 `tower_pool`：
- 删除字段声明 `private GameTower tower_pool;`
- `allocateGameTower()` 中：移除 pool 检查分支，保留直接 `new GameTower()` 逻辑
- `initGame` 中：删除 `this.tower_pool = null;`（如果存在）
- `loadState` 重建塔段中：删除 `tower_pool = null;`

- [ ] **Step 3: PlayerPrefs mixer 键改为常量**

在 `PlayerPrefs.java` 中已有常量定义区，追加：
```java
    private static final String KEY_MIXER_VALUE = "mixer_value";
    private static final String KEY_TOWER_MIXER_VALUE = "tower_mixer_value";
```

将 `getMixerValue()` / `putMixerValue()` / `getTowerMixerValue()` / `putTowerMixerValue()` 中的硬编码字符串 `"mixer_value"` / `"tower_mixer_value"` 替换为常量引用。

- [ ] **Step 4: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add -A core/src/main/java/com/rdefense/core/
git commit -m "chore: 清理死代码 — HudRenderer / tower_pool / mixer 键常量化

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: CLAUDE.md 文档更新

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: 更新文档以反映新事实**

在 `CLAUDE.md` 末尾追加：

```markdown
## APK 保真修复记录（2026-07-17〜18）

基于 `docs/06-APK差距分析报告.md` 完成三轮修复（计划①〜③），覆盖 6 个 P0 与约 33 条 P1。

### 已修复的关键偏差
- 恢复 `saveScore` 四奖金结算（20% 胜利奖金 + 1% 生命奖金 + 20% 完美奖金 + 金钱×难度×2）
- 恢复 `RewardData.gameWon` 难度递增体系
- 恢复成就弹窗链路（dequeueEarned → showAchievement，经 GamePlayScreen 轮询）
- 实现混合器 5 位数码选择面板（LevelSelectScreen 内嵌）
- 实现 ScoreOverlay 结算动画、Starfield 星空粒子、控制按钮接入
- 击杀得分除数 500、逐图分数倍率保真（含 skytower 第 7 种地图）
- 数值保真 10 项（溅射半径 2500→256、出售倍率 2×→1.5×、死亡帧 10→(value«1)+10 等）
- 存档补全 10 个字段（8 个成就追踪 + at_exit/exiting_grid）
- 健壮性（事件/敌人上限安全网、读档强制暂停、自动存档请求）

### 新发现（此前文档需订正）
- 原版共 **7 种地图**（0-6，含 skytower），非此前记载的 6 种
- `HudRenderer` 为死代码——HUD 实际由 `GamePlayScreen` 直接渲染
- `GameRewardCalculator` 系数表（ICE/LAVA/EXTREME）为臆造，已删除

### 仍有意的差异
- 死代码 `HudRenderer` / `tower_pool` 已清理
- Z_ACCEL 符号保留当前（更物理正确的弹道）
- FAST_FORWARD_LEVEL_PAUSE / FAST_FORWARD_LIFE_PAUSE 保留当前语义
- ENABLE_SOUND 默认 true 保留
```

- [ ] **Step 2: 提交**
```bash
git add CLAUDE.md
git commit -m "docs: 更新 CLAUDE.md — APK 保真修复记录与新发现事实

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: 全量构建最终验证

- [ ] **Step 1: 全量构建**
```bash
./gradlew :desktop:dist
```
期望：BUILD SUCCESSFUL

- [ ] **Step 2: 提交最终记录**
```bash
git add docs/superpowers/plans/2026-07-17-apk-parity-fix-4-cleanup.md
git commit -m "docs: 计划④运行时验证 — 全量构建通过"
```

---

## 计划自审记录

- 规格覆盖：域② #13（PerformanceMonitor）、P3 死代码清理、文档更新
- 占位符：无
- 类型一致性：N/A（无跨任务接口依赖）
