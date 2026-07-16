package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.AchievementData;
import com.rdefense.core.platform.GameRenderer;

import java.util.ArrayList;
import java.util.List;

/**
 * 成就界面 — 分类标签 + 可滚动列表
 */
public class AchievementScreen extends GameScreen implements InputProcessor {

    private static final int ROW_H = 44;
    private static final int TAB_W = 58, TAB_H = 30, TAB_GAP = 5;

    private AchievementData.AchievementCategory selectedCategory = null;
    private int scrollOffset = 0;
    private List<Integer> filtered;

    // 布局
    private int sw, sh;
    private int tabY, listY, listH, backX, backY, backW, backH;
    private int visibleRows;

    public AchievementScreen(RoboDefenseGame game) {
        super(game);
        updateFiltered();
    }

    private InputProcessor prevProcessor;

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
        prevProcessor = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void hide() {
        // 恢复之前的 InputProcessor（如 GamePlayScreen 的输入）
        if (prevProcessor != null) Gdx.input.setInputProcessor(prevProcessor);
    }

    // InputProcessor — 仅需滚轮
    @Override public boolean scrolled(float amtX, float amtY) {
        int maxS = Math.max(0, filtered.size() - visibleRows);
        scrollOffset = Math.max(0, Math.min(maxS, scrollOffset + (amtY > 0 ? -2 : 2)));
        return true;
    }
    @Override public boolean keyDown(int k) { return false; }
    @Override public boolean keyUp(int k) { return false; }
    @Override public boolean keyTyped(char c) { return false; }
    @Override public boolean touchDown(int sx, int sy, int p, int b) { return false; }
    @Override public boolean touchUp(int sx, int sy, int p, int b) { return false; }
    @Override public boolean touchCancelled(int sx, int sy, int p, int b) { return false; }
    @Override public boolean touchDragged(int sx, int sy, int p) { return false; }
    @Override public boolean mouseMoved(int sx, int sy) { return false; }

    // ==================== 筛选 ====================

    private void updateFiltered() {
        filtered = new ArrayList<>();
        for (int i = 0; i < AchievementData.ACHIEVEMENT_TYPE_COUNT; i++) {
            if (selectedCategory != null && AchievementData.getCategory(i) != selectedCategory) continue;
            filtered.add(i);
        }
    }

    private void setCategory(AchievementData.AchievementCategory cat) {
        selectedCategory = cat;
        scrollOffset = 0;
        updateFiltered();
    }

    // ==================== 布局 ====================

    private void computeLayout() {
        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();

        // 分类标签行
        tabY = sh - 60;

        // 列表区域
        listY = tabY - 8;
        listH = listY - 56;
        visibleRows = listH / ROW_H;

        // 返回按钮
        backW = 100; backH = 34;
        backX = sw - backW - 16; backY = 14;
    }

    // ==================== 输入 ====================

