package com.rdefense.core.game;

/**
 * 敌人类 — 处理敌人移动、状态、伤害
 * 对应原版 Enemy
 */
public class Enemy extends GridObject {

    // 火焰粒子常量
    private static final int FLAME_LENGTH_RANGE = 2;
    private static final int FLAME_MAX_LENGTH = 4;
    private static final int FLAME_MIN_LENGTH = 2;
    private static final int MAX_FLAME_COUNT = 5;

    // 敌人状态
    boolean at_exit;
    private int death_frame;
    private int drop_counter;
    boolean exiting_grid;
    int fire_counter;
    private FlameProps[] flame_props;
    public Enemy grid_next;
    int health;
    int max_health;
    public Enemy next;
    protected int orientation;
    int path_num;
    int slow_counter;
    protected int x_offset;
    protected int y_offset;

    Enemy() {
    }

    /**
     * 初始化敌人
     */
    public void init(int gridx, int gridy, int orientation, int type, int first_state, int health, int path_num) {
        init_base(gridx, gridy, type, 2, first_state);
        this.health = health;
        this.max_health = health;
        this.path_num = path_num;
        this.at_exit = false;
        this.slow_counter = 0;
        this.fire_counter = 0;
        this.drop_counter = EnemyData.dropDelay(type);
        this.death_frame = -1;
        teleportTo(gridx, gridy, orientation);
        flameInit();
    }

    /**
     * 传送到指定位置
     */
    public void teleportTo(int gridx, int gridy, int orientation) {
        this.gridx = gridx;
        this.gridy = gridy;
        this.orientation = orientation;
        this.x_offset = 0;
        this.y_offset = 0;
        this.exiting_grid = true;
    }

    /**
     * 初始化火焰粒子
     */
    private void flameInit() {
        if (this.flame_props == null) {
            this.flame_props = new FlameProps[MAX_FLAME_COUNT];
            for (int i = 0; i < this.flame_props.length; i++) {
                this.flame_props[i] = new FlameProps();
            }
        }
    }

    /**
     * 更新敌人状态
     * @return true 表示敌人已到达出口
     */
    public boolean nextState(GameState game_state, MovementGrid movement_grid, int state_index) {
        int speed = EnemyData.speed(this.type);

        // 减速效果
        if (this.slow_counter > 0) {
            speed >>= 1;
            this.slow_counter--;
        }

        // 灼烧效果
        if (this.fire_counter > 0 && this.health > 0) {
            this.fire_counter--;
            this.health--;
            for (int i = 0; i < this.flame_props.length; i++) {
                this.flame_props[i].nextState();
            }
        }

        // 移动
        if (this.health > 0) {
            switch (this.orientation) {
                case 1: // 右
                    this.x_offset += speed;
                    if (this.x_offset >= 128) { // GRID_HSIZE
                        this.x_offset -= 256; // GRID_SIZE
                        move(movement_grid, this.gridx + 1, this.gridy);
                    } else if (!this.exiting_grid && this.x_offset >= 0) {
                        setOrientation(movement_grid);
                    }
                    break;
                case 2: // 上
                    this.y_offset -= speed;
                    if (this.y_offset <= -128) {
                        this.y_offset += 256;
                        move(movement_grid, this.gridx, this.gridy - 1);
                    } else if (!this.exiting_grid && this.y_offset <= 0) {
                        setOrientation(movement_grid);
                    }
                    break;
                case 3: // 左
                    this.x_offset -= speed;
                    if (this.x_offset <= -128) {
                        this.x_offset += 256;
                        move(movement_grid, this.gridx - 1, this.gridy);
                    } else if (!this.exiting_grid && this.x_offset <= 0) {
                        setOrientation(movement_grid);
                    }
                    break;
                case 4: // 下
                    this.y_offset += speed;
                    if (this.y_offset >= 128) {
                        this.y_offset -= 256;
                        move(movement_grid, this.gridx, this.gridy + 1);
                    } else if (!this.exiting_grid && this.y_offset >= 0) {
                        setOrientation(movement_grid);
                    }
                    break;
            }

            // 投放单位
            if (EnemyData.dropType(this.type) != -1) {
                if (this.drop_counter > 0) {
                    this.drop_counter--;
                } else if (tryDropUnit(game_state, movement_grid, this.gridx, this.gridy)) {
                    this.drop_counter = EnemyData.dropDelay(this.type);
                }
            }
        }

        return this.at_exit;
    }

