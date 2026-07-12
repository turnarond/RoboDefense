# 成就系统完整重构实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重构桌面版成就系统，完全复刻原版 Android 版的 88 种成就，包括中文名称、详细描述、分类标签和进度显示。

**Architecture:** 在 AchievementData 中添加成就分类枚举和中文名称/描述字段，更新 AchievementScreen 添加分类筛选标签栏，更新 AchievementRenderer 显示中文名称。

**Tech Stack:** Java, libGDX, SQLite

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `AchievementData.java` | 成就数据模型、分类枚举、88种成就定义 |
| `AchievementScreen.java` | 成就界面、分类筛选、列表显示 |
| `AchievementRenderer.java` | 成就达成通知动画 |

---

### Task 1: 重构 AchievementData 数据模型

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/game/AchievementData.java`

- [ ] **Step 1: 添加成就分类枚举**

在 AchievementData 类顶部添加分类枚举：

```java
public enum AchievementCategory {
    DIFFICULTY("难度成就", 0),
    MAP("地图成就", 1),
    PERFECT("完美成就", 2),
    SCORE("积分成就", 3),
    SPECIAL("特殊成就", 4),
    SURVIVAL("生存成就", 5),
    VR("训练成就", 6);

    public final String label;
    public final int iconIndex;

    AchievementCategory(String label, int iconIndex) {
        this.label = label;
        this.iconIndex = iconIndex;
    }
}
```

- [ ] **Step 2: 增强 AchievementProp 类**

修改 AchievementProp 内部类，添加 nameZh、description、category 字段：

```java
private static class AchievementProp {
    String key;
    String name;
    String nameZh;        // 中文名称
    String description;   // 成就描述
    AchievementCategory category;  // 成就分类
    private String cached_description;
    private int level;
    private int saved_level;
    private int trigger_level;

    public AchievementProp(String key, String nameZh, String description, 
                           AchievementCategory category, int trigger_level) {
        this.key = key;
        this.name = key;
        this.nameZh = nameZh;
        this.description = description;
        this.category = category;
        this.trigger_level = trigger_level;
        this.level = 0;
        
        if (saveManager != null && key.length() > 0) {
            String saved = saveManager.getAchievement(key);
            if (saved != null) {
                try {
                    this.level = Integer.parseInt(saved);
                } catch (NumberFormatException e) {
                    this.level = 0;
                }
            }
        }
        this.saved_level = this.level;
        if (this.level >= trigger_level) {
            total_count++;
        }
    }
    
    // ... 其他方法保持不变
}
```

- [ ] **Step 3: 添加静态方法获取中文名称和分类**

```java
public static String getNameZh(int type) {
    if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
        return "未知";
    }
    return achievement_props[type].nameZh;
}

public static AchievementCategory getCategory(int type) {
    if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
        return AchievementCategory.SPECIAL;
    }
    return achievement_props[type].category;
}

public static String getDescription(int type) {
    if (type < 0 || type >= ACHIEVEMENT_TYPE_COUNT) {
        return "";
    }
    AchievementProp prop = achievement_props[type];
    if (prop.description == null || prop.description.isEmpty()) {
        return prop.getDescription();  // 返回进度
    }
    return prop.description;
}
```

- [ ] **Step 4: 更新 88 种成就初始化**

修改 init() 方法中的成就定义，添加中文名称、描述和分类：

```java
// 难度成就
achievement_props[COMPLETE_DIFFICULTY_0] = new AchievementProp(
    "Complete Any Map", "通关任意地图", "通关任意地图", 
    AchievementCategory.DIFFICULTY, 1);
achievement_props[COMPLETE_DIFFICULTY_2] = new AchievementProp(
    "Complete Difficulty 2", "通关难度 2", "通关一个地图难度等级达到 2 或更大", 
    AchievementCategory.DIFFICULTY, 1);
// ... 继续添加其他成就

