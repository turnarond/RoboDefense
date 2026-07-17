# APK 保真修复计划①：结算体系 / 数值保真 / 健壮性

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复原版积分结算/难度递增体系与核心玩法数值，端到端可验收（打完一局 → 结算明细 → 积分与难度递增）。

**Architecture:** 全部改动在 `core` 模块。结算逻辑回归 `GameState`（原版结构），平台副作用（难度持久化、清快速存档、自动存档）经 `GamePlayScreen` 在状态转换处执行，避免 core/game → save/db 反向依赖。`GameRewardCalculator` 删除。

**Tech Stack:** Java 8 / libGDX 1.12.1 / Gradle。**本项目无测试框架**（CLAUDE.md 明确约束），每任务的验证环 = `./gradlew :core:compileJava` 编译门 + 计划末尾的运行时手动验证清单（debug 启动参数）。

**依据：** `docs/06-APK差距分析报告.md`（P0-1、P0-4、域① #1-#8/#10/#11/#13、域② #1/#2/#5/#6/#8/#9/#10/#12/#14、域④部分）。保真决策：TOUGH_MASK 恢复原版死代码行为；Z_ACCEL 保留当前；敌人拒放检查恢复；灼烧封顶恢复原版；声音默认保留当前。

## Global Constraints

- Java 8 语法（禁 `var`、`List.of()` 等 Java 9+ API）
- 注释用简体中文；新增中文 UI 文案需同步 `CHINESE_CHARS` 常量（本计划仅复用已有汉字，无需改）
- 原版基准源码：`C:/Users/yanch/AppData/Local/Temp/apk_decompiled/sources/com/magicwach/rdefense/`
- 每任务提交信息末尾带 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- 编译命令统一：`./gradlew :core:compileJava`，期望输出 `BUILD SUCCESSFUL`

---

### Task 1: RewardData.gameWon 难度递增

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/RewardData.java`（在 `addRewardPoints` 后新增方法）

**Interfaces:**
- Consumes: `PlayerPrefs.getDifficulty()/putDifficulty(int)/getMaxLevelWon()/putMaxLevelWon(int)`（已存在，`save/PlayerPrefs.java:117-132`）
- Produces: `public static void gameWon(PlayerPrefs prefs)` — Task 7 的 GamePlayScreen 在胜利转换时调用

- [ ] **Step 1: 在 RewardData.java 顶部补充 import**

```java
import com.rdefense.core.save.PlayerPrefs;
```

- [ ] **Step 2: 在 `addRewardPoints` 方法之后新增 gameWon（对应原版 RewardData.java:243-251）**

```java
    /**
     * 游戏胜利：当前难度 +1，并更新最高通关难度（原版 RewardData.gameWon）
     */
    public static void gameWon(PlayerPrefs prefs) {
        int level = prefs.getDifficulty();
        int maxLevelWon = prefs.getMaxLevelWon();
        prefs.putDifficulty(level + 1);
        if (level > maxLevelWon) {
            prefs.putMaxLevelWon(level);
        }
    }
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/RewardData.java
git commit -m "feat: 恢复 RewardData.gameWon 难度递增（P0-1）"
```

---

### Task 2: GameState.saveScore 四奖金结算 + endGame 重写 + 删除 GameRewardCalculator

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java:669-688`（endGame）；同文件新增 saveScore 私有方法
- Delete: `core/src/main/java/com/rdefense/core/game/GameRewardCalculator.java`

**Interfaces:**
- Consumes: `GameEvent.VAR_SCORE_*` 常量（已在 `GameEvent.java:75-81` 定义）、`RewardData.addRewardPoints(long)`、`AchievementData.trySaveProgress()`
- Produces: `EVENT_SCORE_SAVED` 事件（var[2..6] = 基础分/胜利奖金/生命奖金/完美奖金/金钱奖金）— Task 7 的 ScoreOverlay 消费；endGame 不再重置难度为 1

- [ ] **Step 1: 在 GameState.java 的 `endGame` 前新增 saveScore（对应原版 GameState.java:906-931）**

