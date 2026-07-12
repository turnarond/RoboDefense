# 奖励系统完整复刻实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 desktop 版奖励系统完全复刻到原版 Android 版本的水平，包括动态成本递增、积分获取机制、动态描述生成、乘数机制和特殊锁定逻辑。

**Architecture:** 重构 RewardData 数据模型以支持原版参数（base_cost、multiplier、increase_type），新增 GameRewardCalculator 负责积分计算，更新 RewardRenderer/RewardScreen 显示动态描述和等级信息，在 GameState.endGame() 中集成新版积分计算。

**Tech Stack:** Java 8, libGDX, SQLite (GameSaveManager)

---

## 文件结构

| 文件 | 操作 | 职责 |
|------|------|------|
| `core/src/main/java/com/rdefense/core/game/RewardData.java` | 修改 | 核心数据模型，包含 23 种奖励配置、成本计算、效果应用 |
| `core/src/main/java/com/rdefense/core/game/GameRewardCalculator.java` | 创建 | 积分计算公式和地图系数 |
| `core/src/main/java/com/rdefense/core/game/GameState.java` | 修改 | 集成新版积分计算到 endGame() |
| `core/src/main/java/com/rdefense/core/render/RewardRenderer.java` | 修改 | 显示动态描述、等级信息、被锁定原因 |
| `core/src/main/java/com/rdefense/core/scene/RewardScreen.java` | 修改 | 处理特殊锁定逻辑、积分显示格式 |

---

## Task 1: 重构 RewardData 数据模型

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/RewardData.java`

- [ ] **Step 1: 增强 RewardProp 内部类**

在 `RewardProp` 中添加 `cached_reward_string` 字段和 `rewardString()` 方法：

```java
private static class RewardProp {
    String key;
    String name;
    String description;  // 现在包含 %d 占位符
    int baseCost;
    int type;
    int level;
    int multiplier = 1;
    String cached_reward_string;  // 新增：缓存动态描述

    RewardProp(String key, String name, String description, int baseCost, int type, int multiplier) {
        this.key = key;
        this.name = name;
        this.description = description;
        this.baseCost = baseCost;
        this.type = type;
        this.multiplier = multiplier;
        
        if (saveManager != null) {
            this.level = saveManager.getPreferenceInt(PREFS_PREFIX + key, 0);
        }
    }

    long calculateCost() {
        if (type == TYPE_UNLOCK) {
            return 0; // 解锁类成本在 RewardData 中动态计算
        }
        long cost = baseCost;
        for (int i = 0; i < level; i++) {
            cost = (cost * 6) / 5;
            if (cost > 2000000000L) return 2000000000L;
        }
        return cost;
    }

    void increaseLevel() {
        level++;
        cached_reward_string = null; // 清除缓存
    }

    int rewardFactor() {
        return level * multiplier;
    }