// 完美成就
achievement_props[PERFECT_25] = new AchievementProp(
    "Perfect 25", "完美通关 25 次", "在一个地图前25关不损失生命值", 
    AchievementCategory.PERFECT, 1);
achievement_props[PERFECT_50] = new AchievementProp(
    "Perfect 50", "完美通关 50 次", "在一个地图前50关不损失生命值", 
    AchievementCategory.PERFECT, 1);
achievement_props[PERFECT_100] = new AchievementProp(
    "Perfect 100", "完美通关 100 次", "通关一个地图不损失生命值", 
    AchievementCategory.PERFECT, 1);

// 积分成就
achievement_props[TWENTY_FIVE_K] = new AchievementProp(
    "25K Club", "25K 俱乐部", "击败2.5万敌军", 
    AchievementCategory.SCORE, 25000);
achievement_props[BIG_SCORE] = new AchievementProp(
    "Big Score", "大积分", "获得 1,000,000 生命值积分", 
    AchievementCategory.SCORE, 1000000);
achievement_props[HUGE_SCORE] = new AchievementProp(
    "Huge Score", "历史高分", "获得 50,000,000 生命值积分", 
    AchievementCategory.SCORE, 50000000);
achievement_props[EXPERIENCED] = new AchievementProp(
    "Experienced", "老手", "通关 1,000 关", 
    AchievementCategory.SCORE, 1000);
achievement_props[ADDICT] = new AchievementProp(
    "Addict", "痴迷者", "通关 5,000 关", 
    AchievementCategory.SCORE, 5000);
achievement_props[SEVENTY_FIVE_K] = new AchievementProp(
    "75K Club", "75K 俱乐部", "击败7.5万敌军", 
    AchievementCategory.SCORE, 75000);

// 特殊成就
achievement_props[FAST_PACED] = new AchievementProp(
    "Fast-Paced", "快节奏", "通关一个地图不使用减速塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[ROCKETMAN] = new AchievementProp(
    "Rocketman", "火箭人", "通关一个地图不使用枪塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[GUNNER] = new AchievementProp(
    "Gunner", "枪手", "通关一个地图不使用火箭塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[CHEAPSKATE] = new AchievementProp(
    "Cheapskate", "吝啬鬼", "通关一个地图不升级任何防御塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[BIG_SPENDER] = new AchievementProp(
    "Big Spender", "大富豪", "在通关一个地图后所有的防御塔都升级到最大等级", 
    AchievementCategory.SPECIAL, 1);
achievement_props[POWERED_UP] = new AchievementProp(
    "Powered up", "动力升级", "在通关一个地图后剩余12个或者更少的防御塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[NO_SALE] = new AchievementProp(
    "No Sale", "非卖品", "通关一个地图不出售防御塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[FAST_LANE] = new AchievementProp(
    "Fast Lane", "快车道", "在一场游戏中使用快进功能通关至少75关", 
    AchievementCategory.SPECIAL, 1);
achievement_props[RISK_TAKER] = new AchievementProp(
    "Risk Taker", "冒险者", "在通关一个地图后剩余1点生命值", 
    AchievementCategory.SPECIAL, 1);
achievement_props[SOLID_EFFORT] = new AchievementProp(
    "Solid Effort", "坚实的努力", "在通关一个地图后剩余至少18生命值", 
    AchievementCategory.SPECIAL, 1);
achievementProps[RAINY_DAY] = new AchievementProp(
    "Rainy Day", "下雨天", "在一场游戏中获得 $1,000", 
    AchievementCategory.SPECIAL, 1);
achievement_props[BIG_SAVER] = new AchievementProp(
    "Big Saver", "大储蓄", "在一场游戏中获得 $2,500", 
    AchievementCategory.SPECIAL, 1);
achievement_props[CRAZY_SAVER] = new AchievementProp(
    "Crazy Saver", "疯狂的储蓄者", "在一场游戏中获得 $5,000", 
    AchievementCategory.SPECIAL, 1);
