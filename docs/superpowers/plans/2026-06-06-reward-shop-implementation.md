# 奖励商店系统实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpower:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现完整的奖励商店系统，移植原版23种奖励，采用卡片网格布局

**Architecture:** 基于 libGDX 渲染系统，采用模块化设计，数据与界面分离

**Tech Stack:** Java, libGDX, SQLite

---

## 文件结构

```
core/src/main/java/com/rdefense/core/
├── game/
│   └── RewardData.java              # 奖励数据模型（新建）
├── render/
│   └── RewardRenderer.java          # 奖励卡片渲染器（新建）
├── scene/
│   └── RewardScreen.java            # 奖励商店界面（升级）
└── RoboDefenseGame.java             # 游戏主类（修改）
```

---

## Task 1: 创建 RewardData 数据模型

**Files:**
- Create: `core/src/main/java/com/rdefense/core/game/RewardData.java`

- [ ] **Step 1: 创建奖励数据模型类**

```java
package com.rdefense.core.game;

import com.rdefense.core.save.db.GameSaveManager;

public final class RewardData {
    // 23种奖励类型常量
    public static final int REWARD_TYPE_COUNT = 23;
    
    // 强化类奖励
    public static final int BULLET_UPGRADE = 0;
    public static final int EXPLOSIVES_UPGRADE = 1;
    public static final int ROCKET_SPEED_UPGRADE = 2;
    public static final int ANTIAIR_SPEED_UPGRADE = 3;
    public static final int ARTILLERY_SPEED_UPGRADE = 4;
    public static final int FLAME_DURATION_UPGRADE = 5;
    public static final int SLOW_DURATION_UPGRADE = 6;
    public static final int HEALTH_UPGRADE = 7;
    public static final int STARTING_CASH_UPGRADE = 8;
    
    // 解锁类奖励
    public static final int TELEPORT_TOWER = 9;
    public static final int MINE_TOWER = 10;
    public static final int URANIUM_SHELLS = 11;
    public static final int NAPALM_SHELLS = 12;
    public static final int AIR_SNIPER = 13;
    public static final int CHEAP_FIREWORKS = 14;
    public static final int SLOW_BURN = 15;
    public static final int FLEA_MARKET = 16;
    public static final int SCRAMBLER = 17;
    public static final int CANNONBALL = 18;
    public static final int BONUS = 19;
    public static final int AIR_BURST = 20;
    public static final int SHOCKWAVE = 21;
    public static final int FLARE_TOWER = 22;

    // 效果类型
    private static final int TYPE_POWER = 0;
    private static final int TYPE_PERCENT_POWER = 1;
    private static final int TYPE_PERCENT_SPEED = 2;
    private static final int TYPE_PERCENT_EFFECT = 3;
    private static final int TYPE_UNLOCK = 4;

    // 存储键前缀
    private static final String PREFS_PREFIX = "Reward:";

    // 静态数据
    private static RewardProp[] reward_props;
    private static long reward_points;
    private static GameSaveManager saveManager;

    public static void init(GameSaveManager manager) {
        saveManager = manager;
        reward_points = saveManager.getRewardPoints();
        reward_props = new RewardProp[REWARD_TYPE_COUNT];
        
        // 初始化23种奖励
        reward_props[BULLET_UPGRADE] = new RewardProp("Stronger Bullets", "更强子弹", "+100 伤害", 50000, TYPE_POWER);
        // ... (其余22种奖励)
    }

    public static long getRewardPoints() {
        return reward_points;
    }

    public static void addRewardPoints(long points) {
        reward_points += points;
        if (saveManager != null) {
            saveManager.setRewardPoints(reward_points);
        }
    }

    public static int getLevel(int type) {
        return reward_props[type].level;
    }

    public static long getCost(int type) {
        return reward_props[type].calculateCost();
    }

    public static boolean tryUpgrade(int type) {
        long cost = getCost(type);
        if (cost > reward_points) return false;
        
        reward_points -= cost;
        reward_props[type].increaseLevel();
        saveProgress();
        return true;
    }

    public static int applyReward(int baseValue, int type) {
        RewardProp prop = reward_props[type];
        switch (prop.type) {
            case TYPE_POWER:
                return baseValue + prop.level * prop.multiplier;
            case TYPE_PERCENT_POWER:
            case TYPE_PERCENT_EFFECT:
                return baseValue * (100 + prop.level * prop.multiplier) / 100;
            case TYPE_PERCENT_SPEED:
                return baseValue * 100 / (100 + prop.level * prop.multiplier);
            case TYPE_UNLOCK:
                return prop.level > 0 ? 1 : 0;
        }
        return baseValue;
    }

    public static String getName(int type) {
        return reward_props[type].name;
    }

    public static String getDescription(int type) {
        return reward_props[type].description;
    }

    public static boolean isUnlockable(int type) {
        return reward_props[type].type == TYPE_UNLOCK;
    }

    public static boolean isMaxLevel(int type) {
        return reward_props[type].type == TYPE_UNLOCK && reward_props[type].level > 0;
    }

    public static boolean canAfford(int type) {
        return getCost(type) <= reward_points;
    }

    private static void saveProgress() {
        if (saveManager != null) {
            for (int i = 0; i < REWARD_TYPE_COUNT; i++) {
                saveManager.setPreferenceInt(PREFS_PREFIX + reward_props[i].key, reward_props[i].level);
            }
        }
    }

    private static class RewardProp {
        String key;
        String name;
        String description;
        int baseCost;
        int type;
        int level;
        int multiplier = 1;

        RewardProp(String key, String name, String description, int baseCost, int type) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.baseCost = baseCost;
            this.type = type;
            
            if (saveManager != null) {
                this.level = saveManager.getPreferenceInt(PREFS_PREFIX + key, 0);
            }
        }

        long calculateCost() {
            if (type == TYPE_UNLOCK && level > 0) return Long.MAX_VALUE;
            long cost = baseCost;
            for (int i = 0; i < level; i++) {
                cost = (cost * 6) / 5;
                if (cost > 2000000000L) return 2000000000L;
            }
            return cost;
        }

        void increaseLevel() {
            level++;
        }
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add core/src/main/java/com/rdefense/core/game/RewardData.java
git commit -m "feat: 创建奖励数据模型 RewardData"
```

