package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.Bullet;
import com.rdefense.core.game.BulletData;
import com.rdefense.core.game.Enemy;
import com.rdefense.core.game.EnemyData;
import com.rdefense.core.game.GameEvent;
import com.rdefense.core.game.GameState;
import com.rdefense.core.game.GameTower;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.platform.libgdx.LibGdxInputAdapter;
import com.rdefense.core.platform.libgdx.LibGdxRenderer;
import com.rdefense.core.config.OptionsData;
import com.rdefense.core.render.CameraManager;
import com.rdefense.core.render.GameLoop;
import com.rdefense.core.render.SpriteNames;
import com.rdefense.core.render.UiRenderer;
import com.rdefense.core.input.GameInputHandlerImpl;

/**
 * 游戏主场景 — 运行游戏的核心场景
 */
public class GamePlayScreen extends GameScreen {

    private static final int GRID_PIXEL_SIZE = 32;

    private GameState gameState;
    private UiRenderer uiRenderer;
    private CameraManager camera;
    private GameLoop gameLoop;
    private OptionsData options;
    private LibGdxInputAdapter inputAdapter;
    private boolean initialized = false;
    private String errorMsg = null;
    // 可选的预配置关卡（由 LevelSelectScreen 调用）
    private int pendingLevel = -1;
    private int pendingDifficulty = 0;

    public GamePlayScreen(RoboDefenseGame game) {
        super(game);
    }

    /**
     * 配置要启动的关卡（由 LevelSelectScreen 调用）
     */
    public void configureLevel(int levelType, int difficulty) {
        this.pendingLevel = levelType;
        this.pendingDifficulty = difficulty;
    }

    /**
     * 兼容原有构造函数签名：部分界面会使用 GamePlayScreen(game, boolean)
     * 仅用于保持向后兼容性，实际逻辑集中在 configureLevel/init
     */
    public GamePlayScreen(RoboDefenseGame game, boolean dummy) {
        this(game);
    }

    @Override
    public void showShortMessage(String msg) {
        if (uiRenderer != null) uiRenderer.showShortMessage(msg);
    }

    @Override
    protected void init() {
        if (initialized) return;

        try {
            GameState.initBaseSystems();
            EnemyData.init();

            gameState = new GameState(1, 0, false);
            gameState.setOptions(game.getOptions());
            // 如果有预配置的关卡，则使用之
            int levelToInit = (pendingLevel >= 0) ? pendingLevel : 0;
            gameState.initGame(levelToInit);

            GameRenderer renderer = game.getServices().getRenderer();
            camera = game.getCamera();
            options = game.getOptions();
            uiRenderer = new UiRenderer(renderer, camera);

            // 若游戏创建期间发生了 HD 回退且有待展示消息，则消费并展示
            try {
                String pending = game.consumePendingShortMessage();
                if (pending != null && uiRenderer != null) uiRenderer.showShortMessage(pending);
            } catch (Throwable ignored) { }

            int gridW = gameState.getLevelData().getGridWidth();
            int gridH = gameState.getLevelData().getGridHeight();
            camera.init(
                    gridW * GRID_PIXEL_SIZE,
                    gridH * GRID_PIXEL_SIZE,
                    renderer.getScreenWidth(),
                    renderer.getScreenHeight(),
                    GRID_PIXEL_SIZE
            );

            gameLoop = game.getGameLoop();
            gameLoop.init();

            // 调试用：如果 JVM 系统属性 rd.autostart=true，则自动开始游戏（用于无 GUI 测试）
            try {
                String autoStart = System.getProperty("rd.autostart");
                if ("true".equals(autoStart)) {
                    gameState.startGame();
                    // 放置两个测试塔：机枪（GUN_TOWER）与冰塔（SLOW_TOWER），放在地图中心附近
                    int gx = gameState.getLevelData().getGridWidth() / 2;
                    int gy = gameState.getLevelData().getGridHeight() / 2;
                    int res1 = gameState.tryPlaceTower(TowerData.GUN_TOWER, gx, gy);
                    System.out.println("[AutoStart] tryPlaceTower GUN at (" + gx + "," + gy + ") -> " + res1);
                    int res2 = gameState.tryPlaceTower(TowerData.SLOW_TOWER, Math.max(0, gx - 1), gy);
                    System.out.println("[AutoStart] tryPlaceTower SLOW at (" + (Math.max(0, gx - 1)) + "," + gy + ") -> " + res2);
                    // 如果需要自动进入快进模式，可设置系统属性 rd.autofast=true
                    try {
                        String autoFast = System.getProperty("rd.autofast");
                        if ("true".equals(autoFast)) {
                            gameState.toggleFastFwd();
                            System.out.println("[AutoStart] fast-forward enabled");
                        }
                    } catch (Throwable ignored) { }
                }
            } catch (Throwable ignored) { }
            GameInputHandlerImpl inputHandler = new GameInputHandlerImpl(gameState, camera, uiRenderer);
            inputAdapter = new LibGdxInputAdapter(inputHandler);
            Gdx.input.setInputProcessor(inputAdapter.getInputProcessor());
            Gdx.input.setCursorCatched(false); // 确保鼠标光标可见

            initialized = true;
            Gdx.app.log("GamePlayScreen", "游戏场景初始化成功");
        } catch (Exception e) {
            errorMsg = "初始化失败: " + e.getMessage();
            Gdx.app.error("GamePlayScreen", errorMsg, e);
        }
    }