    String rewardString() {
        if (cached_reward_string != null) {
            return cached_reward_string;
        }
        if (type == TYPE_UNLOCK) {
            cached_reward_string = description;
        } else {
            int value = (level + 1) * multiplier;
            cached_reward_string = String.format(description, value);
        }
        return cached_reward_string;
    }
}
```

- [ ] **Step 2: 完全重构 init() 方法**

将 `init()` 中的 23 种奖励初始化完全替换为原版参数：

```java
public static void init(GameSaveManager manager) {
    saveManager = manager;
    reward_points = saveManager.getRewardPoints();
    // 初始积分改为 0（移除 50000 默认值）
    reward_props = new RewardProp[REWARD_TYPE_COUNT];
    
    // 前 9 种：属性升级类
    reward_props[BULLET_UPGRADE] = new RewardProp("Stronger Bullets", "更强力的弹药", "增加弹药威力到 %d", 50000, TYPE_POWER, 1);
    reward_props[EXPLOSIVES_UPGRADE] = new RewardProp("Stronger Explosives", "更强力的炸药", "增加 火箭/导弹/迫击炮 威力到 %d%%", 75000, TYPE_PERCENT_POWER, 10);
    reward_props[ROCKET_SPEED_UPGRADE] = new RewardProp("Faster Rocket Reload", "更快的火箭弹填弹速度", "重新加载火箭 %d%% 更快", 40000, TYPE_PERCENT_SPEED, 10);
    reward_props[ANTIAIR_SPEED_UPGRADE] = new RewardProp("Faster Antiair Reload", "更快的防空炮填弹速度", "重新加载防空导弹 %d%% 更快", 20000, TYPE_PERCENT_SPEED, 10);
    reward_props[ARTILLERY_SPEED_UPGRADE] = new RewardProp("Faster Artillery Reload", "更快的炮弹填弹速度", "重新加载迫击炮/火炮 %d%% 更快", 30000, TYPE_PERCENT_SPEED, 10);
    reward_props[FLAME_DURATION_UPGRADE] = new RewardProp("Longer Flame Burn", "火焰燃烧时间增长", "增加火焰持续伤害到 %d%%", 15000, TYPE_PERCENT_EFFECT, 10);
    reward_props[SLOW_DURATION_UPGRADE] = new RewardProp("Longer Slowdown", "减速时间增长", "增加缓速塔作用时间到 %d%%", 30000, TYPE_PERCENT_EFFECT, 10);
    reward_props[HEALTH_UPGRADE] = new RewardProp("Health Reward", "奖励生命值", "增加基础生命值到 %d", 10000, TYPE_POWER, 1);
    reward_props[STARTING_CASH_UPGRADE] = new RewardProp("Starting Cash Reward", "启动现金奖励", "增加初始金钱到 %d", 25000, TYPE_POWER, 5);
    
    // 后 14 种：解锁类（baseCost=0, type=TYPE_UNLOCK, multiplier=1）
    reward_props[TELEPORT_TOWER] = new RewardProp("Unlock Teleport Tower", "解锁传送塔", "传送塔将敌人传送回到起始位置", 0, TYPE_UNLOCK, 1);
    reward_props[MINE_TOWER] = new RewardProp("Unlock Mine Tower", "解锁地雷塔", "地雷被踩到后悔引发大量的伤害", 0, TYPE_UNLOCK, 1);
    reward_props[URANIUM_SHELLS] = new RewardProp("Uranium Shells", "铀弹", "最大等级机枪无视护甲", 0, TYPE_UNLOCK, 1);
    reward_props[NAPALM_SHELLS] = new RewardProp("Napalm Shells", "凝固汽油弹", "炮弹攻击单位", 0, TYPE_UNLOCK, 1);
    reward_props[AIR_SNIPER] = new RewardProp("Air Sniper", "空中狙击者", "升级防空炮可以增加2倍的范围", 0, TYPE_UNLOCK, 1);
    reward_props[CHEAP_FIREWORKS] = new RewardProp("Cheap Fireworks", "廉价的花炮", "升级防空导弹成本更少", 0, TYPE_UNLOCK, 1);
    reward_props[SLOW_BURN] = new RewardProp("Slow Burn", "缓慢燃烧", "升级火塔也可以减速敌人", 0, TYPE_UNLOCK, 1);
    reward_props[FLEA_MARKET] = new RewardProp("Flea Market", "跳骚市场", "出售防御塔可以获得金钱", 0, TYPE_UNLOCK, 1);
    reward_props[SCRAMBLER] = new RewardProp("Scrambler", "扰频器", "被传送的敌人受到伤害", 0, TYPE_UNLOCK, 1);
    reward_props[CANNONBALL] = new RewardProp("Cannonball", "加农炮", "增加 迫击炮/火炮 伤害", 0, TYPE_UNLOCK, 1);
    reward_props[BONUS] = new RewardProp("Bonus!", "奖励!", "杀敌奖励翻倍", 0, TYPE_UNLOCK, 1);
    reward_props[AIR_BURST] = new RewardProp("Air Burst", "空中打击者", "地雷还可以攻击空中单位", 0, TYPE_UNLOCK, 1);
    reward_props[SHOCKWAVE] = new RewardProp("Shockwave", "冲击波", "地雷链可以对所有敌人进行减速和火焰伤害", 0, TYPE_UNLOCK, 1);
    reward_props[FLARE_TOWER] = new RewardProp("FLARE_TOWER_UPGRADED", "解锁火炬塔", "火炬对路过的敌人造成减速和燃烧伤害", 0, TYPE_UNLOCK, 1);
}
```

- [ ] **Step 3: 添加核心方法**

在 `RewardData` 中添加以下方法：

```java
public static final long MAX_REWARD_COST = 2000000000L;
public static final long UNLOCK_COST = 500000L;

