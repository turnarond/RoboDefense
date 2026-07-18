# APK 保真修复计划③：混合器选择器 / 游戏内 UI / 星空背景

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 恢复混合器 5 位数码选择界面（P0-3）、接入可见控制按钮（P0-5）、实现星空粒子背景（P0-6）、补全新游戏确认对话框/缩放滑块/关卡选择声音开关（域③ P1）。

**Architecture:** 混合器选择器作为 LevelSelectScreen 内嵌面板（点击混合器勾选框展开），写入 `PlayerPrefs.putMixerValue/putTowerMixerValue`；控制按钮在 GamePlayScreen 绘制循环中调用现有的 `uiRenderer.renderControlButtons(runState)` ；星空为独立粒子类 `Starfield.java`，在 `GameWorldRenderer` 的宇宙/公路图背景中绘制。

**Tech Stack:** Java 8 / libGDX 1.12.1 / Gradle。

**依据：** `docs/06-APK差距分析报告.md`（P0-3/P0-5/P0-6 + 域③ P1 #2/#4/#5/#7）。

## Global Constraints

- Java 8 语法（禁 `var`、`List.of()`）
- 注释用简体中文
- 编译：`./gradlew :core:compileJava`，期望 `BUILD SUCCESSFUL`
- 每提交末尾 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- 项目无测试框架，验证环 = 编译门 + 手动冒烟
- UI 文本若新增汉字需同步 `CHINESE_CHARS` 常量（位于 `LibGdxRenderer.java` 或字体配置中）

---

### Task 1: 控制按钮接入（P0-5）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java`（render 方法中调用 renderControlButtons）

- [ ] **Step 1: 在 GamePlayScreen 绘制循环中调用控制按钮**

在 render 方法中（搜 `uiRenderer.renderTowerButtons`），在该行之后追加：
```java
        // 底部可见控制按钮（暂停/快进/菜单）
        uiRenderer.renderControlButtons(gameState.getRunState());
```

- [ ] **Step 2: 编译**
```bash
./gradlew :core:compileJava
```

- [ ] **Step 3: 提交**
```bash
git add core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
git commit -m "feat: 接入可见控制按钮 — renderControlButtons 调用（P0-5）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 2: 星空粒子背景 Starfield（P0-6）

**Files:**
- Create: `core/src/main/java/com/rdefense/core/game/Starfield.java`
- Modify: `core/src/main/java/com/rdefense/core/render/GameWorldRenderer.java`（星空图背景时绘制）
- Modify: `core/src/main/java/com/rdefense/core/game/LevelData.java`（星空标记字段）

- [ ] **Step 1: 创建 Starfield.java**

```java
package com.rdefense.core.game;

/** 星空粒子背景（对应原版 Starfield），用于宇宙/公路类关卡 */
public final class Starfield {

    private static final int NUM_STARS = 80;
    private static final int FIELD_WIDTH = 640;
    private static final int FIELD_HEIGHT = 384;
    private static final int SCROLL_SPEED = 1;

    private final short[] starX = new short[NUM_STARS];
    private final short[] starY = new short[NUM_STARS];
    private final byte[] starAlpha = new byte[NUM_STARS];
    private final FastRandom rng = new FastRandom();

    public Starfield(int seed) {
        FastRandom rand = new FastRandom();
        rand.setSeed(seed);
        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] = (short) (rand.nextInt(FIELD_WIDTH));
            starY[i] = (short) (rand.nextInt(FIELD_HEIGHT));
            starAlpha[i] = (byte) (60 + rand.nextInt(196));
        }
    }

    /** 每帧推进：所有星向下滚动，出屏者循环到顶部 */
    public void update() {
        for (int i = 0; i < NUM_STARS; i++) {
            int y = (starY[i] & 0xFFFF) + SCROLL_SPEED;
            if (y >= FIELD_HEIGHT) {
                y -= FIELD_HEIGHT;
                starX[i] = (short) (rng.nextInt(FIELD_WIDTH));
                starAlpha[i] = (byte) (60 + rng.nextInt(196));
            }
            starY[i] = (short) y;
        }
    }

    /** 将星空绘制到渲染器（坐标已按相机变换） */
    public void draw(com.rdefense.core.platform.GameRenderer renderer, int offsetX, int offsetY) {
        for (int i = 0; i < NUM_STARS; i++) {
            int sx = (starX[i] & 0xFFFF) + offsetX;
            int sy = (starY[i] & 0xFFFF) + offsetY;
            float a = (starAlpha[i] & 0xFF) / 255f;
            renderer.drawRect(sx, sy, 2, 2, 1f, 1f, 1f, a);
        }
    }
}
```

- [ ] **Step 2: LevelData 中增加 hasStarfield 标记**

在 `LevelData.java` 字段区新增：
```java
    private Starfield starfield;
    public Starfield getStarfield() { return starfield; }
