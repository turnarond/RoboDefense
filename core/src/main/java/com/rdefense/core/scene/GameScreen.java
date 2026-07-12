package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.platform.GameRenderer;

/**
 * 游戏场景基类 — 提供通用的场景生命周期管理
 * 所有游戏场景（主菜单、关卡选择、游戏、设置）都继承此类
 */
public abstract class GameScreen implements Screen {

    protected final RoboDefenseGame game;
    private String shortMessage;
    private int shortMessageTimer;
    private static final int SHORT_MESSAGE_DURATION = 150;

    public GameScreen(RoboDefenseGame game) {
        this.game = game;
    }

    /**
     * 场景初始化（在 show 之后调用一次）
     */
    protected abstract void init();

    /**
     * 更新游戏逻辑
     * @param delta 距上一帧的时间（秒）
     */
    protected abstract void update(float delta);

    /**
     * 渲染画面
     * @param delta 距上一帧的时间（秒）
     */
    protected abstract void draw(float delta);

    @Override
    public void show() {
        init();
    }

    @Override
    public void render(float delta) {
        // 清屏
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        update(delta);
        draw(delta);
        updateShortMessage();
        renderShortMessage();
    }

    private void updateShortMessage() {
        if (shortMessageTimer > 0) {
            shortMessageTimer--;
            if (shortMessageTimer <= 0) {
                shortMessage = null;
            }
        }
    }

    private void renderShortMessage() {
        if (shortMessage == null || shortMessageTimer <= 0) {
            return;
        }
        GameRenderer renderer = game.getServices().getRenderer();
        if (renderer == null) {
            return;
        }
        renderer.begin();
        float x = renderer.getScreenWidth() / 2.0f;
        float y = renderer.getScreenHeight() - 22;
        renderer.drawText(shortMessage, x, y, 1.0f, 0.9f, 0.3f, 1.0f);
        renderer.end();
    }

    @Override
    public void resize(int width, int height) {
        // 子类按需重写
    }

    @Override
    public void pause() {
        // 子类按需重写
    }

    @Override
    public void resume() {
        // 子类按需重写
    }

    @Override
    public void hide() {
        // 子类按需重写
    }

    @Override
    public void dispose() {
        // 子类按需重写
    }

    /**
     * 切换到指定场景
     */
    protected void switchScreen(GameScreen newScreen) {
        dispose();
        game.setScreen(newScreen);
    }

    /**
     * 在屏幕上显示一条短消息（默认存储到基类显示逻辑）
     */
    public void showShortMessage(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        this.shortMessage = msg;
        this.shortMessageTimer = SHORT_MESSAGE_DURATION;
    }
}
