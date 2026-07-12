# 奖励系统完整复刻设计文档

## 概述

本文档描述如何将 desktop 版的奖励系统完全复刻到原版 Android 版本的水平，包括动态成本递增、积分获取机制、动态描述生成等核心功能。

## 目标

1. 完全复刻原版奖励系统的 23 种奖励及其详细配置
2. 实现动态成本递增机制（每次升级成本 × 1.2）
3. 实现基于游戏结果的积分获取计算
4. 实现动态数值描述（如"增加弹药威力到 150"）
5. 实现乘数机制（不同奖励有不同乘数）
6. 实现特殊锁定逻辑（闪光塔需要先解锁地雷塔）

## 架构设计

### 核心类结构

```
RewardData (核心数据管理)
├── RewardProp (奖励属性)
├── nextLevelRewardCost() (动态成本计算)
├── applyAward() (效果应用)
├── rewardString() (动态描述)
└── towerString() (等级信息)

GameRewardCalculator (积分计算)
├── calculateRewardPoints() (主计算方法)
├── getMapCoefficient() (地图系数)
└── calculateBonus() (加成计算)

GameState (游戏状态集成)
└── onLevelComplete() (积分获取触发)
```

## 详细设计

### 1. 数据模型重构

#### 1.1 RewardProp 类增强

```java
public class RewardProp {
    // 基础字段
    public String key;              // 奖励键值（英文）
    public String name;             // 奖励名称（中文）
    public String description;      // 描述模板（支持 %d 占位符）
    public int base_cost;           // 基础成本
    public int reward_level;        // 当前等级
    
    // 新增字段
    public int multiplier = 1;      // 乘数（1、5、10）
    public int increase_type = 0;   // 类型（0=POWER, 1=PERCENT_POWER, 2=PERCENT_SPEED, 3=PERCENT_TIME, 4=UNLOCK）
    public String cached_reward_string; // 缓存的动态描述
}
```

#### 1.2 奖励类型常量

```java
public static final int TYPE_POWER = 0;           // 直接加成（如 +100 伤害）
public static final int TYPE_PERCENT_POWER = 1;   // 百分比伤害加成（如 +10% 伤害）
public static final int TYPE_PERCENT_SPEED = 2;   // 百分比速度加成（如 -10% 冷却）
public static final int TYPE_PERCENT_TIME = 3;    // 百分比时间加成（如 +10% 持续）
public static final int TYPE_UNLOCK = 4;          // 解锁类（如解锁传送塔）
```

#### 1.3 23 种奖励完整配置