```

在 LevelData.init 中的 ROADWAY_LEVEL 和 SKYTOWER_LEVEL 分支中（初始化 obstacle 后），调用：
```java
        this.starfield = new Starfield(level_seed);
```

其余图分支设 `this.starfield = null;`

- [ ] **Step 3: GameWorldRenderer 绘制星空**

在 `drawBackground` 或地图绘制循环中（搜 "roadway" 或背景绘制处），星空图时追加：
```java
        if (levelData.getStarfield() != null) {
            levelData.getStarfield().update();
            levelData.getStarfield().draw(renderer, 0, 0);
        }
```

- [ ] **Step 4: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/game/Starfield.java core/src/main/java/com/rdefense/core/game/LevelData.java core/src/main/java/com/rdefense/core/render/GameWorldRenderer.java
git commit -m "feat: 星空粒子背景 — 宇宙/公路关卡视觉恢复（P0-6）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 3: 混合器 5 位数码选择器（P0-3）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/LevelSelectScreen.java`

- [ ] **Step 1: 状态字段**

在 LevelSelectScreen 类字段区新增：
```java
    private boolean mixerPanelOpen = false;  // 混合器选择面板是否展开
    private static final int DIGIT_COUNT = 5;
    private int[] mixerDigits = new int[DIGIT_COUNT]; // 每位的值 0-9
    private int[] towerMixerDigits = new int[DIGIT_COUNT];
    private boolean editingTowerMixer = false; // true=塔混合器, false=普通混合器
```

- [ ] **Step 2: init 中从 prefs 加载当前值**

在 `init()` 或构造函数中已有 `mixerValue`/`towerMixerValue` 读取处，追加：
```java
        if (mixerValue <= 0) mixerValue = new com.rdefense.core.game.FastRandom().nextInt(99999);
        if (towerMixerValue <= 0) towerMixerValue = new com.rdefense.core.game.FastRandom().nextInt(99999);
        loadDigits(mixerValue, mixerDigits);
        loadDigits(towerMixerValue, towerMixerDigits);
```

辅助方法：
```java
    private static void loadDigits(int value, int[] digits) {
        for (int i = DIGIT_COUNT - 1; i >= 0; i--) {
            digits[i] = value % 10;
            value /= 10;
        }
    }
    private static int digitsToValue(int[] digits) {
        int v = 0;
        for (int i = 0; i < DIGIT_COUNT; i++) v = v * 10 + digits[i];
        return v;
    }
```

- [ ] **Step 3: 混合器展开面板 UI**

在 draw 方法中（混合器勾选框 `drawToggle` 之后），当 `mixerPanelOpen` 时绘制面板：
```java
        if (mixerPanelOpen) {
            drawMixerPanel(r);
        }
```

