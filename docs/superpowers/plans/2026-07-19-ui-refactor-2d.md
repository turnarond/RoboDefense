# UI 重构实施计划 2D：阶段三 管线优化

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 合并 begin/end 到 2 对、塔按钮脏标记、CameraManager 视锥 getter。纯性能优化，不改行为。

**依据:** `docs/08-UI渲染架构评审报告.md` 第五章阶段三。

## Global Constraints
- Java 8 / Gradle / BUILD SUCCESSFUL
- 不改运行时行为
- 每提交 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`

---

### Task 1: 合并 begin/end 到 2 对（P0）

**Files:** `core/.../render/GameSceneRenderer.java`

在 GameSceneRenderer 中新增 `renderWorldLayer(GameState)` 和 `renderScreenLayer(GameState)` 两个高层方法：

**renderWorldLayer**：单次 begin/end。包含 drawBackground → drawTowers → drawEnemies → drawBullets → drawEnemyDefeatedEvents → drawActiveTowerPreview。这些步骤共享同一相机变换。

**renderScreenLayer**：单次 begin/end。包含 drawHud → drawTowerButtons → renderControlButtons → renderScaleSlider → drawTowerButtonSprites → 全部 UiRenderer 覆盖层。

GamePlayScreen.draw() 改为调用这两个高层方法替代原来的逐个 draw 调用。

目标：常规帧 begin/end 从 10 对降到 2 对。

---

### Task 2: 塔按钮脏标记（P1，~5 行）

**Files:** `core/.../render/GameSceneRenderer.java` 的 drawTowerButtons

新增字段 `private int lastMoney = -1; private int lastActiveTowerId = -2;`

在 drawTowerButtons 开头检测：
```java
if (money == lastMoney && uiRenderer.getActiveTowerId() == lastActiveTowerId) return;
lastMoney = money;
lastActiveTowerId = uiRenderer.getActiveTowerId();
```
如果 UiRenderer 无 getActiveTowerId，改为 `renderer.getActiveTowerId()` 或在 GameSceneRenderer 中维护 activeTowerId 引用。

---

### Task 3: CameraManager 视锥 getter（P2，~4 行）

**Files:** `core/.../render/CameraManager.java`

新增 4 个 getter：
```java
public float getViewLeft() { return xBase; }
public float getViewRight() { return xBase + viewportWidth * zoom; }
public float getViewTop() { return yBase; }
public float getViewBottom() { return yBase + viewportHeight * zoom; }
```
纯基础设施，不在此阶段接入裁剪逻辑。

---

### Task 4: 全量构建 + 冒烟

```bash
./gradlew :desktop:dist
timeout 12 java -Drdefense.debugStartGame=true -jar desktop/build/libs/desktop.jar
```
期望：BUILD SUCCESSFUL + EXIT=124。
