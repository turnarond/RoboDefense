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
import com.rdefense.core.game.Starfield;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.platform.libgdx.LibGdxInputAdapter;
import com.rdefense.core.platform.libgdx.LibGdxRenderer;
import com.rdefense.core.config.OptionsData;
import com.rdefense.core.render.CameraManager;
import com.rdefense.core.render.GameLoop;
import com.rdefense.core.render.SpriteNames;
import com.rdefense.core.render.GameSceneRenderer;
import com.rdefense.core.render.UiRenderer;
import com.rdefense.core.input.GameInputController;
import com.rdefense.core.input.GameInputHandlerImpl;

/**
 * 游戏主场景 — 运行游戏的核心场景
 */
public class GamePlayScreen extends GameScreen implements GameInputController.GameInputCallbacks {

    private GameState gameState;
    private UiRenderer uiRenderer;
    private CameraManager camera;
    private GameLoop gameLoop;
    private GameSceneRenderer sceneRenderer;
    private OptionsData options;
    private LibGdxInputAdapter inputAdapter;
    private boolean initialized = false;
    private String errorMsg = null;
    private boolean showPauseMenu = false;
    // 可选的预配置关卡（由 LevelSelectScreen 调用）
    private int pendingLevel = -1;
    private int pendingDifficulty = 0;
    private boolean resumeMode = false;     // 从快速存档恢复
    private int loadSlotId = -1;            // 从指定槽位加载（>=0 有效）
    private int lastRunState = -1; // 上一帧 run_state，用于检测胜负转换
    private boolean upgradeDialogWasVisible; // 升级对话框上一帧可见状态（用于自动暂停）
    private GameInputController inputController = new GameInputController();

    public GamePlayScreen(RoboDefenseGame game) {
        super(game);
    }

    /** 配置要启动的关卡（由 LevelSelectScreen 调用） */
    public void configureLevel(int levelType, int difficulty) {
        this.pendingLevel = levelType;
        this.pendingDifficulty = difficulty;
    }

    /** 从快速存档恢复（主菜单"继续游戏"） */
    public void setResumeMode() {
        this.resumeMode = true;
    }

