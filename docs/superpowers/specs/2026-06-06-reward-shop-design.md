# 奖励商店系统设计文档

> 创建日期: 2026-06-06
> 状态: 待用户批准

## 概述

桌面版奖励商店系统，移植自安卓原版逻辑，采用现代化卡片网格布局。

## 需求确认

| 问题 | 选择 |
|------|------|
| 奖励类型 | 完全移植原版（23种奖励） |
| 界面布局 | 卡片网格布局（现代化） |
| 积分获取 | 完全遵循原版（通关获得积分） |
| 购买机制 | 两者结合（强化升级 + 解锁一次性） |

## 系统架构

### 文件结构

```
core/src/main/java/com/rdefense/core/
├── game/
│   └── RewardData.java              # 奖励数据模型（核心）
├── render/
│   └── RewardRenderer.java          # 奖励卡片渲染器
├── scene/
│   └── RewardScreen.java            # 奖励商店界面
└── RoboDefenseGame.java             # 游戏主类（初始化）
```

### 类设计

#### 1. RewardData（核心数据模型）

| 字段/方法 | 类型 | 描述 |
|-----------|------|------|
| `REWARD_TYPE_COUNT` | 常量 | 奖励类型总数（23） |
| `init(GameSaveManager)` | 方法 | 初始化奖励系统 |
| `getRewardPoints()` | 方法 | 获取当前积分 |
| `addRewardPoints(long)` | 方法 | 增加积分 |
| `getName(int type)` | 方法 | 获取奖励名称 |
| `getDescription(int type)` | 方法 | 获取奖励描述 |
| `getLevel(int type)` | 方法 | 获取当前等级 |
| `getCost(int type)` | 方法 | 获取升级费用 |
| `tryUpgrade(int type)` | 方法 | 尝试升级 |
| `isUnlockable(int type)` | 方法 | 是否为解锁类型 |
| `isMaxLevel(int type)` | 方法 | 是否已满级 |
| `isBlocked(int type)` | 方法 | 是否被锁定 |
| `applyReward(int val, int type)` | 方法 | 应用奖励效果 |

#### 2. RewardRenderer（卡片渲染器）

| 方法 | 描述 |
|------|------|
| `drawRewardCard(x, y, width, height, rewardType, state)` | 绘制单个奖励卡片 |
| `drawGrid(rewards, scrollOffset)` | 绘制奖励网格 |

#### 3. RewardScreen（商店界面）

| 方法 | 描述 |
|------|------|
| `init()` | 初始化界面 |
| `update(delta)` | 更新输入和动画 |
| `draw(delta)` | 绘制界面 |
| `handleClick(x, y)` | 处理点击事件 |

## 奖励类型详解

### 23种奖励定义

| ID | 名称 | 类型 | 效果 |
|----|------|------|------|
| 0 | Stronger Bullets | 强化 | +100 伤害 |
| 1 | Stronger Explosives | 百分比 | +10% 伤害 |
| 2 | Faster Rocket Reload | 百分比 | -10% 冷却时间 |
| 3 | Faster Antiair Reload | 百分比 | -10% 冷却时间 |
| 4 | Faster Artillery Reload | 百分比 | -10% 冷却时间 |
| 5 | Longer Flame Burn | 百分比 | +10% 持续时间 |
| 6 | Longer Slowdown | 百分比 | +10% 持续时间 |
| 7 | Health Reward | 加成 | +100 初始生命 |
| 8 | Starting Cash Reward | 加成 | +500 初始金钱 |
| 9 | Unlock Teleport Tower | 解锁 | 解锁传送塔 |
| 10 | Unlock Mine Tower | 解锁 | 解锁地雷塔 |
| 11 | Uranium Shells | 解锁 | 解锁铀弹 |
| 12 | Napalm Shells | 解锁 | 解锁燃烧弹 |
| 13 | Air Sniper | 解锁 | 解锁狙击塔 |
| 14 | Cheap Fireworks | 解锁 | 解锁烟花塔 |
| 15 | Slow Burn | 解锁 | 解锁冰冻塔 |
| 16 | Flea Market | 解锁 | 解锁跳蚤市场 |
| 17 | Scrambler | 解锁 | 解锁干扰塔 |
| 18 | Cannonball | 解锁 | 解锁炮弹塔 |
| 19 | Bonus! | 解锁 | 解锁奖励模式 |
| 20 | Air Burst | 解锁 | 解锁空爆塔 |
| 21 | Shockwave | 解锁 | 解锁冲击波塔 |
| 22 | FLARE Tower | 解锁 | 解锁闪光塔 |