```java
    /**
     * 结算奖励积分（原版 saveScore）：输/赢/退出都按 score 结算；
     * 胜利额外四种奖金。产生 EVENT_SCORE_SAVED 供结算界面显示。
     */
    private void saveScore(int new_run_state) {
        if (this.score > 0) {
            int won_bonus = 0;
            int health_bonus = 0;
            int perfect_bonus = 0;
            int money_bonus = 0;
            if (new_run_state == GAME_WON) {
                won_bonus = (this.score * 20) / 100;
                health_bonus = (this.score * this.health) / 100;
                if (this.health == this.starting_health) {
                    perfect_bonus = (this.score * 20) / 100;
                }
                money_bonus = this.money * this.difficulty_level * 2;
            }
            GameEvent e = allocateGameEvent(GameEvent.EVENT_SCORE_SAVED);
            e.var[GameEvent.VAR_SCORE_FRAME_INDEX] = 0;
            e.var[GameEvent.VAR_SCORE_STATE] = 0;
            e.var[GameEvent.VAR_SCORE_ADD] = this.score;
            e.var[GameEvent.VAR_SCORE_WON_BONUS] = won_bonus;
            e.var[GameEvent.VAR_SCORE_HEALTH_BONUS] = health_bonus;
            e.var[GameEvent.VAR_SCORE_PERFECT_BONUS] = perfect_bonus;
            e.var[GameEvent.VAR_SCORE_MONEY_BONUS] = money_bonus;
            RewardData.addRewardPoints((long) this.score + won_bonus + health_bonus
                    + money_bonus + perfect_bonus);
            this.score = 0;
        }
    }
```

- [ ] **Step 2: 重写 endGame（替换 GameState.java:669-688 整个方法体）**

```java
    /**
     * 结束游戏（原版 endGame）：先结算积分与成就，再重置。
     * 难度持久化递增与清快速存档由 GamePlayScreen 在状态转换处执行。
     */
    public void endGame(int new_run_state) {
        if (this.run_state == GAME_RUNNING || this.run_state == GAME_PAUSED ||
                this.run_state == GAME_FAST_FWD) {
            if (new_run_state == GAME_LOST || new_run_state == GAME_WON ||
                    new_run_state == GAME_NOT_STARTED) {
                if (new_run_state == GAME_WON) {
                    // 成就检测（必须在 initGame 之前，因为 initGame 会重置标志）
                    gameWonAchievements();
                }
                // 输/赢/退出都结算积分（原版行为；使用重置前的 score/health/money）
                saveScore(new_run_state);
                // 保留当前难度（旧代码经单参 initGame 误重置为 1）
                initGame(this.level_data.getLevelType(), this.difficulty_level);
                this.run_state = new_run_state;
            }
        }
        AchievementData.trySaveProgress();
    }
```

- [ ] **Step 3: 删除 GameRewardCalculator 及其引用**

```bash
rm core/src/main/java/com/rdefense/core/game/GameRewardCalculator.java
grep -rn "GameRewardCalculator" core/src/ || echo "无残留引用"
```
Expected: 删除后 grep 无输出（旧引用仅在 endGame，已被 Step 2 覆盖）

- [ ] **Step 4: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A core/src/main/java/com/rdefense/core/game/
git commit -m "feat: 恢复原版 saveScore 四奖金结算，删除臆造的 GameRewardCalculator（P0-1）"
```

---

### Task 3: 得分公式保真（除数 500 / 击杀加成 / 分数倍率表）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java:225`（enemyDefeated）、`GameState.java:983`（getEnemyKillBonus）
- Modify: `core/src/main/java/com/rdefense/core/game/LevelData.java:260`

**Interfaces:**
- Consumes: `AchievementData.totalCount()`（已存在 `AchievementData.java:474`）、`RewardData.getLevel(int)`、`RewardData.BONUS`
- Produces: `getEnemyKillBonus()` 语义变化：`level_bonus + 成就总数`，奖励 19 解锁后 ×2（调用方无需改）

- [ ] **Step 1: enemyDefeated 除数改回 /500 并去掉 Math.max 下限（GameState.java:225）**

替换：
```java
        int base_score_add = Math.max(1, (ge.getMaxHealth() * this.level_data.getScoreMultiplier()) / 100);
```
为：
```java
        // 原版除数 500（C.EVENT_ALLOCATION_SANITY_LIMIT），且允许 0 分击杀
        int base_score_add = (ge.getMaxHealth() * this.level_data.getScoreMultiplier()) / 500;
```