    /**
     * 尝试投放单位
     */
    private boolean tryDropUnit(GameState game_state, MovementGrid movement_grid, int try_gridx, int try_gridy) {
        int grid_orientation;
        if (try_gridx < 0 || try_gridx >= movement_grid.getGridW() ||
                try_gridy < 0 || try_gridy >= movement_grid.getGridH() ||
                (grid_orientation = movement_grid.getOrientation(try_gridx, try_gridy)) < 1 ||
                grid_orientation > 4) {
            return false;
        }
        game_state.dropNewUnit(EnemyData.dropType(this.type), try_gridx, try_gridy,
                grid_orientation, this.path_num);
        return true;
    }

    /**
     * 检查并更新朝向
     */
    public void checkOrientation(MovementGrid movement_grid) {
        if (!EnemyData.isFlyer(this.type)) {
            int grid_orientation = movement_grid.getOrientation(this.gridx, this.gridy);
            if (this.exiting_grid) {
                if (this.orientation != grid_orientation) {
                    reverseOrientation();
                }
            } else {
                int reverse_orientation = getReverseOrientation(this.orientation);
                if (grid_orientation == reverse_orientation) {
                    reverseOrientation();
                }
            }
        }
    }

    /**
     * 应用伤害（精确匹配原版 applyDamage 逻辑）
     *
     * 伤害类型对照：
     *   4  = 减速弹（仅减速，无伤害）
     *   6  = 火焰弹（仅灼烧，无直接伤害）
     *   12 = 穿甲弹/铀弹（无视护甲）
     *   13 = 凝固汽油弹（部分灼烧 + 常规伤害）
     *   14 = 冲击波/缓慢火焰弹（灼烧 + 减速）
     */
    public void applyDamage(int amount, int shot_type) {
        // 减速弹：仅施加减速效果，无伤害
        if (shot_type == 4 || shot_type == BulletData.SLOW) {
            this.slow_counter = amount;
            return;
        }
        // 火焰弹/冲击波：施加灼烧效果（无直接伤害）
        if (shot_type == 6 || shot_type == 14 || shot_type == BulletData.SLOW_FIRE) {
            // 原版语义：每次命中至多 +4，且封顶到本次弹药的 amount（可缩短已有灼烧）
            this.fire_counter += 4;
            if (this.fire_counter > amount) {
                this.fire_counter = amount;
            }
            // 类型14（冲击波/缓慢火焰弹）同时施加减速效果
            if ((shot_type == 14 || shot_type == BulletData.SLOW_FIRE) && this.slow_counter < amount) {
                this.slow_counter = amount;
            }
            // 触发火焰粒子
            if (this.fire_counter > 0 && this.health > 0) {
                addFlame();
            }
            return;
        }
        // 凝固汽油弹：部分灼烧（无直接伤害以外的处理，继续走常规伤害）
        if (shot_type == 13 || shot_type == BulletData.NAPALM_SHELL) {
            this.fire_counter += amount / 4;
        }

        // 穿甲弹（类型12 / URANIUM_BULLET）无视护甲
        if (shot_type != 12 && shot_type != BulletData.URANIUM_BULLET) {
            amount -= EnemyData.armor(this.type);
        }
        if (amount < 1) {
            amount = 1;
        }
        this.health -= amount;
        if (this.health < 0) {
            this.health = 0;
        }
        // 火焰/凝固汽油弹命中时产生火焰粒子
        if ((shot_type == BulletData.FIRE || shot_type == BulletData.SLOW_FIRE ||
             shot_type == BulletData.NAPALM_SHELL) && this.fire_counter > 0 && this.health > 0) {
            addFlame();
        }
    }

    /**
     * 添加火焰粒子
     */
    private void addFlame() {
        int flame_length = FLAME_MIN_LENGTH + (FastRandom.nextInt() % FLAME_LENGTH_RANGE);
        for (int i = 0; i < this.flame_props.length; i++) {
            if (!this.flame_props[i].active) {
                this.flame_props[i].init(flame_length);
                break;
            }
        }
    }

    /**
     * 移动到网格位置
     * 原版 move 只做简单赋值，边界检查在 nextState 的移动逻辑中处理
     */
    private void move(MovementGrid movement_grid, int new_gridx, int new_gridy) {
        if (new_gridx < 0 || new_gridx >= movement_grid.getGridW()) {
            this.at_exit = true;
            return;
        }
        if (new_gridy < 0 || new_gridy >= movement_grid.getGridH()) {
            this.at_exit = true;
            return;
        }
        this.gridx = new_gridx;
        this.gridy = new_gridy;
        this.exiting_grid = false;
        setOrientation(movement_grid);
    }

