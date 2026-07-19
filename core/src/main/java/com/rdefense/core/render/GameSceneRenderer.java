package com.rdefense.core.render;

import com.rdefense.core.config.OptionsData;
import com.rdefense.core.game.Bullet;
import com.rdefense.core.game.BulletData;
import com.rdefense.core.game.Enemy;
import com.rdefense.core.game.EnemyData;
import com.rdefense.core.game.GameEvent;
import com.rdefense.core.game.GameState;
import com.rdefense.core.game.GameTower;
import com.rdefense.core.game.Starfield;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.scene.GameScreen;

/**
 * 游戏场景渲染器 — 从 GamePlayScreen 提取的渲染逻辑
 * 负责绘制地图背景、塔、敌人、子弹、UI 元素等
 *
 * 批次策略：全局缩减为 renderWorldLayer / renderScreenLayer 两对 begin/end，
 * 覆盖层（暂停/胜利/失败/升级对话框）由 GamePlayScreen 条件调用自持批次。
 */
public class GameSceneRenderer {

    public static final int GRID_PIXEL_SIZE = 32;

    private final GameRenderer renderer;
    private final CameraManager camera;
    private final UiRenderer uiRenderer;
    private OptionsData options;

    // HUD 数字滚动动画（渐进逼近目标值）
    private int displayMoney = -1;
    private int displayScore = -1;
    private int displayHealth = -1;

    // 塔按钮脏标记（金钱或激活塔变化时才重绘按钮区域）
    private int lastMoney = -1;
    private int lastActiveTowerId = -2;

    public GameSceneRenderer(GameRenderer renderer, CameraManager camera, UiRenderer uiRenderer) {
        this.renderer = renderer;
        this.camera = camera;
        this.uiRenderer = uiRenderer;
    }

    public void setOptions(OptionsData options) {
        this.options = options;
    }

    public int getDisplayMoney() { return displayMoney; }
    public int getDisplayScore() { return displayScore; }
    public int getDisplayHealth() { return displayHealth; }

    /** HUD 数字滚动动画：渐进逼近 money/score/health 目标值 */
    public void updateHudAnimations(GameState gameState) {
        int targetMoney = gameState.getMoney();
        if (displayMoney < 0) displayMoney = targetMoney;
        else if (displayMoney != targetMoney) {
            int step = Math.max(1, Math.abs(targetMoney - displayMoney) / 6);
            if (displayMoney < targetMoney) displayMoney = Math.min(displayMoney + step, targetMoney);
            else displayMoney = Math.max(displayMoney - step, targetMoney);
        }
        int targetScore = gameState.getScore();
        if (displayScore < 0) displayScore = targetScore;
        else if (displayScore != targetScore) {
            int step = Math.max(1, Math.abs(targetScore - displayScore) / 6);
            if (displayScore < targetScore) displayScore = Math.min(displayScore + step, targetScore);
            else displayScore = Math.max(displayScore - step, targetScore);
        }
        int targetHP = gameState.getHealth();
        if (displayHealth < 0) displayHealth = targetHP;
        else if (displayHealth != targetHP) {
            displayHealth = displayHealth + (targetHP - displayHealth > 0 ? 1 : -1);
        }
    }

    // ============================================================
    // World layer（单次 begin/end，世界坐标）
    // ============================================================

    /**
     * 绘制世界坐标层（单次 begin/end）
     * 对应原版各 draw* 步骤 1-6：背景、塔、敌人、子弹、击败事件、塔预览
     */
    public void renderWorldLayer(GameState gameState, int stateIndex) {
        int levelType = gameState.getLevelData().getLevelType();
        renderer.applyCameraTransform(camera.getXBase(), camera.getYBase(), camera.getScale());
        renderer.begin();
        drawBackground(levelType, gameState);
        drawTowers(gameState, stateIndex);
        drawEnemies(stateIndex, gameState);
        drawBullets(stateIndex, gameState);
        drawEnemyDefeatedEvents(stateIndex, gameState);
        drawActiveTowerPreview(gameState, stateIndex);
        renderer.end();
    }

    // ============================================================
    // Screen layer（单次 begin/end，屏幕坐标）
    // ============================================================

