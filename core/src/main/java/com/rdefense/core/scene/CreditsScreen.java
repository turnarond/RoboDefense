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
        GameRenderer r = game.getServices().getRenderer();
        int sw = r.getScreenWidth(), sh = r.getScreenHeight();
        r.applyCameraTransform(0, 0, 1.0f);
        r.begin();

        r.drawRect(0, 0, sw, sh, 0.03f, 0.05f, 0.12f, 1.0f);

        // 标题栏
        drawTitleBar("Credits");

        // 内容区（居中半透明面板）
        int pw = Math.min(sw - 64, 420), ph = 260;
        int px = (sw - pw) / 2, py = (sh - ph) / 2 - 10;
        r.drawRect(px, py, pw, ph, 0.06f, 0.09f, 0.18f, 0.9f);
        r.drawRect(px, py + ph - 1, pw, 1, 0.18f, 0.32f, 0.5f, 0.6f);

        String[] lines = {"星际塔防", "Robo Defense", "",
            "原作 MagicWach  |  移植 Open Source", "",
            "libGDX 引擎  |  SimHei 字体", "",
            "仅供学习交流使用  |  感谢所有支持者"};
        float ty = py + ph - 30;
        for (String s : lines) {
            if (s.isEmpty()) { ty -= 14; continue; }
            r.drawText(s, px + pw / 2f - s.length() * 3.5f, ty, 0.7f, 0.75f, 0.85f, 1.0f);
            ty -= 22;
        }

        // 返回按钮
        int bw = 100, bh = 34;
        int bx = sw - bw - 16, by = 14;
        r.drawRect(bx, by, bw, bh, 0.08f, 0.12f, 0.25f, 0.88f);
        r.drawRect(bx, by + bh - 1, bw, 1, 0.2f, 0.3f, 0.5f, 0.6f);
        r.drawText("返回", bx + 28, by + 13, 0.78f, 0.82f, 0.88f, 1.0f);

        r.end();
    }
}