    @Override
    protected void update(float delta) {
        if (!initialized || gameState == null) return;
        try {
            // 调试按键：按 F9 强制触发 HD 回退，便于观察短消息
            if (com.badlogic.gdx.Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F9)) {
                try {
                    GameRenderer gr = game.getServices().getRenderer();
                    if (gr instanceof com.rdefense.core.platform.libgdx.LibGdxRenderer) {
                        ((com.rdefense.core.platform.libgdx.LibGdxRenderer) gr).forceHdFallback("F9 key");
                    }
                } catch (Throwable ignored) { }
            }
            int runState = gameState.getRunState();

            // GAME_NOT_STARTED：点击开始游戏
            if (runState == GameState.GAME_NOT_STARTED) {
                if (Gdx.input.justTouched()) {
                    gameState.startGame();
                }
                return;
            }

            // 游戏结束：点击重置
            if (runState == GameState.GAME_LOST || runState == GameState.GAME_WON) {
                if (Gdx.input.justTouched()) {
                    gameState.initGame(gameState.getLevelData().getLevelType());
                    gameLoop.init();
                }
                return;
            }

            // 同步快进模式到 GameLoop，以实现桌面端的快进行为
            gameLoop.setFastFwdMode(runState == GameState.GAME_FAST_FWD);
            gameLoop.tick(stateIndex -> gameState.nextState());
        } catch (Exception e) {
            Gdx.app.error("GamePlayScreen", "更新失败: " + e.getMessage());
        }
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();

        if (errorMsg != null) {
            renderer.begin();
            renderer.drawText(errorMsg, 10, 50, 1.0f, 0.3f, 0.3f, 1.0f);
            renderer.end();
            return;
        }

        if (!initialized || gameState == null) return;

        int stateIndex = gameLoop.getStateIndex();
        int runState = gameState.getRunState();
        int levelType = gameState.getLevelData().getLevelType();

        // 1. 绘制地图背景
        drawBackground(renderer, levelType);

        // 2. 绘制塔
        drawTowers(renderer);

        // 3. 绘制敌人（含血条）
        drawEnemies(renderer, stateIndex);

        // 4. 绘制子弹
        drawBullets(renderer, stateIndex);

        // 5. 绘制敌人击败事件（金钱奖励飘字）
        drawEnemyDefeatedEvents(renderer, stateIndex);

        // 6. 绘制激活塔预览（放置前显示）
        drawActiveTowerPreview(renderer);

        // 6. 绘制 HUD（使用屏幕坐标）
        drawHud(renderer, runState);

        // 7. 绘制塔按钮（使用屏幕坐标）
        drawTowerButtons(renderer, stateIndex);

        // 8. 绘制升级对话框（如果需要）
        if (runState == GameState.GAME_RUNNING || runState == GameState.GAME_FAST_FWD) {
            uiRenderer.renderUpgradeDialog(gameState.getMoney(), stateIndex);
        }

        // 8.1 绘制放置失败提示
        uiRenderer.renderPlacementFailure();
        uiRenderer.updatePlacementFailure();
        // 绘制短消息（如 HD 回退提示）
        uiRenderer.renderShortMessage();

