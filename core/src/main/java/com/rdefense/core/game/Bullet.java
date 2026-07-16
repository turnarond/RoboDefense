package com.rdefense.core.game;

/**
 * 子弹类 — 处理子弹移动、碰撞、伤害
 * 对应原版 Bullet
 */
public class Bullet {

    // 常量
    private static final int MORTAR_FRAMES = 15;
    private static final int SMOKE_MAX_ALPHA = 80;
    private static final int SMOKE_NUM_STATES = 10;
    private static final int SMOKE_SIZE = 3;
    private static final int SMOKE_SLOWDOWN_THRESHOLD = 30;
    private static final int Z_ACCEL = 4; // 重力加速度（正值=向下）
    private static final int Z_SCALE = 250;

    // 子弹状态
    private int x;
    private int y;
    private int z; // Z轴高度（用于抛物线子弹）
    private int z_vel; // Z轴速度

    private int targetx;
    private int targety;
    private int targetx_offset;
    private int targety_offset;

    private Enemy target;
    private int shot_type;
    private int shot_power;
    private int speed;
    private boolean first_frame;

    // 链表
    public Bullet next;

    // 方向向量
    private final Vector vect = new Vector();

    /**
     * 初始化子弹
     * @param x 起始X坐标
     * @param y 起始Y坐标
     * @param target 目标敌人
     * @param shot_type 子弹类型
     * @param shot_power 伤害值
     */
    public void init(int x, int y, Enemy target, int shot_type, int shot_power) {
        int type = target.getType();
        this.x = x;
        this.y = y;
        this.z = 0;
        this.targetx_offset = FastRandom.nextInt() % (EnemyData.imageVisibleWidth(type) / 2);
        this.targety_offset = FastRandom.nextInt() % (EnemyData.imageVisibleHeight(type) / 2);
        this.targetx = target.calcPixelX() + EnemyData.imageCenterX(type) +
                EnemyData.drawShiftX(type) + this.targetx_offset;
        this.targety = target.calcPixelY() + EnemyData.imageCenterY(type) +
                EnemyData.drawShiftY(type) + this.targety_offset;
        this.target = target;
        this.shot_type = shot_type;
        this.shot_power = shot_power;
        this.speed = BulletData.speed(shot_type);
        this.first_frame = true;

        // 抛物线子弹（迫击炮、火炮、凝固汽油弹）需要计算Z轴速度
        if (shot_type == BulletData.MORTAR || shot_type == BulletData.ARTILLERY || shot_type == BulletData.NAPALM_SHELL) {
            calcInitialZVelocity();
        }
    }

    /**
     * 更新子弹状态
     * @return true 表示子弹命中或消失
     */
    public boolean nextState(int state_index, GameState game_state) {
        // 更新目标位置
        if (this.target != null) {
            if (this.target.finished()) {
                this.target = null;
            } else {
                int type = this.target.getType();
                this.targetx = this.target.calcPixelX() + EnemyData.imageCenterX(type) +
                        EnemyData.drawShiftX(type) + this.targetx_offset;
                this.targety = this.target.calcPixelY() + EnemyData.imageCenterY(type) +
                        EnemyData.drawShiftY(type) + this.targety_offset;
            }
        }

        int this_speed = this.speed;
        if (this.first_frame) {
            this_speed = state_index % this_speed;
            this.first_frame = false;
        }

        // 处理特殊子弹类型
        switch (this.shot_type) {
            case BulletData.ROCKET:     // 火箭，烟尾
            case BulletData.SURFAIR:    // 导弹，烟尾
                // 添加烟尾粒子
                if (game_state.activeEventCount() < 400 &&
                        (game_state.activeEventCount() < PerformanceMonitor.maxSmokeParticles() || (state_index & 3) == 0)) {
                    addSmokeParticle(game_state, state_index);
                }
                break;
            case BulletData.MORTAR:     // 迫击炮，抛物线
            case BulletData.ARTILLERY:  // 火炮，抛物线
            case BulletData.NAPALM_SHELL: // 凝固汽油弹，抛物线
                // 抛物线运动
                this.z += this.z_vel;
                this.z_vel -= Z_ACCEL;
                if (this.z < 0) {
                    this.z = 0;
                    this.speed = 100;
                }
                this_speed = this.speed;
                break;
        }

        // 朝目标移动
        this.vect.x = this.targetx - this.x;
        this.vect.y = this.targety - this.y;
        VectorLookup.scaleVector(this.vect, this_speed);
        this.x += this.vect.x;
        this.y += this.vect.y;

        // 检查是否命中
        boolean hit_target = (this.vect.x > 0 && this.x >= this.targetx) ||
                (this.vect.x < 0 && this.x <= this.targetx) ||
                (this.vect.y > 0 && this.y >= this.targety) ||
                (this.vect.y < 0 && this.y <= this.targety);

        if (hit_target) {
            // 对目标造成伤害
            if (this.target != null) {
                this.target.applyDamage(this.shot_power, this.shot_type);

                // 爆炸子弹添加爆炸效果
                if (this.shot_type == BulletData.ROCKET || this.shot_type == BulletData.SURFAIR) {
                    addExplosion(game_state, state_index);
                }
            }

            // 溅射伤害（迫击炮、火炮、凝固汽油弹）
            if (this.shot_type == BulletData.MORTAR || this.shot_type == BulletData.ARTILLERY || this.shot_type == BulletData.NAPALM_SHELL) {
                addExplosion(game_state, state_index);
                int splash_radius_sq = BulletData.SPLASH_RADIUS_SQ;

                for (Enemy e = game_state.getEnemyList(); e != null; e = e.next) {
                    int xdelta = this.x - e.calcPixelX();
                    int ydelta = this.y - e.calcPixelY();
                    if ((xdelta * xdelta) + (ydelta * ydelta) < splash_radius_sq) {
                        e.applyDamage(this.shot_power / 2, this.shot_type);
                    }
                }
            }
        }

        // 子弹消失条件
        return hit_target || (this.target == null && this.vect.x == 0 && this.vect.y == 0);
    }

