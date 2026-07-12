package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.render.SpriteNames;
import com.rdefense.core.save.PlayerPrefs;
import com.rdefense.core.save.db.GameSaveManager;

import java.util.Random;

/**
 * 主菜单场景 — 对应原版 TitleActivity
 *
 * <p>参照 Android 原版 TitleActivity 的设计（保持布局语义一致）：</p>
 * <ul>
 *   <li>按钮布局：开始游戏、继续存档、成就、奖励、Credits、退出、设置</li>
 *   <li>继续游戏按钮在无存档时灰显</li>
 *   <li>奖励积分显示（第一阶段占位）</li>
 *   <li>版本号显示</li>
 *   <li>支持键盘快捷键（数字键 1-7 直达对应功能）</li>
 *   <li>支持点击屏幕中央进入"开始游戏"</li>
 * </ul>
 */
public class MainMenuScreen extends GameScreen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 42;
    private static final int BUTTON_GAP = 6;
    private static final int BUTTON_LEFT = 16;

    /** 当前选中的标题背景图（与原版 ConcurrentBackground 类似，每次启动随机选 1-9） */
    private final String bgSprite;

    public MainMenuScreen(RoboDefenseGame game) {
        super(game);
        // 模拟原版 ConcurrentBackground.changeMap：根据时间戳随机选一张标题图
        int idx = Math.abs((int) (System.currentTimeMillis() / 100)) % 9 + 1;
        this.bgSprite = SpriteNames.titleBackground(idx);
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    /**
     * 主菜单按钮定义（与原版 R.id.* 一一对应）
     */
    private enum MenuButton {
        NEW_GAME(1, "新游戏"),
        RESUME(2, "继续游戏"),
        LOAD_SAVE(3, "载入存档"),
        ACHIEVEMENTS(4, "成就"),
        REWARDS(5, "奖励"),
        CREDITS(6, "Credits"),
        EXIT(7, "退出"),
        SETTINGS(8, "设置");

        final int id;
        final String label;
        MenuButton(int id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    @Override
    protected void update(float delta) {
        // 键盘快捷键
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            // 桌面端不主动退出，让用户通过退出按钮明确操作
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.N)) {
            startNewGame();
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
            resumeGame();
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.L)) {
            switchScreen(new GameSaveScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.A)) {
            switchScreen(new AchievementScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.W)) {
            switchScreen(new RewardScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.C)) {
            switchScreen(new CreditsScreen(game));
            return;
        }

        // 鼠标点击
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();

            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            // 设置按钮在右上角
            int settingsX = screenWidth - BUTTON_WIDTH - 16;
            int settingsY = screenHeight - BUTTON_HEIGHT - 16;
            if (x >= settingsX && x <= settingsX + BUTTON_WIDTH &&
                    y >= settingsY && y <= settingsY + BUTTON_HEIGHT) {
                switchScreen(new OptionsScreen(game));
                return;
            }

            // 中央面板内的"点击开始"
            int centerX = screenWidth / 2;
            int centerY = screenHeight / 2;
            int panelHalf = 100;
            if (x >= centerX - panelHalf && x <= centerX + panelHalf &&
                    y >= centerY - panelHalf && y <= centerY + panelHalf) {
                startNewGame();
                return;
            }

            // 左侧按钮列
            int buttonX = BUTTON_LEFT;
            int buttonY = screenHeight - 80;
            for (MenuButton btn : MenuButton.values()) {
                if (btn == MenuButton.SETTINGS) continue;
                if (x >= buttonX && x <= buttonX + BUTTON_WIDTH &&
                        y >= buttonY && y <= buttonY + BUTTON_HEIGHT) {
                    onMenuClick(btn);
                    return;
                }
                buttonY -= BUTTON_HEIGHT + BUTTON_GAP;
            }
        }
    }

    private void onMenuClick(MenuButton btn) {
        switch (btn) {
            case NEW_GAME:      startNewGame(); break;
            case RESUME:        resumeGame(); break;
            case LOAD_SAVE:     switchScreen(new GameSaveScreen(game)); break;
            case ACHIEVEMENTS:  switchScreen(new AchievementScreen(game)); break;
            case REWARDS:       switchScreen(new RewardScreen(game)); break;
            case CREDITS:       switchScreen(new CreditsScreen(game)); break;
            case EXIT:          Gdx.app.exit(); break;
            case SETTINGS:      switchScreen(new OptionsScreen(game)); break;
        }
    }

    /**
     * 开始新游戏 — 对应原版 TitleActivity.newGame()
     * 原版行为：清除快速存档后跳转到 LevelSelectActivity
     */
    private void startNewGame() {
        GameSaveManager saveManager = game.getGameSaveManager();
        if (saveManager != null && saveManager.hasQuickSave()) {
            saveManager.clearQuickSave();
        }
        switchScreen(new LevelSelectScreen(game));
    }

    /**
     * 继续游戏 — 对应原版 TitleActivity.resumeGame()
     * 仅在有快速存档时启用
     */
    private void resumeGame() {
        GameSaveManager saveManager = game.getGameSaveManager();
        if (saveManager == null || !saveManager.hasQuickSave()) {
            showShortMessage("没有可用的存档");
            return;
        }
        switchScreen(new GamePlayScreen(game, true));
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        renderer.begin();

        // 1. 全屏背景图
        renderer.drawSprite(bgSprite, 0, 0, screenWidth, screenHeight);

        // 2. 中央半透明暗色面板
        float panelW = Math.min(screenWidth * 0.55f, 480);
        float panelH = 200;
        float panelX = (screenWidth - panelW) / 2f;
        float panelY = (screenHeight - panelH) / 2f;
        renderer.drawRect(panelX, panelY, panelW, panelH, 0f, 0f, 0f, 0.65f);

        // 3. 主标题
        float titleX = screenWidth / 2f - 72;
        float titleY = panelY + panelH - 32;
        renderer.drawText("星际塔防", titleX, titleY, 1.0f, 0.9f, 0.2f, 1.0f);

        // 4. 副标题（英文）
        float subX = screenWidth / 2f - 54;
        float subY = titleY - 28;
        renderer.drawText("Robo Defense", subX, subY, 0.8f, 0.8f, 0.8f, 1.0f);

        // 5. 提示文字（闪烁效果，与原版相同节奏）
        boolean blink = (System.currentTimeMillis() / 600) % 2 == 0;
        if (blink) {
            float hintX = screenWidth / 2f - 60;
            float hintY = panelY + 40;
            renderer.drawText("点击中央开始游戏", hintX, hintY, 0.6f, 1.0f, 0.6f, 1.0f);
        }

        // 6. 左侧按钮列（与原版按钮顺序保持一致）
        int buttonX = BUTTON_LEFT;
        int buttonY = screenHeight - 80;
        GameSaveManager saveManager = game.getGameSaveManager();
        boolean hasSave = (saveManager != null && saveManager.hasQuickSave());
        for (MenuButton btn : MenuButton.values()) {
            if (btn == MenuButton.SETTINGS) continue;
            boolean enabled = (btn == MenuButton.RESUME) ? hasSave : true;
            drawMenuButton(renderer, btn.label, buttonX, buttonY, enabled);
            buttonY -= BUTTON_HEIGHT + BUTTON_GAP;
        }

        // 7. 右上角设置按钮
        int settingsX = screenWidth - BUTTON_WIDTH - 16;
        int settingsY = screenHeight - BUTTON_HEIGHT - 16;
        drawMenuButton(renderer, "设置", settingsX, settingsY, true);

        // 8. 底部信息栏：奖励积分 + 版本号（对应原版 reward_msg + version TextView）
        long rewardPoints = RewardData.getRewardPoints();
        renderer.drawText("奖励积分: " + rewardPoints, 16, 24, 1.0f, 0.85f, 0.3f, 1.0f);
        renderer.drawText("V2.5.0  Build 2900", screenWidth - 130, 24, 0.7f, 0.7f, 0.7f, 1.0f);

        renderer.end();
    }

    private void drawMenuButton(GameRenderer renderer, String label, int x, int y, boolean enabled) {
        if (enabled) {
            renderer.drawRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 0.1f, 0.1f, 0.3f, 0.9f);
        } else {
            renderer.drawRect(x, y, BUTTON_WIDTH, BUTTON_HEIGHT, 0.15f, 0.15f, 0.18f, 0.6f);
        }
        float r = enabled ? 1.0f : 0.5f;
        float g = enabled ? 1.0f : 0.5f;
        float b = enabled ? 1.0f : 0.5f;
        renderer.drawText(label, x + 18, y + BUTTON_HEIGHT - 14, r, g, b, 1.0f);
    }
}