| ID | 英文键值 | 中文名称 | 基础成本 | 乘数 | 类型 | 描述模板 |
|----|---------|---------|---------|------|------|---------|
| 0 | Stronger Bullets | 更强子弹 | 50000 | 1 | POWER | 增加弹药威力到 %d |
| 1 | Stronger Explosives | 更强爆炸物 | 75000 | 10 | PERCENT_POWER | 增加 火箭/导弹/迫击炮 威力到 %d%% |
| 2 | Faster Rocket Reload | 火箭射速 | 40000 | 10 | PERCENT_SPEED | 重新加载火箭 %d%% 更快 |
| 3 | Faster Antiair Reload | 防空射速 | 20000 | 10 | PERCENT_SPEED | 重新加载防空导弹 %d%% 更快 |
| 4 | Faster Artillery Reload | 火炮射速 | 30000 | 10 | PERCENT_SPEED | 重新加载迫击炮/火炮 %d%% 更快 |
| 5 | Longer Flame Burn | 火焰延长 | 15000 | 10 | PERCENT_TIME | 增加火焰持续伤害到 %d%% |
| 6 | Longer Slowdown | 减速延长 | 30000 | 10 | PERCENT_TIME | 增加缓速塔作用时间到 %d%% |
| 7 | Health Reward | 生命奖励 | 10000 | 1 | POWER | 增加基础生命值到 %d |
| 8 | Starting Cash Reward | 初始现金 | 25000 | 5 | POWER | 增加初始金钱到 %d |
| 9 | Unlock Teleport Tower | 传送塔 | 0 | 1 | UNLOCK | 传送塔将敌人传送回到起始位置 |
| 10 | Unlock Mine Tower | 地雷塔 | 0 | 1 | UNLOCK | 地雷被踩到后悔引发大量的伤害 |
| 11 | Uranium Shells | 铀弹 | 0 | 1 | UNLOCK | 最大等级机枪无视护甲 |
| 12 | Napalm Shells | 燃烧弹 | 0 | 1 | UNLOCK | 炮弹攻击单位 |
| 13 | Air Sniper | 防空狙击 | 0 | 1 | UNLOCK | 升级防空炮可以增加2倍的范围 |
| 14 | Cheap Fireworks | 烟花塔 | 0 | 1 | UNLOCK | 升级防空导弹成本更少 |
| 15 | Slow Burn | 冰冻塔 | 0 | 1 | UNLOCK | 升级火塔也可以减速敌人 |
| 16 | Flea Market | 跳蚤市场 | 0 | 1 | UNLOCK | 出售防御塔可以获得金钱 |
| 17 | Scrambler | 干扰塔 | 0 | 1 | UNLOCK | 被传送的敌人受到伤害 |
| 18 | Cannonball | 炮弹塔 | 0 | 1 | UNLOCK | 增加 迫击炮/火炮 伤害 |
| 19 | Bonus! | 奖励模式 | 0 | 1 | UNLOCK | 杀敌奖励翻倍 |
| 20 | Air Burst | 空爆塔 | 0 | 1 | UNLOCK | 地雷还可以攻击空中单位 |
| 21 | Shockwave | 冲击波 | 0 | 1 | UNLOCK | 地雷链可以对所有敌人进行减速和火焰伤害 |
| 22 | FLARE Tower | 闪光塔 | 0 | 1 | UNLOCK | 火炬对路过的敌人造成减速和燃烧伤害 |

### 2. 动态成本计算

#### 2.1 普通奖励成本递增

```java
public long nextLevelRewardCost(int type) {
    RewardProp rp = reward_props[type];
    
    // 解锁类奖励使用特殊计算
    if (rp.increase_type == TYPE_UNLOCK) {
        return calculateUnlockCost();
    }
    
    // 普通奖励：base_cost × 1.2^level
    long cost = rp.base_cost;
    for (int i = 0; i < rp.reward_level; i++) {
        cost = (6 * cost) / 5; // 等同于 cost × 1.2
        if (cost > MAX_REWARD_COST) {
            return MAX_REWARD_COST; // MAX_REWARD_COST = 2000000000
        }
    }
    return cost;
}
```

**成本递增示例：**
- 更强子弹（base_cost=50000）：
  - 等级 0 → 1: 50000
  - 等级 1 → 2: 60000 (50000 × 1.2)
  - 等级 2 → 3: 72000 (60000 × 1.2)
  - 等级 3 → 4: 86400 (72000 × 1.2)

#### 2.2 解锁类奖励成本计算

```java
private long calculateUnlockCost() {
    int unlocked_count = 0;
    for (int i = 0; i < reward_props.length; i++) {
        if (reward_props[i].increase_type == TYPE_UNLOCK 
            && reward_props[i].reward_level > 0) {
            unlocked_count++;
        }
    }
    return UNLOCK_COST * (unlocked_count + 1); // UNLOCK_COST = 500000
}
```

**解锁成本示例：**
- 第 1 个解锁：500000 × 1 = 500000
- 第 2 个解锁：500000 × 2 = 1000000
- 第 3 个解锁：500000 × 3 = 1500000

### 3. 积分获取系统

#### 3.1 积分计算公式