    /** 从指定槽位加载（保存管理界面） */
    public void setLoadSlot(int slotId) {
        this.loadSlotId = slotId;
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
            int levelToInit = (pendingLevel >= 0) ? pendingLevel : 0;
            int diffToInit = (pendingDifficulty >= 0) ? pendingDifficulty : 1;
            gameState.initGame(levelToInit, diffToInit);

            // 加载存档（resumeMode 或 loadSlot）
            boolean loaded = false;
            if (resumeMode) {
                loaded = game.getGameSaveManager().quickLoad(gameState);
                if (loaded) Gdx.app.log("GamePlayScreen", "快速存档加载成功");
                else Gdx.app.log("GamePlayScreen", "快速存档加载失败，开始新游戏");
            } else if (loadSlotId >= 0) {
                loaded = game.getGameSaveManager().loadSave(loadSlotId, gameState);
                if (loaded) {
                    Gdx.app.log("GamePlayScreen", "槽位 " + loadSlotId + " 加载成功");
                } else {
                    errorMsg = "存档加载失败";
                    Gdx.app.error("GamePlayScreen", "槽位 " + loadSlotId + " 加载失败");
                    return;
                }
            }

            GameRenderer renderer = game.getServices().getRenderer();
            camera = game.getCamera();
            options = game.getOptions();
            uiRenderer = new UiRenderer(renderer, camera);
            sceneRenderer = new GameSceneRenderer(renderer, camera, uiRenderer);
            sceneRenderer.setOptions(options);

            // 若游戏创建期间发生了 HD 回退且有待展示消息，则消费并展示
            try {
                String pending = game.consumePendingShortMessage();
                if (pending != null && uiRenderer != null) uiRenderer.showShortMessage(pending);
            } catch (Throwable ignored) { }

            int gridW = gameState.getLevelData().getGridWidth();
            int gridH = gameState.getLevelData().getGridHeight();
            camera.init(
                    gridW * GameSceneRenderer.GRID_PIXEL_SIZE,
                    gridH * GameSceneRenderer.GRID_PIXEL_SIZE,
                    renderer.getScreenWidth(),
                    renderer.getScreenHeight(),
                    GameSceneRenderer.GRID_PIXEL_SIZE
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
            if (inputController.handleKeys(gameState, showPauseMenu, this)) {
                showPauseMenu = true;
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
                    // 关闭结算明细事件（否则残留到下一局）
                    for (com.rdefense.core.game.GameEvent e =
                            gameState.getGameEventList(com.rdefense.core.game.GameEvent.EVENT_SCORE_SAVED);
                            e != null; e = e.next) {
                        e.finished = true;
                    }
                    gameState.initGame(gameState.getLevelData().getLevelType());
                    gameLoop.init();
                }
                return;
            }

            // 同步快进模式到 GameLoop，以实现桌面端的快进行为
            gameLoop.setFastFwdMode(runState == GameState.GAME_FAST_FWD);
            gameLoop.tick(stateIndex -> gameState.nextState());

            // HUD 数字滚动动画：渐进逼近
            sceneRenderer.updateHudAnimations(gameState);

            // 成就弹窗：轮询队列并触发渲染器动画
            int achievementType = com.rdefense.core.game.AchievementData.dequeueEarned();
            if (achievementType >= 0) {
                game.getAchievementRenderer().showAchievement(achievementType);
            }

            // 胜负转换时执行平台副作用（原版 endGame 中的 SharedPreferences 部分）
            int rs = gameState.getRunState();
            if (rs != lastRunState) {
                if (rs == GameState.GAME_WON) {
                    com.rdefense.core.game.RewardData.gameWon(game.getPlayerPrefs());
                    game.getGameSaveManager().clearQuickSave();
                } else if (rs == GameState.GAME_LOST) {
                    game.getGameSaveManager().clearQuickSave();
                }
                lastRunState = rs;
            }
            // 每 10 关自动快速存档（原版 QuickSave.saveState(this, false)）
            if (gameState.consumeAutoSaveRequest()) {
                game.getGameSaveManager().quickSave(gameState);
            }
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
        sceneRenderer.drawBackground(renderer, levelType, gameState);

        // 2. 绘制塔
        sceneRenderer.drawTowers(renderer, gameState, gameLoop);

        // 3. 绘制敌人（含血条）
        sceneRenderer.drawEnemies(renderer, stateIndex, gameState);

        // 4. 绘制子弹
        sceneRenderer.drawBullets(renderer, stateIndex, gameState);

        // 5. 绘制敌人击败事件（金钱奖励飘字）
        sceneRenderer.drawEnemyDefeatedEvents(renderer, stateIndex, gameState);

        // 6. 绘制激活塔预览（放置前显示）
        sceneRenderer.drawActiveTowerPreview(renderer, gameState, gameLoop);

        // 6. 绘制 HUD（使用屏幕坐标）
        drawHud(renderer, runState);

        // 7. 绘制塔按钮（使用屏幕坐标）
        sceneRenderer.drawTowerButtons(renderer, stateIndex, gameState);

        // 8. 绘制升级对话框（如果需要）
        boolean dialogVisible = false;
        if (runState == GameState.GAME_RUNNING || runState == GameState.GAME_FAST_FWD) {
            uiRenderer.renderUpgradeDialog(gameState.getMoney(), stateIndex);
            dialogVisible = uiRenderer.isUpgradeDialogVisible();
        }
        if (dialogVisible && !upgradeDialogWasVisible) {
            int rs = gameState.getRunState();
            if (rs == GameState.GAME_RUNNING || rs == GameState.GAME_FAST_FWD) {
                gameState.togglePause();
            }
        }
        upgradeDialogWasVisible = dialogVisible;

        // 8.1 绘制放置失败提示
        uiRenderer.renderPlacementFailure();
        uiRenderer.updatePlacementFailure();
        // 绘制短消息（如 HD 回退提示）
        uiRenderer.renderShortMessage();

        // 8.2 更新和绘制成就弹窗动画
        game.getAchievementRenderer().update(game.getServices().getAudio());
        game.getAchievementRenderer().draw(renderer);

        // 9. 状态覆盖层
        switch (runState) {
            case GameState.GAME_PAUSED:
                if (showPauseMenu) {
                    renderPauseMenu(renderer);
                } else {
                    uiRenderer.renderPauseOverlay("游戏暂停");
                }
                break;
            case GameState.GAME_LOST:
                uiRenderer.renderGameOverOverlay("游戏结束", "点击重试");
                uiRenderer.renderScoreOverlay(gameState);
                break;
            case GameState.GAME_WON:
                uiRenderer.renderGameOverOverlay("胜利！", "点击继续");
                uiRenderer.renderScoreOverlay(gameState);
                break;
            case GameState.GAME_NOT_STARTED:
                uiRenderer.renderGameOverOverlay("星际塔防", "点击开始");
                break;
        }
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

        // 顶部信息栏背景（科幻面板风格）
        renderer.drawRect(0, screenH - 34, screenW, 34, 0.06f, 0.08f, 0.16f, 0.93f);
        renderer.drawRect(0, screenH - 1, screenW, 2, 0.2f, 0.36f, 0.55f, 0.85f);
        renderer.drawRect(0, screenH - 34, screenW, 1, 0.12f, 0.24f, 0.4f, 0.6f);

        float yTop = screenH - 18;
        float xStart = 12;

        renderer.drawText("L:" + gameState.getLevelData().getLevelNum(), xStart, yTop,
                0.9f, 0.95f, 1.0f, 1.0f);
        renderer.drawText("$" + formatWithCommas(sceneRenderer.getDisplayMoney()), xStart + 80, yTop,
                0.3f, 1.0f, 0.3f, 1.0f);
        // 生命值 + 血量条
        int hp = sceneRenderer.getDisplayHealth();
        renderer.drawText("HP " + hp, xStart + 200, yTop,
                0.15f, 0.92f, 0.28f, 1.0f);
        int hpMax = gameState.getStartingHealth();
        float hpRatio = Math.min(1.0f, (float)hp / Math.max(1, hpMax));
        renderer.drawRect(xStart + 242, screenH - 16, 40, 4, 0.35f, 0.12f, 0.12f, 0.7f);
        renderer.drawRect(xStart + 242, screenH - 16, 40 * hpRatio, 4, 0.1f, 0.85f, 0.22f, 0.9f);

        // 分数显示：处理累计动画
        int score = sceneRenderer.getDisplayScore();
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
            scoreText = "PTS:" + formatWithCommas(score);
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
     * 渲染暂停菜单（ESC 调出）
     */
    private void renderPauseMenu(GameRenderer renderer) {
        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();
        float sw = renderer.getScreenWidth();
        float sh = renderer.getScreenHeight();

        // 半透明遮罩
        renderer.drawRect(0, 0, sw, sh, 0.03f, 0.04f, 0.1f, 0.72f);

        // 居中面板
        float pw = 180, ph = 200;
        float px = sw / 2 - pw / 2;
        float py = sh / 2 - ph / 2;
        renderer.drawRect(px, py, pw, ph, 0.08f, 0.11f, 0.2f, 0.94f);
        renderer.drawRect(px, py + ph - 1, pw, 2, 0.2f, 0.36f, 0.55f, 0.8f);

        // 标题
        float cx = sw / 2;
        renderer.drawText("游戏暂停", cx - 30, py + ph - 24, 0.7f, 0.82f, 0.95f, 1.0f);

        // 按钮（从下到上排列，无重叠）
        float bw = 140, bh = 36, gap = 12;
        float bx = cx - bw / 2;

        float by3 = py + 16;                        // 不保存退出（底）
        float by2 = by3 + bh + gap;                 // 保存并退出（中）
        float by1 = by2 + bh + gap;                 // 继续游戏（顶）

        // 继续游戏（绿）
        renderer.drawRect(bx, by1, bw, bh, 0.08f, 0.28f, 0.12f, 0.9f);
        renderer.drawRect(bx, by1 + bh - 1, bw, 1, 0.15f, 0.45f, 0.2f, 0.7f);
        renderer.drawText("继续游戏", bx + bw/2 - 22, by1 + 14, 0.75f, 0.92f, 0.8f, 1.0f);

        // 保存并退出（蓝）
        renderer.drawRect(bx, by2, bw, bh, 0.06f, 0.12f, 0.3f, 0.9f);
        renderer.drawRect(bx, by2 + bh - 1, bw, 1, 0.12f, 0.25f, 0.5f, 0.7f);
        renderer.drawText("保存并退出", bx + bw/2 - 28, by2 + 14, 0.75f, 0.85f, 0.95f, 1.0f);

        // 不保存退出（红）
        renderer.drawRect(bx, by3, bw, bh, 0.25f, 0.08f, 0.1f, 0.85f);
        renderer.drawRect(bx, by3 + bh - 1, bw, 1, 0.45f, 0.12f, 0.15f, 0.7f);
        renderer.drawText("不保存退出", bx + bw/2 - 28, by3 + 14, 0.9f, 0.65f, 0.65f, 1.0f);

        renderer.end();

        // 处理点击
        if (Gdx.input.justTouched()) {
            float sx = Gdx.input.getX();
            float sy = sh - Gdx.input.getY(); // flip Y
            if (sx >= bx && sx <= bx + bw) {
                if (sy >= by1 && sy <= by1 + bh) {
                    showPauseMenu = false;
                    gameState.togglePause();
                } else if (sy >= by2 && sy <= by2 + bh) {
                    try {
                        game.getGameSaveManager().quickSave(gameState);
                    } catch (Exception e) {
                        Gdx.app.error("GamePlayScreen", "快速保存失败", e);
                    }
                    gameState.endGame(GameState.GAME_NOT_STARTED);
                    showPauseMenu = false;
                    switchScreen(new MainMenuScreen(game));
                } else if (sy >= by3 && sy <= by3 + bh) {
                    // 不保存退出
                    showPauseMenu = false;
                    switchScreen(new MainMenuScreen(game));
                }
            }
        }
    }

    // === GameInputController.GameInputCallbacks 实现 ===

    @Override
    public void onTogglePause() {
        gameState.togglePause();
    }

    @Override
    public void onToggleFastFwd() {
        gameState.toggleFastFwd();
    }

    @Override
    public void onPauseMenuContinue() {
        showPauseMenu = false;
        gameState.togglePause();
    }

    @Override
    public void onPauseMenuSaveAndQuit() {
        try {
            game.getGameSaveManager().quickSave(gameState);
        } catch (Exception e) {
            Gdx.app.error("GamePlayScreen", "快速保存失败", e);
        }
        gameState.endGame(GameState.GAME_NOT_STARTED);
        showPauseMenu = false;
        switchScreen(new MainMenuScreen(game));
    }

    @Override
    public void onPauseMenuQuitWithoutSave() {
        showPauseMenu = false;
        switchScreen(new MainMenuScreen(game));
    }

    @Override
    public void onForceHdFallback() {
        try {
            GameRenderer gr = game.getServices().getRenderer();
            if (gr instanceof LibGdxRenderer) {
                ((LibGdxRenderer) gr).forceHdFallback("F9 key");
            }
        } catch (Throwable ignored) { }
    }

}