- [ ] **Step 2: getEnemyKillBonus 补全成就加成与 BONUS 翻倍（GameState.java:983）**

替换：
```java
    public int getEnemyKillBonus() { return this.level_bonus; }
```
为：
```java
    public int getEnemyKillBonus() {
        int bonus = this.level_bonus + AchievementData.totalCount();
        if (RewardData.getLevel(RewardData.BONUS) > 0) {
            return bonus * 2;
        }
        return bonus;
    }
```

- [ ] **Step 3: LevelData 分数倍率恢复原版逐图数值（LevelData.java:260）**

替换：
```java
        this.level_score_multiplier = 100 + (this.level_type * 25);
```
为：
```java
        // 原版逐图倍率：basic=100, ruins=125, factory=140, courtyard=160,
        // mixer=180, roadway=100, skytower=125
        final int[] SCORE_MULTIPLIERS = {100, 125, 140, 160, 180, 100, 125};
        this.level_score_multiplier = (this.level_type >= 0
                && this.level_type < SCORE_MULTIPLIERS.length)
                ? SCORE_MULTIPLIERS[this.level_type] : 100;
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java core/src/main/java/com/rdefense/core/game/LevelData.java
git commit -m "fix: 击杀得分除数/500、击杀加成含成就数、逐图分数倍率保真（域② #1/#2、域① #7）"
```

---

### Task 4: 数值保真批 A（半径 / 速度 / 死亡帧 / SAM 减免 / 出售倍率）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/BulletData.java:29-32`
- Modify: `core/src/main/java/com/rdefense/core/game/EnemyData.java:254-257,328-330`
- Modify: `core/src/main/java/com/rdefense/core/game/TowerData.java:98,464-476`

**Interfaces:**
- Consumes: `RewardData.getLevel(int)`、`RewardData.CHEAP_FIREWORKS`；`EnemyProp` 已有 `speed`、`value` 字段
- Produces: 数值语义与原版一致；对外签名不变

- [ ] **Step 1: BulletData 半径改为原版公式值（32px 网格，BulletData.java:29-32）**

```java
    // 溅射半径平方：原版 GRID_PIXEL_SIZE²/4 = 32*32/4
    public static final int SPLASH_RADIUS_SQ = 256;

    // 地雷爆炸半径平方：原版 GRID_PIXEL_SIZE*2*GRID_PIXEL_SIZE = 32*2*32
    public static final int MINE_RADIUS_SQ = 2048;
```

- [ ] **Step 2: EnemyData.speed 恢复网格缩放（EnemyData.java:254-257）**

```java
    public static int speed(int type) {
        EnemyProp prop = enemy_props[type];
        // 原版：(speed * GRID_PIXEL_SIZE) / 40，本项目网格 32px
        return prop != null ? (prop.speed * 32) / 40 : 10;
    }
```

- [ ] **Step 3: EnemyData.deathFrames 恢复按价值计算（EnemyData.java:328-330）**

```java
    public static int deathFrames(int type) {
        // 原版：(价值 << 1) + 10，高价值敌人死亡动画更长（泰坦 510 帧）
        EnemyProp prop = enemy_props[type];
        return prop != null ? (prop.value << 1) + 10 : 10;
    }
```

- [ ] **Step 4: TowerData.cost 高级 SAM 减免（TowerData.java:98）**

替换：
```java
            case 15: return 90;
```
为：
```java
            // 廉价花炮（奖励14）解锁后高级 SAM 降为 50（原版 applyTowerAwards）
            case 15: return RewardData.getLevel(RewardData.CHEAP_FIREWORKS) > 0 ? 50 : 90;
```

- [ ] **Step 5: TowerData.sellValue 跳蚤市场倍率 2×→1.5×（TowerData.java:471-474）**

替换：
```java
        // 跳蚤市场：出售价值翻倍
        if (RewardData.rewardLevel(RewardData.FLEA_MARKET) > 0) {
            value *= 2;
        }
```
为：
```java
        // 跳蚤市场：出售价值 ×1.5（原版 value + value/2）
        if (RewardData.rewardLevel(RewardData.FLEA_MARKET) > 0) {
            value += value / 2;
        }
```
注：若该处实际调用名为 `RewardData.getLevel`，保持文件现状的调用名不变，只改倍率行。

