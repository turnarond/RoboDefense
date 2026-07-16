package com.rdefense.core.game;

/**
 * 游戏塔类 — 处理塔的逻辑、射击、目标选择
 * 对应原版 GameTower
 */
public final class GameTower extends GridObject {

    private static Vector vect;
    private static int mine_chain_length;  // 地雷链长度（用于冲击波计算）

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

        // 传送塔：将敌人传送回起点
        if (shot_type == BulletData.TELEPORT) {
            handleTeleport(game_state, this.active_enemy);
            return false;
        }

        // 地雷塔：链式爆炸 + 冲击波
        if (shot_type == BulletData.MINE) {
            mine_chain_length = 0;
            handleMine(game_state);
            // 地雷链 >= 10 触发成就（原版 achievement 57）
            if (mine_chain_length >= 10) {
                AchievementData.increaseLevel(AchievementData.CHAIN_SMOKER, 1);
            }
            if (mine_chain_length >= 2 && RewardData.rewardLevel(RewardData.SHOCKWAVE) > 0) {
                handleShockwave(game_state, mine_chain_length);
            }
            // 爆炸后设置冷却，防止每帧重复触发
            this.shot_delay = TowerData.shotDelay(this.type);
            return false;
        }

        // 发射普通子弹
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
     * 处理传送效果（完整复刻原版 handleTeleport）
     * 将目标敌人传送回路径起点，清除所有追踪该敌人的子弹
     * 若解锁扰频器（SCRAMBLER），额外造成 1/3 生命值的伤害
     */
    private void handleTeleport(GameState game_state, Enemy active_enemy) {
        // 传送前爆炸效果
        ExplosionData.addExplosion(game_state,
                active_enemy.calcPixelX(), active_enemy.calcPixelY(), 1);

        LevelData level_data = game_state.getLevelData();
        int path_num = active_enemy.getPathNum();
        active_enemy.teleportTo(
                level_data.getStartX(path_num),
                level_data.getStartY(path_num),
                level_data.getStartOrientation(path_num));

        // 扰频器：造成目标当前生命值 1/3 的伤害
        if (RewardData.rewardLevel(RewardData.SCRAMBLER) > 0) {
            int damage = active_enemy.getHealth() / 3;
            if (damage > 0) {
                active_enemy.applyDamage(damage, 2); // type 2 = GUN（普通伤害）
            }
        }

        // 清除所有追踪该敌人的子弹
        for (Bullet b = game_state.getBulletList(); b != null; b = b.next) {
            b.clearThisTarget(active_enemy);
        }

        // 传送后爆炸效果
        ExplosionData.addExplosion(game_state,
                active_enemy.calcPixelX(), active_enemy.calcPixelY(), 1);

        // 传送塔使用后自动降级为未激活状态
        this.type = 18;
        game_state.allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
    }

    /**
     * 处理地雷效果（完整复刻原版 handleMine + 链式爆炸）
     * 对范围内的敌人造成伤害，并触发相邻地雷的链式爆炸
     */
    private void handleMine(GameState game_state) {
        mine_chain_length++;

        // 对爆炸范围内的所有敌人造成伤害
        for (Enemy e = game_state.getEnemyList(); e != null; e = e.next) {
            boolean canHit = !EnemyData.isFlyer(e.getType())
                    || RewardData.rewardLevel(RewardData.AIR_BURST) > 0;
            if (canHit) {
                int xdelta = (this.gridx * 32) - e.calcPixelX();
                int ydelta = (this.gridy * 32) - e.calcPixelY();
                if ((xdelta * xdelta) + (ydelta * ydelta) <= BulletData.MINE_RADIUS_SQ) {
                    e.applyDamage(TowerData.power(this.type), TowerData.shotType(this.type));
                    ExplosionData.addExplosion(game_state,
                            e.calcPixelX(), e.calcPixelY(), 0);
                }
            }
        }

        // 地雷使用后降级为未触发状态
        this.type = 20;

        // 链式爆炸：引爆相邻（曼哈顿距离 ≤ 1）的已触发地雷
        for (GameTower gt = game_state.getTowerList(); gt != null; gt = gt.next) {
            if (gt != this && gt.getType() == 21) { // 21 = 已触发的地雷
                int xdelta2 = gt.getGridX() - this.gridx;
                int ydelta2 = gt.getGridY() - this.gridy;
                if (xdelta2 <= 1 && xdelta2 >= -1 && ydelta2 <= 1 && ydelta2 >= -1) {
                    ExplosionData.addExplosion(game_state,
                            gt.getGridX() * 32,
                            gt.getGridY() * 32, 0);
                    gt.handleMine(game_state);
                }
            }
        }

        game_state.allocateGameEvent(GameEvent.EVENT_TOWERS_CHANGED);
    }

    /**
     * 处理冲击波效果（地雷链触发后，对全体敌人施加减速和灼烧）
     */
    private void handleShockwave(GameState game_state, int chainLength) {
        int effectiveLength = chainLength - 1;
        int fireAmount = (TowerData.power(21) * effectiveLength) / 4;

        // 对全体敌人施加减速和灼烧
        for (Enemy e = game_state.getEnemyList(); e != null; e = e.next) {
            e.applyDamage(effectiveLength * 30, BulletData.SLOW);  // 减速
            e.setFireCounter(fireAmount);                           // 直接设置灼烧值
        }

        // 冲击波视觉特效（全屏闪烁）
        int fade_length = (effectiveLength * 2) + 3;
        if (fade_length > 15) {
            fade_length = 15;
        }
        GameEvent e2 = game_state.allocateGameEvent(GameEvent.EVENT_PARTICLE);
        e2.var[GameEvent.VAR_PARTICLE_X] = 0;
        e2.var[GameEvent.VAR_PARTICLE_Y] = 0;
        e2.var[GameEvent.VAR_PARTICLE_XVEL] = 0;
        e2.var[GameEvent.VAR_PARTICLE_YVEL] = 0;
        e2.var[GameEvent.VAR_PARTICLE_COLOR] = -1;
        e2.var[GameEvent.VAR_PARTICLE_MAX_ALPHA] = 180;
        e2.var[GameEvent.VAR_PARTICLE_SIZE] = 10000;
        e2.var[GameEvent.VAR_PARTICLE_FIRST_STATE] = game_state.getStateIndex();
        e2.var[GameEvent.VAR_PARTICLE_NUM_STATES] = fade_length;
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
