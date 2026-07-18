package com.rdefense.core.game;

import com.rdefense.core.config.OptionsData;
import com.rdefense.core.game.GridObject.GameSaveReader;
import com.rdefense.core.game.GridObject.GameSaveWriter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 游戏状态类 — 管理游戏核心逻辑
 * 对应原版 GameState（简化版，核心功能）
 */
public final class GameState {

    // 游戏运行状态常量
    public static final int GAME_RUNNING = 0;
    public static final int GAME_LOST = 1;
    public static final int GAME_WON = 2;
    public static final int GAME_PAUSED = 3;
    public static final int GAME_FAST_FWD = 4;
    public static final int GAME_NOT_STARTED = 5;

    // 游戏状态
    private int state_index;
    private int score;
    private int level_bonus;
    private int run_state;
    private int money;
    private int health;
    private int starting_health;
    private int difficulty_level;
    private boolean survival_mode;

    // 游戏对象链表
    private Enemy enemy_list;
    private Enemy enemy_graveyard;
    private Enemy enemy_pool;
    private GameTower tower_list;
    private GameTower tower_pool;
    private Bullet bullet_list;
    private Bullet bullet_pool;
    private GameEvent[] event_list = new GameEvent[GameEvent.NUM_EVENT_TYPES];
    private GameEvent event_pool;

    // 网格系统
    private CollisionGrid collision_grid;
    private MovementGrid[] movement_grid;
    private final GridObjectOrder grid_order = new GridObjectOrder();

    // 关卡数据
    private LevelData level_data;
    private OptionsData options;

    // 统计
    private int enemies_allocated;
    private int events_allocated;
    private int towers_allocated;
    private int active_events;
    private boolean unit_created_this_level;
    private int tower_powup_counter;
    private boolean only_one_tower_created = true;
    private boolean no_gun_towers_created = true;
    private boolean no_slow_towers_created = true;
    private boolean no_rocket_towers_created = true;

    // 成就追踪标志
    private boolean cheapskate = true;     // 是否从未升级塔
    private boolean no_sale = true;        // 是否从未出售塔
    private int fast_fwd_counter = 0;      // 快进通关关卡计数
    private boolean auto_save_requested;    // 每 10 关自动存档请求（由 GamePlayScreen 消费）

    /**
     * 创建游戏状态
     */
    public GameState(int difficulty_level, int mixer_seed, boolean survival_mode) {
        this.level_data = new LevelData(mixer_seed);
        this.difficulty_level = difficulty_level;
        this.survival_mode = survival_mode;
    }

    public void setOptions(OptionsData options) {
        this.options = options;
    }

    /**
     * 初始化新游戏
     */
    public void initGame(int map_id, int difficulty) {
        this.state_index = 0;
        this.score = 0;
        this.level_bonus = 0;
        this.run_state = GAME_NOT_STARTED;
        this.enemy_list = null;
        this.enemy_graveyard = null;
        this.enemy_pool = null;
        this.enemies_allocated = 0;
        this.bullet_list = null;
        this.tower_list = null;
        this.unit_created_this_level = false;
        // 重置成就追踪标志
        this.cheapskate = true;
        this.no_sale = true;
        this.fast_fwd_counter = 0;
        this.only_one_tower_created = true;
        this.no_gun_towers_created = true;
        this.no_slow_towers_created = true;
        this.no_rocket_towers_created = true;
        this.tower_powup_counter = 0;
        this.towers_allocated = 0;
        this.event_pool = null;
        // 原版：event_list 残留事件保留并重新计数（配合 500 上限）
        this.events_allocated = 0;
        this.active_events = 0;
        for (int event_type = 0; event_type < GameEvent.NUM_EVENT_TYPES; event_type++) {
            for (GameEvent ev = this.event_list[event_type]; ev != null; ev = ev.next) {
                this.events_allocated++;
                this.active_events++;
            }
        }
        this.difficulty_level = difficulty;

        this.grid_order.clear();
        this.level_data.init(map_id, difficulty);
        this.collision_grid = new CollisionGrid(
                this.level_data.getGridWidth(),
                this.level_data.getGridHeight()
        );

        initMovementGrid();

        int startingCash = RewardData.applyReward(25, RewardData.STARTING_CASH_UPGRADE);
        setMoney(startingCash);
        this.starting_health = RewardData.applyReward(20, RewardData.HEALTH_UPGRADE);
        setHealth(this.starting_health);
        if (this.survival_mode) {
            this.difficulty_level = LevelDataGenerator.getSurvivalDifficultyLevel(
                    this.level_data.getLevelNum());
        }
    }

    public void initGame(int map_id) {
        initGame(map_id, 1);
    }

    /**
     * 更新游戏状态（核心循环，每帧调用）
     */
    public void nextState() {
        recycleGameEvents();
        // 原版 dequeueAchievements：将待显示成就转为事件
        dequeueAchievements();

        if (this.run_state == GAME_RUNNING || this.run_state == GAME_FAST_FWD) {
            this.state_index++;
            this.collision_grid.reset();

            // 更新敌人
            advanceActiveEnemiesState();

            // 更新塔
            advanceTowersState();

            // 更新子弹
            advanceBulletsState();

            // 更新关卡
            advanceLevelState();

            // 确保Y排序
            this.grid_order.ensureSorted();
        }
    }

    /**
     * 更新敌人状态
     */
    private void advanceActiveEnemiesState() {
        for (Enemy ge = this.enemy_list; ge != null; ge = ge.next) {
            if (ge.nextState(this, this.movement_grid[ge.getPathNum()], this.state_index)) {
                // 敌人到达出口
                if (this.run_state == GAME_FAST_FWD && this.options != null && this.options.optionValue(OptionsData.FAST_FORWARD_LIFE_PAUSE)) {
                    this.run_state = GAME_PAUSED;
                }
                setHealth(this.health - 1);
                if (this.health > 0) {
                    this.level_bonus = 0;
                    allocateGameEvent(GameEvent.EVENT_HEALTH_CHANGED);
                } else {
                    endGame(GAME_LOST);
                }
            } else if (ge.getHealth() > 0) {
                this.collision_grid.add(ge);
            }
        }

        // 清理死亡/离开敌人
        Enemy last_ge = null;
        Enemy ge2 = this.enemy_list;
        while (ge2 != null) {
            Enemy next_ge = ge2.next;
            if (ge2.atExit()) {
                freeGameEnemy(ge2, last_ge);
            } else if (ge2.getHealth() <= 0) {
                ge2.setDeathFrame(this.state_index);
                enemyDefeated(ge2);
                killGameEnemy(ge2, last_ge);
            } else {
                last_ge = ge2;
            }
            ge2 = next_ge;
        }

        // 清理墓地
        Enemy last_ge2 = null;
        Enemy ge3 = this.enemy_graveyard;
        while (ge3 != null) {
            Enemy next_ge2 = ge3.next;
            if (ge3.getDeathFrame() + EnemyData.deathFrames(ge3.getType()) <= this.state_index) {
                freeDeadGameEnemy(ge3, last_ge2);
            } else {
                last_ge2 = ge3;
            }
            ge3 = next_ge2;
        }
    }

