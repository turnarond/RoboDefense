package com.rdefense.core.game;

/**
 * 游戏塔类 — 处理塔的逻辑、射击、目标选择
 * 对应原版 GameTower
 */
public final class GameTower extends GridObject {

    private static Vector vect;

    // 塔状态
    Enemy active_enemy;
    int shot_delay;
    int last_direction;

    // 碰撞检测范围
    private int cbottom;
    private int cleft;
    private int cright;
    private int ctop;

    // 链表
    public GameTower next;

    /**
     * 初始化塔
     */
    public void init(CollisionGrid collision_grid, int gridx, int gridy, int type, int first_state) {
        if (vect == null) {
            vect = new Vector();
        }
        init_base(gridx, gridy, type, 1, first_state);
        System.out.println("[TowerDebug] GameTower.init stored grid=(" + gridx + "," + gridy + ") type=" + type);
        this.shot_delay = TowerData.shotDelay(type) / 2;
        this.last_direction = 270;
        collision_grid.setupGrid(this);
    }

    /**
     * 更新塔状态
     * @return true 表示发射了子弹
     */
    public boolean nextState(GameState game_state, CollisionGrid collision_grid, Bullet bullet) {
        int shot_type = TowerData.shotType(this.type);
        this.shot_delay--;

        if (this.shot_delay > 0) {
            return false;
        }

        if (shot_type == 0) {
            this.shot_delay = TowerData.shotDelay(this.type);
            return false;
        }

        // 选择目标
        chooseTarget(collision_grid);
        if (this.active_enemy == null) {
            this.shot_delay = 0;
            return false;
        }

        // 特殊塔类型处理（传送塔、地雷塔等需要单独实现）
        // 注意：shot_type == 1 是机枪子弹（GUN），不是传送塔
        // 传送塔和地雷塔有各自独立的 shot_type 常量

        // 发射子弹
        this.shot_delay = TowerData.shotDelay(this.type);
        int x = (this.gridx * 32) + 16; // 格子中心 X
        // 发射位置 Y：与 Android 原版一致，使用格子中心减去 gunHeight
        int y = (this.gridy * 32) + 16 - TowerData.gunHeight(this.type);

        vect.x = (this.active_enemy.calcPixelX() + 16) - x;
        vect.y = (this.active_enemy.calcPixelY() + 16) - y;
        VectorLookup.scaleVector(vect, TowerData.gunRadius(this.type));
        vect.y = (vect.y * 3) / 4;

        // 播放发射音效
        com.rdefense.core.audio.SoundManager.getInstance().fireBullet(shot_type);

        bullet.init(x + vect.x, y + vect.y, this.active_enemy, shot_type, TowerData.power(this.type));
        return true;
    }

    /**
     * 选择目标
     */
    private void chooseTarget(CollisionGrid collision_grid) {
        if (!validTarget(this.active_enemy)) {
            this.active_enemy = null;
            int oldest_state = Integer.MAX_VALUE;

            for (int y = this.ctop; y <= this.cbottom; y++) {
                for (int x = this.cleft; x <= this.cright; x++) {
                    for (Enemy e = collision_grid.getList(x, y); e != null; e = e.grid_next) {
                        if (validTarget(e) && e.getFirstState() < oldest_state) {
                            this.active_enemy = e;
                            oldest_state = e.getFirstState();
                        }
                    }
                }
            }
        }
    }

    /**
     * 验证目标是否有效
     */
    private boolean validTarget(Enemy e) {
        if (e == null || e.finished()) {
            return false;
        }
        // 检查子弹类型是否匹配目标
        int shotType = TowerData.shotType(this.type);
        switch (shotType) {
            case 4: // 减速弹
                if (e.getSlowCounter() > 0) return false;
                break;
            case 6: // 火焰弹
            case 14: // 冲击波
                if (EnemyData.isFlyer(e.getType())) return false;
                break;
            case 7: // 对空弹
            case 8: // 火箭
                if (!EnemyData.isFlyer(e.getType())) return false;
                break;
            case 9: // 地雷
            case 10: // 火焰
            case 13: // 激光
                if (EnemyData.isFlyer(e.getType())) return false;
                break;
        }
        // 检查实际像素距离是否小于攻击半径
        // 原版使用格子左上角作为基准：(gridx * GRID_PIXEL_SIZE)
        // 因为 TowerData.attackRadiusSq 也是基于格子坐标计算的
        int xdelta = (this.gridx * 32) - e.calcPixelX();
        int ydelta = (this.gridy * 32) - e.calcPixelY();
        int distSq = (xdelta * xdelta) + (ydelta * ydelta);
        int radiusSq = TowerData.attackRadiusSq(this.type);
        return distSq < radiusSq;
    }

    /**
     * 处理传送效果
     */
    private void handleTeleport(GameState game_state, Enemy target) {
        // 简化版：传送到路径起点
        target.teleportTo(
                game_state.getLevelData().getStartX(target.getPathNum()),
                game_state.getLevelData().getStartY(target.getPathNum()),
                game_state.getLevelData().getStartOrientation(target.getPathNum())
        );
    }

    /**
     * 处理地雷效果
     */
    private void handleMine(GameState game_state) {
        // 简化版：对周围敌人造成伤害
        int range = 2;
        for (int dy = -range; dy <= range; dy++) {
            for (int dx = -range; dx <= range; dx++) {
                int cx = this.gridx + dx;
                int cy = this.gridy + dy;
                if (game_state.inBounds(cx, cy)) {
                    Enemy e = game_state.findEnemyAt(cx, cy);
                    if (e != null) {
                        e.applyDamage(TowerData.power(this.type), TowerData.shotType(this.type));
                    }
                }
            }
        }
    }

    /**
     * 设置碰撞范围
     */
    public void setCollisionRange(int left, int top, int right, int bottom) {
        this.cleft = left;
        this.ctop = top;
        this.cright = right;
        this.cbottom = bottom;
    }

    /**
     * 获取方向（炮口朝向当前目标敌人）
     * 原版逻辑：每次调用都重新计算到 active_enemy 的角度
     * 注意：精灵图帧排列是逆时针的，需要反转角度
     */
    public int getDirection() {
        if (this.active_enemy != null) {
            int xdelta = (this.gridx * 32) - this.active_enemy.calcPixelX();
            int ydelta = (this.gridy * 32) - this.active_enemy.calcPixelY();
            int rawAngle = Vector.arctan(xdelta, ydelta);
            // 上下翻转：将角度沿垂直轴镜像
            this.last_direction = (180 - rawAngle + 360) % 360;
        }
        return this.last_direction;
    }

    /**
     * 设置方向
     */
    public void setDirection(int dir) {
        this.last_direction = dir;
    }

    @Override
    public int getClassType() {
        return 1; // 塔类
    }

    @Override
    public void saveState(GameSaveWriter out) throws Exception {
        saveBaseState(out);
        out.writeInt(this.shot_delay);
    }

    @Override
    public boolean loadState(GameSaveReader in, GameState game) throws Exception {
        boolean ok = loadBaseState(in, game);
        if (ok) {
            this.shot_delay = in.readInt();
            if (getClassType() != 1) {
                ok = false;
            } else if (this.shot_delay < 0 || this.shot_delay > 10000) {
                ok = false;
            }
            this.active_enemy = null;
        }
        return ok;
    }
}
