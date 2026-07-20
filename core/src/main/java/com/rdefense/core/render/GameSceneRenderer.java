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
        drawSortedObjects(gameState, stateIndex);
        drawBullets(stateIndex, gameState);
        drawEnemyDefeatedEvents(gameState);
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

        // 塔按钮面板+文字
        drawTowerButtons(money, stateIndex);
        // 按钮底座精灵（缩到30px嵌在价格和塔名之间）
        drawTowerButtonIcons();

        // UI 组件
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
     * Y 排序绘制塔+敌人（GridObjectOrder 确保远处→近处渲染顺序）
     */
    private void drawSortedObjects(GameState gameState, int stateIndex) {
        com.rdefense.core.game.GridObject obj = gameState.getSortedList();
        while (obj != null) {
            if (obj.getClassType() == 1) { // 塔
                GameTower t = (GameTower) obj;
                drawSingleTower(t, stateIndex);
            } else if (obj.getClassType() == 2) { // 敌人
                Enemy e = (Enemy) obj;
                drawSingleEnemy(e, stateIndex, gameState);
            }
            obj = obj.next_y;
        }
    }

    private void drawSingleTower(GameTower t, int stateIndex) {
        int type = t.getType();
        int direction = t.getDirection();
        int animFrame = stateIndex >> 1;
        int frameIndex = TowerData.getDirectionFrameIndex(type, direction, animFrame);
        int totalFrames = TowerData.getTotalFrames(type);
        String spriteSheetName = SpriteNames.tower(type);
        float wx = t.getGridX() * GRID_PIXEL_SIZE;
        float wy = t.getGridY() * GRID_PIXEL_SIZE;
        float towerSize = GRID_PIXEL_SIZE;
        int towerHeight = TowerData.towerHeight(type);
        float turretWorldY = wy + towerHeight;
        renderer.drawSpriteFrame(spriteSheetName, 0, totalFrames, wx, wy, towerSize, towerSize);
        renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, turretWorldY, towerSize, towerSize);
    }

    private void drawSingleEnemy(Enemy e, int stateIndex, GameState gameState) {
        int wx = e.calcPixelX();
        int wy = e.calcPixelY();
        float size = GRID_PIXEL_SIZE;
        String spriteSheetName = SpriteNames.enemy(e.getType());
        int totalFrames = EnemyData.getTotalFrames(e.getType());
        int animationFrame = (e.getFirstState() + stateIndex);
        if (e.getSlowCounter() > 0) animationFrame >>= 1;
        int frameIndex = animationFrame % totalFrames;
        renderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size);
        // 减速/灼烧效果
        if (e.getSlowCounter() > 0) {
            renderer.drawRect(wx, wy, size, size, 0.2f, 0.3f, 0.9f, 0.3f);
        }
        if (e.getFireCounter() > 0) {
            renderer.drawRect(wx, wy, size, size, 1.0f, 0.3f, 0.1f, 0.35f);
        }
        // 血条
        int maxHp = e.getMaxHealth();
        if (maxHp > 0 && e.getHealth() < maxHp) {
            float hpRatio = (float) e.getHealth() / maxHp;
            renderer.drawRect(wx + 4, wy + size + 2, 24, 3, 0.3f, 0.08f, 0.08f, 0.6f);
            renderer.drawRect(wx + 4, wy + size + 2, 24 * hpRatio, 3, 0.1f, 0.85f, 0.22f, 0.8f);
        }
    }

    /**
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
    private void drawEnemyDefeatedEvents(GameState gameState) {
        int now = gameState.getStateIndex();
        for (GameEvent e = gameState.getGameEventList(GameEvent.EVENT_ENEMY_DEFEATED); e != null; e = e.next) {
            int dist = now - e.var[GameEvent.VAR_ENEMY_STATE_IDX];
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
    private int hpPulseTimer; // HP ≤ 3 时呼吸脉冲计时器

    private void drawHud(GameState gameState, int currentFps, int batteryLevel) {
        renderer.begin();
        int screenW = renderer.getScreenWidth();
        int screenH = renderer.getScreenHeight();
        int runState = gameState.getRunState();
        float yTop = screenH - 18;
        float xStart = 12;

        // === 顶部信息条（半透明面板，紧凑一行） ===
        renderer.drawRect(0, screenH - 32, screenW, 32, 0.03f, 0.04f, 0.1f, 0.88f);
        renderer.drawRect(0, screenH - 1, screenW, 1, 0.12f, 0.2f, 0.3f, 0.5f);

        // 地图 + 波次
        com.rdefense.core.game.LevelData ld = gameState.getLevelData();
        String[] mapShort = {"草地","遗迹","工厂","庭院","混合","公路","天空"};
        String mapName = ld.getLevelType() >= 0 && ld.getLevelType() < mapShort.length
                ? mapShort[ld.getLevelType()] : "??";
        renderer.drawText(mapName + " L:" + ld.getLevelNum(),
                xStart, yTop, 0.5f, 0.55f, 0.65f, 1.0f);
        // 难度（括号内，紧接地名后）
        renderer.drawText("难度" + gameState.getDifficultyLevel(), xStart + 100, yTop,
                0.45f, 0.5f, 0.58f, 0.85f);

        // 金钱
        renderer.drawText("$" + GameScreen.formatWithCommas(displayMoney), xStart + 160, yTop,
                0.83f, 0.67f, 0.16f, 1.0f);

        // 击杀奖励
        int bonus = gameState.getEnemyKillBonus();
        if (bonus > 0) {
            renderer.drawText("击杀+" + bonus, xStart + 215, yTop, 0.55f, 0.5f, 0.7f, 0.85f);
        }

        // 生命值 + 血量条（HP≤3 时脉冲呼吸）
        int hp = displayHealth;
        int hpMax = gameState.getStartingHealth();
        float hpRatio = Math.min(1.0f, (float)hp / Math.max(1, hpMax));
        float hpAlpha;
        if (hp <= 3) {
            hpPulseTimer++;
            hpAlpha = 0.5f + 0.5f * (float)Math.abs(Math.sin(hpPulseTimer * 0.05));
        } else {
            hpAlpha = 1.0f;
        }

        float hpX = xStart + 285;
        renderer.drawText("HP " + hp, hpX, yTop,
                0.6f * hpAlpha, 0.9f * hpAlpha, 0.25f * hpAlpha, 1.0f);
        float barW = 50;
        renderer.drawRect(hpX + 34, screenH - 15, barW, 3, 0.3f, 0.08f, 0.08f, 0.5f);
        renderer.drawRect(hpX + 34, screenH - 15, barW * hpRatio, 3,
                0.1f * hpAlpha, 0.85f * hpAlpha, 0.22f * hpAlpha, 0.9f);

        // 分数
        int score = displayScore;
        int pendingScore = 0;
        for (GameEvent e = gameState.getGameEventList(GameEvent.EVENT_ENEMY_DEFEATED); e != null; e = e.next) {
            if (e.var[GameEvent.VAR_ENEMY_FULL_SCORE] > 0) {
                pendingScore += e.var[GameEvent.VAR_ENEMY_FULL_SCORE];
            }
        }
        String scoreText = (score >= 1000) ? "PTS:" + GameScreen.formatWithCommas(score)
                                           : "PTS:" + score;
        if (pendingScore > 0) scoreText += " +" + pendingScore;
        renderer.drawText(scoreText, hpX + 34 + barW + 12, yTop, 0.55f, 0.58f, 0.65f, 1.0f);

        // 状态
        String stateStr;
        float sr, sg, sb;
        switch (runState) {
            case GameState.GAME_RUNNING:
                stateStr = "运行中"; sr = 0.3f; sg = 0.95f; sb = 0.3f; break;
            case GameState.GAME_PAUSED:
                stateStr = "暂停";   sr = 0.9f; sg = 0.7f; sb = 0.2f; break;
            case GameState.GAME_FAST_FWD:
                stateStr = "快进中"; sr = 0.9f; sg = 0.5f; sb = 0.2f; break;
            case GameState.GAME_NOT_STARTED:
                stateStr = "准备";   sr = 0.5f; sg = 0.5f; sb = 0.5f; break;
            default:
                stateStr = "";       sr = sg = sb = 0.5f; break;
        }

        // 状态文字移到脚注中

        // FPS/缩放（调试层，可选显示）
        if (options != null && options.optionValue(OptionsData.SHOW_DRAW_PERFORMANCE)) {
            renderer.drawText("FPS:" + currentFps, 12, screenH - 48, 0.4f, 0.45f, 0.55f, 0.7f);
        }
        if (options != null && options.optionValue(OptionsData.SHOW_ZOOM_LEVEL)) {
            renderer.drawText("×" + String.format("%.1f", camera.getScale()), 12, screenH - 60,
                    0.4f, 0.45f, 0.55f, 0.7f);
        }

        // === 脚注：快捷键 + 状态 ===
        renderer.drawText("[1]机枪 [2]冰塔 [3]火箭  [空格]暂停 [F]快进 [Esc]退出",
                10, 16, 0.5f, 0.52f, 0.56f, 0.7f);
        renderer.drawText(stateStr, screenW - 52, 16, sr, sg, sb, 0.85f);
        renderer.end();
    }

    /**
     * 绘制塔按钮（初始化 + 按钮渲染，不含控制按钮/缩放滑块/精灵预览）
     */
    private void drawTowerButtons(int money, int stateIndex) {
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


    // ============================================================
    /** 按钮图标：底座+转头，缩放到填充按钮（留4px边距） */
    private void drawTowerButtonIcons() {
        UiRenderer.TowerButtonInfo[] buttons = uiRenderer.getTowerButtons();
        if (buttons == null) return;
        renderer.begin();
        for (UiRenderer.TowerButtonInfo btn : buttons) {
            if (btn == null) continue;
            String sheet = SpriteNames.tower(btn.towerType);
            int totalFrames = TowerData.getTotalFrames(btn.towerType);
            if (totalFrames < 1) continue;
            int fw = renderer.getSpriteFrameWidth(sheet, totalFrames);
            int fh = renderer.getSpriteFrameHeight(sheet, totalFrames);
            if (fw <= 0 || fh <= 0) { fw = 32; fh = 32; }
            // 缩放到填充按钮（留4px边距）
            float s = Math.min((btn.width - 8) / (float)fw, (btn.height - 8) / (float)fh);
            int iw = (int)(fw * s), ih = (int)(fh * s);
            float ix = btn.screenX + (btn.width - iw) / 2f;
            float iy = btn.screenY + (btn.height - ih) / 2f;
            // 底座
            renderer.drawSpriteFrame(sheet, 0, totalFrames, ix, iy, iw, ih);
            // 转头（底座上方，参考 drawTowers 的 turretY = wy + towerHeight）
            if (totalFrames > 1) {
                int towerH = TowerData.towerHeight(btn.towerType);
                float turretY = iy + towerH * s * 1.2f; // +20% 上移，避免偏下
                renderer.drawSpriteFrame(sheet, 1, totalFrames, ix, turretY, iw, ih);
            }
        }
        renderer.end();
    }

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
