package com.rdefense.core.game;

/**
 * 爆炸数据类 — 管理爆炸效果
 * 对应原版 ExplosionData
 */
public final class ExplosionData {

    /**
     * 获取爆炸效果大小
     */
    public static int size(int type) {
        switch (type) {
            case 0: return 32; // 标准爆炸
            case 1: return 24; // 小型爆炸
            case 2: return 48; // 大型爆炸
            default: return 32;
        }
    }

    /**
     * 添加爆炸效果到游戏事件
     */
    public static void addExplosion(GameState game_state, int x, int y, int type) {
        GameEvent e = game_state.allocateGameEvent(GameEvent.EVENT_EXPLOSION);
        e.var[GameEvent.VAR_EXPLOSION_X] = x;
        e.var[GameEvent.VAR_EXPLOSION_Y] = y;
        e.var[GameEvent.VAR_EXPLOSION_TYPE] = type;
        e.var[GameEvent.VAR_EXPLOSION_FIRST_STATE] = game_state.getStateIndex();
        e.var[GameEvent.VAR_EXPLOSION_NUM_STATES] = 15; // 15帧爆炸动画
    }

    /**
     * 获取爆炸图像名称
     */
    public static String getImageName(int type, int frame) {
        return "explosion_" + type + "_" + frame;
    }
}