- [ ] **Step 6: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/BulletData.java core/src/main/java/com/rdefense/core/game/EnemyData.java core/src/main/java/com/rdefense/core/game/TowerData.java
git commit -m "fix: 溅射/地雷半径、敌人速度、死亡帧数、SAM 减免、出售倍率保真（域① #2/#3/#4/#6/#13）"
```

---

### Task 5: 数值保真批 B（冲击波 / 触雷豁免 / 灼烧封顶 / TOUGH_MASK / 敌人拒放）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameTower.java:127-159,245-253`
- Modify: `core/src/main/java/com/rdefense/core/game/Enemy.java:204-218`
- Modify: `core/src/main/java/com/rdefense/core/game/LevelDataGenerator.java:109-112`
- Modify: `core/src/main/java/com/rdefense/core/game/MovementGrid.java:140-154,219-233`

**Interfaces:**
- Consumes: `BulletData.MINE=11`、`BulletData.SLOW=4`、`RewardData.AIR_BURST`、`Enemy.getPathNum()/getGridX()/getGridY()`
- Produces: `MovementGrid` 新增私有字段 `lastTrialGrid`（仅类内使用）

- [ ] **Step 1: 冲击波补类型 10 直接火焰伤害（GameTower.java:250-253，对应原版 197 行）**

替换循环体：
```java
        for (Enemy e = game_state.getEnemyList(); e != null; e = e.next) {
            e.applyDamage(effectiveLength * 30, BulletData.SLOW);  // 减速
            e.setFireCounter(fireAmount);                           // 直接设置灼烧值
        }
```
为：
```java
        for (Enemy e = game_state.getEnemyList(); e != null; e = e.next) {
            e.applyDamage(effectiveLength * 30, BulletData.SLOW); // 减速
            e.applyDamage(fireAmount, 10);                        // 直接火焰伤害（原版第二段）
            e.setFireCounter(fireAmount);                         // 持续灼烧
        }
```

- [ ] **Step 2: validTarget 补地雷飞行豁免（GameTower.java:133-150 的 switch 中新增 case）**

在 `case 9: case 10: case 13:` 分支之后新增：
```java
            case BulletData.MINE: // 11 地雷：无空中打击（奖励20）时不打飞行单位
                if (EnemyData.isFlyer(e.getType())
                        && RewardData.getLevel(RewardData.AIR_BURST) == 0) {
                    return false;
                }
                break;
```

- [ ] **Step 3: 灼烧封顶恢复原版语义（Enemy.java:205-209，保真决策 4）**

替换：
```java
            int oldFire = this.fire_counter;
            this.fire_counter += 4;
            if (this.fire_counter > amount) {
                this.fire_counter = Math.max(oldFire, amount); // 不缩短已有灼烧
            }
```
为：
```java
            // 原版语义：每次命中至多 +4，且封顶到本次弹药的 amount（可缩短已有灼烧）
            this.fire_counter += 4;
            if (this.fire_counter > amount) {
                this.fire_counter = amount;
            }
```

- [ ] **Step 4: TOUGH_MASK 恢复原版死代码行为（LevelDataGenerator.java:110-112，保真决策 1）**

替换：
```java
        // TOUGH_MASK (bit 2,3,4): 如果为 0，增加 25% 生命值
        if ((features & TOUGH_MASK) == 0) {
            health_per_group = health_per_group + (health_per_group * HEALTH_TOUGH_PCT) / 100;
        }
```
为：
```java
        // 原版此分支为死代码（计算结果赋给未使用的局部变量），保真起见不生效。
        // 保留注释以说明 TOUGH_MASK 在原版即无实际作用。
```

- [ ] **Step 5: MovementGrid 恢复敌人占格拒放检查（保真决策 3）**

5a. 在 `searchGreedy`（MovementGrid.java:140-154）中把试算网格存入字段。类字段区新增：
```java
    private byte[] lastTrialGrid; // 最近一次 searchGreedy 的试算网格，供敌人占格检查
```
在 `trialGrid[gridW * endY + endX] = CELL_EMPTY;` 与 `return findPathGreedy(trialGrid, false);` 之间插入：
```java
        this.lastTrialGrid = trialGrid;
```

