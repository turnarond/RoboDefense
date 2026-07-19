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
        // 世界坐标层（1 次 begin/end，包含背景/塔/敌人/子弹/事件/预览）
        sceneRenderer.renderWorldLayer(gameState, stateIndex);

        // 升级对话框（条件绘制，自持 batch——不自动暂停，玩家按 ESC 手动暂停）
        if (runState == GameState.GAME_RUNNING || runState == GameState.GAME_FAST_FWD
                || runState == GameState.GAME_PAUSED) {
            uiRenderer.renderUpgradeDialog(gameState.getMoney(), stateIndex);
        }

        // 8.1 绘制放置失败提示
        uiRenderer.renderPlacementFailure();
        uiRenderer.updatePlacementFailure();
        // 绘制短消息（如 HD 回退提示）
        uiRenderer.renderShortMessage();

        // 8.2 更新和绘制成就弹窗动画
        game.getAchievementRenderer().update(game.getServices().getAudio());
        game.getAchievementRenderer().draw(renderer);

        // 屏幕坐标层（1 次 begin/end，包含 HUD + 塔按钮 + 控制按钮 + 缩放滑块 + 塔精灵）
        int currentFps = gameLoop.getCurrentFps();
        int batteryLevel = game.getServices().getBatteryLevel();
        sceneRenderer.renderScreenLayer(gameState, stateIndex, gameState.getMoney(), options, currentFps, batteryLevel);

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
     * 渲染暂停菜单（ESC 调出）
     */
    private void renderPauseMenu(GameRenderer renderer) {
        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();
        float sw = renderer.getScreenWidth();
        float sh = renderer.getScreenHeight();

        // 轻遮罩——游戏仍然可见
        renderer.drawRect(0, 0, sw, sh, 0.02f, 0.03f, 0.06f, 0.45f);

        // 玻璃面板（半透明，游戏透出）
        float pw = 170, ph = 185;
        float px = sw / 2 - pw / 2;
        float py = sh / 2 - ph / 2;
        renderer.drawRect(px, py, pw, ph, 0.06f, 0.08f, 0.16f, 0.85f);
        renderer.drawRect(px, py + ph - 1, pw, 2, 0.83f, 0.67f, 0.16f, 0.5f);

        // 标题
        float cx = sw / 2;
        renderer.drawText("暂停", cx - 14, py + ph - 22, 0.75f, 0.82f, 0.92f, 1.0f);

        // 三个按钮
        float bw = 130, bh = 34, gap = 8;
        float bx = cx - bw / 2;
        float by3 = py + 14;
        float by2 = by3 + bh + gap;
        float by1 = by2 + bh + gap;

        // 继续游戏
        renderer.drawRect(bx, by1, bw, bh, 0.08f, 0.24f, 0.12f, 0.8f);
        renderer.drawText("继续游戏", bx + bw/2 - 22, by1 + 14, 0.78f, 0.9f, 0.8f, 1.0f);
        // 保存并退出
        renderer.drawRect(bx, by2, bw, bh, 0.06f, 0.12f, 0.25f, 0.8f);
        renderer.drawText("保存并退出", bx + bw/2 - 28, by2 + 14, 0.75f, 0.85f, 0.92f, 1.0f);
        // 不保存退出
        renderer.drawRect(bx, by3, bw, bh, 0.3f, 0.1f, 0.1f, 0.78f);
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
