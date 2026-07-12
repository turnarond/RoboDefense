package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.config.OptionsData;
import com.rdefense.core.platform.GameRenderer;

/**
 * 选项场景 — 显示并切换用户设置项
 */
public class OptionsScreen extends GameScreen {

    private static final int ROW_HEIGHT = 40;
    private static final int ROW_PADDING = 8;
    private static final int TITLE_HEIGHT = 60;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 40;

    private final OptionsData options;
    private int pressedIndex = -1;

    public OptionsScreen(RoboDefenseGame game) {
        super(game);
        this.options = game.getOptions();
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    protected void update(float delta) {
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            int backX = screenWidth - BUTTON_WIDTH - 16;
            int backY = 16;
            if (x >= backX && x <= backX + BUTTON_WIDTH && y >= backY && y <= backY + BUTTON_HEIGHT) {
                switchScreen(new MainMenuScreen(game));
                return;
            }

            int optionCount = OptionsData.OPTION_TYPE_COUNT;
            int startY = screenHeight - TITLE_HEIGHT - ROW_PADDING;
            for (int i = 0; i < optionCount; i++) {
                int rowY = startY - i * (ROW_HEIGHT + ROW_PADDING) - ROW_HEIGHT;
                if (y >= rowY && y <= rowY + ROW_HEIGHT) {
                    boolean updated = options.setOptionValue(i, !options.optionValue(i));
                    if (updated) {
                        game.applyOptions();
                    }
                    return;
                }
            }
        }

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
        }
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        renderer.drawRect(0, 0, screenWidth, screenHeight, 0.02f, 0.02f, 0.06f, 1.0f);
        renderer.drawText("设置", 24, screenHeight - 24, 1.0f, 0.9f, 0.7f, 1.0f);
        renderer.drawText("点击切换选项，Esc 或 返回 返回主菜单", 24, screenHeight - 52, 0.8f, 0.8f, 0.8f, 1.0f);

        int startY = screenHeight - TITLE_HEIGHT - ROW_PADDING;
        for (int i = 0; i < OptionsData.OPTION_TYPE_COUNT; i++) {
            int rowY = startY - i * (ROW_HEIGHT + ROW_PADDING) - ROW_HEIGHT;
            boolean value = options.optionValue(i);
            float itemR = value ? 0.12f : 0.12f;
            float itemG = value ? 0.5f : 0.12f;
            float itemB = value ? 0.12f : 0.12f;
            float itemA = 0.8f;
            renderer.drawRect(16, rowY, screenWidth - 32, ROW_HEIGHT, itemR, itemG, itemB, itemA);

            String label = options.optionName(i);
            renderer.drawText(label, 28, rowY + ROW_HEIGHT - 14, 1.0f, 1.0f, 1.0f, 1.0f);
            String state = value ? "开" : "关";
            renderer.drawText(state, screenWidth - 52, rowY + ROW_HEIGHT - 14, 0.9f, 0.9f, 0.9f, 1.0f);
        }

        int backX = screenWidth - BUTTON_WIDTH - 16;
        int backY = 16;
        renderer.drawRect(backX, backY, BUTTON_WIDTH, BUTTON_HEIGHT, 0.2f, 0.2f, 0.5f, 0.9f);
        renderer.drawText("返回", backX + 24, backY + BUTTON_HEIGHT - 14, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }
}