5b. 替换 `checkTowerPlacement`（MovementGrid.java:223-233）中 searchGreedy 调用后的注释块：
```java
            // 注意：原代码中有一个敌人位置检查，但逻辑有误
            // （在 BFS 之前检查 trialGrid[enemyPos] == 0，导致总是返回 false）
            // 已移除该检查，因为 searchGreedy 已经正确检查了路径是否可达
```
为：
```java
            // 原版检查：BFS 后若本路径敌人所站格仍不可达（未被寻路标记），
            // 说明放塔会困住该敌人 → 拒绝放置（原版 MovementGrid.java:50-64）
            if (cachedTowerResult && enemy_list != null && lastTrialGrid != null) {
                for (Enemy e = enemy_list; e != null; e = e.next) {
                    if (e.getPathNum() == pathIdx
                            && lastTrialGrid[(e.getGridY() * gridW) + e.getGridX()] == CELL_EMPTY) {
                        cachedTowerResult = false;
                        break;
                    }
                }
            }
```
注：`findPathGreedy` 会在可达格写入方向值，BFS 后仍为 `CELL_EMPTY` 的格 = 不可达，与原版 `trial_grid[...] == 0` 语义一致。

- [ ] **Step 6: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/GameTower.java core/src/main/java/com/rdefense/core/game/Enemy.java core/src/main/java/com/rdefense/core/game/LevelDataGenerator.java core/src/main/java/com/rdefense/core/game/MovementGrid.java
git commit -m "fix: 冲击波伤害、触雷豁免、灼烧封顶、TOUGH_MASK 保真、敌人拒放检查（域① #1/#5/#8/#10/#11）"
```

---

### Task 6: GameState 健壮性（上限安全网 / 读档校验 / 自动存档 / 成就时机）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`（allocateGameEvent、allocateGameEnemy、initGame、loadState、nextLevel、upgradeTower）

**Interfaces:**
- Produces: `public boolean consumeAutoSaveRequest()` — Task 7 的 GamePlayScreen 每帧轮询；`loadState` 结束后强制 `GAME_PAUSED` 并产生 `EVENT_GAME_LOAD_SUCCESS`

- [ ] **Step 1: 事件 500 上限 + clearEventQueue（GameState.java:552-565 的 allocateGameEvent）**

在 `e = new GameEvent(); this.events_allocated++;` 之后插入：
```java
            if (this.events_allocated > 500) {
                // 原版防 OOM：超限时清空粒子与爆炸事件队列
                clearEventQueue(GameEvent.EVENT_PARTICLE);
                clearEventQueue(GameEvent.EVENT_EXPLOSION);
            }
```
并在 `recycleGameEvents` 方法后新增：
```java
    /** 清空指定类型的事件队列（原版 clearEventQueue） */
    private void clearEventQueue(int event_type) {
        while (this.event_list[event_type] != null) {
            this.events_allocated--;
            this.active_events--;
            this.event_list[event_type] = this.event_list[event_type].next;
        }
    }
```

- [ ] **Step 2: 敌人 100 上限安全网（GameState.java:596-608 的 allocateGameEnemy）**

在 `ge = new Enemy(); this.enemies_allocated++;` 之后插入：
```java
            if (this.enemies_allocated > 100) {
                showError("内部错误: 敌人分配数 " + this.enemies_allocated + " > 100");
                endGame(GAME_NOT_STARTED);
            }
```

- [ ] **Step 3: initGame 重算 events_allocated（GameState.java:112 附近）**

替换：
```java
        this.event_pool = null;
        this.events_allocated = 0;
        this.active_events = 0;
```
为：
```java
        this.event_pool = null;
        // 原版：event_list 残留事件保留并重新计数（配合 500 上限）
        this.events_allocated = 0;
        this.active_events = 0;
        for (int event_type = 0; event_type < GameEvent.NUM_EVENT_TYPES; event_type++) {
            for (GameEvent ev = this.event_list[event_type]; ev != null; ev = ev.next) {
                this.events_allocated++;
                this.active_events++;
            }
        }
```

- [ ] **Step 4: initGame 末尾恢复生存模式难度曲线（域② #14，方法末尾 setHealth 之后）**

```java
        if (this.survival_mode) {
            this.difficulty_level = LevelDataGenerator.getSurvivalDifficultyLevel(
                    this.level_data.getLevelNum());
        }
```