---

## Task 2: 创建 RewardRenderer 渲染器

**Files:**
- Create: `core/src/main/java/com/rdefense/core/render/RewardRenderer.java`

- [ ] **Step 1: 创建卡片渲染器类**

```java
package com.rdefense.core.render;

import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;

public class RewardRenderer {

    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 130;
    private static final int CARD_PADDING = 10;
    private static final int COLUMNS = 4;

    private int selectedIndex = -1;
    private int hoverIndex = -1;

    public void drawRewardGrid(GameRenderer renderer, int scrollOffset, long rewardPoints) {
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();
        
        int startX = (screenWidth - (COLUMNS * (CARD_WIDTH + CARD_PADDING))) / 2;
        int startY = screenHeight - 150;

        for (int i = 0; i < RewardData.REWARD_TYPE_COUNT; i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            int x = startX + col * (CARD_WIDTH + CARD_PADDING);
            int y = startY - row * (CARD_HEIGHT + CARD_PADDING);

            if (y < 100) continue;

            CardState state = getCardState(i, rewardPoints);
            drawCard(renderer, x, y, i, state);
        }
    }

    private CardState getCardState(int type, long rewardPoints) {
        if (RewardData.isMaxLevel(type)) return CardState.MAXED;
        if (RewardData.isUnlockable(type) && RewardData.getLevel(type) > 0) return CardState.UNLOCKED;
        if (!RewardData.canAfford(type)) return CardState.CANNOT_AFFORD;
        return CardState.AVAILABLE;
    }

    private void drawCard(GameRenderer renderer, int x, int y, int type, CardState state) {
        float[] bgColor = getBackgroundColor(state);
        renderer.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT, bgColor[0], bgColor[1], bgColor[2], 0.9f);

        // 绘制名称
        renderer.drawText(RewardData.getName(type), x + 10, y + CARD_HEIGHT - 20, 1.0f, 1.0f, 1.0f, 1.0f);

        // 绘制等级
        int level = RewardData.getLevel(type);
        String levelText = RewardData.isUnlockable(type) ? 
            (level > 0 ? "已解锁" : "未解锁") : 
            "Lv." + (level + 1);
        renderer.drawText(levelText, x + 10, y + CARD_HEIGHT - 40, 0.7f, 0.7f, 0.7f, 1.0f);

        // 绘制费用/按钮
        if (state == CardState.AVAILABLE) {
            long cost = RewardData.getCost(type);
            renderer.drawText(formatCost(cost), x + 10, y + 20, 0.8f, 0.8f, 0.8f, 1.0f);
            renderer.drawRect(x + 10, y + 5, CARD_WIDTH - 20, 25, 0.2f, 0.6f, 0.2f, 0.9f);
            renderer.drawText("升级", x + 55, y + 17, 1.0f, 1.0f, 1.0f, 1.0f);
        } else if (state == CardState.MAXED) {
            renderer.drawText("已满级", x + 40, y + 17, 0.6f, 0.6f, 0.6f, 1.0f);
        } else if (state == CardState.CANNOT_AFFORD) {
            long cost = RewardData.getCost(type);
            renderer.drawText("需要 " + formatCost(cost), x + 20, y + 17, 0.8f, 0.4f, 0.4f, 1.0f);
        }
    }

    private float[] getBackgroundColor(CardState state) {
        switch (state) {
            case AVAILABLE: return new float[]{0.1f, 0.2f, 0.4f};
            case MAXED: return new float[]{0.2f, 0.2f, 0.2f};
            case CANNOT_AFFORD: return new float[]{0.15f, 0.1f, 0.1f};
            default: return new float[]{0.1f, 0.15f, 0.3f};
        }
    }

    private String formatCost(long cost) {
        if (cost >= 1000000) return (cost / 1000000) + "M";
        if (cost >= 1000) return (cost / 1000) + "K";
        return String.valueOf(cost);
    }

    public int getCardAt(int x, int y, int startX, int startY) {
        int col = (x - startX) / (CARD_WIDTH + CARD_PADDING);
        int row = (startY - y) / (CARD_HEIGHT + CARD_PADDING);
        if (col < 0 || col >= COLUMNS) return -1;
        int index = row * COLUMNS + col;
        if (index >= RewardData.REWARD_TYPE_COUNT) return -1;
        return index;
    }

    private enum CardState {
        AVAILABLE,
        MAXED,
        CANNOT_AFFORD,
        UNLOCKED
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add core/src/main/java/com/rdefense/core/render/RewardRenderer.java
git commit -m "feat: 创建奖励卡片渲染器 RewardRenderer"
```