    @Override
    protected void update(float delta) {
        computeLayout();

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE))
            { switchScreen(new MainMenuScreen(game)); return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP))
            { scrollOffset = Math.max(0, scrollOffset - 1); return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN))
            { scrollOffset = Math.min(Math.max(0, filtered.size() - visibleRows), scrollOffset + 1); return; }

        if (!Gdx.input.justTouched()) return;
        int x = Gdx.input.getX();
        int y = sh - Gdx.input.getY();

        if (hit(x, y, backX, backY, backW, backH)) { switchScreen(new MainMenuScreen(game)); return; }

        // 分类标签点击
        int tx = 16;
        // "全部" 标签
        if (hit(x, y, tx, tabY, TAB_W, TAB_H)) { setCategory(null); return; }
        tx += TAB_W + TAB_GAP;
        for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
            if (hit(x, y, tx, tabY, TAB_W, TAB_H)) { setCategory(cat); return; }
            tx += TAB_W + TAB_GAP;
        }

        // 滚动条
        int scrollBarX = sw - 16;
        if (filtered.size() > visibleRows && x >= scrollBarX - 8 && x <= scrollBarX + 8 && y >= 56 && y <= listY) {
            float ratio = (float)(listY - y) / listH;
            int maxS = filtered.size() - visibleRows;
            scrollOffset = Math.max(0, Math.min(maxS, (int)(ratio * maxS)));
        }
    }

    private boolean hit(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    // ==================== 渲染 ====================

    @Override
    protected void draw(float delta) {
        computeLayout();
        GameRenderer r = game.getServices().getRenderer();
        r.applyCameraTransform(0, 0, 1.0f);
        r.begin();

        r.drawRect(0, 0, sw, sh, 0.03f, 0.05f, 0.12f, 1.0f);

        // 标题栏
        r.drawRect(0, sh - 34, sw, 34, 0.06f, 0.08f, 0.16f, 0.93f);
        r.drawRect(0, sh - 1, sw, 2, 0.2f, 0.36f, 0.55f, 0.85f);
        r.drawText("成就", 16, sh - 20, 0.75f, 0.85f, 0.95f, 1.0f);
        int earned = AchievementData.totalCount();
        r.drawText(earned + " / " + AchievementData.ACHIEVEMENT_TYPE_COUNT,
                sw - 70, sh - 20, 0.5f, 0.7f, 0.5f, 0.9f);

        // 分类标签
        int tx = 16;
        drawTab(r, tx, tabY, "全部", selectedCategory == null);
        tx += TAB_W + TAB_GAP;
        for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
            boolean sel = cat == selectedCategory;
            String label = cat.label.replace("成就", "");
            drawTab(r, tx, tabY, label, sel);
            tx += TAB_W + TAB_GAP;
        }

        // 列表背景
        r.drawRect(14, 56, sw - 28, listH, 0.05f, 0.07f, 0.14f, 0.7f);

        // 列表项
        int rowY = listY - ROW_H;
        int max = Math.min(scrollOffset + visibleRows, filtered.size());
        for (int i = scrollOffset; i < max; i++) {
            int id = filtered.get(i);
            boolean done = AchievementData.isAchieved(id);
            float br = done ? 0.1f : 0.06f, bg = done ? 0.22f : 0.08f, bb = done ? 0.32f : 0.12f;
            r.drawRect(18, rowY, sw - 36, ROW_H - 2, br, bg, bb, 0.78f);

            String name = AchievementData.getNameZh(id);
            float nr = done ? 0.9f : 0.55f, ng = done ? 0.88f : 0.55f, nb = done ? 0.8f : 0.55f;
            r.drawText(name, 28, rowY + ROW_H - 14, nr, ng, nb, 1.0f);

            // 进度
            int level = AchievementData.getLevel(id);
            int trigger = AchievementData.getTriggerLevel(id);
            if (trigger > 1) {
                r.drawText(level + "/" + trigger, sw - 80, rowY + ROW_H - 14, 0.45f, 0.55f, 0.7f, 0.8f);
            }

            // 状态
            if (done) {
                r.drawRect(sw - 44, rowY + 10, 22, 18, 0.12f, 0.5f, 0.2f, 0.85f);
                r.drawText("OK", sw - 40, rowY + 25, 0.6f, 0.95f, 0.6f, 1.0f);
            } else {
                r.drawRect(sw - 44, rowY + 10, 22, 18, 0.18f, 0.18f, 0.22f, 0.6f);
                r.drawText("--", sw - 38, rowY + 25, 0.4f, 0.4f, 0.4f, 0.8f);
            }

            rowY -= ROW_H;
        }

        // 滚动条
        if (filtered.size() > visibleRows) {
            int sx = sw - 16;
            r.drawRect(sx - 4, 56, 8, listH, 0.12f, 0.12f, 0.18f, 0.5f);
            int handleH = Math.max(24, listH * visibleRows / filtered.size());
            int handleY = 56 + (listH - handleH) * scrollOffset / (filtered.size() - visibleRows);
            r.drawRect(sx - 4, handleY, 8, handleH, 0.35f, 0.4f, 0.55f, 0.85f);
        }

        // 返回
        r.drawRect(backX, backY, backW, backH, 0.08f, 0.12f, 0.25f, 0.88f);
        r.drawRect(backX, backY + backH - 1, backW, 1, 0.2f, 0.3f, 0.5f, 0.6f);
        r.drawText("返回", backX + backW/2 - 12, backY + 13, 0.78f, 0.82f, 0.88f, 1.0f);

        r.end();
    }

    private void drawTab(GameRenderer r, int x, int y, String label, boolean sel) {
        float br = sel ? 0.2f : 0.08f, bg = sel ? 0.3f : 0.1f, bb = sel ? 0.5f : 0.16f;
        r.drawRect(x, y, TAB_W, TAB_H, br, bg, bb, sel ? 0.9f : 0.7f);
        r.drawRect(x, y + TAB_H - 1, TAB_W, 1, sel ? 0.3f : 0.12f, sel ? 0.5f : 0.2f, sel ? 0.7f : 0.3f, 0.5f);
        int tx = x + (TAB_W - label.length() * 7) / 2;
        r.drawText(label, tx, y + TAB_H - 10, sel ? 0.85f : 0.55f, sel ? 0.88f : 0.55f, sel ? 0.95f : 0.55f, 1.0f);
    }
}
