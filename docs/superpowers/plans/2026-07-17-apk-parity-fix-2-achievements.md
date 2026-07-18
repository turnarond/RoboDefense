# APK 保真修复计划②：成就弹窗链路 / 存档字段补全 / 初始事件 / HUD

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复成就获得弹窗链路（P0-2），补全存档 10 个漏存字段，恢复游戏初始事件，HUD 添加千位分隔符。

**Architecture:** 成就弹窗通过 GamePlayScreen 在每帧轮询 `AchievementData.dequeueEarned()` 并调用 `achievementRenderer.showAchievement()`——不经过 GameEvent 体系（原版的 EVENT_ACHIEVEMENT_EARNED 事件通道在 UI 层无监听，重构风险高于直接调用）。存档字段在 `GameState.saveState/loadState` 和 `Enemy.saveState` 中按原版顺序恢复。HUD 在 GamePlayScreen 中追加 `,` 分隔。

**Tech Stack:** Java 8 / libGDX 1.12.1 / Gradle。

**依据：** `docs/06-APK差距分析报告.md`（P0-2、域② #3/#11、域③ #1、域⑤ P1）。

## Global Constraints

- Java 8 语法（`import` 用完整路径，禁 `var`、`List.of()`）
- 注释用简体中文
- 编译：`./gradlew :core:compileJava`，期望 `BUILD SUCCESSFUL`
- 每提交末尾 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- 项目无测试框架，验证环 = 编译门 + 手动冒烟
- 已有 `AchievementData.dequeueEarned()`（`AchievementData.java:506`）、`achievementRenderer.showAchievement(int type)`（`AchievementRenderer.java:34`）、`game.getAchievementRenderer()`（`RoboDefenseGame.java:321`）

---

### Task 1: 成就弹窗链路（P0-2）— GameState + GamePlayScreen

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java:138-159`（nextState）
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java`（每帧轮询成就队列）

**Interfaces:**
- Consumes: `AchievementData.dequeueEarned()`（静态方法，返回下一个成就类型，无待处理返回 -1）
- Produces: GamePlayScreen 每帧调 `achievementRenderer.showAchievement(type)` 输入类型 `int`，输出动画并播放音效（无返回值）

- [ ] **Step 1: GameState.nextState 中增加成就弹出调用（原版 dequeueAchievements）**

GameState.java 第 138 行 `nextState()` 方法体内，`recycleGameEvents();` 之后、`if (this.run_state == ...)` 之前插入：

```java
        // 原版 dequeueAchievements：将待显示成就转为事件
        dequeueAchievements();
```

然后在 `gameWonAchievements()` 方法之前新增：

```java
    /** 原版 dequeueAchievements：从成就队列取出新达成项并分配事件（类型 10） */
    private void dequeueAchievements() {
        for (int type = AchievementData.dequeueEarned(); type >= 0;
                type = AchievementData.dequeueEarned()) {
            GameEvent e = allocateGameEvent(GameEvent.EVENT_ACHIEVEMENT_EARNED);
            e.var[GameEvent.VAR_ACHIEVEMENT_TYPE] = type;
            e.var[GameEvent.VAR_ACHIEVEMENT_FRAME] = 0;
            e.var[GameEvent.VAR_ACHIEVEMENT_STATE] = 0;
        }
    }
```

- [ ] **Step 2: GamePlayScreen 每帧轮询成就队列并触发弹窗**

**2a.** 确认 `achievementRenderer` 字段引用的位置：GamePlayScreen 已经引入 `game`（搜 `game.get` 确认存在 `game.getAchievementRenderer()` 的调用效果）。

**2b.** 在 render 方法或 update 方法的主循环中（`gameLoop.tick(..)` 后）增加：

```java
        // 成就弹窗：轮询队列并触发渲染器动画
        int achievementType = com.rdefense.core.game.AchievementData.dequeueEarned();
        if (achievementType >= 0) {
            game.getAchievementRenderer().showAchievement(achievementType);
        }
```

**2c.** 在 draw 方法的覆盖层/地图绘制**之前**（约 250 行附近 render 方法中状态获取处）增加：

```java
        // 更新和绘制成就弹窗动画
        game.getAchievementRenderer().update(game.getAudio());
        game.getAchievementRenderer().draw(renderer);
```

- [ ] **Step 3: 编译**

```bash
./gradlew :core:compileJava
```