    /**
     * 绘制屏幕坐标层（单次 begin/end）
     * 对应 HUD + 塔按钮 + 控制按钮 + 缩放滑块 + 塔精灵预览
     */
    public void renderScreenLayer(GameState gameState, int stateIndex, int money, OptionsData options,
                                  int currentFps, int batteryLevel) {
        renderer.applyCameraTransform(0, 0, 1.0f);

        // HUD（自持 batch)
        drawHud(gameState, currentFps, batteryLevel);

        // 塔按钮与精灵（自持 batch，含脏标记）
        drawTowerButtons(money, stateIndex);
        drawTowerButtonSprites();

        // UI 组件（各自管理 begin/end，不可包在外部 batch 内）
        uiRenderer.renderControlButtons(gameState.getRunState());
        if (options != null) uiRenderer.renderScaleSlider(options, camera);
    }

    // ============================================================
    // 世界坐标绘制子方法（无 begin/end，无相机变换）
    // ============================================================

    /**
     * 绘制地图背景图
     * 原版逻辑：redrawBackground 在 (0,0) 绘制，但 Canvas 已应用了 matrix 变换
     * 这里先应用相机变换，然后在世界坐标原点绘制地图
     */
    private void drawBackground(int levelType, GameState gameState) {
        boolean useClassic = options != null && options.optionValue(OptionsData.CLASSIC_BACKGROUNDS);
        String bgName = useClassic
                ? SpriteNames.levelBackgroundClassic(levelType)
                : SpriteNames.levelBackground(levelType);
        // 使用地图实际宽高（世界坐标）
        int worldW = gameState.getLevelData().getGridWidth() * GRID_PIXEL_SIZE;
        int worldH = gameState.getLevelData().getGridHeight() * GRID_PIXEL_SIZE;

        // 在世界坐标原点绘制地图（会被相机变换映射到正确位置）
        renderer.drawSprite(bgName, 0, 0, worldW, worldH);

        // 星空粒子背景（公路/宇宙关卡）
        Starfield starfield = gameState.getLevelData().getStarfield();
        if (starfield != null) {
            starfield.update();
            starfield.draw(renderer, 0, 0);
        }
    }