    /**
     * 敌人被击败
     */
    private void enemyDefeated(Enemy ge) {
        int enemy_type = ge.getType();
        setMoney(this.money + EnemyData.value(enemy_type));

        // 播放敌人被击败音效
        com.rdefense.core.audio.SoundManager.getInstance().playEnemyDefeated();

        // 原版除数 500（C.EVENT_ALLOCATION_SANITY_LIMIT），且允许 0 分击杀
        int base_score_add = (ge.getMaxHealth() * this.level_data.getScoreMultiplier()) / 500;
        int kill_bonus = getEnemyKillBonus();
        GameEvent e = allocateGameEvent(GameEvent.EVENT_ENEMY_DEFEATED);
        e.var[GameEvent.VAR_ENEMY_STATE_IDX] = this.state_index;
        e.var[GameEvent.VAR_ENEMY_TYPE] = enemy_type;
        e.var[GameEvent.VAR_ENEMY_ORIENTATION] = ge.getOrientation();
        e.var[GameEvent.VAR_ENEMY_PIXEL_X] = ge.calcPixelX();
        e.var[GameEvent.VAR_ENEMY_PIXEL_Y] = ge.calcPixelY();
        e.var[GameEvent.VAR_ENEMY_BASE_SCORE] = base_score_add;
        e.var[GameEvent.VAR_ENEMY_FULL_SCORE] = base_score_add + kill_bonus;

        int score_add = base_score_add + kill_bonus;
        this.score += score_add;

        // 击杀得分成就
        if (score_add >= 500) {
            AchievementData.increaseLevel(AchievementData.BIG_ONE, 1);
            if (score_add >= 1000) {
                AchievementData.increaseLevel(AchievementData.WHOPPER, 1);
            }
        }
        // 总分里程碑
        if (this.score >= 250000) {
            AchievementData.increaseLevel(AchievementData.GOOD_GAME, 1);
            if (this.score >= 500000) {
                AchievementData.increaseLevel(AchievementData.GREAT_GAME, 1);
            }
            if (this.score >= 1000000) {
                AchievementData.increaseLevel(AchievementData.AMAZING_GAME, 1);
            }
            if (this.score >= 5000000) {
                AchievementData.increaseLevel(AchievementData.ULTRA_GAME, 1);
            }
        }
        // 击败泰坦
        if (enemy_type == 9) {
            AchievementData.increaseLevel(AchievementData.DEFEAT_TITAN, 1);
        }
        // 累计积分/击杀数
        AchievementData.increaseLevel(AchievementData.BIG_SCORE, score_add);
        AchievementData.increaseLevel(AchievementData.HUGE_SCORE, score_add);
        AchievementData.increaseLevel(AchievementData.TWENTY_FIVE_K, 1);
        AchievementData.increaseLevel(AchievementData.SEVENTY_FIVE_K, 1);
    }

    /**
     * 更新塔状态
     */
    private void advanceTowersState() {
        if (this.bullet_pool == null) {
            this.bullet_pool = new Bullet();
        }

        for (GameTower t = this.tower_list; t != null; t = t.next) {
            if (t.nextState(this, this.collision_grid, this.bullet_pool)) {
                Bullet b = this.bullet_pool.next;
                this.bullet_pool.next = this.bullet_list;
                this.bullet_list = this.bullet_pool;
                if (b == null) {
                    this.bullet_pool = new Bullet();
                } else {
                    this.bullet_pool = b;
                }
            }
        }
    }

    /**
     * 更新子弹状态
     */
    private void advanceBulletsState() {
        Bullet last_b = null;
        Bullet b = this.bullet_list;
        while (b != null) {
            Bullet next_b = b.next;
            if (b.nextState(this.state_index, this)) {
                // 子弹命中，回收到池
                if (last_b == null) {
                    this.bullet_list = b.next;
                } else {
                    last_b.next = b.next;
                }
                b.next = this.bullet_pool;
                this.bullet_pool = b;
            } else {
                last_b = b;
            }
            b = next_b;
        }
    }

    /**
     * 更新关卡状态
     */
    private void advanceLevelState() {
        this.level_data.nextState();

        if (this.level_data.getUnitType() >= 0) {
            deployNewUnit();
        } else if (this.level_data.levelHasEnded() && this.enemy_list == null) {
            nextLevel();
        }
    }

    /**
     * 部署新单位
     */
    private void deployNewUnit() {
        int unit_type = this.level_data.getUnitType();
        int path_num = this.level_data.getUnitPathNum();

        Enemy ge = allocateGameEnemy();
        ge.init(
                this.level_data.getStartX(path_num),
                this.level_data.getStartY(path_num),
                this.level_data.getStartOrientation(path_num),
                unit_type,
                this.state_index,
                EnemyData.baseHealth(unit_type, this.level_data.getLevelNum(), this.difficulty_level),
                path_num
        );
        this.unit_created_this_level = true;
    }

