package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.render.SpriteNames;
import com.rdefense.core.save.db.GameSaveManager;

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

    private final String bgSprite;

    public MainMenuScreen(RoboDefenseGame game) {
        super(game);
        int idx = Math.abs((int) (System.currentTimeMillis() / 100)) % 9 + 1;
        this.bgSprite = SpriteNames.titleBackground(idx);
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    protected void update(float delta) {
        if (!Gdx.input.justTouched()) return;

        int x = Gdx.input.getX();
        int y = Gdx.graphics.getHeight() - Gdx.input.getY();
        int sw = Gdx.graphics.getWidth();
        int sh = Gdx.graphics.getHeight();
        int cx = sw / 2;

        // 主按钮：开始游戏
        int mainW = 220, mainH = 48;
        int mainX = cx - mainW / 2, mainY = sh / 2 + 20;
        if (inRect(x, y, mainX, mainY, mainW, mainH)) { startNewGame(); return; }

        // 次要按钮：继续 + 载入
        int secW = 140, secH = 38, secGap = 16;
        int secY = mainY - secH - 18;
        int sec1X = cx - secW - secGap / 2;
        int sec2X = cx + secGap / 2;
        if (inRect(x, y, sec1X, secY, secW, secH)) { resumeGame(); return; }
        if (inRect(x, y, sec2X, secY, secW, secH)) { switchScreen(new GameSaveScreen(game)); return; }

        // 小按钮行：成就 奖励 Credits 设置 退出
        int smSize = 64, smGap = 14, smCount = 5;
        int smTotalW = smCount * smSize + (smCount - 1) * smGap;
        int smX = cx - smTotalW / 2;
        int smY = secY - smSize - 22;
        String[] smLabels = {"成就", "奖励", "Credits", "设置", "退出"};
        for (int i = 0; i < smCount; i++) {
            int bx = smX + i * (smSize + smGap);
            if (inRect(x, y, bx, smY, smSize, smSize)) {
                switch (i) {
                    case 0: switchScreen(new AchievementScreen(game)); break;
                    case 1: switchScreen(new RewardScreen(game)); break;
                    case 2: switchScreen(new CreditsScreen(game)); break;
                    case 3: switchScreen(new OptionsScreen(game)); break;
                    case 4: Gdx.app.exit(); break;
                }
                return;
            }
        }
    }

    private boolean inRect(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
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
        GamePlayScreen gps = new GamePlayScreen(game);
        gps.setResumeMode();
        switchScreen(gps);
    }

    @Override
    protected void draw(float delta) {
        GameRenderer r = game.getServices().getRenderer();
        int sw = r.getScreenWidth();
        int sh = r.getScreenHeight();
        int cx = sw / 2;

        r.begin();

        // 1. 全屏背景图 + 暗色遮罩
        r.drawSprite(bgSprite, 0, 0, sw, sh);
        r.drawRect(0, 0, sw, sh, 0.02f, 0.03f, 0.08f, 0.55f);

        // 2. 标题区域
        float titleY = sh * 0.78f;
        r.drawText("星 际 塔 防", cx - 54, titleY, 0.95f, 0.85f, 0.25f, 1.0f);
        r.drawText("Robo Defense", cx - 48, titleY - 22, 0.55f, 0.62f, 0.75f, 0.9f);

        // 分隔线
        r.drawRect(cx - 80, titleY - 30, 160, 1, 0.22f, 0.36f, 0.55f, 0.5f);

        GameSaveManager saveMgr = game.getGameSaveManager();
        boolean hasSave = (saveMgr != null && saveMgr.hasQuickSave());

        // 3. 主按钮：开始游戏（最大，金色边框）
        int mainW = 220, mainH = 48;
        int mainX = cx - mainW / 2, mainY = sh / 2 + 20;
        drawBtn(r, mainX, mainY, mainW, mainH, "开 始 游 戏",
                0.06f, 0.1f, 0.22f, 0.92f, 0.3f, 0.55f, 0.15f);

        // 4. 次要按钮行：继续游戏 | 载入存档
        int secW = 140, secH = 38, secGap = 16;
        int secY = mainY - secH - 18;
        int sec1X = cx - secW - secGap / 2;
        int sec2X = cx + secGap / 2;
        if (hasSave) {
            drawBtn(r, sec1X, secY, secW, secH, "继续游戏",
                    0.06f, 0.12f, 0.2f, 0.88f, 0.4f, 0.65f, 0.15f);
        } else {
            drawBtn(r, sec1X, secY, secW, secH, "继续游戏",
                    0.1f, 0.1f, 0.14f, 0.5f, 0.2f, 0.2f, 0.15f);
        }
        drawBtn(r, sec2X, secY, secW, secH, "载入存档",
                0.06f, 0.12f, 0.2f, 0.88f, 0.4f, 0.65f, 0.15f);

        // 5. 小按钮行：成就 | 奖励 | Credits | 设置 | 退出
        int smSize = 64, smGap = 14;
        String[] smLabels = {"成就", "奖励", "Credits", "设置", "退出"};
        float[][] smColors = {
            {0.15f, 0.22f, 0.35f}, {0.22f, 0.18f, 0.3f},
            {0.15f, 0.25f, 0.3f},   {0.18f, 0.22f, 0.28f},
            {0.28f, 0.12f, 0.14f}
        };
        int smTotalW = smLabels.length * smSize + (smLabels.length - 1) * smGap;
        int smX = cx - smTotalW / 2;
        int smY = secY - smSize - 18;
        for (int i = 0; i < smLabels.length; i++) {
            int bx = smX + i * (smSize + smGap);
            float[] c = smColors[i];
            drawBtn(r, bx, smY, smSize, smSize, smLabels[i],
                    c[0], c[1], c[2], 0.85f, c[0] * 1.8f, c[1] * 1.6f, c[2] * 1.6f);
        }

        // 6. 底部信息
        long pts = RewardData.getRewardPoints();
        r.drawText("奖励积分  " + pts, 16, 26, 0.8f, 0.72f, 0.28f, 1.0f);
        r.drawText("V2.5.0 Build 2900", sw - 120, 26, 0.45f, 0.5f, 0.6f, 0.8f);

        r.end();
    }

    /** 绘制科幻风格按钮（文字自适应居中） */
    private void drawBtn(GameRenderer r, int x, int y, int w, int h, String label,
                         float br, float bg, float bb, float ba, float lr, float lg, float lb) {
        r.drawRect(x, y, w, h, br, bg, bb, ba);
        r.drawRect(x, y + h - 1, w, 1, lr, lg, lb, 0.7f);
        // 中文字符约 7px 宽，英文约 4px，混合估算约 6px/字符
        float textW = label.length() * 6f;
        float tx = x + (w - textW) / 2f;
        float ty = y + (h + 5) / 2f;
        r.drawText(label, tx, ty, 0.82f, 0.85f, 0.9f, 1.0f);
    }
}