```java
public static long calculateRewardPoints(
    int difficultyLevel,      // 难度等级
    int mapId,                // 地图ID
    int remainingHealth,      // 剩余生命
    int maxHealth,           // 最大生命
    int remainingCash,        // 剩余金钱
    int enemiesKilled,        // 击杀敌人数
    int expectedKills,       // 预期击杀数
    long gameTimeMs,         // 游戏时长（毫秒）
    long standardTimeMs      // 标准时长（毫秒）
) {
    // 基础积分 = 难度等级 × 地图系数 × 100
    float mapCoeff = getMapCoefficient(mapId);
    long basePoints = (long)(difficultyLevel * mapCoeff * 100);
    
    // 生命加成 = 剩余生命 / 最大生命 × 0.5
    float healthBonus = ((float)remainingHealth / maxHealth) * 0.5f;
    
    // 效率加成 = min(1.0, 标准时间 / 实际时间) × 0.3
    float efficiencyBonus = (float)Math.min(1.0, (double)standardTimeMs / gameTimeMs) * 0.3f;
    
    // 击杀加成 = min(1.0, 击杀数 / 预期击杀数) × 0.2
    float killBonus = (float)Math.min(1.0, (double)enemiesKilled / expectedKills) * 0.2f;
    
    // 最终积分 = 基础积分 × (1 + 生命加成 + 效率加成 + 击杀加成)
    long finalPoints = (long)(basePoints * (1.0f + healthBonus + efficiencyBonus + killBonus));
    
    return finalPoints;
}
```

#### 3.2 地图系数定义

```java
private static float getMapCoefficient(int mapId) {
    switch (mapId) {
        case MAP_BASIC:     return 1.0f;  // 基础地图
        case MAP_COURTYARD: return 1.2f;  // 庭院
        case MAP_ICE:       return 1.3f;  // 冰原
        case MAP_LAVA:      return 1.5f;  // 熔岩
        case MAP_EXTREME:   return 2.0f;  // 极限
        default:            return 1.0f;
    }
}
```

#### 3.3 积分获取示例

**示例 1：基础难度，完美通关**
- 难度等级：10
- 地图：基础（系数 1.0）
- 剩余生命：100/100
- 游戏时间：标准时间
- 击杀数：预期击杀数

计算：
- 基础积分 = 10 × 1.0 × 100 = 1000
- 生命加成 = 1.0 × 0.5 = 0.5
- 效率加成 = 1.0 × 0.3 = 0.3
- 击杀加成 = 1.0 × 0.2 = 0.2
- 最终积分 = 1000 × (1 + 0.5 + 0.3 + 0.2) = 1000 × 2.0 = 2000

**示例 2：高难度，一般通关**
- 难度等级：20
- 地图：庭院（系数 1.2）
- 剩余生命：50/100
- 游戏时间：1.5 倍标准时间
- 击杀数：0.8 倍预期击杀数

计算：
- 基础积分 = 20 × 1.2 × 100 = 2400
- 生命加成 = 0.5 × 0.5 = 0.25
- 效率加成 = 0.67 × 0.3 = 0.2
- 击杀加成 = 0.8 × 0.2 = 0.16
- 最终积分 = 2400 × (1 + 0.25 + 0.2 + 0.16) = 2400 × 1.61 = 3864

### 4. 动态描述生成

#### 4.1 奖励描述生成

```java
public String rewardString(int type) {
    RewardProp rp = reward_props[type];
    
    // 使用缓存避免重复计算
    if (rp.cached_reward_string != null) {
        return rp.cached_reward_string;
    }
    
    if (rp.increase_type == TYPE_UNLOCK) {
        // 解锁类：直接返回描述
        rp.cached_reward_string = rp.description;
    } else {
        // 其他类：动态计算数值
        int value = (rp.reward_level + 1) * rp.multiplier;
        rp.cached_reward_string = String.format(rp.description, value);
    }
    
    return rp.cached_reward_string;
}
```

**描述生成示例：**
- 更强子弹（等级 2，乘数 1）：
  - 描述模板："增加弹药威力到 %d"
  - 计算值：(2 + 1) × 1 = 3
  - 结果："增加弹药威力到 3"

- 更强爆炸物（等级 2，乘数 10）：
  - 描述模板："增加 火箭/导弹/迫击炮 威力到 %d%%"
  - 计算值：(2 + 1) × 10 = 30
  - 结果："增加 火箭/导弹/迫击炮 威力到 30%"

#### 4.2 等级信息显示

