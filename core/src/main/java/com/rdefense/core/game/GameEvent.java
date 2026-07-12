package com.rdefense.core.game;

/**
 * 游戏事件类 — 用于游戏系统间通信
 * 对应原版 GameEvent
 *
 * 事件类型：
 * 0 - 消息事件 (EVENT_MESSAGE)
 * 1 - 敌人击败事件 (EVENT_ENEMY_DEFEATED)
 * 2 - 金钱变化事件 (EVENT_MONEY_CHANGED)
 * 3 - 游戏初始化事件 (EVENT_GAME_INIT)
 * 4 - 生命值变化事件 (EVENT_HEALTH_CHANGED)
 * 5 - 关卡奖励丢失事件 (EVENT_LEVEL_BONUS_LOST)
 * 6 - 塔变化事件 (EVENT_TOWERS_CHANGED)
 * 7 - 积分保存事件 (EVENT_SCORE_SAVED)
 * 8 - 粒子事件 (EVENT_PARTICLE)
 * 9 - 爆炸事件 (EVENT_EXPLOSION)
 * 10 - 成就获得事件 (EVENT_ACHIEVEMENT_EARNED)
 * 11 - 游戏加载成功事件 (EVENT_GAME_LOAD_SUCCESS)
 */
public final class GameEvent {

    // 事件类型常量
    public static final int EVENT_MESSAGE = 0;
    public static final int EVENT_ENEMY_DEFEATED = 1;
    public static final int EVENT_MONEY_CHANGED = 2;
    public static final int EVENT_GAME_INIT = 3;
    public static final int EVENT_HEALTH_CHANGED = 4;
    public static final int EVENT_LEVEL_BONUS_LOST = 5;
    public static final int EVENT_TOWERS_CHANGED = 6;
    public static final int EVENT_SCORE_SAVED = 7;
    public static final int EVENT_PARTICLE = 8;
    public static final int EVENT_EXPLOSION = 9;
    public static final int EVENT_ACHIEVEMENT_EARNED = 10;
    public static final int EVENT_GAME_LOAD_SUCCESS = 11;
    public static final int NUM_EVENT_TYPES = 12;

    // 变量槽位数量
    private static final int NUM_VAR_SLOTS = 9;

    // 变量索引常量
    public static final int VAR_MESSAGE_FRAMES = 0;
    public static final int VAR_MESSAGE_Y_SLOT = 1;

    public static final int VAR_MONEY_OLD_AMOUNT = 0;

    public static final int VAR_ENEMY_STATE_IDX = 0;
    public static final int VAR_ENEMY_TYPE = 1;
    public static final int VAR_ENEMY_ORIENTATION = 2;
    public static final int VAR_ENEMY_PIXEL_X = 3;
    public static final int VAR_ENEMY_PIXEL_Y = 4;
    public static final int VAR_ENEMY_BASE_SCORE = 5;
    public static final int VAR_ENEMY_FULL_SCORE = 6;

    public static final int VAR_ACHIEVEMENT_TYPE = 0;
    public static final int VAR_ACHIEVEMENT_FRAME = 1;
    public static final int VAR_ACHIEVEMENT_STATE = 2;

    public static final int VAR_PARTICLE_X = 0;
    public static final int VAR_PARTICLE_Y = 1;
    public static final int VAR_PARTICLE_XVEL = 2;
    public static final int VAR_PARTICLE_YVEL = 3;
    public static final int VAR_PARTICLE_COLOR = 4;
    public static final int VAR_PARTICLE_MAX_ALPHA = 5;
    public static final int VAR_PARTICLE_SIZE = 6;
    public static final int VAR_PARTICLE_FIRST_STATE = 7;
    public static final int VAR_PARTICLE_NUM_STATES = 8;

    public static final int VAR_EXPLOSION_X = 0;
    public static final int VAR_EXPLOSION_Y = 1;
    public static final int VAR_EXPLOSION_TYPE = 2;
    public static final int VAR_EXPLOSION_FIRST_STATE = 3;
    public static final int VAR_EXPLOSION_NUM_STATES = 4;

    public static final int VAR_SCORE_FRAME_INDEX = 0;
    public static final int VAR_SCORE_STATE = 1;
    public static final int VAR_SCORE_ADD = 2;
    public static final int VAR_SCORE_WON_BONUS = 3;
    public static final int VAR_SCORE_HEALTH_BONUS = 4;
    public static final int VAR_SCORE_PERFECT_BONUS = 5;
    public static final int VAR_SCORE_MONEY_BONUS = 6;

    // 事件数据
    public boolean finished;
    public GameEvent next;
    public int[] var = new int[NUM_VAR_SLOTS];
    public String str = null;

    /**
     * 初始化事件（重置所有字段）
     */
    public void init() {
        this.str = null;
        this.finished = false;
        for (int i = 0; i < var.length; i++) {
            var[i] = 0;
        }
    }

    /**
     * 创建消息事件
     */
    public static GameEvent createMessage(String message, int displayFrames) {
        GameEvent e = new GameEvent();
        e.str = message;
        e.var[VAR_MESSAGE_FRAMES] = displayFrames;
        return e;
    }

    /**
     * 创建粒子事件
     */
    public static GameEvent createParticle(int x, int y, int xVel, int yVel, int color,
                                            int maxAlpha, int size, int firstState, int numStates) {
        GameEvent e = new GameEvent();
        e.var[VAR_PARTICLE_X] = x;
        e.var[VAR_PARTICLE_Y] = y;
        e.var[VAR_PARTICLE_XVEL] = xVel;
        e.var[VAR_PARTICLE_YVEL] = yVel;
        e.var[VAR_PARTICLE_COLOR] = color;
        e.var[VAR_PARTICLE_MAX_ALPHA] = maxAlpha;
        e.var[VAR_PARTICLE_SIZE] = size;
        e.var[VAR_PARTICLE_FIRST_STATE] = firstState;
        e.var[VAR_PARTICLE_NUM_STATES] = numStates;
        return e;
    }

    /**
     * 创建爆炸事件
     */
    public static GameEvent createExplosion(int x, int y, int type, int firstState, int numStates) {
        GameEvent e = new GameEvent();
        e.var[VAR_EXPLOSION_X] = x;
        e.var[VAR_EXPLOSION_Y] = y;
        e.var[VAR_EXPLOSION_TYPE] = type;
        e.var[VAR_EXPLOSION_FIRST_STATE] = firstState;
        e.var[VAR_EXPLOSION_NUM_STATES] = numStates;
        return e;
    }
}