    /**
     * 下一关
     */
    private void nextLevel() {
        // 快进计数器
        if (this.run_state == GAME_FAST_FWD) {
            this.fast_fwd_counter++;
            if (this.fast_fwd_counter >= 75) {
                AchievementData.increaseLevel(AchievementData.FAST_LANE, 1);
            }
            if (this.options != null && this.options.optionValue(OptionsData.FAST_FORWARD_LEVEL_PAUSE)) {
                this.run_state = GAME_RUNNING;
            }
        }
        if (this.level_data.getLevelNum() > 1) {
            this.level_bonus++;
            if (this.level_bonus == 25) {
                AchievementData.increaseLevel(AchievementData.PERFECT_25, 1);
            } else if (this.level_bonus == 50) {
                AchievementData.increaseLevel(AchievementData.PERFECT_50, 1);
            }
        }
        // 生存模式里程碑
        if (this.survival_mode) {
            switch (this.level_data.getLevelNum()) {
                case 10:  AchievementData.increaseLevel(AchievementData.SURVIVAL_10, 1); break;
                case 20:  AchievementData.increaseLevel(AchievementData.SURVIVAL_20, 1); break;
                case 30:  AchievementData.increaseLevel(AchievementData.SURVIVAL_30, 1); break;
                case 40:  AchievementData.increaseLevel(AchievementData.SURVIVAL_40, 1); break;
                case 50:  AchievementData.increaseLevel(AchievementData.SURVIVAL_50, 1); break;
                case 60:  AchievementData.increaseLevel(AchievementData.SURVIVAL_60, 1); break;
                case 70:  AchievementData.increaseLevel(AchievementData.SURVIVAL_70, 1); break;
                case 80:  AchievementData.increaseLevel(AchievementData.SURVIVAL_80, 1); break;
                case 90:  AchievementData.increaseLevel(AchievementData.SURVIVAL_90, 1); break;
            }
        }

        if (!this.level_data.nextLevel()) {
            endGame(GAME_WON);
        } else {
            // 播放波次开始音效
            com.rdefense.core.audio.SoundManager.getInstance().playWaveStart();
            
            if (this.run_state == GAME_FAST_FWD && this.options != null && this.options.optionValue(OptionsData.FAST_FORWARD_LEVEL_PAUSE)) {
                this.run_state = GAME_RUNNING;
            }
            if (this.level_data.getLevelNum() % 10 == 0) {
                this.auto_save_requested = true; // GamePlayScreen 轮询后执行 quickSave
            }
        }

        if (this.survival_mode) {
            this.difficulty_level = LevelDataGenerator.getSurvivalDifficultyLevel(
                    this.level_data.getLevelNum());
        }

        AchievementData.increaseLevel(AchievementData.EXPERIENCED, 1);
        AchievementData.increaseLevel(AchievementData.ADDICT, 1);
        this.unit_created_this_level = false;
    }

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

