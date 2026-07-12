package com.rdefense.core.game;

/**
 * 网格对象基类 — 塔和敌人的共同父类
 * 对应原版 GridObject
 */
public abstract class GridObject {
    protected int gridx;
    protected int gridy;
    protected int type;
    protected int first_state;

    // Y-sort 链表
    public GridObject next_y;
    public GridObject prev_y;

    /**
     * 初始化基类字段
     */
    protected void init_base(int gridx, int gridy, int type, int classType, int first_state) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.type = type;
        this.first_state = first_state;
    }

    public int getGridX() {
        return gridx;
    }

    public int getGridY() {
        return gridy;
    }

    public int getType() {
        return type;
    }

    public int getFirstState() {
        return first_state;
    }

    /**
     * 获取对象类型：1=塔, 2=敌人
     */
    public abstract int getClassType();

    /**
     * 保存状态（用于存档）
     */
    public abstract void saveState(GameSaveWriter out) throws Exception;

    /**
     * 加载状态（用于读档）
     */
    public abstract boolean loadState(GameSaveReader in, GameState game) throws Exception;

    /**
     * 存档写入器接口
     */
    public interface GameSaveWriter {
        void writeInt(int value) throws Exception;
        void writeBoolean(boolean value) throws Exception;
    }

    /**
     * 存档读取器接口
     */
    public interface GameSaveReader {
        int readInt() throws Exception;
        boolean readBoolean() throws Exception;
    }

    /**
     * 保存基类状态
     */
    protected void saveBaseState(GameSaveWriter oout) throws Exception {
        oout.writeInt(this.gridx);
        oout.writeInt(this.gridy);
        oout.writeInt(this.type);
        oout.writeInt(this.first_state);
    }

    /**
     * 加载基类状态
     */
    protected boolean loadBaseState(GameSaveReader oin, GameState game) throws Exception {
        this.gridx = oin.readInt();
        this.gridy = oin.readInt();
        this.type = oin.readInt();
        this.first_state = oin.readInt();
        return true;
    }
}