新增 `drawMixerPanel` 方法（约 60 行，参数为 renderer）：
```java
    private void drawMixerPanel(GameRenderer r) {
        int px = mixChkX + 40;
        int py = modeRowY - 10;
        int panelW = 280;
        int panelH = 90;
        r.drawRect(px, py, panelW, panelH, 0.05f, 0.08f, 0.12f, 0.92f);
        r.drawText(editingTowerMixer ? "塔混合器种子" : "关卡混合器种子", px + 8, py + 4,
                0.75f, 0.85f, 0.95f, 1f);

        int[] digits = editingTowerMixer ? towerMixerDigits : mixerDigits;
        for (int i = 0; i < DIGIT_COUNT; i++) {
            int dx = px + 20 + i * 48;
            int dy = py + 30;
            // 上箭头
            r.drawRect(dx + 10, dy - 16, 16, 12, 0.2f, 0.7f, 0.9f, 1f);
            r.drawText("▲", dx + 12, dy - 15, 0.9f, 0.9f, 0.9f, 1f);
            // 数字
            r.drawText(Integer.toString(digits[i]), dx + 14, dy + 8, 1f, 1f, 0.3f, 1f);
            // 下箭头
            r.drawRect(dx + 10, dy + 18, 16, 12, 0.2f, 0.7f, 0.9f, 1f);
            r.drawText("▼", dx + 12, dy + 19, 0.9f, 0.9f, 0.9f, 1f);
        }
        // 随机按钮
        int rx = px + panelW - 50;
        r.drawRect(rx, py + 50, 40, 28, 0.15f, 0.5f, 0.15f, 1f);
        r.drawText("随机", rx + 4, py + 58, 0.7f, 1f, 0.7f, 1f);
        // 切换按钮（普通/塔混合器）
        r.drawRect(px + 8, py + 50, 80, 28, 0.3f, 0.3f, 0.5f, 1f);
        r.drawText(editingTowerMixer ? "← 普通" : "→ 塔", px + 12, py + 58,
                0.9f, 0.85f, 0.7f, 1f);
    }
```

- [ ] **Step 4: 点击检测**

在 LevelSelectScreen 的触摸/点击处理中（已有 `hit()` 调用处），面板打开时追加：
```java
        if (mixerPanelOpen) {
            if (handleMixerPanelClick(x, y)) return;
        }
```

`handleMixerPanelClick` 方法：
```java
    private boolean handleMixerPanelClick(int x, int y) {
        int px = mixChkX + 40;
        int py = modeRowY - 10;
        int[] digits = editingTowerMixer ? towerMixerDigits : mixerDigits;

        // 随机按钮
        if (x >= px + 230 && x <= px + 270 && y >= py + 50 && y <= py + 78) {
            com.rdefense.core.game.FastRandom rng = new com.rdefense.core.game.FastRandom();
            int seed = rng.nextInt(99999);
            loadDigits(seed, digits);
            return true;
        }
        // 切换按钮
        if (x >= px + 8 && x <= px + 88 && y >= py + 50 && y <= py + 78) {
            editingTowerMixer = !editingTowerMixer;
            return true;
        }
        // 每位数码的上/下箭头
        for (int i = 0; i < DIGIT_COUNT; i++) {
            int dx = px + 30 + i * 48;
            if (x >= dx && x <= dx + 26) {
                if (y >= py + 14 && y <= py + 26) { // 上箭头
                    digits[i] = (digits[i] + 1) % 10;
                    return true;
                }
                if (y >= py + 48 && y <= py + 60) { // 下箭头
                    digits[i] = (digits[i] + 9) % 10;
                    return true;
                }
            }
        }
        return false;
    }
```

- [ ] **Step 5: 面板打开/关闭 + 值持久化**

勾选框点击处理中（已有 `towerMixerEnabled = !towerMixerEnabled` 行），在 `towerMixerEnabled` 被设为 true 时展开面板：
```java
        if (towerMixerEnabled) mixerPanelOpen = true;
```

面板外点击关闭面板并保存值；在 draw 循环末尾（draw 方法最后）添加：若面板打开且用户点击了面板外区域，则：
```java
        // 面板关闭时将当前数值写入 PlayerPrefs
        // （由配置界面的导航回退或选中关卡时触发：在 configureLevel 调用前写入）
```

在 `configureLevel` 调用前（约 196-197 行）：
```java
        prefs.putMixerValue(digitsToValue(mixerDigits));
        prefs.putTowerMixerValue(digitsToValue(towerMixerDigits));
```

- [ ] **Step 6: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/scene/LevelSelectScreen.java
git commit -m "feat: 混合器 5 位数码选择面板 + mixerValue 写入路径（P0-3）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 4: 新游戏快速存档覆盖确认对话框（域③ P1）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/MainMenuScreen.java`

