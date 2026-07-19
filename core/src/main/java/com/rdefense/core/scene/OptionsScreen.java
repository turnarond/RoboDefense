package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.config.OptionsData;
import com.rdefense.core.platform.GameRenderer;

/**
 * 选项场景 — 显示并切换用户设置项
 */
public class OptionsScreen extends GameScreen {

    private static final int ROW_HEIGHT = 38;
    private static final int ROW_SPACING = 44;

    private final OptionsData options;
    private int sw, sh, backX, backY, backW, backH;

    public OptionsScreen(RoboDefenseGame game) {
        super(game);
        this.options = game.getOptions();
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    private void computeLayout() {
        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();
        backW = 100; backH = 34;
        backX = sw - backW - 16; backY = 14;
    }

    @Override
    protected void update(float delta) {
        computeLayout();
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = sh - Gdx.input.getY();

            if (hit(x, y, backX, backY, backW, backH)) {
                switchScreen(new MainMenuScreen(game)); return;
            }

            int startY = sh - 60;
            for (int i = 0; i < OptionsData.OPTION_TYPE_COUNT; i++) {
                int rowY = startY - i * ROW_SPACING;
                if (hit(x, y, 16, rowY, sw - 32, ROW_HEIGHT)) {
                    if (options.setOptionValue(i, !options.optionValue(i))) game.applyOptions();
                    return;
                }
            }
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
        }
    }

    private boolean hit(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    @Override
    protected void draw(float delta) {
        computeLayout();
        GameRenderer r = game.getServices().getRenderer();
        r.applyCameraTransform(0, 0, 1.0f);
        r.begin();

        r.drawRect(0, 0, sw, sh, 0.03f, 0.05f, 0.12f, 1.0f);

        // 标题栏
        drawTitleBar("设置");

        // 选项列表（与 update 坐标完全一致）
        int startY = sh - 60;
        for (int i = 0; i < OptionsData.OPTION_TYPE_COUNT; i++) {
            int rowY = startY - i * ROW_SPACING;
            boolean v = options.optionValue(i);
            float rowR = v ? 0.1f : 0.08f, rowG = v ? 0.25f : 0.12f, rowB = v ? 0.15f : 0.18f;
            r.drawRect(16, rowY, sw - 32, ROW_HEIGHT, rowR, rowG, rowB, 0.85f);
            r.drawRect(16, rowY + ROW_HEIGHT - 1, sw - 32, 1, v ? 0.2f : 0.12f, v ? 0.4f : 0.2f, v ? 0.28f : 0.25f, 0.5f);
            r.drawText(options.optionName(i), 28, rowY + 14, 0.8f, 0.84f, 0.9f, 1.0f);
            r.drawText(v ? "开" : "关", sw - 48, rowY + 14, v ? 0.3f : 0.55f, v ? 0.9f : 0.5f, v ? 0.35f : 0.5f, 1.0f);
        }

        r.drawRect(backX, backY, backW, backH, 0.08f, 0.12f, 0.25f, 0.88f);
        r.drawRect(backX, backY + backH - 1, backW, 1, 0.2f, 0.3f, 0.5f, 0.6f);
        r.drawText("返回", backX + backW/2 - 12, backY + 13, 0.78f, 0.82f, 0.88f, 1.0f);

        r.end();
    }
}