- [ ] **Step 4: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
git commit -m "feat: 成就弹窗链路恢复 — nextState 出队 + GamePlayScreen 轮询触发（P0-2）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: 存档 8 个成就追踪字段补全（域② #11）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`（saveState 与 loadState）

**Interfaces:**
- Consumes: `GameSaveWriter.writeBoolean(boolean)`、`writeInt(int)`；`GameSaveReader.readBoolean()`、`readInt()`
- Produces: 存档中的 8 个字段——其它任务不消费这些字段的内部值，但读档后 behavior 影响成就检测

- [ ] **Step 1: saveState 末尾增加 8 字段写入**（当前 saveState 约 1025-1063 行）

在 `saveState(GameSaveWriter out)` 方法的 `// enemy 链表` 块结束到方法结尾 `}` 之间，新增（在写完 enemy 链表 **之后**、方法 `}` **之前**）：

```java
            // 成就追踪字段（原版 GameState.saveState 写入；读档后 initGame 重置，需后期恢复）
            out.writeBoolean(no_slow_towers_created);
            out.writeBoolean(no_gun_towers_created);
            out.writeBoolean(no_rocket_towers_created);
            out.writeBoolean(cheapskate);
            out.writeBoolean(no_sale);
            out.writeBoolean(only_one_tower_created);
            out.writeInt(tower_powup_counter);
            out.writeInt(fast_fwd_counter);
```

- [ ] **Step 2: loadState 读取敌人块结束后额外读 8 字段**（约 1136-1197 行间）

**2a.** 在 `// 6. 重建 movement_grid 路径` 块之前（约 1200 行），读取敌人块末尾后插入：

```java
        // 读取成就追踪字段（对应 saveState 末尾写入顺序）
        boolean saved_no_slow   = in.readBoolean();
        boolean saved_no_gun    = in.readBoolean();
        boolean saved_no_rocket = in.readBoolean();
        boolean saved_cheapskate = in.readBoolean();
        boolean saved_no_sale   = in.readBoolean();
        boolean saved_only_one  = in.readBoolean();
        int saved_powup         = in.readInt();
        int saved_fastfwd       = in.readInt();
```

**2b.** 在 `// 7. 恢复被 initGame 重置的标量字段` 块（约 1205-1215 行）中，8 个变量恢复行之后追加：

```java
        no_slow_towers_created = saved_no_slow;
        no_gun_towers_created = saved_no_gun;
        no_rocket_towers_created = saved_no_rocket;
        cheapskate = saved_cheapskate;
        no_sale = saved_no_sale;
        only_one_tower_created = saved_only_one;
        tower_powup_counter = saved_powup;
        fast_fwd_counter = saved_fastfwd;
```

- [ ] **Step 3: 编译** → `./gradlew :core:compileJava`

- [ ] **Step 4: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "fix: 存档补全 8 个成就追踪字段 — saveState 写入 + loadState 恢复（域② #11）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: Enemy at_exit / exiting_grid 漏存修复（域⑤ P1）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/Enemy.java`（saveState）
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`（loadState 敌人重建块）

**Interfaces:**
- Consumes: `GameSaveWriter.writeBoolean`、`GameSaveReader.readBoolean`；`Enemy.at_exit` / `exiting_grid` 字段（`Enemy.java` 约 37-38 行已有字段但未序列化）
- Produces: 敌人存档中有 `at_exit` / `exiting_grid` 字段——GameState.loadState 读取恢复

- [ ] **Step 1: Enemy.saveState 追加两个布尔字段**

在 `Enemy.java` 的 `saveState(GameSaveWriter out)` 方法（约 389-400 行，搜 `x_offset` 找到敌人类存档写入末尾），在最后一个写 int 字段之后增加：

```java
            out.writeBoolean(at_exit);
            out.writeBoolean(exiting_grid);
```

- [ ] **Step 2: GameState.loadState 敌人重建时读两个字段**

在 `GameState.java` loadState 方法中（约 1131-1135 行，`// death_frame（无需恢复）` 对应的 `in.readInt()` 行），在 `in.readInt();`（death_frame 被跳过读取）之后增加这两行的读取：

```java
            boolean savedAtExit = in.readBoolean();
            boolean savedExitingGrid = in.readBoolean();
```

**2b.** 在敌人重建块（约 1183-1197 行，`e.init(...)` 之后、`e.at_exit = false; e.exiting_grid = false;` 这两行），将：
```java
            e.at_exit = false;
            e.exiting_grid = false;
```
替换为：
```java
            e.at_exit = savedAtExit;
            e.exiting_grid = savedExitingGrid;
```
（`savedAtExit`/`savedExitingGrid` 需要作用域提升——把声明从 2a 移到 2a 之前的临时变量区）