- [ ] **Step 5: loadState 尾部强制暂停 + 加载成功事件（GameState.java:1206-1215，"恢复标量"块之后、`return true` 之前）**

```java
        // 原版：读档后强制暂停并通知 UI（玩家确认后再继续）
        this.run_state = GAME_PAUSED;
        allocateGameEvent(GameEvent.EVENT_GAME_LOAD_SUCCESS);
```
并将其上一行 `run_state = savedRunState;` 删除（`savedRunState` 变量保留读取以保持流位置）。

- [ ] **Step 6: 每 10 关自动存档请求（GameState.java:395-398）**

类字段区新增：
```java
    private boolean auto_save_requested; // 每 10 关自动存档请求（由 GamePlayScreen 消费）
```
替换 nextLevel 中：
```java
            if (this.level_data.getLevelNum() % 10 == 0) {
                // 每10关自动保存
                // QuickSave.saveState(this, false);
            }
```
为：
```java
            if (this.level_data.getLevelNum() % 10 == 0) {
                this.auto_save_requested = true; // GamePlayScreen 轮询后执行 quickSave
            }
```
Getter 区新增：
```java
    /** 取走自动存档请求（一次性），由 GamePlayScreen 每帧轮询 */
    public boolean consumeAutoSaveRequest() {
        boolean pending = this.auto_save_requested;
        this.auto_save_requested = false;
        return pending;
    }
```

- [ ] **Step 7: 成就时机修正（GameState.java:786-803 的 upgradeTower）**

替换整个方法体：
```java
    public void upgradeTower(GameTower tower, int new_tower_id) {
        if (new_tower_id == 0) {
            sellTower(tower);
            // 原版：出售时 ≤12 才递减强化计数（POWERED_UP 成就条件）
            if (this.tower_powup_counter <= 12) {
                this.tower_powup_counter--;
            }
            return;
        }
        if (TowerData.cost(new_tower_id) <= this.money) {
            setMoney(this.money - TowerData.cost(new_tower_id));
            int old_id = tower.getType();
            tower.init(this.collision_grid, tower.getGridX(), tower.getGridY(), new_tower_id, this.state_index);
            if (TowerData.isBlocking(old_id) != TowerData.isBlocking(new_tower_id)) {
                towersChanged();
            } else {
                allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
            }
            // 原版：仅升级成功后才失去"吝啬鬼"资格
            this.cheapskate = false;
        }
    }
```
（`no_sale = false` 已在 `sellTower` 内处理，不动。）

- [ ] **Step 8: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "fix: 事件/敌人上限安全网、读档强制暂停、自动存档请求、成就时机保真（域② #5/#6/#7/#8/#10/#12/#14/#15）"
```

---

### Task 7: ScoreOverlay 结算明细 + GamePlayScreen 状态转换接线

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/render/UiRenderer.java`（新增 renderScoreOverlay）
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java:270-288`（覆盖层 switch）及 update 循环

**Interfaces:**
- Consumes: `GameEvent.EVENT_SCORE_SAVED` + `VAR_SCORE_*`（Task 2 产生）、`RewardData.gameWon(PlayerPrefs)`（Task 1）、`GameState.consumeAutoSaveRequest()`（Task 6）、`game.getPlayerPrefs()`（`RoboDefenseGame.java:317`）、`game.getGameSaveManager()`（GamePlayScreen:678 已用）
- Produces: `UiRenderer.renderScoreOverlay(GameState gameState)`；GamePlayScreen 新增字段 `lastRunState`

- [ ] **Step 1: UiRenderer 新增 renderScoreOverlay（放在 renderGameOverOverlay 附近）**

```java
    /**
     * 关卡结算明细（消费 EVENT_SCORE_SAVED，对应原版 ScoreOverlay 的简化实现：
     * 五行明细逐行显示 + 合计，帧驱动淡入）
     */
    public void renderScoreOverlay(com.rdefense.core.game.GameState gameState) {
        com.rdefense.core.game.GameEvent e =
                gameState.getGameEventList(com.rdefense.core.game.GameEvent.EVENT_SCORE_SAVED);
        if (e == null) return;

        int frame = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_FRAME_INDEX]++;
        int scoreAdd = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_ADD];
        int wonBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_WON_BONUS];
        int healthBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_HEALTH_BONUS];
        int perfectBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_PERFECT_BONUS];
        int moneyBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_MONEY_BONUS];
        long total = (long) scoreAdd + wonBonus + healthBonus + perfectBonus + moneyBonus;

        int cx = renderer.getScreenWidth() / 2 - 110;
        int cy = renderer.getScreenHeight() / 2 - 90;
        renderer.begin();
        renderer.drawRect(cx - 20, cy - 30, 280, 190, 0f, 0f, 0f, 0.75f);
        // 每 12 帧多显示一行（原版为逐项弹出动画）
        String[] labels = {"得分", "胜利奖励", "生命奖励", "完美奖励", "金钱奖励"};
        int[] values = {scoreAdd, wonBonus, healthBonus, perfectBonus, moneyBonus};
        int shown = Math.min(labels.length, frame / 12 + 1);
        for (int i = 0; i < shown; i++) {
            renderer.drawText(labels[i], cx, cy + i * 24, 0.75f, 0.85f, 0.95f, 1.0f);
            renderer.drawText("+" + values[i], cx + 150, cy + i * 24, 1.0f, 0.9f, 0.2f, 1.0f);
        }
        if (shown >= labels.length) {
            renderer.drawText("合计积分", cx, cy + 132, 1.0f, 1.0f, 1.0f, 1.0f);
            renderer.drawText("+" + total, cx + 150, cy + 132, 0.2f, 1.0f, 0.6f, 1.0f);
        }
        renderer.end();
    }