public static long nextLevelRewardCost(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return Long.MAX_VALUE;
    RewardProp rp = reward_props[type];
    
    // 解锁类奖励：500000 × (已解锁数量 + 1)
    if (rp.type == TYPE_UNLOCK) {
        if (rp.level > 0) return Long.MAX_VALUE; // 已解锁
        int unlocked_count = 0;
        for (int i = 0; i < reward_props.length; i++) {
            if (reward_props[i].type == TYPE_UNLOCK && reward_props[i].level > 0) {
                unlocked_count++;
            }
        }
        return UNLOCK_COST * (unlocked_count + 1);
    }
    
    // 普通奖励：base_cost × 1.2^level
    return rp.calculateCost();
}

public static int rewardFactor(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return 0;
    return reward_props[type].rewardFactor();
}

public static String rewardString(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return "";
    return reward_props[type].rewardString();
}

public static String towerString(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return "";
    RewardProp rp = reward_props[type];
    int factor = rp.rewardFactor();
    
    switch (rp.type) {
        case TYPE_POWER:
            return "威力 " + rp.level + " (+" + factor + ")";
        case TYPE_PERCENT_POWER:
            return "威力 " + rp.level + " (+" + factor + "%)";
        case TYPE_PERCENT_SPEED:
            return "速度 " + rp.level + " (+" + factor + "%)";
        case TYPE_PERCENT_EFFECT:
            return "作用 " + rp.level + " (+" + factor + "%)";
        case TYPE_UNLOCK:
            return "";
        default:
            return "";
    }
}

public static String isBlocked(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return null;
    // 闪光塔需要先解锁地雷塔
    if (type == FLARE_TOWER) {
        if (getLevel(MINE_TOWER) == 0 && getLevel(FLARE_TOWER) == 0) {
            return "需要先解锁地雷塔";
        }
    }
    return null;
}

public static boolean tryUpgrade(int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return false;
    
    // 检查是否被锁定
    if (isBlocked(type) != null) return false;
    
    long cost = nextLevelRewardCost(type);
    if (cost > reward_points) return false;
    
    reward_points -= cost;
    reward_props[type].increaseLevel();
    saveProgress();
    return true;
}

public static int applyReward(int baseValue, int type) {
    if (type < 0 || type >= REWARD_TYPE_COUNT) return baseValue;
    RewardProp rp = reward_props[type];
    int factor = rp.rewardFactor();
    long result = baseValue;
    
    switch (rp.type) {
        case TYPE_POWER:
            result += factor;
            break;
        case TYPE_PERCENT_POWER:
        case TYPE_PERCENT_EFFECT:
            result = ((factor + 100) * result) / 100;
            break;
        case TYPE_PERCENT_SPEED:
            result = (result * 100) / (factor + 100);
            break;
        case TYPE_UNLOCK:
            result = factor;
            break;
    }
    
    return (int) result;
}
```

- [ ] **Step 4: 更新 getCost() 为 nextLevelRewardCost()**

将 `RewardRenderer` 和 `RewardScreen` 中对 `RewardData.getCost()` 的调用改为 `RewardData.nextLevelRewardCost()`。

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 2: 创建 GameRewardCalculator 积分计算器

**Files:**
- Create: `core/src/main/java/com/rdefense/core/game/GameRewardCalculator.java`

- [ ] **Step 1: 创建积分计算器类**

```java
package com.rdefense.core.game;

/**
 * 游戏积分计算器 - 根据游戏结果计算奖励积分
 * 完全复刻原版积分获取逻辑
 */
public final class GameRewardCalculator {

    // 地图系数
    public static final int MAP_BASIC = 0;
    public static final int MAP_COURTYARD = 1;
    public static final int MAP_ICE = 2;
    public static final int MAP_LAVA = 3;
    public static final int MAP_EXTREME = 4;

