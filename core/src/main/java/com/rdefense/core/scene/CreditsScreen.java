package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.platform.GameRenderer;

/**
 * 致谢场景 — 对应原版 CreditsActivity
 * <p>移植原版 credits 静态信息（版本号、作者、许可等），便于桌面端用户查看。</p>
 */
public class CreditsScreen extends GameScreen {

    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 40;
    private static final int SCROLL_SPEED = 30; // 每秒滚动像素

    private float scrollY = 0f;
    private float contentHeight = 0f;

    public CreditsScreen(RoboDefenseGame game) {
        super(game);
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
        scrollY = 0f;
    }

    @Override
    protected void update(float delta) {
        // 向上滚动
        scrollY -= SCROLL_SPEED * delta;
        int screenHeight = Gdx.graphics.getHeight();
        if (contentHeight > 0 && -scrollY > contentHeight - screenHeight * 0.6f) {
            scrollY = -(contentHeight - screenHeight * 0.6f);
        }

        if (Gdx.input.justTouched() || Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            int x = Gdx.input.getX();
            int y = screenHeight - Gdx.input.getY();
            int screenWidth = Gdx.graphics.getWidth();
            int backX = screenWidth - BUTTON_WIDTH - 16;
            int backY = 16;
            if (x >= backX && x <= backX + BUTTON_WIDTH && y >= backY && y <= backY + BUTTON_HEIGHT) {
                switchScreen(new MainMenuScreen(game));
            } else if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
                switchScreen(new MainMenuScreen(game));
            }
        }
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        renderer.drawRect(0, 0, screenWidth, screenHeight, 0.04f, 0.04f, 0.1f, 1.0f);

        // 滚动文字
        String[] credits = new String[] {
            "星际塔防",
            "Robo Defense",
            "",
            "原作: MagicWach",
            "移植与重构: Open Source Contributors",
            "",
            "使用 libGDX 引擎",
            "www.libgdx.com",
            "",
            "字体: SimHei (黑体)",
            "",
            "本项目仅供学习交流使用",
            "感谢所有支持者"
        };

        int y = (int) (screenHeight - 60 + scrollY);
        for (String line : credits) {
            renderer.drawText(line, 32, y, 0.85f, 0.85f, 0.85f, 1.0f);
            y -= 24;
        }
        contentHeight = screenHeight - 60 - y;

        int backX = screenWidth - BUTTON_WIDTH - 16;
        int backY = 16;
        renderer.drawRect(backX, backY, BUTTON_WIDTH, BUTTON_HEIGHT, 0.2f, 0.2f, 0.5f, 0.9f);
        renderer.drawText("返回", backX + 24, backY + BUTTON_HEIGHT - 14, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }
}