        // 9. 状态覆盖层
        switch (runState) {
            case GameState.GAME_PAUSED:
                uiRenderer.renderPauseOverlay("游戏暂停");
                break;
            case GameState.GAME_LOST:
                uiRenderer.renderGameOverOverlay("游戏结束", "点击重试");
                break;
            case GameState.GAME_WON:
                uiRenderer.renderGameOverOverlay("胜利！", "点击继续");
                break;
            case GameState.GAME_NOT_STARTED:
                uiRenderer.renderGameOverOverlay("星际塔防", "点击开始");
                break;
        }
    }

    /**
     * 绘制地图背景图
     * 原版逻辑：redrawBackground 在 (0,0) 绘制，但 Canvas 已应用了 matrix 变换
     * 这里先应用相机变换，然后在世界坐标原点绘制地图
     */
    private void drawBackground(GameRenderer renderer, int levelType) {
        boolean useClassic = options != null && options.optionValue(OptionsData.CLASSIC_BACKGROUNDS);
        String bgName = useClassic
                ? SpriteNames.levelBackgroundClassic(levelType)
                : SpriteNames.levelBackground(levelType);
        // 使用地图实际宽高（世界坐标）
        int worldW = gameState.getLevelData().getGridWidth() * GRID_PIXEL_SIZE;
        int worldH = gameState.getLevelData().getGridHeight() * GRID_PIXEL_SIZE;
        
        // 先应用相机变换
        renderer.applyCameraTransform(camera.getXBase(), camera.getYBase(), camera.getScale());
        
        renderer.begin();
        // 在世界坐标原点绘制地图（会被相机变换映射到正确位置）
        renderer.drawSprite(bgName, 0, 0, worldW, worldH);
        renderer.end();
    }

    /**
     * 绘制所有塔（精灵图）
     * 原版渲染流程：
     * 1. 背景层：绘制 images[0] 底座
     * 2. 前景层：绘制 images[frameIndex] 转头
     */
    private void drawTowers(GameRenderer renderer) {
        LibGdxRenderer libGdxRenderer = (LibGdxRenderer) renderer;
        libGdxRenderer.begin();
        for (GameTower t = gameState.getTowerList(); t != null; t = t.next) {
            int type = t.getType();
            int direction = t.getDirection();
            int animFrame = gameLoop.getStateIndex() >> 1;
            int frameIndex = TowerData.getDirectionFrameIndex(type, direction, animFrame);
            int totalFrames = TowerData.getTotalFrames(type);
            String spriteSheetName = SpriteNames.tower(type);

            float wx = t.getGridX() * GRID_PIXEL_SIZE;
            float wy = t.getGridY() * GRID_PIXEL_SIZE;

            // 获取精灵图的实际帧尺寸
            int frameWidth = libGdxRenderer.getSpriteFrameWidth(spriteSheetName, totalFrames);
            int frameHeight = libGdxRenderer.getSpriteFrameHeight(spriteSheetName, totalFrames);

            // 如果无法获取尺寸，使用默认值
            if (frameWidth <= 0 || frameHeight <= 0) {
                frameWidth = GRID_PIXEL_SIZE;
                frameHeight = GRID_PIXEL_SIZE;
            }

            // 计算炮塔头部世界 Y
            int towerHeight = TowerData.towerHeight(type);
            // Android 原版（左上角锚点）：
            //   底座: drawBitmap(img, gridX*GRID, gridY*GRID) - 左上角
            //   转头: drawBitmap(img, gridX*GRID, gridY*GRID - towerHeight) - 左上角
            // 在 libGDX 中（左下角锚点）：
            //   底座底部 = gridY * GRID_PIXEL_SIZE
            //   转头底部 = 底座底部 + towerHeight（转头与底座重叠 towerHeight 像素）
            float turretWorldY = wy + towerHeight;

            // 1. 绘制底座（帧 0）
            libGdxRenderer.drawSpriteFrame(spriteSheetName, 0, totalFrames, wx, wy, frameWidth, frameHeight);

            // 2. 绘制转头（帧 frameIndex）- 与底座重叠 towerHeight 像素
            libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, turretWorldY, frameWidth, frameHeight);
        }
        libGdxRenderer.end();
    }

    /**
     * 绘制所有敌人（精灵图 + 状态效果 + 血条）
     * 使用世界坐标直接绘制
     */
    private void drawEnemies(GameRenderer renderer, int stateIndex) {
        LibGdxRenderer libGdxRenderer = (LibGdxRenderer) renderer;
        libGdxRenderer.begin();
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
                libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 0.6f, 0.4f, 0.9f, 1.0f);
            } else if (fireCounter > 0) {
                libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 1.0f, 0.5f, 0.1f, 1.0f);
            } else if (slowCounter > 0) {
                libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size, 0.4f, 0.7f, 1.0f, 1.0f);
            } else {
                libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames, wx, wy, size, size);
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
                libGdxRenderer.drawRect(barX, barY, barW, barH, 0.8f, 0.0f, 0.0f, 0.9f);
                libGdxRenderer.drawRect(barX, barY, barW * healthRatio, barH, 0.0f, 0.9f, 0.0f, 0.9f);
            }
        }
        libGdxRenderer.end();
    }

    /**
     * 绘制所有子弹（精灵图）
     * 使用世界坐标直接绘制
     */
    private void drawBullets(GameRenderer renderer, int stateIndex) {
        LibGdxRenderer libGdxRenderer = (LibGdxRenderer) renderer;
        libGdxRenderer.begin();

        for (Bullet b = gameState.getBulletList(); b != null; b = b.next) {
            int size = b.getSize(stateIndex);
            float wx = b.getX() - size / 2f;
            float wy = b.getY() - size / 2f;

            String imgName = SpriteNames.bullet(b.getType());
            if (imgName != null) {
                int totalFrames = BulletData.getNumImages(b.getType());
                if (totalFrames > 1) {
                    int directionIndex = b.getDirectionIndex();
                    libGdxRenderer.drawSpriteFrame(imgName, directionIndex, totalFrames, wx, wy, size, size);
                } else {
                    libGdxRenderer.drawSprite(imgName, wx, wy, size, size);
                }
            } else {
                int color = BulletData.color(b.getType());
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float bl = (color & 0xFF) / 255.0f;
                libGdxRenderer.drawRect(wx, wy, size, size, r, g, bl, 1.0f);
            }
        }

        libGdxRenderer.end();
    }

    /**
     * 绘制 HUD 信息栏
     * HUD 使用屏幕坐标，需要恢复默认矩阵（无相机变换）
     */
    private void drawHud(GameRenderer renderer, int runState) {
        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        int screenW = renderer.getScreenWidth();
        int screenH = renderer.getScreenHeight();

        // 顶部信息栏背景（深蓝色）
        renderer.drawRect(0, screenH - 32, screenW, 32, 0.05f, 0.1f, 0.2f, 0.85f);
        renderer.drawRect(0, screenH - 1, screenW, 1, 0.2f, 0.4f, 0.6f, 0.9f);

        float yTop = screenH - 18;
        float xStart = 12;

        renderer.drawText("L:" + gameState.getLevelData().getLevelNum(), xStart, yTop,
                0.9f, 0.95f, 1.0f, 1.0f);
        renderer.drawText("$" + gameState.getMoney(), xStart + 80, yTop,
                0.3f, 1.0f, 0.3f, 1.0f);
        renderer.drawText("HP:" + gameState.getHealth(), xStart + 200, yTop,
                1.0f, 0.3f, 0.3f, 1.0f);

        // 分数显示：处理累计动画
        int score = gameState.getScore();
        int pendingScore = 0;

        // 从敌人击败事件中获取待添加的分数
        for (GameEvent e = gameState.getGameEventList(GameEvent.EVENT_ENEMY_DEFEATED); e != null; e = e.next) {
            if (e.var[GameEvent.VAR_ENEMY_FULL_SCORE] > 0) {
                pendingScore += e.var[GameEvent.VAR_ENEMY_FULL_SCORE];
                e.var[GameEvent.VAR_ENEMY_FULL_SCORE] = 0; // 标记已处理
            }
        }

        // 显示分数
        String scoreText = "PTS:" + score;
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
            renderer.drawText("FPS:" + gameLoop.getCurrentFps(), 12, screenH - 50,
                    0.6f, 0.8f, 1.0f, 0.9f);
        }
        if (options != null && options.optionValue(OptionsData.SHOW_ZOOM_LEVEL)) {
            renderer.drawText("z:" + String.format("%.1f×", camera.getScale()), 12, screenH - 65,
                    0.7f, 0.7f, 0.7f, 0.9f);
        }
        if (options != null && options.optionValue(OptionsData.SHOW_BATTERY_GAUGE)) {
            int batteryLevel = game.getServices().getBatteryLevel();
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
     * 绘制激活塔预览（放置前显示范围圈和半透明塔）
     */
    private void drawActiveTowerPreview(GameRenderer renderer) {
        int activeTowerId = uiRenderer.getActiveTowerId();
        if (activeTowerId < 0) return;

        LibGdxRenderer libGdxRenderer = (LibGdxRenderer) renderer;

        // 使用输入事件中保存的网格坐标
        int gridX = uiRenderer.getActiveTowerGridX();
        int gridY = uiRenderer.getActiveTowerGridY();

        // 检查放置是否有效
        boolean valid = gameState.checkTowerPlacement(gridX, gridY, activeTowerId, false)
                && TowerData.cost(activeTowerId) <= gameState.getMoney();

        // 应用相机变换（与世界坐标绘制一致）
        libGdxRenderer.applyCameraTransform(camera.getXBase(), camera.getYBase(), camera.getScale());
        libGdxRenderer.begin();

        // 绘制攻击范围圈（原版使用攻击半径，单位是世界像素）
        float worldCenterX = gridX * GRID_PIXEL_SIZE + GRID_PIXEL_SIZE / 2f;
        float worldCenterY = gridY * GRID_PIXEL_SIZE + GRID_PIXEL_SIZE / 2f;
        int attackRadius = TowerData.attackRadius(activeTowerId);
        if (options == null || options.optionValue(OptionsData.SHOW_TURRET_RANGE)) {
            libGdxRenderer.drawCircle(worldCenterX, worldCenterY, attackRadius,
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
        libGdxRenderer.drawRect(rectX, rectY,
                GRID_PIXEL_SIZE, GRID_PIXEL_SIZE,
                validR, validG, validB, previewAlpha);

        // 绘制塔精灵预览（原版绘制 tower image + direction image）
        String spriteSheetName = SpriteNames.tower(activeTowerId);
        int totalFrames = TowerData.getTotalFrames(activeTowerId);
        int animFrame = gameLoop.getStateIndex() >> 1;
        int frameIndex = TowerData.getDirectionFrameIndex(activeTowerId, 270, animFrame);
        int towerHeight = TowerData.towerHeight(activeTowerId);
        int turretOffset = TowerData.turretYOffset(activeTowerId);

        float baseX = gridX * GRID_PIXEL_SIZE;
        float baseY = gridY * GRID_PIXEL_SIZE;
        // Android 原版（左上角锚点）：
        //   底座: drawBitmap(img, gridX*GRID, gridY*GRID) - 左上角
        //   转头: drawBitmap(img, gridX*GRID, gridY*GRID - towerHeight) - 左上角
        // 在 libGDX 中（左下角锚点）：
        //   底座底部 = gridY * GRID_PIXEL_SIZE
        //   转头底部 = 底座底部 + towerHeight（转头与底座重叠 towerHeight 像素）
        float turretY = baseY + towerHeight;

        float size = GRID_PIXEL_SIZE;
        // libGDX batch.draw 以左下角为锚点，传入的 Y 是精灵底部位置
        libGdxRenderer.drawSpriteFrame(spriteSheetName, 0, totalFrames,
                baseX, baseY,
                size, size,
                1.0f, 1.0f, 1.0f, 0.7f);
        libGdxRenderer.drawSpriteFrame(spriteSheetName, frameIndex, totalFrames,
                baseX, turretY,
                size, size,
                1.0f, 1.0f, 1.0f, 0.7f);

        libGdxRenderer.end();
    }

    /**
     * 绘制塔按钮（屏幕坐标右下角）
     */
    private void drawTowerButtons(GameRenderer renderer, int stateIndex) {
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

        uiRenderer.renderTowerButtons(gameState.getMoney(), stateIndex);
    }

    /**
     * 创建塔按钮配置
     */
    private UiRenderer.TowerButtonInfo createTowerButton(int towerType, String name, int slotNum) {
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

    /**
     * 绘制敌人击败事件（金钱奖励和分数飘字）
     * 参考 Android 原版 Display.handleEnemyDefeatedMoneyAdd
     */
    private void drawEnemyDefeatedEvents(GameRenderer renderer, int stateIndex) {
        renderer.begin();

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

        renderer.end();
    }
}