```java
public String towerString(int type) {
    RewardProp rp = reward_props[type];
    int factor = rewardFactor(type); // reward_level × multiplier
    
    switch (rp.increase_type) {
        case TYPE_POWER:
            return "威力 " + rp.reward_level + " (+" + factor + ")";
        case TYPE_PERCENT_POWER:
            return "威力 " + rp.reward_level + " (+" + factor + "%)";
        case TYPE_PERCENT_SPEED:
            return "速度 " + rp.reward_level + " (+" + factor + "%)";
        case TYPE_PERCENT_TIME:
            return "作用 " + rp.reward_level + " (+" + factor + "%)";
        case TYPE_UNLOCK:
            return ""; // 解锁类不显示等级
        default:
            return "";
    }
}
```

**等级信息示例：**
- 更强子弹（等级 3，乘数 1）："威力 3 (+3)"
- 更强爆炸物（等级 2，乘数 10）："威力 2 (+20%)"
- 火箭射速（等级 1，乘数 10）："速度 1 (+10%)"
- 火焰延长（等级 4，乘数 10）："作用 4 (+40%)"

### 5. 奖励效果应用

```java
public int applyAward(int val, int type) {
    int factor = rewardFactor(type); // reward_level × multiplier
    long result = val;
    
    switch (reward_props[type].increase_type) {
        case TYPE_POWER:           // 直接加成
            result += factor;
            break;
        case TYPE_PERCENT_POWER:   // 百分比伤害加成
        case TYPE_PERCENT_TIME:    // 百分比时间加成
            result = ((factor + 100) * result) / 100;
            break;
        case TYPE_PERCENT_SPEED:   // 百分比速度加成（冷却时间减少）
            result = (result * 100) / (factor + 100);
            break;
        case TYPE_UNLOCK:          // 解锁类
            result = factor;
            break;
    }
    
    return (int) result;
}
```

**效果应用示例：**
- 更强子弹（等级 3，乘数 1）应用到基础伤害 100：
  - factor = 3 × 1 = 3
  - result = 100 + 3 = 103

- 更强爆炸物（等级 2，乘数 10）应用到基础伤害 100：
  - factor = 2 × 10 = 20
  - result = (20 + 100) × 100 / 100 = 120

- 火箭射速（等级 1，乘数 10）应用到冷却时间 1000ms：
  - factor = 1 × 10 = 10
  - result = 1000 × 100 / (10 + 100) = 909ms

### 6. 特殊锁定逻辑

```java
public String isBlocked(int type) {
    // 闪光塔（FLARE_TOWER）需要先解锁地雷塔
    if (type == FLARE_TOWER) {
        if (getLevel(MINE_TOWER) == 0 && getLevel(FLARE_TOWER) == 0) {
            return "需要先解锁地雷塔";
        }
    }
    return null; // 不被锁定
}
```

### 7. UI 显示设计

#### 7.1 奖励卡片信息结构

```java
public class RewardCardInfo {
    public String name;              // 奖励名称（中文）
    public String description;       // 动态描述
    public String levelInfo;         // 等级信息（如"威力 3 (+30%)"）
    public long cost;                // 当前升级成本
    public int level;                // 当前等级
    public boolean canAfford;        // 是否买得起
    public boolean isUnlock;         // 是否是解锁类
    public boolean isUnlocked;       // 是否已解锁
    public String blockedReason;     // 被锁定原因
}
```

#### 7.2 积分显示格式

```java
public String formatPoints(long points) {
    if (points >= 1000000) {
        return String.format("%.1fM", points / 1000000.0);
    } else if (points >= 1000) {
        return String.format("%.1fK", points / 1000.0);
    }
    return String.valueOf(points);
}
```

**显示示例：**
- 500 → "500"
- 1500 → "1.5K"
- 1500000 → "1.5M"

## 实现计划

### 阶段 1：数据模型重构（优先级：高）

**任务列表：**
1. 增强 RewardProp 类，添加 multiplier、increase_type、cached_reward_string 字段
2. 重构 RewardData.init()，完全复刻原版 23 种奖励的初始化
3. 实现 nextLevelRewardCost() 动态成本计算
4. 实现 applyAward() 奖励效果应用
5. 实现 rewardString() 动态描述生成
6. 实现 towerString() 等级信息显示
7. 实现 isBlocked() 特殊锁定逻辑
8. 修改初始积分从 50000 改为 0