---

## Task 3: 升级 RewardScreen 界面

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/RewardScreen.java`

- [ ] **Step 1: 升级奖励商店界面**

```java
package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.render.RewardRenderer;

public class RewardScreen extends GameScreen {

    private RewardRenderer rewardRenderer;
    private int scrollOffset = 0;

    public RewardScreen(RoboDefenseGame game) {
        super(game);
        rewardRenderer = new RewardRenderer();
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    protected void update(float delta) {
        // ESC 返回主菜单
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        // 方向键滚动
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            scrollOffset = Math.max(0, scrollOffset - 1);
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            scrollOffset++;
        }

        // 鼠标点击
        if (Gdx.input.justTouched()) {
            handleClick(Gdx.input.getX(), Gdx.graphics.getHeight() - Gdx.input.getY());
        }
    }

    private void handleClick(int screenX, int screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        // 返回按钮
        if (screenX >= 16 && screenX <= 136 && screenY >= 16 && screenY <= 56) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        // 卡片点击
        int startX = (screenWidth - (4 * (150 + 10))) / 2;
        int startY = screenHeight - 150 + scrollOffset * (130 + 10);
        int cardIndex = rewardRenderer.getCardAt(screenX, screenY, startX, startY);
        
        if (cardIndex >= 0 && RewardData.canAfford(cardIndex)) {
            if (RewardData.tryUpgrade(cardIndex)) {
                // 升级成功
            }
        }
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        // 背景
        renderer.drawRect(0, 0, renderer.getScreenWidth(), renderer.getScreenHeight(), 
            0.02f, 0.02f, 0.06f, 1.0f);

        // 标题
        renderer.drawText("奖励商店", 24, renderer.getScreenHeight() - 24, 1.0f, 0.9f, 0.7f, 1.0f);

        // 积分显示
        long points = RewardData.getRewardPoints();
        renderer.drawText("积分: " + formatPoints(points), 
            renderer.getScreenWidth() - 200, renderer.getScreenHeight() - 24, 0.8f, 0.8f, 0.8f, 1.0f);

        // 绘制奖励网格
        rewardRenderer.drawRewardGrid(renderer, scrollOffset, points);

        // 返回按钮
        renderer.drawRect(16, 16, 120, 40, 0.2f, 0.2f, 0.5f, 0.9f);
        renderer.drawText("返回", 40, 36, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }

    private String formatPoints(long points) {
        if (points >= 1000000) return (points / 1000000) + "M";
        if (points >= 1000) return (points / 1000) + "K";
        return String.valueOf(points);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `./gradlew :core:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 提交代码**

```bash
git add core/src/main/java/com/rdefense/core/scene/RewardScreen.java
git commit -m "feat: 升级奖励商店界面 RewardScreen"
```

---

## Task 4: 集成到 GameState

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`

- [ ] **Step 1: 在游戏初始化时应用奖励效果**

在 `GameState.initGame()` 方法中添加：

```java
// 应用奖励效果
int startingHealth = RewardData.applyReward(20, RewardData.HEALTH_UPGRADE);
this.health = startingHealth;

int startingCash = RewardData.applyReward(100, RewardData.STARTING_CASH_UPGRADE);
this.money = startingCash;
```

- [ ] **Step 2: 在游戏胜利时添加积分**

在游戏胜利逻辑中添加：

```java
// 根据难度计算积分奖励
int difficulty = getDifficultyLevel();
long rewardPoints = difficulty * 1000L;
RewardData.addRewardPoints(rewardPoints);
```

- [ ] **Step 3: 验证编译**

Run: `./gradlew :core:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: 提交代码**

```bash
git add core/src/main/java/com/rdefense/core/game/GameState.java
git commit -m "feat: 集成奖励效果到 GameState"
```

---

## Task 5: 添加主菜单入口

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/MainMenuScreen.java`

- [ ] **Step 1: 添加奖励商店入口按钮**

在主菜单按钮列表中添加：

```java
private static final String[] MENU_ITEMS = {
    "开始游戏",
    "奖励商店",
    "成就",
    "设置",
    "退出"
};
```

- [ ] **Step 2: 处理奖励商店按钮点击**

```java
case 1: // 奖励商店
    switchScreen(new RewardScreen(game));
    break;
```

- [ ] **Step 3: 显示当前积分**

在主菜单界面显示积分：

```java
renderer.drawText("积分: " + RewardData.getRewardPoints(), x + 100, y + 50, 0.8f, 0.8f, 0.8f, 1.0f);
```

- [ ] **Step 4: 验证编译**

Run: `./gradlew :core:compileJava --no-daemon`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: 提交代码**

```bash
git add core/src/main/java/com/rdefense/core/scene/MainMenuScreen.java
git commit -m "feat: 添加奖励商店入口到主菜单"
```

---

## 验证测试

1. **编译测试**
   ```bash
   ./gradlew :core:compileJava :desktop:dist
   ```

2. **运行测试**
   ```bash
   java -jar desktop/build/libs/desktop.jar
   ```

3. **功能验证**
   - 主菜单显示"奖励商店"按钮
   - 点击进入奖励商店界面
   - 显示23种奖励卡片
   - 升级按钮可点击（积分足够时）
   - 升级后积分扣除
   - ESC 返回主菜单