### 奖励效果类型

| 类型 | ID | 效果描述 |
|------|-----|----------|
| 加成 | 0 | 基础值 + amount |
| 百分比伤害 | 1 | val * (100 + amount) / 100 |
| 百分比速度 | 2 | val * 100 / (100 + amount) |
| 百分比效果 | 3 | val * (100 + amount) / 100 |
| 解锁 | 4 | 一次性解锁 |

## 界面设计

### 布局结构

```
┌────────────────────────────────────────────────────┐
│  奖励商店                              积分: XXX   │
├────────────────────────────────────────────────────┤
│                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ 强化子弹  │  │ 强化爆炸  │  │ 火箭射速  │       │
│  │ Lv.3     │  │ Lv.5     │  │ 锁定      │       │
│  │ 50,000   │  │ 75,000   │  │ 需先解锁   │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│                                                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐       │
│  │ 防空射速  │  │ 火炮射速  │  │ 火焰延长  │       │
│  │ Lv.2     │  │ Lv.1     │  │ Lv.4     │       │
│  │ 20,000   │  │ 30,000   │  │ 15,000   │       │
│  └──────────┘  └──────────┘  └──────────┘       │
│                                                    │
│  ... (可滚动) ...                                 │
│                                                    │
├────────────────────────────────────────────────────┤
│                      [返回主菜单]                  │
└────────────────────────────────────────────────────┘
```

### 卡片尺寸

| 参数 | 值 |
|------|-----|
| 卡片宽度 | 150px |
| 卡片高度 | 120px |
| 卡片间距 | 10px |
| 网格列数 | 根据屏幕宽度自适应 |
| 卡片圆角 | 8px |

### 卡片内容

```
┌────────────────┐
│   [图标区域]    │  ← 奖励类型图标
│                │
│   奖励名称      │  ← 加粗显示
│   Lv.X / 满级  │  ← 当前等级
│                │
│   升级费用     │  ← 按钮区域
│   [升级/解锁]  │
└────────────────┘
```

### 卡片状态

| 状态 | 背景色 | 按钮文字 | 按钮状态 |
|------|--------|----------|----------|
| 可升级 | 蓝色调 | "升级" | 可点击 |
| 已满级 | 灰色调 | "已满级" | 禁用 |
| 被锁定 | 深灰色 | "锁定" | 禁用 |
| 积分不足 | 原色调 | "积分不足" | 禁用 |

## 数据库设计

### 存储结构

| 键 | 值类型 | 描述 |
|----|--------|------|
| `reward_points` | long | 当前积分 |
| `Reward:Stronger Bullets` | int | 子弹升级等级 |
| `Reward:Stronger Explosives` | int | 爆炸升级等级 |
| ... | ... | ... |

### 存档同步

- 积分变更时自动保存
- 升级后立即持久化
- 加载时从数据库恢复

## 游戏集成

### GameState 集成点

```java
// 游戏初始化时应用奖励效果
public void initGame(int levelType, int difficulty) {
    // 应用奖励效果
    int startingHealth = RewardData.applyAward(BASE_HEALTH, RewardData.HEALTH_UPGRADE);
    int startingCash = RewardData.applyAward(BASE_CASH, RewardData.STARTING_CASH_REWARD);
    
    // 应用强化效果到塔数据
    TowerData.setDamageMultiplier(RewardData.rewardFactor(RewardData.EXPLOSIVES_UPGRADE));
}
```

### 积分获取

```java
// 游戏胜利时调用
public void onGameWon(int difficulty) {
    // 根据难度计算积分奖励
    long rewardPoints = calculateRewardPoints(difficulty);
    RewardData.addRewardPoints(rewardPoints);
}
```

## 实现任务

| 任务 | 描述 | 文件 |
|------|------|------|
| 1 | 创建 RewardData 数据模型 | `game/RewardData.java` |
| 2 | 创建 RewardRenderer 渲染器 | `render/RewardRenderer.java` |
| 3 | 升级 RewardScreen 界面 | `scene/RewardScreen.java` |
| 4 | 集成到 GameState | `game/GameState.java` |
| 5 | 添加积分显示到主界面 | `scene/MainMenuScreen.java` |

## 验收标准

1. ✅ 显示23种奖励的卡片网格
2. ✅ 正确显示当前等级和升级费用
3. ✅ 升级按钮正确扣除积分
4. ✅ 解锁类奖励只需购买一次
5. ✅ 游戏开始时正确应用奖励效果
6. ✅ 积分和等级正确保存到数据库
7. ✅ 支持键盘和鼠标交互