- [ ] **Step 1: 状态字段**

```java
    private boolean showNewGameConfirm = false;
```

- [ ] **Step 2: "新游戏"按钮点击时弹确认框**

在按钮点击处理中（搜 "新游戏" 或 `LevelSelect` 的跳转处），有快速存档时：
```java
        if (game.getGameSaveManager().hasQuickSave()) {
            showNewGameConfirm = true;
        } else {
            game.setScreen(new LevelSelectScreen(game));
        }
```

- [ ] **Step 3: 确认对话框绘制**

在 draw 方法末尾（或覆盖层区）：
```java
        if (showNewGameConfirm) {
            int cx = renderer.getScreenWidth() / 2 - 120;
            int cy = renderer.getScreenHeight() / 2 - 40;
            renderer.drawRect(cx, cy, 240, 80, 0.05f, 0.08f, 0.15f, 0.95f);
            renderer.drawText("已有快速存档，是否覆盖？", cx + 20, cy + 15, 1f, 1f, 1f, 1f);
            // 确定按钮
            renderer.drawRect(cx + 30, cy + 45, 70, 25, 0.15f, 0.6f, 0.15f, 1f);
            renderer.drawText("确定", cx + 48, cy + 50, 1f, 1f, 1f, 1f);
            // 取消按钮
            renderer.drawRect(cx + 140, cy + 45, 70, 25, 0.6f, 0.2f, 0.2f, 1f);
            renderer.drawText("取消", cx + 158, cy + 50, 1f, 1f, 1f, 1f);
        }
```

- [ ] **Step 4: 点击处理**

```java
        if (showNewGameConfirm) {
            // 确定 → 清存档进选关
            if (hit(x, y, cx + 30, cy + 45, 70, 25)) {
                game.getGameSaveManager().clearQuickSave();
                game.setScreen(new LevelSelectScreen(game));
            }
            // 取消 → 关闭对话框
            else { showNewGameConfirm = false; }
            return;
        }
```

- [ ] **Step 5: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/scene/MainMenuScreen.java
git commit -m "fix: 新游戏时快速存档覆盖确认对话框（域③ P1）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 5: 缩放滑块条 DisplayScaleUI（域③ P1）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/render/UiRenderer.java`
- Modify: `core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java`

- [ ] **Step 1: UiRenderer 新增缩放滑块绘制方法**

```java
    private boolean scaleSliderDragging = false;

    /** 绘制缩放滑块条（顶部，受选项 6 控制） */
    public void renderScaleSlider(com.rdefense.core.config.OptionsData options,
                                  com.rdefense.core.render.CameraManager camera) {
        if (options == null || options.optionValue(6) == 0) return;
        int screenW = renderer.getScreenWidth();
        int barX = screenW / 2 - 75;
        int barY = 6;
        int barW = 150;
        int barH = 14;
        renderer.begin();
        // 滑块背景
        renderer.drawRect(barX, barY, barW, barH, 0.15f, 0.15f, 0.2f, 0.8f);
        // 当前缩放位置
        float scale = camera.getScale();
        float pct = (scale - 0.5f) / (2.0f - 0.5f); // 假设缩放范围 0.5-2.0
        int knobX = barX + (int)(pct * (barW - 10));
        renderer.drawRect(knobX, barY + 1, 10, barH - 2, 0.3f, 0.7f, 0.95f, 1f);
        renderer.end();
        this.scaleSliderRect = new int[]{barX, barY, barW, barH};
    }

    private int[] scaleSliderRect;

    /** 检查点击是否在缩放滑块上，并更新缩放 */
    public boolean handleScaleSliderClick(int clickX, int clickY, CameraManager camera) {
        if (scaleSliderRect == null) return false;
        int barX = scaleSliderRect[0], barY = scaleSliderRect[1],
            barW = scaleSliderRect[2], barH = scaleSliderRect[3];
        if (clickX < barX || clickX > barX + barW || clickY < barY - 6 || clickY > barY + barH + 6)
            return false;
        float pct = (float)(clickX - barX) / barW;
        float newScale = 0.5f + pct * 1.5f;
        camera.setScale(Math.max(0.5f, Math.min(2.0f, newScale)));
        scaleSliderDragging = true;
        return true;
    }

    public void releaseScaleSlider() { scaleSliderDragging = false; }
```