```
注：文案「得分/胜利奖励/生命奖励/完美奖励/金钱奖励/合计积分」所含汉字须包含于 `CHINESE_CHARS`；执行时若缺字（渲染为方块），把缺失汉字追加到该常量。

- [ ] **Step 2: GamePlayScreen 覆盖层 switch 中接入（GamePlayScreen.java:279-284）**

替换：
```java
            case GameState.GAME_LOST:
                uiRenderer.renderGameOverOverlay("游戏结束", "点击重试");
                break;
            case GameState.GAME_WON:
                uiRenderer.renderGameOverOverlay("胜利！", "点击继续");
                break;
```
为：
```java
            case GameState.GAME_LOST:
                uiRenderer.renderGameOverOverlay("游戏结束", "点击重试");
                uiRenderer.renderScoreOverlay(gameState);
                break;
            case GameState.GAME_WON:
                uiRenderer.renderGameOverOverlay("胜利！", "点击继续");
                uiRenderer.renderScoreOverlay(gameState);
                break;
```

- [ ] **Step 3: GamePlayScreen 状态转换检测（update 中、gameLoop.tick 调用之后）**

类字段区新增：
```java
    private int lastRunState = -1; // 上一帧 run_state，用于检测胜负转换
```
update 中插入：
```java
        // 胜负转换时执行平台副作用（原版 endGame 中的 SharedPreferences 部分）
        int rs = gameState.getRunState();
        if (rs != lastRunState) {
            if (rs == GameState.GAME_WON) {
                com.rdefense.core.game.RewardData.gameWon(game.getPlayerPrefs());
                game.getGameSaveManager().clearQuickSave();
            } else if (rs == GameState.GAME_LOST) {
                game.getGameSaveManager().clearQuickSave();
            }
            lastRunState = rs;
        }
        // 每 10 关自动快速存档（原版 QuickSave.saveState(this, false)）
        if (gameState.consumeAutoSaveRequest()) {
            game.getGameSaveManager().quickSave(gameState);
        }
```

- [ ] **Step 4: 点击继续时结束结算事件（GamePlayScreen 处理 WON/LOST 状态点击的分支中）**

在点击继续/重试的现有处理前插入：
```java
        // 关闭结算明细事件（否则残留到下一局）
        for (com.rdefense.core.game.GameEvent e =
                gameState.getGameEventList(com.rdefense.core.game.GameEvent.EVENT_SCORE_SAVED);
                e != null; e = e.next) {
            e.finished = true;
        }
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add core/src/main/java/com/rdefense/core/render/UiRenderer.java core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
git commit -m "feat: 结算明细覆盖层 + 胜负转换难度递增/清存档/自动存档接线（P0-1/P0-4 收尾）"
```

---

### Task 8: 消息系统槽位与 showMessage（域② #4）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java:966-972`

