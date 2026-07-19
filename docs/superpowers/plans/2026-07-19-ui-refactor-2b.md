# UI 重构实施计划 2B：阶段一 低风险清理

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development.

**Goal:** 提取标题栏/返回按钮到 GameScreen 基类、清理 UiRenderer 死代码、统一短消息、移动 formatWithCommas。

**Architecture:** 标题栏和返回按钮作为 `GameScreen` 的 protected 方法。7 Screen 的标题栏替换为 `drawTitleBar("标题")` 一行调用，6 Screen 的返回按钮替换为 `drawBackButton(x, y, w, h)`。

**依据:** `docs/08-UI渲染架构评审报告.md` 第五章阶段一。

## Global Constraints
- Java 8 / libGDX 1.12.1 / Gradle
- 编译：`./gradlew :core:compileJava`，期望 BUILD SUCCESSFUL
- 注释用简体中文
- 每提交 `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`

---

### Task 1: GameScreen 基类新增 drawTitleBar + drawBackButton

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/GameScreen.java`

- [ ] **Step 1: 新增两个 protected 方法**

在 GameScreen 类中（`showShortMessage` 方法附近）新增：

```java
    /** 标准标题栏（深蓝底条 + 蓝顶线 + 标题文字）。7 Screen 共用。 */
    protected void drawTitleBar(String title) {
        int sh = com.badlogic.gdx.Gdx.graphics.getHeight();
        int sw = com.badlogic.gdx.Gdx.graphics.getWidth();
        com.rdefense.core.platform.GameRenderer r = game.getServices().getRenderer();
        r.drawRect(0, sh - 34, sw, 34, 0.06f, 0.08f, 0.16f, 0.93f);
        r.drawRect(0, sh - 1, sw, 2, 0.2f, 0.36f, 0.55f, 0.85f);
        r.drawText(title, 16, sh - 20, 0.75f, 0.85f, 0.95f, 1.0f);
    }

    /** 标准返回按钮（科幻蓝色）。6 Screen 共用。 */
    protected void drawBackButton(float x, float y, float w, float h) {
        com.rdefense.core.platform.GameRenderer r = game.getServices().getRenderer();
        r.drawRect(x, y, w, h, 0.08f, 0.12f, 0.25f, 0.93f);
        r.drawRect(x, y, w, 2, 0.2f, 0.3f, 0.5f, 0.9f);
        r.drawText("返回", x + w / 2 - 12, y + h / 2 - 5, 0.78f, 0.82f, 0.88f, 1.0f);
    }

    /** 移动 formatWithCommas 工具方法（从 GamePlayScreen 移来） */
    protected static String formatWithCommas(int value) {
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

编译 + 提交。

---

### Task 2: 7 Screen 替换标题栏

**Files:** 修改 LevelSelectScreen, AchievementScreen, RewardScreen, GameSaveScreen, OptionsScreen, CreditsScreen, GamePlayScreen

每个 Screen 的 draw 方法开头，找到标题栏的 3 行 drawRect/drawText 块，替换为 `drawTitleBar("标题文字")` 一行调用。

- LevelSelectScreen: "关卡选择"
- AchievementScreen: "成就"
- RewardScreen: "奖励商店"
- GameSaveScreen: mode==LOAD ? "加载存档" : "保存游戏"
- OptionsScreen: "设置"
- CreditsScreen: "Credits"
- GamePlayScreen: HUD 保留当前实现（有额外底线和游戏数据）→ 不替换

编译 + 提交。

---

### Task 3: 6 Screen 替换返回按钮

**Files:** 修改 LevelSelectScreen, AchievementScreen, OptionsScreen, CreditsScreen, RewardScreen, GameSaveScreen

找到返回按钮的 drawRect + drawText 块，替换为 `drawBackButton(x, y, w, h)` 一行。
注意各 Screen 的坐标参数保留（右上 vs 左下不同位置）。

编译 + 提交。

---

### Task 4: 死代码清理 + 短消息统一

- 从 UiRenderer.java 删除 `renderActiveTowerPreview()` 方法（零调用者）。
- 从 GamePlayScreen.java 删除 `formatWithCommas` 方法（已移至 GameScreen 基类）。
- 从 GamePlayScreen.java 删除 `upgradeDialogWasVisible` 字段如果它仍然未使用。

编译 + 提交。

---

### Task 5: 全量构建验证

```bash
./gradlew :desktop:dist
```
期望：BUILD SUCCESSFUL。提交。
