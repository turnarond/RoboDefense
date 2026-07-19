# UI 重构实施计划 2C：阶段二 架构调整

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 从 GamePlayScreen 提取 GameSceneRenderer（~550 行渲染代码）和 GameInputController（~100 行输入处理），缩减 GamePlayScreen 到协调角色；消除 LibGdxRenderer 强制转型。

**Architecture:**
- `GameSceneRenderer`：持有 `GameRenderer`/`CameraManager`/`UiRenderer`，提供全部 draw* 方法 + HUD 动画数字维护
- `GameInputController`：接管 ESC/SPACE/F9 按键 + 暂停菜单点击，通过回调接口与 GamePlayScreen 通信
- `GamePlayScreen`：保留 init/生命周期/runState 切换/游戏循环集成，~200 行

**Tech Stack:** Java 8 / libGDX 1.12.1 / Gradle。

**依据:** `docs/08-UI渲染架构评审报告.md` 第二节 + 第五章阶段二。

## Global Constraints
- Java 8 / Gradle
- 编译：`./gradlew :core:compileJava`，期望 BUILD SUCCESSFUL
- 注释用简体中文
- 每提交 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- **重构不改变任何运行时行为**——仅移动代码

---

### Task 1: 创建 GameSceneRenderer

**Files:**
- Create: `core/src/main/java/com/rdefense/core/render/GameSceneRenderer.java`
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java`

从 GamePlayScreen 提取以下方法到 GameSceneRenderer（复制粘贴，不改方法体）：
- `drawBackground` + `getBackgroundName`
- `drawTowers` + `drawTower`
- `drawEnemies` + `drawEnemy`
- `drawBullets`
- `drawEnemyDefeatedEvents`
- `drawActiveTowerPreview`
- `drawTowerButtons` + `drawTowerButtonSprites` + `createTowerButton`

GameSceneRenderer 字段：`renderer`, `camera`, `uiRenderer`, `displayMoney`, `displayScore`, `displayHealth`, `TOWER_BUTTON_X_OFFSETS`。
提供 `updateAnimations(GameState)` 方法（HUD 数字渐进逼近，从 GamePlayScreen.update 移来）。

GamePlayScreen 创建 `sceneRenderer` 字段并在 init 中初始化，所有 draw* 调用改为 `sceneRenderer.drawXxx(...)`。

编译验证 + 提交。

---

### Task 2: 创建 GameInputController

**Files:**
- Create: `core/src/main/java/com/rdefense/core/input/GameInputController.java`
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java`

提取 ESC/SPACE/F9 按键处理 + 暂停菜单点击到 GameInputController。回调接口：
```java
public interface GameInputCallbacks {
    void onTogglePause();
    void onToggleFastFwd();
    void onForceHdFallback();
    void onPauseMenuContinue();
    void onPauseMenuSaveAndQuit();
    void onPauseMenuQuitWithoutSave();
}
```

GamePlayScreen 实现该接口，在 update 中将按键检测委托给 `inputController.handleKeys(gameState)`，暂停菜单点击委托给 `inputController.handlePauseMenuClick(x, y)`。

编译验证 + 提交。

---

### Task 3: GameRenderer 接口添加 applyCameraTransform

**Files:**
- Modify: `core/.../platform/GameRenderer.java`（接口）
- Modify: `core/.../platform/libgdx/LibGdxRenderer.java`（实现）
- Modify: `core/.../scene/GamePlayScreen.java`（移除强制转型）

在 GameRenderer 接口中新增：`void applyCameraTransform(float x, float y, float zoom);`
在 LibGdxRenderer 中实现（从 GamePlayScreen 的现有 `applyCameraTransform` inline 代码搬来）。

删除 GamePlayScreen 中的 `(LibGdxRenderer) renderer` 强制转型。

编译验证 + 提交。

---

### Task 4: 全量构建 + 冒烟测试验证

```bash
./gradlew :desktop:dist
timeout 12 java -Drdefense.debugStartGame=true -jar desktop/build/libs/desktop.jar; echo "EXIT=$?"
```
期望：BUILD SUCCESSFUL + EXIT=124（窗口正常运行无异常）