**Interfaces:**
- Produces: `public void showMessage(String message)`（45 帧短消息）；`showError` 改走槽位分配；消息事件 `var[VAR_MESSAGE_FRAMES] = 显示帧数 + 30`（30 为淡入帧），`var[VAR_MESSAGE_Y_SLOT]` = 0-9 槽位

- [ ] **Step 1: 替换 showError 并新增 showMessage/createMessageEvent（GameState.java:966-972）**

```java
    /** 显示短消息（原版 showMessage，45 帧） */
    public void showMessage(String message) {
        createMessageEvent(message, 45);
    }

    /** 显示错误消息（原版 showError，90 帧） */
    public void showError(String message) {
        createMessageEvent(message, 90);
    }

    /** 创建消息事件：分配 0-9 号不重叠槽位，+30 帧淡入（原版 createMessageEvent） */
    private void createMessageEvent(String message, int display_frames) {
        int used_slots = 0;
        for (GameEvent e = getGameEventList(GameEvent.EVENT_MESSAGE); e != null; e = e.next) {
            used_slots |= 1 << e.var[GameEvent.VAR_MESSAGE_Y_SLOT];
        }
        int slot_num = 0;
        while (slot_num < 10 && ((1 << slot_num) & used_slots) != 0) {
            slot_num++;
        }
        GameEvent e2 = allocateGameEvent(GameEvent.EVENT_MESSAGE);
        e2.str = message;
        e2.var[GameEvent.VAR_MESSAGE_FRAMES] = display_frames + 30;
        e2.var[GameEvent.VAR_MESSAGE_Y_SLOT] = slot_num;
    }
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "fix: 消息事件槽位管理与 showMessage 恢复（域② #4）"
```

---

### Task 9: 全量构建与运行时手动验证

**Files:**
- 无代码改动；产出验证记录

- [ ] **Step 1: 全量构建**

Run: `./gradlew :desktop:dist`
Expected: BUILD SUCCESSFUL，产出 `desktop/build/libs/desktop.jar`

- [ ] **Step 2: 冒烟启动（debug 直入游戏）**

Run: `timeout 20 java -Drdefense.debugStartGame=true -jar desktop/build/libs/desktop.jar; echo "EXIT_CODE=$?"`
Expected: 窗口正常打开无异常栈；EXIT_CODE=124（超时正常终止）

- [ ] **Step 3: 手动验证清单（人工执行，逐项打勾）**

1. 新开一局 basic 难度 N → 故意输掉 → 出现结算明细（仅「得分」行有值）→ 奖励商店积分增加了该得分值
2. 打赢一局 → 结算明细五行 + 合计 → 返回关卡选择：难度滑块默认值 = N+1
3. 胜/负后主菜单「继续游戏」不可用（快速存档已清除）
4. 通过 10 关时 ESC 退出再「继续游戏」→ 从第 10 关恢复（自动存档生效）
5. 击杀普通敌人得分显著低于修复前（除数 500）；击杀泰坦死亡动画明显变长
6. 在敌人将被围死的位置放阻挡塔 → 被拒绝
7. 读档后游戏处于暂停态

- [ ] **Step 4: 提交验证记录**

把上述清单勾选结果追加到本文件末尾「验证记录」节并提交：
```bash
git add docs/superpowers/plans/2026-07-17-apk-parity-fix-1-scoring-numeric.md
git commit -m "docs: 计划①运行时验证记录"
```

---

## 计划自审记录

- 规格覆盖：P0-1/P0-4 全覆盖；域① 13 条中 #1-#8/#10/#11/#13 覆盖（#9 Z_ACCEL 按决策保留、#12 imageCenter 低影响移计划④）；域② 15 条中 #1/#2/#4/#5/#6/#7/#8/#10/#12/#14/#15 覆盖（#3 事件通道与 #9 存档校验依赖计划②③的 UI/存档改造，#13 PerformanceMonitor 移计划④）；存档字段補存（域② #11）属计划②
- 占位符扫描：所有代码步骤给出完整代码；Task 4 Step 5 对调用名差异给了双分支处理说明
- 类型一致性：`gameWon(PlayerPrefs)`（Task 1）↔ Task 7 调用；`consumeAutoSaveRequest()`（Task 6）↔ Task 7 轮询；`VAR_SCORE_*`（Task 2 写入）↔ Task 7 读取，均一致