**验证标准：**
- 所有 23 种奖励正确初始化
- 成本计算与原版一致
- 效果应用与原版一致
- 描述生成与原版一致

### 阶段 2：积分获取系统（优先级：高）

**任务列表：**
1. 创建 GameRewardCalculator 类
2. 实现 calculateRewardPoints() 主计算方法
3. 实现 getMapCoefficient() 地图系数获取
4. 在 GameState 中添加积分计算所需的数据收集
5. 在 GameState.onLevelComplete() 中集成积分计算
6. 实现积分获取提示 UI（HudRenderer 或新建 RewardEarnedRenderer）
7. 添加积分获取音效

**验证标准：**
- 积分计算公式正确
- 地图系数正确
- 积分在游戏胜利时正确添加
- 积分获取提示正确显示

### 阶段 3：UI 更新（优先级：中）

**任务列表：**
1. 更新 RewardRenderer 显示动态描述
2. 更新 RewardRenderer 显示等级信息
3. 更新 RewardScreen 处理特殊锁定逻辑
4. 更新 RewardScreen 显示被锁定原因
5. 优化积分显示格式（使用 formatPoints）
6. 调整卡片布局以适应动态描述长度

**验证标准：**
- 动态描述正确显示
- 等级信息正确显示
- 特殊锁定逻辑正确工作
- 积分显示格式正确

### 阶段 4：测试和调优（优先级：中）

**任务列表：**
1. 编写单元测试：成本计算
2. 编写单元测试：效果应用
3. 编写单元测试：描述生成
4. 编写单元测试：积分计算
5. 编写集成测试：升级流程
6. 编写集成测试：积分获取流程
7. 进行平衡性测试：验证与原版一致性
8. 性能优化：缓存机制验证

**验证标准：**
- 所有单元测试通过
- 所有集成测试通过
- 与原版行为一致
- 性能无明显下降

## 风险和注意事项

### 1. 数据迁移

**风险：** 现有存档使用旧的奖励系统格式

**解决方案：**
- 在 RewardData.init() 中检测旧格式并迁移
- 保持向后兼容，不丢失玩家已有进度
- 添加版本号标识

### 2. 平衡性

**风险：** 新系统可能导致游戏过难或过易

**解决方案：**
- 严格按照原版参数实现
- 进行充分的平衡性测试
- 准备调整参数的配置文件

### 3. 性能

**风险：** 动态计算可能导致性能下降

**解决方案：**
- 使用 cached_reward_string 缓存描述
- 避免在渲染循环中重复计算
- 只在升级时更新缓存

### 4. UI 布局

**风险：** 动态描述可能较长，超出卡片边界

**解决方案：**
- 测试最长描述的显示效果
- 必要时调整卡片尺寸
- 考虑文本换行或省略

### 5. 初始积分

**风险：** 初始积分为 0 可能导致新玩家无法立即体验奖励系统

**解决方案：**
- 在教程或首次游戏时给予初始积分
- 或在首次游戏胜利时给予额外积分奖励
- 确保第一场游戏能获得足够积分购买第一个奖励

## 成功标准

1. **功能完整性：** 所有 23 种奖励正确实现，与原版功能一致
2. **数值准确性：** 成本计算、效果应用、描述生成与原版完全一致
3. **积分获取：** 积分计算公式正确，玩家能通过游戏获得积分
4. **UI 显示：** 动态描述、等级信息、积分显示正确清晰
5. **游戏平衡：** 升级曲线合理，与原版体验一致
6. **性能优化：** 无明显性能下降，缓存机制有效
7. **数据兼容：** 现有存档能正确迁移，不丢失进度

## 参考资料

- 原版 RewardData.java：`app/src/main/java/com/magicwach/rdefense/RewardData.java`
- 原版字符串资源：`app/src/main/res/values/strings.xml`
- 当前 desktop 版 RewardData：`core/src/main/java/com/rdefense/core/game/RewardData.java`
- 当前 desktop 版 RewardRenderer：`core/src/main/java/com/rdefense/core/render/RewardRenderer.java`
- 当前 desktop 版 RewardScreen：`core/src/main/java/com/rdefense/core/scene/RewardScreen.java`
