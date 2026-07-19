package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.render.RewardRenderer;

public class RewardScreen extends GameScreen implements InputProcessor {

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 34;

    private RewardRenderer rewardRenderer;
    private int scrollOffset = 0;
    private int maxScrollOffset = 0;

    public RewardScreen(RoboDefenseGame game) {
        super(game);
        rewardRenderer = new RewardRenderer();
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
        Gdx.input.setInputProcessor(this);
    }

    @Override
    protected void update(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            scrollOffset = Math.max(0, scrollOffset - 8);
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            scrollOffset = Math.min(maxScrollOffset, scrollOffset + 8);
        }

        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            handleClick(x, y);
        }

        // 更新最大滚动偏移量
        maxScrollOffset = getMaxScrollOffset();
    }

    private int getMaxScrollOffset() {
        int screenHeight = Gdx.graphics.getHeight();
        int totalRows = RewardData.REWARD_TYPE_COUNT;
        int totalHeight = totalRows * (48 + 4);  // 行高 + padding
        return Math.max(0, totalHeight - screenHeight + 120);
    }

    private void handleClick(int screenX, int screenY) {
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        int backX = screenWidth - BUTTON_WIDTH - 16;
        int backY = 16;
        if (screenX >= backX && screenX <= backX + BUTTON_WIDTH &&
                screenY >= backY && screenY <= backY + BUTTON_HEIGHT) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        // 检查点击的行
        int rowIndex = rewardRenderer.getRowAt(screenX, screenY, scrollOffset, screenHeight);
        if (rowIndex >= 0) {
            if (RewardData.isBlocked(rowIndex) != null) {
                return;
            }
            if (RewardData.canAfford(rowIndex)) {
                RewardData.tryUpgrade(rowIndex);
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

        // 背景
        renderer.drawRect(0, 0, screenWidth, screenHeight, 0.03f, 0.05f, 0.12f, 1.0f);

        // 标题栏
        drawTitleBar("奖励商店");

        long points = RewardData.getRewardPoints();
        renderer.drawText("积分 " + formatPoints(points), 140, screenHeight - 20, 0.82f, 0.75f, 0.28f, 1.0f);
        renderer.drawText("共 " + RewardData.REWARD_TYPE_COUNT + " 项", screenWidth - 90, screenHeight - 20, 0.45f, 0.5f, 0.6f, 0.8f);

        // 列表表头
        renderer.drawRect(12, screenHeight - 52, screenWidth - 24, 1, 0.12f, 0.2f, 0.35f, 0.5f);
        renderer.drawText("名称", 40, screenHeight - 62, 0.5f, 0.55f, 0.65f, 0.9f);
        renderer.drawText("等级", 180, screenHeight - 62, 0.5f, 0.55f, 0.65f, 0.9f);
        renderer.drawText("效果", 280, screenHeight - 62, 0.5f, 0.55f, 0.65f, 0.9f);
        renderer.drawText("操作", 390, screenHeight - 62, 0.5f, 0.55f, 0.65f, 0.9f);

        // 渲染奖励列表
        rewardRenderer.drawRewardGrid(renderer, scrollOffset, points);

        // 返回按钮
        int backX = screenWidth - BUTTON_WIDTH - 16;
        int backY = 16;
        drawBackButton(backX, backY, BUTTON_WIDTH, BUTTON_HEIGHT);

        renderer.end();
    }

    private String formatPoints(long points) {
        if (points >= 1000000) {
            return String.format("%.1fM", points / 1000000.0);
        } else if (points >= 1000) {
            return String.format("%dK", points / 1000);
        }
        return String.valueOf(points);
    }

    // InputProcessor 接口实现 - 处理滚轮事件
    @Override
    public boolean scrolled(float amountX, float amountY) {
        // amountY > 0 表示向下滚动，amountY < 0 表示向上滚动
        scrollOffset = Math.max(0, Math.min(maxScrollOffset, scrollOffset + (int)(amountY * 15)));
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