achievement_props[BIG_ONE] = new AchievementProp(
    "Big One", "大收获", "从一个敌人那获得 500+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[WHOPPER] = new AchievementProp(
    "Whopper", "特大之物", "从一个敌人那获得 1,000+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[GOOD_GAME] = new AchievementProp(
    "Good Game", "干得不错", "在一场游戏中获得 250,000+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[GREAT_GAME] = new AchievementProp(
    "Great Game", "干得真棒", "在一场游戏中获得 500,000+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[AMAZING_GAME] = new AchievementProp(
    "Amazing Game", "太棒了", "在一场游戏中获得 1,000,000+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[THIRTY_THIRTY] = new AchievementProp(
    "30/30", "30/30", "通关一场游戏时等级 >= 30 生命值 >= 30", 
    AchievementCategory.SPECIAL, 1);
achievement_props[DEFEAT_TITAN] = new AchievementProp(
    "Titan Defeated", "泰坦被打败", "找到并干掉泰坦", 
    AchievementCategory.SPECIAL, 1);
achievement_props[CHAIN_SMOKER] = new AchievementProp(
    "Chain Smoker", "大烟枪", "引爆 10 个连锁爆炸", 
    AchievementCategory.SPECIAL, 1);
achievement_props[PYRO] = new AchievementProp(
    "Pyro", "喷火兵", "通关一场比赛使用25个或更多火焰塔", 
    AchievementCategory.SPECIAL, 1);
achievement_props[SUPER_POWERED] = new AchievementProp(
    "Super Powered", "超级动力", "用12个或更少的防御塔完美通关地图", 
    AchievementCategory.SPECIAL, 1);
achievement_props[ULTRA_GAME] = new AchievementProp(
    "Ultra Game", "最强的比赛", "在一场游戏中获得 5,000,000+ 积分", 
    AchievementCategory.SPECIAL, 1);
achievement_props[SHOW_OFF] = new AchievementProp(
    "Show Off", "炫耀", "用一个防御塔通关地图", 
    AchievementCategory.SPECIAL, 1);

// 地图成就
achievement_props[BASIC_10] = new AchievementProp(
    "Basic 10", "基地×10", "通关基地地图 10 次", 
    AchievementCategory.MAP, 10);
achievement_props[BASIC_25] = new AchievementProp(
    "Basic 25", "基地×25", "通关基地地图 25 次", 
    AchievementCategory.MAP, 25);
achievement_props[BASIC_LVL_40] = new AchievementProp(
    "Basic level 40", "基地×难度等级 40", "在难度 40通关基地地图", 
    AchievementCategory.MAP, 1);
achievement_props[BASIC_LVL_60] = new AchievementProp(
    "Basic level 60", "基地×难度等级 60", "在难度 60通关基地地图", 
    AchievementCategory.MAP, 1);
achievement_props[BASIC_LVL_80] = new AchievementProp(
    "Basic level 80", "基地×难度等级 80", "在难度 80通关基地地图", 
    AchievementCategory.MAP, 1);
achievement_props[BASIC_LVL_100] = new AchievementProp(
    "Basic level 100", "基地×难度等级 100", "在难度 100通关基地地图", 
    AchievementCategory.MAP, 1);

achievement_props[RUINS_10] = new AchievementProp(
    "Ruins 10", "遗迹×10", "通关遗迹地图 10 次", 
    AchievementCategory.MAP, 10);
achievement_props[RUINS_25] = new AchievementProp(
    "Ruins 25", "遗迹×25", "通关遗迹地图 25 次", 
    AchievementCategory.MAP, 25);
achievement_props[RUINS_LVL_30] = new AchievementProp(
    "The Ruins level 30", "遗迹×难度等级 30", "在难度等级 30通关遗迹地图", 
    AchievementCategory.MAP, 1);