    /**
     * 游戏胜利时触发所有相关成就检测
     */
    private void gameWonAchievements() {
        // 难度成就
        AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_0, 1);
        if (this.difficulty_level >= 2)  AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_2, 1);
        if (this.difficulty_level >= 5)  AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_5, 1);
        if (this.difficulty_level >= 8)  AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_8, 1);
        if (this.difficulty_level >= 11) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_11, 1);
        if (this.difficulty_level >= 14) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_14, 1);
        if (this.difficulty_level >= 17) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_17, 1);
        if (this.difficulty_level >= 20) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_20, 1);
        if (this.difficulty_level >= 40) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_40, 1);
        if (this.difficulty_level >= 60) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_60, 1);
        if (this.difficulty_level >= 80) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_80, 1);
        if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.COMPLETE_DIFFICULTY_100, 1);

        // 生命值特殊成就
        if (this.health == this.starting_health) AchievementData.increaseLevel(AchievementData.PERFECT_100, 1);
        if (this.health == 1) AchievementData.increaseLevel(AchievementData.RISK_TAKER, 1);
        if (this.health >= 18) AchievementData.increaseLevel(AchievementData.SOLID_EFFORT, 1);

        // 不使用特定类型塔
        if (this.no_slow_towers_created)   AchievementData.increaseLevel(AchievementData.FAST_PACED, 1);
        if (this.no_gun_towers_created)    AchievementData.increaseLevel(AchievementData.ROCKETMAN, 1);
        if (this.no_rocket_towers_created) AchievementData.increaseLevel(AchievementData.GUNNER, 1);
        if (this.cheapskate)               AchievementData.increaseLevel(AchievementData.CHEAPSKATE, 1);
        if (this.no_sale)                  AchievementData.increaseLevel(AchievementData.NO_SALE, 1);
        if (this.only_one_tower_created)   AchievementData.increaseLevel(AchievementData.SHOW_OFF, 1);

        // 少量塔成就
        if (this.tower_powup_counter <= 12) {
            AchievementData.increaseLevel(AchievementData.POWERED_UP, 1);
            if (this.health == this.starting_health) {
                AchievementData.increaseLevel(AchievementData.SUPER_POWERED, 1);
            }
        }

        // 30/30 成就
        if (this.difficulty_level >= 30 && this.health >= 30) {
            AchievementData.increaseLevel(AchievementData.THIRTY_THIRTY, 1);
        }

        // 地图相关成就
        int levelType = this.level_data.getLevelType();
        switch (levelType) {
            case LevelData.BASIC_LEVEL:
                AchievementData.increaseLevel(AchievementData.BASIC_10, 1);
                AchievementData.increaseLevel(AchievementData.BASIC_25, 1);
                if (this.difficulty_level >= 40)  AchievementData.increaseLevel(AchievementData.BASIC_LVL_40, 1);
                if (this.difficulty_level >= 60)  AchievementData.increaseLevel(AchievementData.BASIC_LVL_60, 1);
                if (this.difficulty_level >= 80)  AchievementData.increaseLevel(AchievementData.BASIC_LVL_80, 1);
                if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.BASIC_LVL_100, 1);
                break;
            case LevelData.RUINS_LEVEL:
                AchievementData.increaseLevel(AchievementData.RUINS_10, 1);
                AchievementData.increaseLevel(AchievementData.RUINS_25, 1);
                if (this.difficulty_level >= 30)  AchievementData.increaseLevel(AchievementData.RUINS_LVL_30, 1);
                if (this.difficulty_level >= 50)  AchievementData.increaseLevel(AchievementData.RUINS_LVL_50, 1);
                if (this.difficulty_level >= 70)  AchievementData.increaseLevel(AchievementData.RUINS_LVL_70, 1);
                if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.RUINS_LVL_100, 1);
                break;
            case LevelData.FACTORY_LEVEL:
                AchievementData.increaseLevel(AchievementData.FACTORY_10, 1);
                AchievementData.increaseLevel(AchievementData.FACTORY_25, 1);
                if (this.difficulty_level >= 20)  AchievementData.increaseLevel(AchievementData.FACTORY_LVL_20, 1);
                if (this.difficulty_level >= 40)  AchievementData.increaseLevel(AchievementData.FACTORY_LVL_40, 1);
                if (this.difficulty_level >= 70)  AchievementData.increaseLevel(AchievementData.FACTORY_LVL_70, 1);
                if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.FACTORY_LVL_100, 1);
                break;
            case LevelData.COURTYARD_LEVEL:
                AchievementData.increaseLevel(AchievementData.COURTYARD_10, 1);
                AchievementData.increaseLevel(AchievementData.COURTYARD_25, 1);
                if (this.difficulty_level >= 15)  AchievementData.increaseLevel(AchievementData.COURTYARD_LVL_15, 1);
                if (this.difficulty_level >= 25)  AchievementData.increaseLevel(AchievementData.COURTYARD_LVL_25, 1);
                if (this.difficulty_level >= 50)  AchievementData.increaseLevel(AchievementData.COURTYARD_LVL_50, 1);
                if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.COURTYARD_LVL_100, 1);
                break;
            case LevelData.MIXER_LEVEL:
                if (this.level_data.isFixedPath()) {
                    AchievementData.increaseLevel(AchievementData.MIXER_E, 1);
                } else {
                    switch (this.level_data.getPathCount()) {
                        case 1:  AchievementData.increaseLevel(AchievementData.MIXER_A, 1); break;
                        case 2:  AchievementData.increaseLevel(AchievementData.MIXER_B, 1); break;
                        case 3:  AchievementData.increaseLevel(AchievementData.MIXER_C, 1); break;
                        default: AchievementData.increaseLevel(AchievementData.MIXER_D, 1); break;
                    }
                }
                break;
            case LevelData.ROADWAY_LEVEL:
                if (this.difficulty_level >= 10)  AchievementData.increaseLevel(AchievementData.ROADWAY_LVL_10, 1);
                if (this.difficulty_level >= 50)  AchievementData.increaseLevel(AchievementData.ROADWAY_LVL_50, 1);
                if (this.difficulty_level >= 100) AchievementData.increaseLevel(AchievementData.ROADWAY_LVL_100, 1);
                break;
        }

        // 大富豪成就（所有塔升级到最大）
        bigSpenderAchievement();

        // 喷火兵成就（25+个火焰塔）
        pyroAchievement();

        // 生存模式通关
        if (this.survival_mode) {
            AchievementData.increaseLevel(AchievementData.SURVIVAL_100, 1);
        }
    }

    /**
     * 大富豪成就：所有塔都升级到最大等级
     */
    private void bigSpenderAchievement() {
        boolean bigSpender = true;
        for (GameTower t = this.tower_list; bigSpender && t != null; t = t.next) {
            bigSpender = TowerData.upgradeType(t.getType(), 0) < 0;
        }
        if (bigSpender) {
            AchievementData.increaseLevel(AchievementData.BIG_SPENDER, 1);
        }
    }

    /**
     * 喷火兵成就：使用 25+ 个火焰塔
     */
    private void pyroAchievement() {
        int flameCount = 0;
        for (GameTower t = this.tower_list; t != null; t = t.next) {
            if (t.getType() == 10 || t.getType() == 11) {
                flameCount++;
            }
        }
        if (flameCount >= 25) {
            AchievementData.increaseLevel(AchievementData.PYRO, 1);
        }
    }

    /**
     * 分配游戏事件
     */
    public GameEvent allocateGameEvent(int event_type) {
        GameEvent e = this.event_pool;
        if (e == null) {
            e = new GameEvent();
            this.events_allocated++;
            if (this.events_allocated > 500) {
                // 原版防 OOM：超限时清空粒子与爆炸事件队列
                clearEventQueue(GameEvent.EVENT_PARTICLE);
                clearEventQueue(GameEvent.EVENT_EXPLOSION);
            }
        } else {
            this.event_pool = this.event_pool.next;
        }
        e.init();
        e.next = this.event_list[event_type];
        this.event_list[event_type] = e;
        this.active_events++;
        return e;
    }

    /**
     * 回收已完成的事件
     */
    private void recycleGameEvents() {
        for (int event_type = 0; event_type < GameEvent.NUM_EVENT_TYPES; event_type++) {
            GameEvent last_ge = null;
            GameEvent ge = this.event_list[event_type];
            while (ge != null) {
                GameEvent next_ge = ge.next;
                if (ge.finished) {
                    if (last_ge == null) {
                        this.event_list[event_type] = ge.next;
                    } else {
                        last_ge.next = ge.next;
                    }
                    ge.next = this.event_pool;
                    this.event_pool = ge;
                    this.active_events--;
                } else {
                    last_ge = ge;
                }
                ge = next_ge;
            }
        }
    }

    /** 清空指定类型的事件队列（原版 clearEventQueue） */
    private void clearEventQueue(int event_type) {
        while (this.event_list[event_type] != null) {
            this.events_allocated--;
            this.active_events--;
            this.event_list[event_type] = this.event_list[event_type].next;
        }
    }

    /**
     * 分配敌人
     */
    private Enemy allocateGameEnemy() {
        Enemy ge = this.enemy_pool;
        if (ge == null) {
            ge = new Enemy();
            this.enemies_allocated++;
            if (this.enemies_allocated > 100) {
                showError("内部错误: 敌人分配数 " + this.enemies_allocated + " > 100");
                endGame(GAME_NOT_STARTED);
            }
        } else {
            this.enemy_pool = this.enemy_pool.next;
        }
        ge.next = this.enemy_list;
        this.enemy_list = ge;
        this.grid_order.insertObject(ge);
        return ge;
    }

    /**
     * 释放敌人（回收到池）
     */
    private void freeGameEnemy(Enemy ge, Enemy last_ge) {
        if (last_ge == null) {
            this.enemy_list = ge.next;
        } else {
            last_ge.next = ge.next;
        }
        ge.next = this.enemy_pool;
        this.enemy_pool = ge;
        this.grid_order.deleteObject(ge);
    }

    private void killGameEnemy(Enemy ge, Enemy last_ge) {
        if (last_ge == null) {
            this.enemy_list = ge.next;
        } else {
            last_ge.next = ge.next;
        }
        ge.next = this.enemy_graveyard;
        this.enemy_graveyard = ge;
        this.grid_order.deleteObject(ge);
    }

    private void freeDeadGameEnemy(Enemy ge, Enemy last_ge) {
        if (last_ge == null) {
            this.enemy_graveyard = ge.next;
        } else {
            last_ge.next = ge.next;
        }
        ge.next = this.enemy_pool;
        this.enemy_pool = ge;
    }

    /**
     * 设置金钱
     */
    private void setMoney(int new_money) {
        GameEvent e = allocateGameEvent(GameEvent.EVENT_MONEY_CHANGED);
        e.var[GameEvent.VAR_MONEY_OLD_AMOUNT] = this.money;
        this.money = new_money;
        // 金钱里程碑成就
        if (this.money >= 1000) AchievementData.increaseLevel(AchievementData.RAINY_DAY, 1);
        if (this.money >= 2500) AchievementData.increaseLevel(AchievementData.BIG_SAVER, 1);
        if (this.money >= 5000) AchievementData.increaseLevel(AchievementData.CRAZY_SAVER, 1);
    }

    /**
     * 设置生命值
     */
    private void setHealth(int new_health) {
        allocateGameEvent(GameEvent.EVENT_HEALTH_CHANGED);
        this.health = new_health;
    }

    /**
     * 结算奖励积分（原版 saveScore）：输/赢/退出都按 score 结算；
     * 胜利额外四种奖金。产生 EVENT_SCORE_SAVED 供结算界面显示。
     */
    private void saveScore(int new_run_state) {
        if (this.score > 0) {
            int won_bonus = 0;
            int health_bonus = 0;
            int perfect_bonus = 0;
            int money_bonus = 0;
            if (new_run_state == GAME_WON) {
                won_bonus = (this.score * 20) / 100;
                health_bonus = (this.score * this.health) / 100;
                if (this.health == this.starting_health) {
                    perfect_bonus = (this.score * 20) / 100;
                }
                money_bonus = this.money * this.difficulty_level * 2;
            }
            GameEvent e = allocateGameEvent(GameEvent.EVENT_SCORE_SAVED);
            e.var[GameEvent.VAR_SCORE_FRAME_INDEX] = 0;
            e.var[GameEvent.VAR_SCORE_STATE] = 0;
            e.var[GameEvent.VAR_SCORE_ADD] = this.score;
            e.var[GameEvent.VAR_SCORE_WON_BONUS] = won_bonus;
            e.var[GameEvent.VAR_SCORE_HEALTH_BONUS] = health_bonus;
            e.var[GameEvent.VAR_SCORE_PERFECT_BONUS] = perfect_bonus;
            e.var[GameEvent.VAR_SCORE_MONEY_BONUS] = money_bonus;
            RewardData.addRewardPoints((long) this.score + won_bonus + health_bonus
                    + money_bonus + perfect_bonus);
            this.score = 0;
        }
    }

    /**
     * 结束游戏（原版 endGame）：先结算积分与成就，再重置。
     * 难度持久化递增与清快速存档由 GamePlayScreen 在状态转换处执行。
     */
    public void endGame(int new_run_state) {
        if (this.run_state == GAME_RUNNING || this.run_state == GAME_PAUSED ||
                this.run_state == GAME_FAST_FWD) {
            if (new_run_state == GAME_LOST || new_run_state == GAME_WON ||
                    new_run_state == GAME_NOT_STARTED) {
                if (new_run_state == GAME_WON) {
                    // 成就检测（必须在 initGame 之前，因为 initGame 会重置标志）
                    gameWonAchievements();
                }
                // 输/赢/退出都结算积分（原版行为；使用重置前的 score/health/money）
                saveScore(new_run_state);
                // 保留当前难度（旧代码经单参 initGame 误重置为 1）
                initGame(this.level_data.getLevelType(), this.difficulty_level);
                this.run_state = new_run_state;
            }
        }
        AchievementData.trySaveProgress();
    }

    /**
     * 开始游戏（从 GAME_NOT_STARTED 切换到 GAME_RUNNING）
     */
    public void startGame() {
        if (this.run_state == GAME_NOT_STARTED) {
            this.run_state = GAME_RUNNING;
        }
    }

    /**
     * 切换暂停
     */
    public void togglePause() {
        if (this.run_state == GAME_RUNNING || this.run_state == GAME_FAST_FWD) {
            this.run_state = GAME_PAUSED;
        } else if (this.run_state == GAME_PAUSED) {
            this.run_state = GAME_RUNNING;
        }
    }

    /**
     * 切换快进
     */
    public void toggleFastFwd() {
        if (this.run_state == GAME_RUNNING) {
            this.run_state = GAME_FAST_FWD;
        } else if (this.run_state == GAME_FAST_FWD) {
            this.run_state = GAME_RUNNING;
        }
    }

    /**
     * 放置塔
     * @return 0=成功, 1=金钱不足, 2=超出边界, 3=已有塔, 4=阻挡路径, -1=无效类型
     */
    public int tryPlaceTower(int object_type, int gridx, int gridy) {
        if (object_type < 0) {
            return -1;
        }
        
        if (TowerData.cost(object_type) > this.money) {
            return 1;
        }
        
        // 检查是否在边界内
        if (!inBounds(gridx, gridy)) {
            return 2;
        }
        
        // 检查是否与已有塔重叠
        GameTower existingTower = findTowerAt(gridx, gridy);
        if (existingTower != null) {
            return 3;
        }
        
        // 检查是否阻挡路径
        for (int idx = 0; idx < this.movement_grid.length; idx++) {
            boolean pathValid = this.movement_grid[idx].checkTowerPlacement(
                    this.tower_list,
                    this.enemy_list,
                    gridx, gridy, idx,
                    TowerData.isBlocking(object_type)
            );
            if (!pathValid) {
                return 4;
            }
        }
        
        // 放置成功
        GameTower tower = allocateGameTower();
        tower.init(this.collision_grid, gridx, gridy, object_type, this.state_index);
        towersChanged();
        setMoney(this.money - TowerData.cost(object_type));
        this.tower_powup_counter++;
        if (this.tower_powup_counter > 1) {
            this.only_one_tower_created = false;
        }
        switch (object_type) {
            case TowerData.GUN_TOWER:
                this.no_gun_towers_created = false;
                break;
            case TowerData.SLOW_TOWER:
                this.no_slow_towers_created = false;
                break;
            case TowerData.ROCKET_TOWER:
                this.no_rocket_towers_created = false;
                break;
        }
        return 0;
    }

    /**
     * 升级或出售塔
     * @param tower 要操作的塔
     * @param new_tower_id 新塔类型（0 = 出售，> 0 = 升级）
     */
    public void upgradeTower(GameTower tower, int new_tower_id) {
        if (new_tower_id == 0) {
            sellTower(tower);
            // 原版：出售时 ≤12 才递减强化计数（POWERED_UP 成就条件）
            if (this.tower_powup_counter <= 12) {
                this.tower_powup_counter--;
            }
            return;
        }
        if (TowerData.cost(new_tower_id) <= this.money) {
            setMoney(this.money - TowerData.cost(new_tower_id));
            int old_id = tower.getType();
            tower.init(this.collision_grid, tower.getGridX(), tower.getGridY(), new_tower_id, this.state_index);
            if (TowerData.isBlocking(old_id) != TowerData.isBlocking(new_tower_id)) {
                towersChanged();
            } else {
                allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
            }
            // 原版：仅升级成功后才失去"吝啬鬼"资格
            this.cheapskate = false;
        }
    }

    /**
     * 出售塔
     */
    public void sellTower(GameTower tower) {
        // 出售塔 → 不是"非卖品"
        this.no_sale = false;
        setMoney(this.money + TowerData.sellValue(tower.getType()));
        
        // 先从排序链表中移除（必须在修改 tower_list 之前）
        this.grid_order.deleteObject(tower);
        
        // 从塔链表中移除
        GameTower last_t = null;
        GameTower t = this.tower_list;
        while (t != null && t != tower) {
            last_t = t;
            t = t.next;
        }
        if (t != null) {
            if (last_t == null) {
                this.tower_list = t.next;
            } else {
                last_t.next = t.next;
            }
            t.next = null;
        }
        
        towersChanged();
    }

    /**
     * 分配塔（使用对象池）
     */
    private GameTower allocateGameTower() {
        GameTower t = this.tower_pool;
        if (t == null) {
            t = new GameTower();
            this.towers_allocated++;
        } else {
            this.tower_pool = this.tower_pool.next;
        }
        t.next = this.tower_list;
        this.tower_list = t;
        this.grid_order.insertObject(t);
        return t;
    }

    /**
     * 检查塔放置位置
     */
    public boolean checkTowerPlacement(int gridx, int gridy, int tower_type, boolean iterate_all) {
        boolean valid = true;

        // 检查是否在边界内
        if (!inBounds(gridx, gridy)) {
            return false;
        }

        // 检查是否与已有塔重叠
        GameTower existingTower = findTowerAt(gridx, gridy);
        if (existingTower != null) {
            return false;
        }

        // 检查是否与敌人重叠
        for (Enemy ge = this.enemy_list; valid && ge != null; ge = ge.next) {
            valid = !(ge.getGridX() == gridx && ge.getGridY() == gridy);
        }

        // 检查是否阻挡路径
        if (valid) {
            for (int idx = 0; idx < this.movement_grid.length; idx++) {
                boolean pathValid = this.movement_grid[idx].checkTowerPlacement(
                        this.tower_list,
                        iterate_all ? this.enemy_list : null,
                        gridx, gridy, idx,
                        TowerData.isBlocking(tower_type)
                );
                if (!pathValid) {
                    valid = false;
                    break;
                }
            }
        }

        return valid;
    }

    /**
     * 塔变化后更新路径
     */
    private void towersChanged() {
        allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
        for (int idx = 0; idx < this.movement_grid.length; idx++) {
            this.movement_grid[idx].calcPaths(this.tower_list);
        }
        for (Enemy e = this.enemy_list; e != null; e = e.next) {
            e.checkOrientation(this.movement_grid[e.getPathNum()]);
        }
    }

    /**
     * 初始化移动网格
     */
    private void initMovementGrid() {
        this.movement_grid = new MovementGrid[this.level_data.getPathCount()];
        for (int i = 0; i < this.movement_grid.length; i++) {
            this.movement_grid[i] = new MovementGrid(this.level_data, i);
        }
    }

    /**
     * 投放新单位（调试用）
     */
    public void dropNewUnit(int unit_type, int gridx, int gridy, int orientation, int path_num) {
        if (this.enemies_allocated < 100) {
            Enemy ge = allocateGameEnemy();
            if (gridx == -1) {
                gridx = this.level_data.getStartX(path_num);
                gridy = this.level_data.getStartY(path_num);
                orientation = this.level_data.getStartOrientation(path_num);
            }
            ge.init(gridx, gridy, orientation, unit_type, this.state_index,
                    EnemyData.baseHealth(unit_type, this.level_data.getLevelNum(),
                            this.difficulty_level),
                    path_num);
        }
    }

    /**
     * 查找指定位置的敌人
     */
    public Enemy findEnemyAt(int gridx, int gridy) {
        for (Enemy e = this.enemy_list; e != null; e = e.next) {
            if (e.getGridX() == gridx && e.getGridY() == gridy) {
                return e;
            }
        }
        return null;
    }

    /**
     * 查找指定位置的塔
     */
    public GameTower findTowerAt(int gridx, int gridy) {
        GameTower ret = this.tower_list;
        while (ret != null && (ret.getGridX() != gridx || ret.getGridY() != gridy)) {
            ret = ret.next;
        }
        return ret;
    }

    /**
     * 检查坐标是否在边界内
     */
    public boolean inBounds(int gridx, int gridy) {
        return gridx >= 0 && gridx < this.level_data.getGridWidth() &&
                gridy >= 0 && gridy < this.level_data.getGridHeight();
    }

    /** 显示短消息（原版 showMessage，45 帧） */
    public void showMessage(String message) {
        createMessageEvent(message, 45);
    }

    /** 显示错误消息（原版 showError，90 帧） */
    public void showError(String message) {
        createMessageEvent(message, 90);
    }

    /** 创建消息事件：分配 0-9 号不重叠槽位，+30 帧淡入（原版 createMessageEvent） */
    private void createMessageEvent(String message, int display_frames) {
        int used_slots = 0;
        for (GameEvent e = getGameEventList(GameEvent.EVENT_MESSAGE); e != null; e = e.next) {
            used_slots |= 1 << e.var[GameEvent.VAR_MESSAGE_Y_SLOT];
        }
        int slot_num = 0;
        while (slot_num < 10 && ((1 << slot_num) & used_slots) != 0) {
            slot_num++;
        }
        GameEvent e2 = allocateGameEvent(GameEvent.EVENT_MESSAGE);
        e2.str = message;
        e2.var[GameEvent.VAR_MESSAGE_FRAMES] = display_frames + 30;
        e2.var[GameEvent.VAR_MESSAGE_Y_SLOT] = slot_num;
    }

    // ========== Getter 方法 ==========

    public int getStateIndex() { return state_index; }
    public int getScore() { return score; }
    public int getMoney() { return money; }
    public int getHealth() { return health; }
    public int getRunState() { return run_state; }
    public int getStartingHealth() { return starting_health; }
    public int getDifficultyLevel() { return difficulty_level; }
    public int getEnemyKillBonus() {
        int bonus = this.level_bonus + AchievementData.totalCount();
        if (RewardData.getLevel(RewardData.BONUS) > 0) {
            return bonus * 2;
        }
        return bonus;
    }
    public int activeEventCount() { return active_events; }
    public LevelData getLevelData() { return level_data; }
    public GridObject getSortedList() { return grid_order.getSortedList(); }
    public GameTower getTowerList() { return tower_list; }
    public Enemy getEnemyList() { return enemy_list; }
    public GameEvent getGameEventList(int event_type) { return event_list[event_type]; }
    public Bullet getBulletList() { return bullet_list; }

    public boolean isSurvivalMode() { return survival_mode; }
    public void setSurvivalMode(boolean enabled) { this.survival_mode = enabled; }

    /** 取走自动存档请求（一次性），由 GamePlayScreen 每帧轮询 */
    public boolean consumeAutoSaveRequest() {
        boolean pending = this.auto_save_requested;
        this.auto_save_requested = false;
        return pending;
    }

    // ========== 存档接口（对应原版 GameState.saveState/loadState）==========
    //
    // 写入顺序（与原版 ObjectOutputStream 写入顺序保持一致，便于二进制兼容）：
    //   1. state_index
    //   2. score
    //   3. level_bonus
    //   4. run_state
    //   5. money
    //   6. health
    //   7. starting_health
    //   8. difficulty_level
    //   9. survival_mode
    //  10. level_data
    //  11. tower 链表（每个塔 saveState）
    //  12. enemy 链表（每个敌人 saveState）
    //  13. level_type, level_num, level_seed, mixer_seed
    //  14. 仅_game_list 与 graveyard 中的敌人需要存（graveyard 仅短暂存在，不必持久化）

    /**
     * 将当前游戏状态写入输出流（对应原版 GameState.saveState(ObjectOutputStream)）
     * @param oout Java 二进制输出流
     */
    public void saveState(ObjectOutputStream oout) throws IOException {
        GameSaveWriterAdapter w = new GameSaveWriterAdapter(oout);
        saveState(w);
    }

    /**
     * 以 GameSaveWriter 协议保存状态（与 GridObject 体系共用）
     */
    public void saveState(GameSaveWriter out) throws IOException {
        try {
            out.writeInt(state_index);
            out.writeInt(score);
            out.writeInt(level_bonus);
            out.writeInt(run_state);
            out.writeInt(money);
            out.writeInt(health);
            out.writeInt(starting_health);
            out.writeInt(difficulty_level);
            out.writeBoolean(survival_mode);

            // level_data 完整状态
            if (level_data == null) {
                out.writeInt(-1);
            } else {
                level_data.saveState(out);
            }

            // tower 链表
            int towerCount = 0;
            for (GameTower t = tower_list; t != null; t = t.next) towerCount++;
            out.writeInt(towerCount);
            for (GameTower t = tower_list; t != null; t = t.next) {
                t.saveState(out);
            }

            // enemy 链表
            int enemyCount = 0;
            for (Enemy e = enemy_list; e != null; e = e.next) enemyCount++;
            out.writeInt(enemyCount);
            for (Enemy e = enemy_list; e != null; e = e.next) {
                e.saveState(out);
            }

            // 成就追踪字段（原版写入；读档后需恢复）
            out.writeBoolean(no_slow_towers_created);
            out.writeBoolean(no_gun_towers_created);
            out.writeBoolean(no_rocket_towers_created);
            out.writeBoolean(cheapskate);
            out.writeBoolean(no_sale);
            out.writeBoolean(only_one_tower_created);
            out.writeInt(tower_powup_counter);
            out.writeInt(fast_fwd_counter);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * 从输入流加载游戏状态（对应原版 GameState.loadState(ObjectInputStream)）
     */
    public boolean loadState(ObjectInputStream oin) throws IOException {
        GameSaveReaderAdapter r = new GameSaveReaderAdapter(oin);
        try {
            return loadState(r);
        } catch (IOException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    /**
     * 以 GameSaveReader 协议加载状态
     *
     * <p>读取顺序：先缓存所有标量，再调用 initGame 重建网格/移动系统，
     * 然后重建塔与敌人链表，最后恢复被 initGame 重置的标量。</p>
     */
    public boolean loadState(GameSaveReader in) throws Exception {
        // 1. 先把所有标量读到临时变量
        int savedStateIndex = in.readInt();
        int savedScore = in.readInt();
        int savedLevelBonus = in.readInt();
        int savedRunState = in.readInt();
        int savedMoney = in.readInt();
        int savedHealth = in.readInt();
        int savedStartingHealth = in.readInt();
        int savedDifficulty = in.readInt();
        boolean savedSurvival = in.readBoolean();

        // 加载 level_data 完整状态
        boolean levelDataOk = level_data.loadState(in);
        if (!levelDataOk) {
            return false;
        }

        int towerCount = in.readInt();

        // 2. 缓存塔数据
        int[][] towerData = new int[towerCount][5]; // gridx, gridy, type, first_state, shot_delay
        for (int i = 0; i < towerCount; i++) {
            towerData[i][0] = in.readInt();
            towerData[i][1] = in.readInt();
            towerData[i][2] = in.readInt();
            towerData[i][3] = in.readInt();
            towerData[i][4] = in.readInt();
        }

        int enemyCount = in.readInt();

        // 3. 缓存敌人数据
        int[][] enemyData = new int[enemyCount][12];
        if (enemyCount > 0) {
            for (int i = 0; i < enemyCount; i++) {
                enemyData[i][0] = in.readInt(); // gridx
                enemyData[i][1] = in.readInt(); // gridy
                enemyData[i][2] = in.readInt(); // type
                enemyData[i][3] = in.readInt(); // first_state
                enemyData[i][4] = in.readInt(); // health
                enemyData[i][5] = in.readInt(); // max_health
                enemyData[i][6] = in.readInt(); // orientation
                enemyData[i][7] = in.readInt(); // path_num
                enemyData[i][8] = in.readInt(); // x_offset
                enemyData[i][9] = in.readInt(); // y_offset
                enemyData[i][10] = in.readInt(); // slow_counter
                enemyData[i][11] = in.readInt(); // fire_counter
                in.readInt();                    // death_frame（无需恢复）
            }
        }

        // 读取成就追踪字段（对应 saveState 末尾写入顺序）
        boolean saved_no_slow   = in.readBoolean();
        boolean saved_no_gun    = in.readBoolean();
        boolean saved_no_rocket = in.readBoolean();
        boolean saved_cheapskate = in.readBoolean();
        boolean saved_no_sale   = in.readBoolean();
        boolean saved_only_one  = in.readBoolean();
        int saved_powup         = in.readInt();
        int saved_fastfwd       = in.readInt();

        // 3. 重新初始化游戏基础设施
        // 注意：不能调用 initGame 会重置 level_data，所以我们先保存 level_data 的关键信息
        int savedLevelType = level_data.getLevelType();
        int savedLevelNum = level_data.getLevelNum();
        int savedLevelSeed = level_data.getLevelSeed();
        int savedMixerSeed = level_data.getMixerSeed();
        int savedIndex = level_data.getIndex();
        int savedSubIndex = level_data.getSubIndex();
        int savedFrameIndex = level_data.getFrameIndex();
        int savedUnitType = level_data.getUnitType();
        int savedPathNum = level_data.getUnitPathNum();
        
        initGame(savedLevelType, savedDifficulty);

        // 3.5. 恢复 level_data 的完整状态
        level_data.setLevelNum(savedLevelNum);
        level_data.setLevelSeed(savedLevelSeed);
        level_data.setMixerSeed(savedMixerSeed);
        level_data.setIndex(savedIndex);
        level_data.setSubIndex(savedSubIndex);
        level_data.setFrameIndex(savedFrameIndex);
        level_data.setUnitType(savedUnitType);
        level_data.setPathNum(savedPathNum);

        // 3.6. 重新生成敌人事件（使用恢复的种子）
        level_data.generateLevel(savedDifficulty, savedSurvival);

        // 4. 重建塔链表
        tower_list = null;
        tower_pool = null;
        grid_order.clear();
        for (int i = 0; i < towerCount; i++) {
            GameTower t = new GameTower();
            t.init(collision_grid, towerData[i][0], towerData[i][1], towerData[i][2], towerData[i][3]);
            t.shot_delay = towerData[i][4];
            t.next = tower_list;
            tower_list = t;
            grid_order.insertObject(t);
            towers_allocated++;
        }

        // 5. 重建敌人链表
        enemy_list = null;
        enemy_graveyard = null;
        enemy_pool = null;
        for (int i = 0; i < enemyCount; i++) {
            Enemy e = new Enemy();
            e.init(enemyData[i][0], enemyData[i][1], enemyData[i][6], enemyData[i][2],
                    enemyData[i][3], enemyData[i][4], enemyData[i][7]);
            e.max_health = enemyData[i][5];
            e.slow_counter = enemyData[i][10];
            e.fire_counter = enemyData[i][11];
            e.x_offset = enemyData[i][8];
            e.y_offset = enemyData[i][9];
            e.at_exit = false;
            e.exiting_grid = false;
            e.next = enemy_list;
            enemy_list = e;
            grid_order.insertObject(e);
            enemies_allocated++;
        }

        // 6. 重建 movement_grid 路径
        for (int idx = 0; idx < movement_grid.length; idx++) {
            movement_grid[idx].calcPaths(tower_list);
        }

        // 7. 恢复被 initGame 重置的标量字段
        state_index = savedStateIndex;
        score = savedScore;
        level_bonus = savedLevelBonus;
        // 原版：读档后不恢复 run_state（让 savedRunState 入流读取位置但不使用），
        // 而是强制暂停并通知 UI（玩家确认后再继续）
        // run_state = savedRunState;
        money = savedMoney;
        health = savedHealth;
        starting_health = savedStartingHealth;
        difficulty_level = savedDifficulty;
        survival_mode = savedSurvival;
        no_slow_towers_created = saved_no_slow;
        no_gun_towers_created = saved_no_gun;
        no_rocket_towers_created = saved_no_rocket;
        cheapskate = saved_cheapskate;
        no_sale = saved_no_sale;
        only_one_tower_created = saved_only_one;
        tower_powup_counter = saved_powup;
        fast_fwd_counter = saved_fastfwd;
        // 原版：读档后强制暂停并通知 UI
        this.run_state = GAME_PAUSED;
        allocateGameEvent(GameEvent.EVENT_GAME_LOAD_SUCCESS);
        return true;
    }

    /**
     * ObjectOutputStream 适配器
     */
    private static class GameSaveWriterAdapter implements GameSaveWriter {
        private final ObjectOutputStream oout;
        GameSaveWriterAdapter(ObjectOutputStream oout) { this.oout = oout; }
        @Override public void writeInt(int value) throws Exception { oout.writeInt(value); }
        @Override public void writeBoolean(boolean value) throws Exception { oout.writeBoolean(value); }
    }

    /**
     * ObjectInputStream 适配器
     */
    private static class GameSaveReaderAdapter implements GameSaveReader {
        private final ObjectInputStream oin;
        GameSaveReaderAdapter(ObjectInputStream oin) { this.oin = oin; }
        @Override public int readInt() throws Exception { return oin.readInt(); }
        @Override public boolean readBoolean() throws Exception { return oin.readBoolean(); }
    }

    /**
     * 初始化基础系统（游戏启动时调用一次）
     */
    public static void initBaseSystems() {
        FastRandom.init();
        VectorLookup.init();
    }
}