    public int getType() {
        return this.shot_type;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y - this.z;
    }

    public Vector getDirection() {
        return this.vect;
    }

    /**
     * 获取子弹方向图像名称
     */
    public String getDirectionImageName() {
        int num_images = BulletData.getNumImages(this.shot_type);
        if (num_images == 1) {
            return BulletData.getDirectionImageName(this.shot_type, 0);
        }
        if (num_images <= 1) {
            return null;
        }
        return BulletData.getDirectionImageName(this.shot_type, Vector.arctan(this.vect));
    }

    public void clearThisTarget(Enemy active_target) {
        if (this.target == active_target) {
            this.target = null;
        }
    }

    /**
     * 添加烟尾粒子效果
     */
    private void addSmokeParticle(GameState game_state, int state_index) {
        int smoke_size = (state_index & 3) + SMOKE_SIZE;
        GameEvent e = game_state.allocateGameEvent(GameEvent.EVENT_PARTICLE);
        e.var[GameEvent.VAR_PARTICLE_X] = (this.x - smoke_size) << 3;
        e.var[GameEvent.VAR_PARTICLE_Y] = (this.y - smoke_size) << 3;
        e.var[GameEvent.VAR_PARTICLE_XVEL] = (-this.vect.x) * 2;
        e.var[GameEvent.VAR_PARTICLE_YVEL] = (-this.vect.y) * 2;
        e.var[GameEvent.VAR_PARTICLE_COLOR] = 0;
        e.var[GameEvent.VAR_PARTICLE_MAX_ALPHA] = SMOKE_MAX_ALPHA;
        e.var[GameEvent.VAR_PARTICLE_SIZE] = smoke_size * 2;
        e.var[GameEvent.VAR_PARTICLE_FIRST_STATE] = state_index;
        e.var[GameEvent.VAR_PARTICLE_NUM_STATES] = SMOKE_NUM_STATES;
    }

    /**
     * 添加爆炸效果
     */
    private void addExplosion(GameState game_state, int state_index) {
        int explosion_size = ExplosionData.size(0) >> 1;
        ExplosionData.addExplosion(game_state,
                this.targetx - explosion_size,
                this.targety - explosion_size, 0);
    }

    /**
     * 计算初始Z轴速度（用于抛物线子弹）
     */
    private void calcInitialZVelocity() {
        int dx = (this.targetx - this.x) / MORTAR_FRAMES;
        int dy = (this.targety - this.y) / MORTAR_FRAMES;
        this.speed = (int) Math.sqrt((dx * dx) + (dy * dy));
        if (this.speed < 1) {
            this.speed = 1;
        }
        this.z_vel = 28;
    }

    /**
     * 获取子弹大小
     */
    public int getSize(int state_index) {
        int size = BulletData.size(this.shot_type);
        switch (this.shot_type) {
            case BulletData.FIRE:       // 火焰弹
            case BulletData.SLOW_FIRE:  // 缓慢火焰弹
                return state_index & 7;
            default:
                return size;
        }
    }

    /**
     * 获取子弹颜色
     */
    public int getColor() {
        return BulletData.color(this.shot_type);
    }

    /**
     * 获取子弹方向帧数（用于多帧精灵图）
     */
    public int getDirectionCount() {
        return BulletData.getNumImages(this.shot_type);
    }

    /**
     * 获取子弹当前方向索引（用于多帧精灵图）
     */
    public int getDirectionIndex() {
        int numImages = BulletData.getNumImages(this.shot_type);
        if (numImages <= 1) {
            return 0;
        }
        // 根据子弹方向向量计算方向索引
        int angle = Vector.arctan(this.vect);
        // 与 GameTower.getDirection() 一致：精灵图需要上下翻转
        angle = (180 - angle + 360) % 360;
        return (numImages * angle) / 360;
    }
}