achievement_props[RUINS_LVL_50] = new AchievementProp(
    "The Ruins level 50", "遗迹×难度等级 50", "在难度等级 50通关遗迹地图", 
    AchievementCategory.MAP, 1);
achievement_props[RUINS_LVL_70] = new AchievementProp(
    "The Ruins level 70", "遗迹×难度等级 70", "在难度等级 70通关遗迹地图", 
    AchievementCategory.MAP, 1);
achievement_props[RUINS_LVL_100] = new AchievementProp(
    "The Ruins level 100", "遗迹×难度等级 100", "在难度等级 100通关遗迹地图", 
    AchievementCategory.MAP, 1);

achievement_props[FACTORY_10] = new AchievementProp(
    "Factory 10", "工厂×10", "通关工厂地图 10 次", 
    AchievementCategory.MAP, 10);
achievement_props[FACTORY_25] = new AchievementProp(
    "Factory 25", "工厂×25", "通关工厂地图 25 次", 
    AchievementCategory.MAP, 25);
achievement_props[FACTORY_LVL_20] = new AchievementProp(
    "The Factory level 20", "工厂×难度等级 20", "在难度等级 20通关工厂地图", 
    AchievementCategory.MAP, 1);
achievement_props[FACTORY_LVL_40] = new AchievementProp(
    "The Factory level 40", "工厂×难度等级 40", "在难度等级 40通关工厂地图", 
    AchievementCategory.MAP, 1);
achievement_props[FACTORY_LVL_70] = new AchievementProp(
    "The Factory level 70", "工厂×难度等级 70", "在难度等级 70通关工厂地图", 
    AchievementCategory.MAP, 1);
achievement_props[FACTORY_LVL_100] = new AchievementProp(
    "The Factory level 100", "工厂×难度等级 100", "在难度等级 100通关工厂地图", 
    AchievementCategory.MAP, 1);

achievement_props[COURTYARD_10] = new AchievementProp(
    "Courtyard 10", "庭院×10", "通关庭院地图 10 次", 
    AchievementCategory.MAP, 10);
achievement_props[COURTYARD_25] = new AchievementProp(
    "Courtyard 25", "庭院×25", "通关庭院地图 25 次", 
    AchievementCategory.MAP, 25);
achievement_props[COURTYARD_LVL_15] = new AchievementProp(
    "The Courtyard level 15", "庭院×难度等级 15", "在难度等级 15通关庭院地图", 
    AchievementCategory.MAP, 1);
achievement_props[COURTYARD_LVL_25] = new AchievementProp(
    "The Courtyard level 25", "庭院×难度等级 25", "在难度等级 25通关庭院地图", 
    AchievementCategory.MAP, 1);
achievement_props[COURTYARD_LVL_50] = new AchievementProp(
    "The Courtyard level 50", "庭院×难度等级 50", "在难度等级 50通关庭院地图", 
    AchievementCategory.MAP, 1);
achievement_props[COURTYARD_LVL_100] = new AchievementProp(
    "The Courtyard level 100", "庭院×难度等级 100", "在难度等级 100通关庭院地图", 
    AchievementCategory.MAP, 1);

achievement_props[ROADWAY_LVL_10] = new AchievementProp(
    "Roadway level 10", "巷道×难度等级 10", "在难度等级 10通关巷道地图", 
    AchievementCategory.MAP, 1);
achievement_props[ROADWAY_LVL_50] = new AchievementProp(
    "Roadway level 50", "巷道×难度等级 50", "在难度等级 50通关巷道地图", 
    AchievementCategory.MAP, 1);
achievement_props[ROADWAY_LVL_100] = new AchievementProp(
    "Roadway level 100", "巷道×难度等级 100", "在难度等级 100通关巷道地图", 
    AchievementCategory.MAP, 1);

