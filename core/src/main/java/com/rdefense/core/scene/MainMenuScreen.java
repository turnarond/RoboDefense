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
    private boolean showNewGameConfirm = false;

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

        // 确认对话框（优先）
        if (showNewGameConfirm) {
            int dlgX = cx - 130, dlgY = sh / 2 - 42;
            if (inRect(x, y, dlgX + 40, dlgY + 44, 70, 26)) {
                GameSaveManager saveMgr = game.getGameSaveManager();
                if (saveMgr != null) saveMgr.clearQuickSave();
                switchScreen(new LevelSelectScreen(game));
            } else {
                showNewGameConfirm = false;
            }
            return;
        }

        // 主按钮：开始游戏 (240x52)
        int mainW = 240, mainH = 52;
        int mainX = cx - mainW / 2, mainY = sh / 2 + 16;
        if (inRect(x, y, mainX, mainY, mainW, mainH)) {
            GameSaveManager saveMgr = game.getGameSaveManager();
            if (saveMgr != null && saveMgr.hasQuickSave()) {
                showNewGameConfirm = true;
            } else {
                switchScreen(new LevelSelectScreen(game));
            }
            return;
        }

        // 次要按钮：继续 + 载入 (150x42)
        int secW = 150, secH = 42, secGap = 20;
        int secY = mainY - secH - 14;
        int sec1X = cx - secW - secGap / 2;
        int sec2X = cx + secGap / 2;
        if (inRect(x, y, sec1X, secY, secW, secH)) { resumeGame(); return; }
        if (inRect(x, y, sec2X, secY, secW, secH)) { switchScreen(new GameSaveScreen(game)); return; }

        // 小按钮行：成就 奖励 制作 设置 退出 (58x58)
        int smSize = 58, smGap = 10, smCount = 5;
        int smTotalW = smCount * smSize + (smCount - 1) * smGap;
        int smX = cx - smTotalW / 2;
        int smY = secY - smSize - 16;
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

    // 扫描线动画：每 4 秒从标题上方扫到下方
    private float scanY = -1;
    private int scanPhase = 0; // 0=扫下, 1=淡出, 2=等待
    private int scanTimer = 0;

    @Override
    protected void draw(float delta) {
        GameRenderer r = game.getServices().getRenderer();
        int sw = r.getScreenWidth();
        int sh = r.getScreenHeight();
        int cx = sw / 2;

        r.begin();

        // === 1. 深空背景 ===
        r.drawSprite(bgSprite, 0, 0, sw, sh);
        r.drawRect(0, 0, sw, sh, 0.02f, 0.02f, 0.04f, 0.65f);

        // === 2. 信标标题区 ===
        float titleY = (int)(sh * 0.82f);
        // 主标题——指挥金
        r.drawText("星 际 塔 防", cx - 54, titleY, 0.83f, 0.67f, 0.16f, 1.0f);
        // 副标题——全息青
        r.drawText("ROBODEFENSE", cx - 46, titleY - 22, 0.05f, 0.94f, 0.94f, 0.85f);
        // 细分割线
        r.drawRect(cx - 90, titleY - 32, 180, 1, 0.15f, 0.45f, 0.55f, 0.4f);

        // === 扫描线动画 ===
        if (scanY < 0) { scanY = titleY + 28; scanPhase = 0; scanTimer = 0; }
        scanTimer++;
        float scanAlpha;
        if (scanPhase == 0) { // 扫下
            scanY -= 0.8f;
            scanAlpha = 0.35f;
            if (scanY <= titleY - 48) { scanPhase = 1; scanTimer = 0; }
        } else if (scanPhase == 1) { // 淡出
            scanAlpha = 0.35f * (1f - scanTimer / 20f);
            if (scanTimer >= 20) { scanPhase = 2; scanTimer = 0; scanAlpha = 0; }
        } else { // 等待后再循环
            scanAlpha = 0;
            if (scanTimer >= 100) { scanY = titleY + 28; scanPhase = 0; scanTimer = 0; }
        }
        if (scanAlpha > 0.01f) {
            r.drawRect(cx - 100, scanY, 200, 1, 0.05f, 0.85f, 0.95f, scanAlpha);
        }

        GameSaveManager saveMgr = game.getGameSaveManager();
        boolean hasSave = (saveMgr != null && saveMgr.hasQuickSave());

        // === 3. 主操作区 ===
        // 开始游戏——最大，金色边框
        int mainW = 240, mainH = 52;
        int mainX = cx - mainW / 2, mainY = sh / 2 + 16;
        r.drawRect(mainX, mainY, mainW, mainH, 0.04f, 0.06f, 0.14f, 0.93f);
        r.drawRect(mainX, mainY + mainH - 2, mainW, 2, 0.83f, 0.67f, 0.16f, 0.85f);
        r.drawText("开 始 游 戏", mainX + mainW / 2 - 32, mainY + 19,
                0.88f, 0.85f, 0.78f, 1.0f);

        // 继续游戏 + 载入存档
        int secW = 150, secH = 42, secGap = 20;
        int secY = mainY - secH - 14;
        int sec1X = cx - secW - secGap / 2;
        int sec2X = cx + secGap / 2;
        // 继续游戏
        float[] sa = hasSave ? new float[]{0.06f,0.12f,0.22f,0.88f, 0.05f,0.7f,0.65f}
                             : new float[]{0.08f,0.09f,0.12f,0.4f, 0.15f,0.18f,0.2f};
        r.drawRect(sec1X, secY, secW, secH, sa[0], sa[1], sa[2], sa[3]);
        r.drawRect(sec1X, secY + secH - 1, secW, 1, sa[4], sa[5], sa[6], 0.6f);
        r.drawText("继续游戏", sec1X + secW / 2 - 22, secY + 16,
                hasSave ? 0.8f : 0.5f, hasSave ? 0.85f : 0.5f, hasSave ? 0.9f : 0.5f, 1.0f);
        // 载入存档
        r.drawRect(sec2X, secY, secW, secH, 0.06f, 0.12f, 0.22f, 0.88f);
        r.drawRect(sec2X, secY + secH - 1, secW, 1, 0.05f, 0.55f, 0.65f, 0.6f);
        r.drawText("载入存档", sec2X + secW / 2 - 22, secY + 16, 0.8f, 0.85f, 0.9f, 1.0f);

        // === 4. 小按钮行 ===
        int smSize = 58, smGap = 10;
        String[] smLabels = {"成就", "奖励", "制作", "设置", "退出"};
        float[][] smBg = {
            {0.06f,0.14f,0.24f}, {0.1f,0.08f,0.22f}, {0.08f,0.15f,0.2f},
            {0.08f,0.12f,0.2f},   {0.45f,0.15f,0.15f}
        };
        int smTotalW = smLabels.length * smSize + (smLabels.length - 1) * smGap;
        int smX = cx - smTotalW / 2;
        int smY = secY - smSize - 16;
        for (int i = 0; i < smLabels.length; i++) {
            int bx = smX + i * (smSize + smGap);
            float[] c = smBg[i];
            r.drawRect(bx, smY, smSize, smSize, c[0], c[1], c[2], 0.82f);
            r.drawRect(bx, smY + smSize - 1, smSize, 1, c[0]*2, c[1]*1.5f, c[2]*1.5f, 0.5f);
            float tw = smLabels[i].length() * 6f;
            r.drawText(smLabels[i], bx + (smSize - tw) / 2f, smY + smSize / 2f + 6,
                    0.78f, 0.82f, 0.88f, 1.0f);
        }

        // === 5. 底部状态栏 ===
        long pts = RewardData.getRewardPoints();
        r.drawText("奖励积分  " + pts, 16, 24, 0.75f, 0.68f, 0.22f, 1.0f);
        r.drawText("V2.5.0", sw - 80, 24, 0.4f, 0.45f, 0.55f, 0.6f);

        // === 6. 确认对话框 ===
        if (showNewGameConfirm) {
            int dlx = cx - 130; int dly = sh / 2 - 42;
            r.drawRect(dlx, dly, 260, 84, 0.04f, 0.05f, 0.12f, 0.96f);
            r.drawRect(dlx, dly + 82, 260, 2, 0.83f, 0.67f, 0.16f, 0.7f);
            r.drawText("已有快速存档，是否覆盖？", dlx + 20, dly + 18, 0.85f, 0.88f, 0.92f, 1f);
            r.drawRect(dlx + 40, dly + 44, 70, 26, 0.1f, 0.45f, 0.15f, 0.9f);
            r.drawText("确定", dlx + 56, dly + 50, 0.95f, 0.95f, 0.95f, 1f);
            r.drawRect(dlx + 150, dly + 44, 70, 26, 0.45f, 0.15f, 0.15f, 0.9f);
            r.drawText("取消", dlx + 165, dly + 50, 0.95f, 0.95f, 0.95f, 1f);
        }

        r.end();
    }

}