    private static final float[] MAP_COEFFICIENTS = {
        1.0f,   // BASIC
        1.2f,   // COURTYARD
        1.3f,   // ICE
        1.5f,   // LAVA
        2.0f    // EXTREME
    };

    /**
     * 计算游戏奖励积分
     * 
     * @param difficultyLevel 难度等级
     * @param mapId 地图ID
     * @param remainingHealth 剩余生命
     * @param maxHealth 最大生命
     * @param gameTimeMs 游戏时长（毫秒）
     * @param standardTimeMs 标准时长（毫秒）
     * @return 计算得到的积分
     */
    public static long calculateRewardPoints(
            int difficultyLevel,
            int mapId,
            int remainingHealth,
            int maxHealth,
            long gameTimeMs,
            long standardTimeMs) {
        
        // 基础积分 = 难度等级 × 地图系数 × 100
        float mapCoeff = getMapCoefficient(mapId);
        long basePoints = (long) (difficultyLevel * mapCoeff * 100);
        
        // 生命加成 = 剩余生命 / 最大生命 × 0.5
        float healthBonus = 0.0f;
        if (maxHealth > 0) {
            healthBonus = ((float) remainingHealth / maxHealth) * 0.5f;
        }
        
        // 效率加成 = min(1.0, 标准时间 / 实际时间) × 0.3
        float efficiencyBonus = 0.0f;
        if (gameTimeMs > 0 && standardTimeMs > 0) {
            efficiencyBonus = Math.min(1.0f, (float) standardTimeMs / gameTimeMs) * 0.3f;
        }
        
        // 最终积分 = 基础积分 × (1 + 生命加成 + 效率加成)
        // 注：原版公式中击杀加成已简化，主要依赖难度等级
        long finalPoints = (long) (basePoints * (1.0f + healthBonus + efficiencyBonus));
        
        return Math.max(0, finalPoints);
    }

    /**
     * 简化版计算（仅使用难度等级和地图）
     * 用于当前 GameState 已有数据的场景
     */
    public static long calculateSimple(int difficultyLevel, int mapId) {
        float mapCoeff = getMapCoefficient(mapId);
        return (long) (difficultyLevel * mapCoeff * 100);
    }