如果 `savedAtExit` / `savedExitingGrid` 在第 3 步缓存的临时变量区内声明，把声明放在敌人数据循环之前的缓存区。

- [ ] **Step 3: 编译** → `./gradlew :core:compileJava`

- [ ] **Step 4: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/Enemy.java core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "fix: Enemy 存档补全 at_exit/exiting_grid — 敌人出口状态持久化（域⑤ P1）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: 游戏初始事件恢复（域② #3）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java:89-129`（initGame）

- [ ] **Step 1: initGame 中增加初始事件分配（对应原版 GameState.java:84,88）**

在 `initGame(int map_id, int difficulty)` 方法中，`this.grid_order.clear();` 之后、`this.level_data.init(map_id, difficulty);` 之前插入：

```java
        allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
```

在 `this.collision_grid = new CollisionGrid(...)` 之后、`initMovementGrid();` 之前插入：

```java
        allocateGameEvent(GameEvent.EVENT_GAME_INIT);
```

- [ ] **Step 2: 编译** → `./gradlew :core:compileJava`

- [ ] **Step 3: 提交**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "fix: 游戏初始化时分配 EVENT_TOWERS_CHANGED 与 EVENT_GAME_INIT（域② #3）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: HUD 千位分隔符（域③ #1）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java:462-492`

- [ ] **Step 1: 在 GamePlayScreen 末尾新增格式化辅助方法**

```java
    /** 千位分隔格式化（原版 HudEntry.makeNumber） */
    private static String formatWithCommas(int value) {
        String s = Integer.toString(value);
        StringBuilder sb = new StringBuilder(s.length() + 2);
        int start = s.length() % 3;
        if (start == 0) start = 3;
        sb.append(s, 0, start);
        for (int i = start; i < s.length(); i += 3) {
            sb.append(',');
            sb.append(s, i, Math.min(i + 3, s.length()));
        }
        return sb.toString();
    }
```

- [ ] **Step 2: 金钱和分数绘制行使用格式化**

替换现有的两行（约 464、476 行）：
```java
renderer.drawText("$" + gameState.getMoney(), ...);
renderer.drawText(scoreText, ...);
```
为：
```java
renderer.drawText("$" + formatWithCommas(gameState.getMoney()), ...);
// scoreText 已在前面构造为 String，改为：
String scoreStr = formatWithCommas(gameState.getScore());
// 然后用 scoreStr 替代 scoreText
```

- [ ] **Step 3: 编译** → `./gradlew :core:compileJava`

- [ ] **Step 4: 提交**

```bash
git add core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
git commit -m "fix: HUD 金钱/分数千位分隔格式化（域③ #1）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: 全量构建与运行时手动验证

- [ ] **Step 1: 全量构建**
```bash
./gradlew :desktop:dist
```
期望：BUILD SUCCESSFUL

- [ ] **Step 2: 冒烟启动**
```bash
timeout 12 java -Drdefense.debugStartGame=true -jar desktop/build/libs/desktop.jar; echo "EXIT=$?"
```
期望：EXIT=124（超时正常退出），窗口无异常栈

- [ ] **Step 3: 手动验证清单**
1. 打一局直到任意成就达成 → 成就名称弹出框出现、播放音效、边框动画后消失
2. 通关 → 结算明细中分数有千位分隔符
3. 读档 → 各成就条件（不建枪塔/不建火箭塔等）在存档/读档间保持
4. 游戏开始时 initGame 不崩溃

- [ ] **Step 4: 提交验证记录**
```bash
git add docs/superpowers/plans/2026-07-17-apk-parity-fix-2-achievements.md
git commit -m "docs: 计划②运行时验证记录"
```

---

## 计划自审记录

- 规格覆盖：P0-2 全覆盖；域② #3 / #11 全覆盖；域③ #1 HUD 千位分隔全覆盖；域⑤ P1（at_exit/exiting_grid）全覆盖。域② #4（消息槽位）在计划①已实现
- 占位符扫描：无 TBD/TODO/placeholder
- 类型一致性：`dequeueEarned()` → `int`（Task 1 匹配）；`showAchievement(int)`（Task 1 匹配）；存档 8 字段为 boolean×6 + int×2（Task 2 匹配）；at_exit/exiting_grid 为 boolean×2（Task 3 匹配）