    /**
     * 绘制所有塔（精灵图）
     * 原版渲染流程：
     * 1. 背景层：绘制 images[0] 底座
     * 2. 前景层：绘制 images[frameIndex] 转头
     */
    private void drawTowers(GameState gameState, int stateIndex) {
        for (GameTower t = gameState.getTowerList(); t != null; t = t.next) {
            int type = t.getType();
            int direction = t.getDirection();
            int animFrame = stateIndex >> 1;
            int frameIndex = TowerData.getDirectionFrameIndex(type, direction, animFrame);
            int totalFrames = TowerData.getTotalFrames(type);
            String spriteSheetName = SpriteNames.tower(type);

            float wx = t.getGridX() * GRID_PIXEL_SIZE;
            float wy = t.getGridY() * GRID_PIXEL_SIZE;

            // 塔 = 一个格子，统一使用 GRID_PIXEL_SIZE（32×32 世界像素）
            float towerSize = GRID_PIXEL_SIZE;

            // 计算炮塔转头世界 Y（转头在底座上方 towerHeight 像素）
            int towerHeight = TowerData.towerHeight(type);
            float turretWorldY = wy + towerHeight;

            // 1. 绘制底座（帧 0）
            renderer.drawSpriteFrame(spriteSheetName, 0, totalFrames, wx, wy, towerSize, towerSize);

            // 2. 绘制转头（帧 frameIndex）
            renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, turretWorldY, towerSize, towerSize);
        }
    }

    /**
     * 绘制所有敌人（精灵图 + 状态效果 + 血条）
     * 使用世界坐标直接绘制
     */
    private void drawEnemies(int stateIndex, GameState gameState) {
        for (Enemy e = gameState.getEnemyList(); e != null; e = e.next) {
            int wx = e.calcPixelX();
            int wy = e.calcPixelY();
            float size = GRID_PIXEL_SIZE;

            String spriteSheetName = SpriteNames.enemy(e.getType());
            int totalFrames = EnemyData.getTotalFrames(e.getType());
            // 动画帧计算：原版逻辑 (ge.getFirstState() + state_index) >> 0
            int animationFrame = (e.getFirstState() + stateIndex);
            // 减速时动画减半
            if (e.getSlowCounter() > 0) {
                animationFrame >>= 1;
            }
            int frameIndex = EnemyData.getAnimationFrameIndex(e.getType(), e.getOrientation(), animationFrame);

            int slowCounter = e.getSlowCounter();
            int fireCounter = e.getFireCounter();

            if (slowCounter > 0 && fireCounter > 0) {
                renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 0.6f, 0.4f, 0.9f, 1.0f);
            } else if (fireCounter > 0) {
                renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 1.0f, 0.5f, 0.1f, 1.0f);
            } else if (slowCounter > 0) {
                renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 0.4f, 0.7f, 1.0f, 1.0f);
            } else {
                renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size);
            }

            // 血条（未满血时显示）
            int health = e.getHealth();
            int maxHealth = e.getMaxHealth();
            if (health > 0 && health < maxHealth) {
                float barH = 2f;
                float healthRatio = (float) health / maxHealth;
                // 原版血条位置：在敌人绘制区域内部，距离顶部 1 像素
                // X 位置有 energyBarOffset 偏移（默认 3 像素）
                int barOffsetX = EnemyData.energyBarOffset(e.getType());
                float barX = wx + barOffsetX;
                float barY = wy + 1;
                // 血条总宽度 = 敌人尺寸 - 左右边距
                float barW = size - (barOffsetX * 2);
                renderer.drawRect(barX, barY, barW, barH, 0.8f, 0.0f, 0.0f, 0.9f);
                renderer.drawRect(barX, barY, barW * healthRatio, barH, 0.0f, 0.9f, 0.0f, 0.9f);
            }
        }
    }

    /**
     * 绘制所有子弹（精灵图）
     * 使用世界坐标直接绘制
     */
    private void drawBullets(int stateIndex, GameState gameState) {
        for (Bullet b = gameState.getBulletList(); b != null; b = b.next) {
            int size = b.getSize(stateIndex);
            float wx = b.getX() - size / 2f;
            float wy = b.getY() - size / 2f;

            String imgName = SpriteNames.bullet(b.getType());
            if (imgName != null) {
                int totalFrames = BulletData.getNumImages(b.getType());
                if (totalFrames > 1) {
                    int directionIndex = b.getDirectionIndex();
                    renderer.drawSpriteFrame(imgName, directionIndex, totalFrames, wx, wy, size, size);
                } else {
                    renderer.drawSprite(imgName, wx, wy, size, size);
                }
            } else {
                int color = BulletData.color(b.getType());
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float bl = (color & 0xFF) / 255.0f;
                renderer.drawRect(wx, wy, size, size, r, g, bl, 1.0f);
            }
        }
    }

    /**
     * 绘制敌人击败事件（金钱奖励和分数飘字）
     * 参考 Android 原版 Display.handleEnemyDefeatedMoneyAdd
     */
    private void drawEnemyDefeatedEvents(int stateIndex, GameState gameState) {
        for (GameEvent e = gameState.getGameEventList(GameEvent.EVENT_ENEMY_DEFEATED); e != null; e = e.next) {
            int dist = stateIndex - e.var[GameEvent.VAR_ENEMY_STATE_IDX];
            if (dist < 0 || dist >= 10) {
                e.finished = true;
                continue;
            }

            int xpos = e.var[GameEvent.VAR_ENEMY_PIXEL_X] + 5;
            int ypos = e.var[GameEvent.VAR_ENEMY_PIXEL_Y] + GRID_PIXEL_SIZE / 2 - dist;

            // 显示金钱奖励 ($X)
            int enemyType = e.var[GameEvent.VAR_ENEMY_TYPE];
            int moneyAmount = EnemyData.value(enemyType);
            String moneyString = moneyAmount > 100 ? ">$100" : "$" + moneyAmount;

            renderer.drawText(moneyString, xpos + 1, ypos + 1, 0.0f, 0.0f, 0.0f, 0.8f);
            renderer.drawText(moneyString, xpos, ypos, 1.0f, 1.0f, 0.0f, 1.0f);

            // 显示分数奖励 (+XXX)
            int scoreAdd = e.var[GameEvent.VAR_ENEMY_FULL_SCORE];
            String scoreString = "+" + scoreAdd;

            renderer.drawText(scoreString, xpos + 1, ypos - 12 + 1, 0.0f, 0.0f, 0.0f, 0.8f);
            renderer.drawText(scoreString, xpos, ypos - 12, 0.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    /**
     * 绘制激活塔预览（放置前显示范围圈和半透明塔）
     */
    private void drawActiveTowerPreview(GameState gameState, int stateIndex) {
        int activeTowerId = uiRenderer.getActiveTowerId();
        if (activeTowerId < 0) return;

        // 使用输入事件中保存的网格坐标
        int gridX = uiRenderer.getActiveTowerGridX();
        int gridY = uiRenderer.getActiveTowerGridY();

        // 检查放置是否有效
        boolean valid = gameState.checkTowerPlacement(gridX, gridY, activeTowerId, false)
                && TowerData.cost(activeTowerId) <= gameState.getMoney();

        // 绘制攻击范围圈（原版使用攻击半径，单位是世界像素）
        float worldCenterX = gridX * GRID_PIXEL_SIZE + GRID_PIXEL_SIZE / 2f;
        float worldCenterY = gridY * GRID_PIXEL_SIZE + GRID_PIXEL_SIZE / 2f;
        int attackRadius = TowerData.attackRadius(activeTowerId);
        if (options == null || options.optionValue(OptionsData.SHOW_TURRET_RANGE)) {
            renderer.drawCircle(worldCenterX, worldCenterY, attackRadius,
                    0.5f, 0.5f, 1.0f, 0.3f);
        }

        // 绘制塔预览方块（原版使用 valid/invalid 颜色）
        // ShapeRenderer.rect 以左下角为锚点，与 drawSpriteFrame 保持一致
        float previewAlpha = 0.5f;
        float validR = valid ? 0.2f : 0.8f;
        float validG = valid ? 0.6f : 0.2f;
        float validB = valid ? 0.2f : 0.2f;
        float rectX = gridX * GRID_PIXEL_SIZE;
        float rectY = gridY * GRID_PIXEL_SIZE;
        renderer.drawRect(rectX, rectY,
                GRID_PIXEL_SIZE, GRID_PIXEL_SIZE,
                validR, validG, validB, previewAlpha);

        // 绘制塔精灵预览（原版绘制 tower image + direction image）
        String spriteSheetName = SpriteNames.tower(activeTowerId);
        int totalFrames = TowerData.getTotalFrames(activeTowerId);
        int animFrame = stateIndex >> 1;
        int frameIndex = TowerData.getDirectionFrameIndex(activeTowerId, 270, animFrame);
        int towerHeight = TowerData.towerHeight(activeTowerId);

        float baseX = gridX * GRID_PIXEL_SIZE;
        float baseY = gridY * GRID_PIXEL_SIZE;
        // Android 原版（左上角锚点）：
        //   底座: drawBitmap(img, gridX*GRID, gridY*GRID) - 左上角
        //   转头: drawBitmap(img, gridX*GRID, gridY*GRID - towerHeight) - 左上角
        // 在 libGDX 中（左下角锚点）：
        //   底座底部 = gridY * GRID_PIXEL_SIZE
        //   转头底部 = 底座底部 + towerHeight（转头与底座重叠 towerHeight 像素）
        float turretY = baseY + towerHeight;

        // 塔 = 一个格子，统一使用 GRID_PIXEL_SIZE
        float towerSize = GRID_PIXEL_SIZE;

        // 绘制底座 + 转头（与原版渲染一致）
        renderer.drawSpriteFrame(spriteSheetName, 0, totalFrames,
                baseX, baseY,
                towerSize, towerSize,
                1.0f, 1.0f, 1.0f, 0.7f);
        renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames,
                baseX, turretY,
                towerSize, towerSize,
                1.0f, 1.0f, 1.0f, 0.7f);
    }

    // ============================================================
    // 屏幕坐标绘制子方法（无 begin/end）
    // ============================================================

    /**
     * 绘制 HUD 信息栏
     * HUD 使用屏幕坐标，相机变换已在 renderScreenLayer 中设置
     */
    private void drawHud(GameState gameState, int currentFps, int batteryLevel) {
        renderer.begin();
        int screenW = renderer.getScreenWidth();
        int screenH = renderer.getScreenHeight();
        int runState = gameState.getRunState();

        // 顶部信息栏背景（科幻面板风格）
        renderer.drawRect(0, screenH - 34, screenW, 34, 0.06f, 0.08f, 0.16f, 0.93f);
        renderer.drawRect(0, screenH - 1, screenW, 2, 0.2f, 0.36f, 0.55f, 0.85f);
        renderer.drawRect(0, screenH - 34, screenW, 1, 0.12f, 0.24f, 0.4f, 0.6f);

        float yTop = screenH - 18;
        float xStart = 12;

        renderer.drawText("L:" + gameState.getLevelData().getLevelNum(), xStart, yTop,
                0.9f, 0.95f, 1.0f, 1.0f);
        renderer.drawText("$" + GameScreen.formatWithCommas(displayMoney), xStart + 80, yTop,
                0.3f, 1.0f, 0.3f, 1.0f);
        // 生命值 + 血量条
        int hp = displayHealth;
        renderer.drawText("HP " + hp, xStart + 200, yTop,
                0.15f, 0.92f, 0.28f, 1.0f);
        int hpMax = gameState.getStartingHealth();
        float hpRatio = Math.min(1.0f, (float)hp / Math.max(1, hpMax));
        renderer.drawRect(xStart + 242, screenH - 16, 40, 4, 0.35f, 0.12f, 0.12f, 0.7f);
        renderer.drawRect(xStart + 242, screenH - 16, 40 * hpRatio, 4, 0.1f, 0.85f, 0.22f, 0.9f);

        // 分数显示：处理累计动画
        int score = displayScore;
        int pendingScore = 0;

        // 从敌人击败事件中获取待添加的分数
        for (GameEvent e = gameState.getGameEventList(GameEvent.EVENT_ENEMY_DEFEATED); e != null; e = e.next) {
            if (e.var[GameEvent.VAR_ENEMY_FULL_SCORE] > 0) {
                pendingScore += e.var[GameEvent.VAR_ENEMY_FULL_SCORE];
                e.var[GameEvent.VAR_ENEMY_FULL_SCORE] = 0; // 标记已处理
            }
        }

        // 显示分数
        String scoreText;
        if (score >= 1000) {
            scoreText = "PTS:" + GameScreen.formatWithCommas(score);
        } else {
            scoreText = "PTS:" + score;
        }
        if (pendingScore > 0) {
            scoreText += " +" + pendingScore;
        }
        renderer.drawText(scoreText, xStart + 320, yTop,
                1.0f, 1.0f, 0.3f, 1.0f);

        String stateStr;
        float stateColor_r, stateColor_g, stateColor_b;
        switch (runState) {
            case GameState.GAME_RUNNING:
                stateStr = "▶ 运行中";
                stateColor_r = 0.3f; stateColor_g = 1.0f; stateColor_b = 0.3f;
                break;
            case GameState.GAME_PAUSED:
                stateStr = "⏸ 暂停";
                stateColor_r = 1.0f; stateColor_g = 0.8f; stateColor_b = 0.2f;
                break;
            case GameState.GAME_FAST_FWD:
                stateStr = "⏩ 快进";
                stateColor_r = 1.0f; stateColor_g = 0.5f; stateColor_b = 0.2f;
                break;
            case GameState.GAME_NOT_STARTED:
                stateStr = "⏹ 准备";
                stateColor_r = 0.6f; stateColor_g = 0.6f; stateColor_b = 0.6f;
                break;
            default:
                stateStr = "";
                stateColor_r = stateColor_g = stateColor_b = 0.7f;
                break;
        }
        renderer.drawText(stateStr, screenW - 120, yTop, stateColor_r, stateColor_g, stateColor_b, 1.0f);

        if (options != null && options.optionValue(OptionsData.SHOW_DRAW_PERFORMANCE)) {
            renderer.drawText("FPS:" + currentFps, 12, screenH - 50,
                    0.6f, 0.8f, 1.0f, 0.9f);
        }
        if (options != null && options.optionValue(OptionsData.SHOW_ZOOM_LEVEL)) {
            renderer.drawText("z:" + String.format("%.1f×", camera.getScale()), 12, screenH - 65,
                    0.7f, 0.7f, 0.7f, 0.9f);
        }
        if (options != null && options.optionValue(OptionsData.SHOW_BATTERY_GAUGE)) {
            if (batteryLevel >= 0) {
                renderer.drawText("🔋 " + batteryLevel + "%", screenW - 80, screenH - 50,
                        0.7f, 0.7f, 0.7f, 0.9f);
            }
        }

        renderer.drawRect(0, 0, screenW, 20, 0.02f, 0.02f, 0.03f, 0.7f);
        renderer.drawRect(0, 20, screenW, 1, 0.15f, 0.15f, 0.15f, 0.9f);
        renderer.drawText("[1]机枪  [2]冰塔  [3]火箭  [空格]暂停  [F]快进  [Esc]退出", 10, 14,
                0.65f, 0.7f, 0.75f, 1.0f);
        renderer.end();
    }

    /**
     * 绘制塔按钮（初始化 + 按钮渲染，不含控制按钮/缩放滑块/精灵预览）
     */
    private void drawTowerButtons(int money, int stateIndex) {
        // 脏标记：金钱和激活塔未变化时跳过重绘（按钮绘制是 renderScreenLayer 中最耗时的批次操作）
        if (money == lastMoney && uiRenderer.getActiveTowerId() == lastActiveTowerId) return;
        lastMoney = money;
        lastActiveTowerId = uiRenderer.getActiveTowerId();

        // 初始化塔按钮（只初始化一次）
        if (uiRenderer.getTowerButtons() == null) {
            UiRenderer.TowerButtonInfo[] buttons = new UiRenderer.TowerButtonInfo[3];

            // 减速塔
            buttons[0] = createTowerButton(TowerData.SLOW_TOWER, "减速塔", 0);
            // 火箭塔
            buttons[1] = createTowerButton(TowerData.ROCKET_TOWER, "火箭塔", 1);
            // 机枪塔
            buttons[2] = createTowerButton(TowerData.GUN_TOWER, "机枪塔", 2);

            uiRenderer.initTowerButtons(buttons);
        }

        uiRenderer.renderTowerButtons(money, stateIndex);
    }

    /**
     * 在塔按钮上绘制塔精灵图（无 batch 边界）
     */
    private void drawTowerButtonSprites() {
        UiRenderer.TowerButtonInfo[] buttons = uiRenderer.getTowerButtons();
        if (buttons == null) return;
        renderer.begin();

        for (UiRenderer.TowerButtonInfo button : buttons) {
            if (button == null) continue;
            String sheetName = SpriteNames.tower(button.towerType);
            int totalFrames = TowerData.getTotalFrames(button.towerType);
            int fw = renderer.getSpriteFrameWidth(sheetName, totalFrames);
            int fh = renderer.getSpriteFrameHeight(sheetName, totalFrames);
            if (fw <= 0 || fh <= 0) { fw = 32; fh = 32; }

            // 将精灵缩放适配按钮尺寸
            float scale = Math.min(
                    (button.width - 12) / (float) fw,
                    (button.height - 26) / (float) fh);
            int drawW = (int) (fw * scale);
            int drawH = (int) (fh * scale);

            // 居中绘制底座
            float drawX = button.screenX + (button.width - drawW) / 2f;
            float drawY = button.screenY + (button.height - drawH) / 2f - 6;

            renderer.drawSpriteFrame(sheetName, 0, totalFrames,
                    drawX, drawY, drawW, drawH);

            // 绘制转头 — 居中略上移，使炮管在底座上方可见
            if (totalFrames > 1) {
                int towerH = TowerData.towerHeight(button.towerType);
                float turretY = drawY - (towerH * scale) * 0.5f;
                renderer.drawSpriteFrame(sheetName, 1, totalFrames,
                        drawX, turretY, drawW, drawH);
            }
        }
        renderer.end();
    }

    // ============================================================
    // 公开辅助方法
    // ============================================================

    /**
     * 创建塔按钮配置
     */
    public UiRenderer.TowerButtonInfo createTowerButton(int towerType, String name, int slotNum) {
        UiRenderer.TowerButtonInfo button = new UiRenderer.TowerButtonInfo();
        button.towerType = towerType;
        button.towerName = name;
        button.cost = TowerData.cost(towerType);

        // 按钮位置：屏幕右下角，从右往左排列
        int buttonSize = 64;
        int buttonGap = 8;
        int border = 4;

        int screenWidth = camera.getScreenWidth();
        int screenHeight = camera.getScreenHeight();

        // 屏幕坐标（左上角原点，与输入事件一致）
        button.screenX = screenWidth - border - buttonSize - (buttonSize + buttonGap) * slotNum;
        button.screenY = screenHeight - border - buttonSize;  // 从底部算起，但输入y也是从顶部flip后的，所以一致
        button.width = buttonSize;
        button.height = buttonSize;
        button.selected = false;

        return button;
    }
}
