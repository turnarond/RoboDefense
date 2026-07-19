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

    /** 标准标题栏（深蓝底条 + 蓝顶线 + 标题文字）。7 Screen 共用。 */
    protected void drawTitleBar(String title) {
        int sh = com.badlogic.gdx.Gdx.graphics.getHeight();
        int sw = com.badlogic.gdx.Gdx.graphics.getWidth();
        com.rdefense.core.platform.GameRenderer r = game.getServices().getRenderer();
        r.drawRect(0, sh - 34, sw, 34, 0.06f, 0.08f, 0.16f, 0.93f);
        r.drawRect(0, sh - 1, sw, 2, 0.2f, 0.36f, 0.55f, 0.85f);
        r.drawText(title, 16, sh - 20, 0.75f, 0.85f, 0.95f, 1.0f);
    }

    /** 标准返回按钮（科幻蓝色）。6 Screen 共用。 */
    protected void drawBackButton(float x, float y, float w, float h) {
        com.rdefense.core.platform.GameRenderer r = game.getServices().getRenderer();
        r.drawRect(x, y, w, h, 0.08f, 0.12f, 0.25f, 0.93f);
        r.drawRect(x, y, w, 2, 0.2f, 0.3f, 0.5f, 0.9f);
        r.drawText("返回", x + w / 2 - 12, y + h / 2 - 5, 0.78f, 0.82f, 0.88f, 1.0f);
    }

    /** 千位分隔格式化 */
    public static String formatWithCommas(int value) {
        String s = Integer.toString(value);
        StringBuilder sb = new StringBuilder(s.length() + 2);
        int start = s.length() % 3;
        if (start == 0) start = 3;
        sb.append(s, 0, start);
        for (int i = start; i < s.length(); i += 3) {
            sb.append(',');
            sb.append(s, i, Math.min(i + 3, s.length()));
        }
        return sb.toString();
    }
}