- [ ] **Step 2: GamePlayScreen 中调用**

render 方法中 `uiRenderer.renderControlButtons` 之后追加：
```java
        uiRenderer.renderScaleSlider(options, camera);
```

点击处理中（`onPointerDown` 或等效处）优先检测滑块：
```java
        if (uiRenderer.handleScaleSliderClick(px, py, camera)) return;
```

- [ ] **Step 3: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/render/UiRenderer.java core/src/main/java/com/rdefense/core/scene/GamePlayScreen.java
git commit -m "fix: 缩放滑块条 UI 恢复（域③ P1）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 6: 关卡选择界面声音开关 + Options 入口（域③ P1）

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/LevelSelectScreen.java`

- [ ] **Step 1: 在 LevelSelectScreen 底部增加声音开关和 Options 按钮**

在 draw 方法末尾（关卡卡片列表之后、模式行之前）追加：
```java
        // 声音开关
        boolean soundOn = prefs.getBool("enable_sound", true);
        r.drawText("声音 " + (soundOn ? "开" : "关"), 20, r.getScreenHeight() - 30,
                0.7f, 0.82f, 0.95f, 1f);
        // Options 入口按钮
        r.drawRect(r.getScreenWidth() - 100, r.getScreenHeight() - 36, 80, 28,
                0.1f, 0.15f, 0.25f, 0.85f);
        r.drawText("设置", r.getScreenWidth() - 92, r.getScreenHeight() - 28,
                0.75f, 0.85f, 0.95f, 1f);
```

- [ ] **Step 2: 点击检测**

在已有 click 处理中增加：
```java
        // 声音开关
        if (hit(x, y, 20, r.getScreenHeight() - 36, 60, 28)) {
            boolean cur = prefs.getBool("enable_sound", true);
            prefs.putBool("enable_sound", !cur);
            return;
        }
        // Options 入口
        if (hit(x, y, r.getScreenWidth() - 100, r.getScreenHeight() - 36, 80, 28)) {
            game.setScreen(new OptionsScreen(game));
            return;
        }
```

- [ ] **Step 3: 编译 + 提交**
```bash
./gradlew :core:compileJava
git add core/src/main/java/com/rdefense/core/scene/LevelSelectScreen.java
git commit -m "fix: 关卡选择界面声音开关与设置入口（域③ P1）

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

### Task 7: 全量构建 + 手动验证

- [ ] **Step 1: 全量构建**
```bash
./gradlew :desktop:dist
```
期望：BUILD SUCCESSFUL

- [ ] **Step 2: 冒烟启动**
```bash
timeout 12 java -Drdefense.debugStartGame=true -jar desktop/build/libs/desktop.jar; echo "EXIT=$?"
```
期望：EXIT=124

- [ ] **Step 3: 手动验证清单**
1. 主菜单"新游戏"→ 有快速存档时弹出确认框
2. 关卡选择：点击塔混合器复选框 → 5 数码面板展开 → 调数字/随机/切换 → 关闭后面板数值已持久化
3. 游戏中：底部可见暂停/快进按钮
4. 公路/宇宙关卡：星空背景粒子可见且滚动
5. 关卡选择：底部声音开关可切换，设置按钮可跳转 Options

- [ ] **Step 4: 提交验证记录**
```bash
git add docs/superpowers/plans/2026-07-17-apk-parity-fix-3-ui.md
git commit -m "docs: 计划③运行时验证记录"
```

---

## 计划自审记录

- 规格覆盖：P0-3/P0-5/P0-6 全覆盖；域③ P1 #2/#4/#5 覆盖
- 占位符扫描：无 TBD/TODO；所有代码给出完整实现
- 类型一致性：`drawMixerPanel` 在 Task 3 定义并在同一任务 draw 调用；`renderScaleSlider` 在 Task 5 定义且 GamePlayScreen 调用；全部 import 使用完整路径避免歧义