// 生存成就
achievement_props[SURVIVAL_10] = new AchievementProp(
    "Cub scout", "幼童军", "在生存模式中通关 10 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_20] = new AchievementProp(
    "Boy scout", "童子军", "在生存模式中通关 20 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_30] = new AchievementProp(
    "Eagle scout", "鹰级童军", "在生存模式中通关 30 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_40] = new AchievementProp(
    "Bear Grylls", "贝尔·格里尔斯", "在生存模式中通关 40 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_50] = new AchievementProp(
    "Les Stroud", "莱斯史特劳", "在生存模式中通关 50 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_60] = new AchievementProp(
    "Scott O'Grady", "斯科特·O·格雷迪", "在生存模式中通关 60 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_70] = new AchievementProp(
    "Aron Ralston", "艾伦·洛斯顿", "在生存模式中通关 70 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_80] = new AchievementProp(
    "Uruguayan Air Force Flight 571", "乌拉圭空军571号", "在生存模式中通关 80 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_90] = new AchievementProp(
    "Cornelius Rost", "极地重生", "在生存模式中通关 90 关", 
    AchievementCategory.SURVIVAL, 1);
achievement_props[SURVIVAL_100] = new AchievementProp(
    "What Robot Apocalypse?", "机器人启示录", "在生存模式中通关 100 关 (解锁新的游戏模式!)", 
    AchievementCategory.SURVIVAL, 1);

// 训练成就
achievement_props[MIXER_A] = new AchievementProp(
    "VR-A", "VR-A", "通关 5 A级训练任务", 
    AchievementCategory.VR, 5);
achievement_props[MIXER_B] = new AchievementProp(
    "VR-B", "VR-B", "通关 5 B级训练任务", 
    AchievementCategory.VR, 5);
achievement_props[MIXER_C] = new AchievementProp(
    "VR-C", "VR-C", "通关 5 C级训练任务", 
    AchievementCategory.VR, 5);
achievement_props[MIXER_D] = new AchievementProp(
    "VR-D", "VR-D", "通关 5 D级训练任务", 
    AchievementCategory.VR, 5);
achievement_props[MIXER_E] = new AchievementProp(
    "VR-E", "VR-E", "通关 5 E级训练任务", 
    AchievementCategory.VR, 5);
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 2: 更新 AchievementScreen 添加分类筛选

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/scene/AchievementScreen.java`

- [ ] **Step 1: 添加分类筛选状态**

在 AchievementScreen 中添加分类筛选字段：

```java
private AchievementData.AchievementCategory selectedCategory = null;
```

- [ ] **Step 2: 修改 updateFilteredList 方法**

```java
private void updateFilteredList() {
    filteredAchievements = new ArrayList<>();
    String lowerSearch = searchText.toLowerCase();

    for (int i = 0; i < AchievementData.ACHIEVEMENT_TYPE_COUNT; i++) {
        String name = AchievementData.getNameZh(i);
        
        if (!lowerSearch.isEmpty() && !name.toLowerCase().contains(lowerSearch)) {
            continue;
        }

        if (selectedCategory != null && AchievementData.getCategory(i) != selectedCategory) {
            continue;
        }

        boolean achieved = AchievementData.isAchieved(i);
        switch (filterMode) {
            case ALL:
                filteredAchievements.add(i);
                break;
            case EARNED:
                if (achieved) filteredAchievements.add(i);
                break;
            case PENDING:
                if (!achieved) filteredAchievements.add(i);
                break;
        }
    }
}
```

- [ ] **Step 3: 添加分类筛选标签栏绘制**

在 draw 方法中添加分类标签栏：

```java
private void drawCategoryTabs(GameRenderer renderer, int screenWidth, int screenHeight) {
    int tabY = screenHeight - 130;
    int tabWidth = 70;
    int tabHeight = 32;
    int startX = 16;

    // "全部" 标签
    boolean allSelected = selectedCategory == null;
    renderer.drawRect(startX, tabY, tabWidth, tabHeight, 
        allSelected ? 0.3f : 0.1f, allSelected ? 0.4f : 0.1f, allSelected ? 0.6f : 0.2f, 
        allSelected ? 0.9f : 0.6f);
    renderer.drawText("全部", startX + 18, tabY + tabHeight - 10, 
        allSelected ? 1.0f : 0.7f, allSelected ? 1.0f : 0.7f, allSelected ? 1.0f : 0.7f, 1.0f);
    startX += tabWidth + 5;

    // 各分类标签
    for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
        boolean selected = selectedCategory == cat;
        renderer.drawRect(startX, tabY, tabWidth, tabHeight, 
            selected ? 0.3f : 0.1f, selected ? 0.4f : 0.1f, selected ? 0.6f : 0.2f, 
            selected ? 0.9f : 0.6f);
        String label = cat.label.replace("成就", "");
        renderer.drawText(label, startX + 8, tabY + tabHeight - 10, 
            selected ? 1.0f : 0.7f, selected ? 1.0f : 0.7f, selected ? 1.0f : 0.7f, 1.0f);
        startX += tabWidth + 5;
    }
}
```

- [ ] **Step 4: 修改成就列表显示中文名称**

在 drawAchievementList 方法中使用 getNameZh：

```java
String name = AchievementData.getNameZh(achievementId);
String desc = AchievementData.getDescription(achievementId);
```

- [ ] **Step 5: 添加分类标签点击处理**

在 handleMouseInput 中添加分类标签点击检测：

```java
int tabY = screenHeight - 130;
int tabWidth = 70;
int tabHeight = 32;
int tabStartX = 16;