    /**
     * 设置朝向
     */
    private void setOrientation(MovementGrid movement_grid) {
        if (!EnemyData.isFlyer(this.type)) {
            int grid_orientation = movement_grid.getOrientation(this.gridx, this.gridy);
            if (grid_orientation >= 1 && grid_orientation <= 4) {
                this.orientation = grid_orientation;
            }
        }
        this.exiting_grid = true;
        // 切换方向时清零垂直方向的 offset
        if (this.orientation == 3 || this.orientation == 1) {
            this.y_offset = 0;
        } else {
            this.x_offset = 0;
        }
        // 检查是否到达出口
        this.at_exit = this.gridx == movement_grid.getEndX() && this.gridy == movement_grid.getEndY();
    }

    /**
     * 反转朝向
     */
    private void reverseOrientation() {
        this.orientation = getReverseOrientation(this.orientation);
        this.exiting_grid = !this.exiting_grid;
    }

    /**
     * 获取反向朝向
     */
    private static int getReverseOrientation(int orientation) {
        switch (orientation) {
            case 1: return 3;
            case 2: return 4;
            case 3: return 1;
            case 4: return 2;
            default: return orientation;
        }
    }

    // ========== Getter 方法 ==========

    public int getHealth() { return this.health; }
    public int getMaxHealth() { return this.max_health; }
    public int getPathNum() { return this.path_num; }
    public int getSlowCounter() { return this.slow_counter; }
    public int getFireCounter() { return this.fire_counter; }
    public void setFireCounter(int counter) { this.fire_counter = counter; }
    public int getFlameCount() {
        int count = 0;
        if (this.flame_props != null) {
            for (FlameProps fp : this.flame_props) {
                if (fp != null && fp.active) count++;
            }
        }
        return count;
    }
    public int getFlameFrame(int index) {
        if (this.flame_props != null && index < this.flame_props.length) {
            return this.flame_props[index].frame;
        }
        return -1;
    }
    public int getFlameX(int index) {
        if (this.flame_props != null && index < this.flame_props.length) {
            return this.flame_props[index].x;
        }
        return 0;
    }
    public int getFlameY(int index) {
        if (this.flame_props != null && index < this.flame_props.length) {
            return this.flame_props[index].y;
        }
        return 0;
    }
    public int getDeathFrame() { return this.death_frame; }
    public void setDeathFrame(int frame) { this.death_frame = frame; }

    public boolean finished() {
        return this.health <= 0 || this.at_exit;
    }

    public boolean atExit() {
        return this.at_exit;
    }

    public int getOrientation() {
        return this.orientation;
    }

    /**
     * 计算像素X坐标
     * x_offset 以 1/128 格为单位（GRID_SIZE=256），需要 >> 3 转换为像素
     */
    public int calcPixelX() {
        return (this.gridx * 32) + (this.x_offset >> 3);
    }

    /**
     * 计算像素Y坐标
     * y_offset 以 1/128 格为单位（GRID_SIZE=256），需要 >> 3 转换为像素
     */
    public int calcPixelY() {
        return (this.gridy * 32) + (this.y_offset >> 3);
    }

    @Override
    public int getClassType() {
        return 2; // 敌人类
    }

    @Override
    public void saveState(GameSaveWriter out) throws Exception {
        saveBaseState(out);
        out.writeInt(this.health);
        out.writeInt(this.max_health);
        out.writeInt(this.orientation);
        out.writeInt(this.path_num);
        out.writeInt(this.x_offset);
        out.writeInt(this.y_offset);
        out.writeInt(this.slow_counter);
        out.writeInt(this.fire_counter);
        out.writeInt(this.death_frame);
    }

    @Override
    public boolean loadState(GameSaveReader in, GameState game) throws Exception {
        boolean ok = loadBaseState(in, game);
        if (ok) {
            this.health = in.readInt();
            this.max_health = in.readInt();
            this.orientation = in.readInt();
            this.path_num = in.readInt();
            this.x_offset = in.readInt();
            this.y_offset = in.readInt();
            this.slow_counter = in.readInt();
            this.fire_counter = in.readInt();
            this.death_frame = in.readInt();
            this.at_exit = false;
            this.exiting_grid = false;
            flameInit();
        }
        return ok;
    }

    /**
     * 火焰粒子属性
     */
    private static class FlameProps {
        boolean active;
        int x, y;
        int frame;
        int length;

        void init(int len) {
            this.active = true;
            this.length = len;
            this.frame = 0;
            this.x = FastRandom.nextInt() % 20;
            this.y = FastRandom.nextInt() % 20;
        }

        void nextState() {
            if (this.active) {
                this.frame++;
                if (this.frame >= this.length) {
                    this.active = false;
                }
            }
        }
    }
}