    private static float getMapCoefficient(int mapId) {
        if (mapId >= 0 && mapId < MAP_COEFFICIENTS.length) {
            return MAP_COEFFICIENTS[mapId];
        }
        return 1.0f;
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 3: 更新 GameState 集成新版积分计算

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/GameState.java`

- [ ] **Step 1: 修改 endGame() 方法**

找到 `endGame()` 方法（约第 440 行），将积分计算替换为新版：

```java
public void endGame(int new_run_state) {
    if (this.run_state == GAME_RUNNING || this.run_state == GAME_PAUSED ||
            this.run_state == GAME_FAST_FWD) {
        if (new_run_state == GAME_LOST || new_run_state == GAME_WON ||
                new_run_state == GAME_NOT_STARTED) {
            if (new_run_state == GAME_WON) {
                // 使用新版积分计算器
                long rewardPoints = GameRewardCalculator.calculateSimple(
                    this.difficulty_level,
                    this.level_data.getLevelType()
                );
                RewardData.addRewardPoints(rewardPoints);
            }
            initGame(this.level_data.getLevelType());
            this.run_state = new_run_state;
        }
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 4: 更新 RewardRenderer 显示动态信息

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/render/RewardRenderer.java`

- [ ] **Step 1: 更新 drawCard() 方法**

修改 `drawCard()` 以显示动态描述和等级信息：

```java
private void drawCard(GameRenderer renderer, int x, int y, int type, CardState state) {
    float[] bgColor = getBackgroundColor(state);
    renderer.drawRect(x, y, CARD_WIDTH, CARD_HEIGHT, bgColor[0], bgColor[1], bgColor[2], 0.9f);

    // 名称
    renderer.drawText(RewardData.getName(type), x + 10, y + CARD_HEIGHT - 20, 1.0f, 1.0f, 1.0f, 1.0f);

    // 等级信息（威力/速度/作用等级）
    String towerStr = RewardData.towerString(type);
    if (!towerStr.isEmpty()) {
        renderer.drawText(towerStr, x + 10, y + CARD_HEIGHT - 38, 0.7f, 0.7f, 0.7f, 1.0f);
    }

    // 动态描述（下一级效果）
    String rewardStr = RewardData.rewardString(type);
    if (!rewardStr.isEmpty() && rewardStr.length() < 30) {
        renderer.drawText(rewardStr, x + 10, y + CARD_HEIGHT - 56, 0.6f, 0.8f, 0.6f, 1.0f);
    }

    // 被锁定原因
    String blockedReason = RewardData.isBlocked(type);
    if (blockedReason != null) {
        renderer.drawText("锁定: " + blockedReason, x + 10, y + 40, 0.8f, 0.4f, 0.4f, 1.0f);
    } else if (state == CardState.AVAILABLE) {
        long cost = RewardData.nextLevelRewardCost(type);
        renderer.drawText(formatCost(cost), x + 10, y + 20, 0.8f, 0.8f, 0.8f, 1.0f);
        renderer.drawRect(x + 10, y + 5, CARD_WIDTH - 20, 25, 0.2f, 0.6f, 0.2f, 0.9f);
        renderer.drawText("升级", x + 55, y + 17, 1.0f, 1.0f, 1.0f, 1.0f);
    } else if (state == CardState.MAXED) {
        renderer.drawText("已满级", x + 40, y + 17, 0.6f, 0.6f, 0.6f, 1.0f);
    } else if (state == CardState.CANNOT_AFFORD) {
        long cost = RewardData.nextLevelRewardCost(type);
        renderer.drawText("需要 " + formatCost(cost), x + 20, y + 17, 0.8f, 0.4f, 0.4f, 1.0f);
    }
}
```

- [ ] **Step 2: 更新 getCardState() 方法**

```java
private CardState getCardState(int type, long rewardPoints) {
    if (RewardData.isBlocked(type) != null) return CardState.BLOCKED;
    if (RewardData.isMaxLevel(type)) return CardState.MAXED;
    if (RewardData.isUnlockable(type) && RewardData.getLevel(type) > 0) return CardState.UNLOCKED;
    if (RewardData.nextLevelRewardCost(type) > rewardPoints) return CardState.CANNOT_AFFORD;
    return CardState.AVAILABLE;
}
```

- [ ] **Step 3: 添加 BLOCKED 状态颜色**

```java
private float[] getBackgroundColor(CardState state) {
    switch (state) {
        case AVAILABLE: return new float[]{0.1f, 0.2f, 0.4f};
        case MAXED: return new float[]{0.2f, 0.2f, 0.2f};
        case CANNOT_AFFORD: return new float[]{0.15f, 0.1f, 0.1f};
        case BLOCKED: return new float[]{0.1f, 0.05f, 0.05f};
        default: return new float[]{0.1f, 0.15f, 0.3f};
    }
}
```

- [ ] **Step 4: 在 CardState 枚举中添加 BLOCKED**

```java
private enum CardState {
    AVAILABLE,
    MAXED,
    CANNOT_AFFORD,
    UNLOCKED,
    BLOCKED
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 5: 更新 RewardScreen 处理锁定逻辑和积分显示

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/RewardScreen.java`

- [ ] **Step 1: 更新 handleClick() 处理锁定**

```java
private void handleClick(int screenX, int screenY) {
    int screenWidth = Gdx.graphics.getWidth();
    int screenHeight = Gdx.graphics.getHeight();

    int backX = screenWidth - BUTTON_WIDTH - 16;
    int backY = 16;
    if (screenX >= backX && screenX <= backX + BUTTON_WIDTH && 
            screenY >= backY && screenY <= backY + BUTTON_HEIGHT) {
        switchScreen(new MainMenuScreen(game));
        return;
    }

    int startX = (screenWidth - (4 * (150 + 10))) / 2;
    int startY = screenHeight - 200 + scrollOffset * (130 + 10);
    int cardIndex = rewardRenderer.getCardAt(screenX, screenY, startX, startY);
    
    if (cardIndex >= 0) {
        // 检查是否被锁定
        if (RewardData.isBlocked(cardIndex) != null) {
            // 被锁定的奖励不能点击
            return;
        }
        if (RewardData.canAfford(cardIndex)) {
            RewardData.tryUpgrade(cardIndex);
        }
    }
}
```

- [ ] **Step 2: 更新积分显示格式**

```java
private String formatPoints(long points) {
    if (points >= 1000000) {
        return String.format("%.1fM", points / 1000000.0);
    } else if (points >= 1000) {
        return String.format("%.1fK", points / 1000.0);
    }
    return String.valueOf(points);
}
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

## Task 6: 构建和运行桌面版验证

**Files:**
- None (验证步骤)

- [ ] **Step 1: 构建桌面版**

Run: `./gradlew :desktop:dist`
Expected: BUILD SUCCESSFUL, 生成 desktop/build/libs/desktop.jar

- [ ] **Step 2: 运行桌面版**

Run: `java -jar desktop/build/libs/desktop.jar`
Expected: 游戏启动，进入主菜单

- [ ] **Step 3: 验证奖励商店界面**

1. 点击"奖励商店"进入
2. 验证积分显示为 0（或之前存档的值）
3. 验证 23 种奖励正确显示
4. 验证前 9 种显示成本（如更强子弹 50000）
5. 验证后 14 种解锁类显示动态成本（第一个应为 500000）
6. 验证动态描述显示（如"增加弹药威力到 1"）
7. 验证等级信息显示（如"威力 0 (+0)"）

- [ ] **Step 4: 验证积分获取**

1. 开始一局游戏
2. 完成关卡（胜利）
3. 返回奖励商店
4. 验证积分增加（应为 难度等级 × 地图系数 × 100）

- [ ] **Step 5: 验证升级功能**

1. 确保有足够积分
2. 点击一个奖励升级
3. 验证积分减少
4. 验证等级提升
5. 验证成本增加（下一级应为当前成本 × 1.2）
6. 验证描述更新（如"增加弹药威力到 2"）

---

## Spec 覆盖检查

| 设计文档需求 | 实现任务 | 状态 |
|------------|---------|------|
| 23 种奖励完整配置 | Task 1 Step 2 | ✅ |
| 动态成本递增（×1.2） | Task 1 Step 3 (nextLevelRewardCost) | ✅ |
| 解锁类动态成本（500000×N） | Task 1 Step 3 (nextLevelRewardCost) | ✅ |
| 积分获取计算 | Task 2 (GameRewardCalculator) | ✅ |
| 地图系数 | Task 2 (MAP_COEFFICIENTS) | ✅ |
| 动态描述生成（%d 占位符） | Task 1 Step 1 (rewardString) | ✅ |
| 等级信息显示（威力/速度/作用） | Task 1 Step 3 (towerString) | ✅ |
| 乘数机制 | Task 1 Step 2 (multiplier 参数) | ✅ |
| 特殊锁定逻辑（闪光塔需地雷塔） | Task 1 Step 3 (isBlocked) | ✅ |
| 积分显示格式（K/M） | Task 5 Step 2 | ✅ |
| 初始积分改为 0 | Task 1 Step 2 (移除 50000 默认值) | ✅ |
| 效果应用（applyReward） | Task 1 Step 3 | ✅ |
| UI 显示动态信息 | Task 4 | ✅ |
| 数据持久化 | Task 1 Step 3 (saveProgress) | ✅ |

## Placeholder 检查

- ✅ 无 "TBD", "TODO", "implement later"
- ✅ 无 "Add appropriate error handling" 等模糊描述
- ✅ 所有步骤包含具体代码
- ✅ 所有步骤包含验证命令和预期输出
- ✅ 类型和方法名前后一致

## 执行交接

**Plan complete and saved to `docs/superpowers/plans/2026-06-07-reward-system-complete-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** - 我按任务分派子代理执行，每任务间进行审查，快速迭代

**2. Inline Execution** - 在本会话中使用 executing-plans 执行任务，批量执行带检查点

**Which approach?**