if (y >= tabY && y <= tabY + tabHeight) {
    // "全部" 标签
    if (x >= tabStartX && x <= tabStartX + tabWidth) {
        selectedCategory = null;
        updateFilteredList();
        scrollOffset = 0;
        return;
    }
    tabStartX += tabWidth + 5;

    // 各分类标签
    for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
        if (x >= tabStartX && x <= tabStartX + tabWidth) {
            selectedCategory = cat;
            updateFilteredList();
            scrollOffset = 0;
            return;
        }
        tabStartX += tabWidth + 5;
    }
}
```

- [ ] **Step 6: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 3: 更新 AchievementRenderer 显示中文名称

**Files:**
- Modify: `core/src/main/java/com/rdefense/core/render/AchievementRenderer.java`

- [ ] **Step 1: 修改 drawMain 方法使用中文名称**

```java
private void drawMain(GameRenderer renderer, int left, int top, int right, int bottom, int alpha) {
    float bgAlpha = (alpha * 208) / 255f;
    renderer.drawRect(left, top, right - left, bottom - top, 0, 0, 0, bgAlpha / 255f);

    float textAlpha = alpha / 255f;

    renderer.drawText("成就达成！", left + TITLE_X_PAD, top + TITLE_Y_PAD, 1.0f, 0.9f, 0.2f, textAlpha);

    String achievementName = AchievementData.getNameZh(achievementType);
    renderer.drawText(achievementName, left + NAME_X_PAD, top + NAME_Y_PAD, 1.0f, 1.0f, 1.0f, textAlpha);
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :core:compileJava`
Expected: BUILD SUCCESSFUL

---

### Task 4: 构建和运行桌面版验证

**Files:**
- None (验证任务)

- [ ] **Step 1: 构建桌面版**

Run: `./gradlew :desktop:dist`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: 启动游戏验证成就系统**

Run: `java -jar desktop/build/libs/desktop.jar`

验证项目：
1. 进入成就界面，确认显示 88 种成就
2. 确认成就显示中文名称和描述
3. 点击分类标签，确认筛选功能正常
4. 滚动列表，确认所有成就可查看

---

## 实现总结

| 任务 | 状态 |
|------|------|
| Task 1: 重构 AchievementData | 待实现 |
| Task 2: 更新 AchievementScreen | 待实现 |
| Task 3: 更新 AchievementRenderer | 待实现 |
| Task 4: 构建验证 | 待实现 |
