package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.config.LevelNames;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.render.SpriteNames;
import com.rdefense.core.save.PlayerPrefs;

/**
 * 关卡选择场景 — 卡片式布局
 * 一张大地图预览卡 + 难度滑条 + 模式开关
 */
public class LevelSelectScreen extends GameScreen {

    private static final int MAP_COUNT = 7;
    private static final int MAX_DIFFICULTY = 10;
    private static final int LEVELS_PER_MAP = 10;

    private int currentMap;
    private int currentDifficulty;
    private boolean survivalMode;
    private boolean towerMixerEnabled;
    private int mixerValue;
    private int towerMixerValue;
    private PlayerPrefs prefs;
    private boolean mixerPanelOpen = false;
    private static final int DIGIT_COUNT = 5;
    private int[] mixerDigits = new int[DIGIT_COUNT];
    private int[] towerMixerDigits = new int[DIGIT_COUNT];
    private boolean editingTowerMixer = false;

    // === 布局坐标（computeLayout 统一计算）===
    private int sw, sh;

    // 地图卡片
    private int cardX, cardY, cardW, cardH;
    // 左右箭头（在卡片内）
    private int arrW = 48, arrH = 56;
    private int arrLX, arrLY, arrRX, arrRY;
    // 地图缩略图区域
    private int thumbX, thumbY, thumbW, thumbH;

    // 难度行
    private int diffRowY;
    private int diffMinusX, diffMinusW, diffPlusX, diffPlusW;
    private int diffSliderX, diffSliderW, diffSliderY, diffSliderH;

    // 模式行
    private int modeRowY;
    private int survChkX, survChkW, mixChkX, mixChkW;
    private int chkSize = 36;

    // 底部按钮
    private int backX, backY, backW, backH;
    private int startX, startY, startW, startH;

    public LevelSelectScreen(RoboDefenseGame game) {
        super(game);
        this.prefs = game.getPlayerPrefs();
        if (prefs != null) {
            this.currentMap = prefs.getMap();
            this.currentDifficulty = prefs.getDifficulty();
            this.survivalMode = prefs.isSurvivalMode();
            this.towerMixerEnabled = prefs.isTowerMixerEnabled();
            this.mixerValue = prefs.getMixerValue();
            if (this.mixerValue <= 0) this.mixerValue = Math.abs(com.rdefense.core.game.FastRandom.nextInt() % 100000);
            this.towerMixerValue = prefs.getTowerMixerValue();
            if (this.towerMixerValue <= 0) this.towerMixerValue = Math.abs(com.rdefense.core.game.FastRandom.nextInt() % 100000);
            loadDigits(this.mixerValue, this.mixerDigits);
            loadDigits(this.towerMixerValue, this.towerMixerDigits);
        }
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    // ==================== 布局计算 ====================

    private void computeLayout() {
        sw = Gdx.graphics.getWidth();
        sh = Gdx.graphics.getHeight();

        int pad = 14;

        // 地图卡片：占屏幕中上部绝大部分
        cardX = pad;
        cardW = sw - pad * 2;
        cardH = Math.min(250, sh - 200);
        cardY = sh - 34 - 16 - cardH;  // 标题栏下方 16px

        // 箭头在卡片左右边缘居中
        arrLY = cardY + (cardH - arrH) / 2;
        arrLX = cardX + 8;
        arrRY = arrLY;
        arrRX = cardX + cardW - arrW - 8;

        // 缩略图区域（箭头之间）
        thumbX = arrLX + arrW + 12;
        thumbW = arrRX - thumbX - 12;
        thumbH = cardH - 60;
        thumbY = cardY + 40;

        // 难度行：卡片下方
        int rowH = 44;
        diffRowY = cardY - 8 - rowH;
        diffMinusW = 36; diffPlusW = 36;
        diffMinusX = pad + 60;
        diffPlusX = sw - pad - diffPlusW - 60;
        diffSliderX = diffMinusX + diffMinusW + 16;
        diffSliderW = diffPlusX - diffSliderX - 16;
        diffSliderY = diffRowY + 22;
        diffSliderH = 6;

        // 模式行
        modeRowY = diffRowY - 4 - rowH;
        survChkW = chkSize + 100;  // checkbox + label width
        mixChkW = chkSize + 220;
        survChkX = pad + 10;
        mixChkX = survChkX + survChkW + 20;

        // 底部按钮
        backW = 110; backH = 36;
        backX = pad; backY = 6;
        startW = 150; startH = 42;
        startX = sw - startW - pad; startY = 6;
    }

    // ==================== 输入处理 ====================

    @Override
    protected void update(float delta) {
        computeLayout();

        // 键盘
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE))
            { switchScreen(new MainMenuScreen(game)); return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER))
            { onStart(); return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT))
            { currentMap = (currentMap + MAP_COUNT - 1) % MAP_COUNT; return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT))
            { currentMap = (currentMap + 1) % MAP_COUNT; return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP))
            { currentDifficulty = Math.min(MAX_DIFFICULTY, currentDifficulty + 1); return; }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN))
            { currentDifficulty = Math.max(0, currentDifficulty - 1); return; }

        if (!Gdx.input.justTouched()) return;
        int x = Gdx.input.getX();
        int y = sh - Gdx.input.getY(); // flip to match draw coords

        // 声音开关切换
        if (hit(x, y, 20, 56, 60, 28)) {
            boolean cur = prefs.getBool("enable_sound", true);
            prefs.putBool("enable_sound", !cur);
            return;
        }
        // 设置入口跳转
        int optX = sw - 100;
        if (hit(x, y, optX, 56, 80, 28)) {
            switchScreen(new OptionsScreen(game));
            return;
        }

        // 底部按钮
        if (hit(x, y, backX, backY, backW, backH)) { switchScreen(new MainMenuScreen(game)); return; }
        if (hit(x, y, startX, startY, startW, startH)) { onStart(); return; }

        // 地图箭头
        if (hit(x, y, arrLX, arrLY, arrW, arrH)) { currentMap = (currentMap + MAP_COUNT - 1) % MAP_COUNT; return; }
        if (hit(x, y, arrRX, arrRY, arrW, arrH)) { currentMap = (currentMap + 1) % MAP_COUNT; return; }

        // 难度 +/-
        if (hit(x, y, diffMinusX, diffRowY, diffMinusW, 36))
            { currentDifficulty = Math.max(0, currentDifficulty - 1); return; }
        if (hit(x, y, diffPlusX, diffRowY, diffPlusW, 36))
            { currentDifficulty = Math.min(MAX_DIFFICULTY, currentDifficulty + 1); return; }
        // 点击滑条跳转
        if (y >= diffRowY && y <= diffRowY + 36 && x >= diffSliderX && x <= diffSliderX + diffSliderW) {
            float pct = (float)(x - diffSliderX) / diffSliderW;
            currentDifficulty = Math.round(pct * MAX_DIFFICULTY);
            return;
        }

        // 模式开关
        boolean u = isMapUnlocked(currentMap);
        if (hit(x, y, survChkX, modeRowY, chkSize, chkSize)) { if (u) survivalMode = !survivalMode; return; }
        if (hit(x, y, mixChkX, modeRowY, chkSize, chkSize)) { if (u && isMixerUnlocked()) { towerMixerEnabled = !towerMixerEnabled; if (towerMixerEnabled) mixerPanelOpen = true; } return; }
        if (mixerPanelOpen && handleMixerPanelClick(x, y)) return;
    }

    private boolean hit(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    // ==================== 业务逻辑 ====================

    private boolean isMapUnlocked(int mapId) {
        if (prefs == null) return true;
        return mapId <= prefs.getMaxLevelWon() / LEVELS_PER_MAP;
    }

    private boolean isMixerUnlocked() {
        return mixerValue > 0 || towerMixerValue > 0;
    }

    private void onStart() {
        if (!isMapUnlocked(currentMap)) return;
        if (prefs != null) {
            prefs.putMap(currentMap);
            prefs.putDifficulty(currentDifficulty);
            prefs.putSurvivalMode(survivalMode);
            prefs.putTowerMixerEnabled(towerMixerEnabled);
            prefs.putMixerValue(digitsToValue(mixerDigits));
            prefs.putTowerMixerValue(digitsToValue(towerMixerDigits));
        }
        GamePlayScreen gps = new GamePlayScreen(game);
        gps.configureLevel(currentMap, currentDifficulty);
        switchScreen(gps);
    }

    // ==================== 渲染 ====================

    @Override
    protected void draw(float delta) {
        computeLayout();
        GameRenderer r = game.getServices().getRenderer();
        r.applyCameraTransform(0, 0, 1.0f);
        r.begin();

        // 背景
        r.drawRect(0, 0, sw, sh, 0.03f, 0.05f, 0.12f, 1.0f);

        // 标题栏
        drawTitleBar("关卡选择");

        boolean unlocked = isMapUnlocked(currentMap);

        // ========== 地图卡片 ==========
        r.drawRect(cardX, cardY, cardW, cardH, 0.06f, 0.09f, 0.18f, 0.92f);
        r.drawRect(cardX, cardY + cardH - 1, cardW, 1, 0.18f, 0.32f, 0.5f, 0.7f);

        // 地图缩略图
        String bgName = SpriteNames.levelBackground(currentMap);
        r.drawSprite(bgName, thumbX, thumbY, thumbW, thumbH);

        if (!unlocked) {
            r.drawRect(thumbX, thumbY, thumbW, thumbH, 0f, 0f, 0f, 0.6f);
            int req = currentMap * LEVELS_PER_MAP;
            int cur = (prefs != null) ? prefs.getMaxLevelWon() : 0;
            r.drawText("通关 " + req + " 关解锁", cardX + cardW/2 - 50, cardY + cardH/2 + 10, 0.95f, 0.35f, 0.3f, 1.0f);
        }

        // 地图名（卡片上方）
        String name = LevelNames.getName(currentMap);
        String en = LevelNames.getEnglishName(currentMap);
        float nc = unlocked ? 0.88f : 0.4f;
        r.drawText(name, cardX + arrW + 20, cardY + cardH - 22, nc, nc, nc, 1.0f);
        r.drawText(en, cardX + arrW + 20, cardY + cardH - 42, 0.45f, 0.55f, 0.7f, 0.9f);
        r.drawText((currentMap+1) + "/" + MAP_COUNT, cardX + cardW - 50, cardY + cardH - 22, 0.4f, 0.5f, 0.6f, 0.8f);

        // 左右箭头
        r.drawRect(arrLX, arrLY, arrW, arrH, 0.08f, 0.15f, 0.28f, 0.88f);
        r.drawText("<", arrLX + 18, arrLY + 20, 0.7f, 0.8f, 0.9f, 1.0f);
        r.drawRect(arrRX, arrRY, arrW, arrH, 0.08f, 0.15f, 0.28f, 0.88f);
        r.drawText(">", arrRX + 18, arrRY + 20, 0.7f, 0.8f, 0.9f, 1.0f);

        // ========== 难度行 ==========
        r.drawRect(diffMinusX - 10, diffRowY, diffSliderW + diffMinusW + diffPlusW + 36, 36,
                0.06f, 0.09f, 0.18f, 0.85f);
        // -
        r.drawRect(diffMinusX, diffRowY + 4, diffMinusW, 28, 0.08f, 0.15f, 0.35f, 0.85f);
        r.drawText("-", diffMinusX + 14, diffRowY + 14, 0.9f, 0.5f, 0.5f, 1.0f);
        // 滑条
        r.drawRect(diffSliderX, diffSliderY, diffSliderW, diffSliderH, 0.08f, 0.12f, 0.22f, 0.8f);
        float fill = (float) currentDifficulty / MAX_DIFFICULTY;
        r.drawRect(diffSliderX, diffSliderY, diffSliderW * fill, diffSliderH, 0.2f, 0.55f, 0.25f, 0.9f);
        // 数值
        r.drawText(currentDifficulty + "/" + MAX_DIFFICULTY,
                diffSliderX + diffSliderW/2 - 14, diffRowY + 14, 0.85f, 0.88f, 0.92f, 1.0f);
        // +
        r.drawRect(diffPlusX, diffRowY + 4, diffPlusW, 28, 0.08f, 0.2f, 0.15f, 0.85f);
        r.drawText("+", diffPlusX + 14, diffRowY + 14, 0.5f, 0.95f, 0.5f, 1.0f);

        // ========== 模式行 ==========
        drawToggle(r, survChkX, modeRowY, survivalMode && unlocked, "生存模式", unlocked);
        boolean mixOk = isMixerUnlocked();
        drawToggle(r, mixChkX, modeRowY, towerMixerEnabled && unlocked && mixOk, "塔混合器", unlocked && mixOk);
        if (!mixOk) {
            r.drawText("通关生存100关解锁", mixChkX + chkSize + 8, modeRowY + 12, 0.35f, 0.4f, 0.45f, 0.8f);
        } else {
            r.drawText("", mixChkX, modeRowY, 0, 0, 0, 0); // placeholder
        }

        if (mixerPanelOpen) drawMixerPanel(r);

        // ========== 声音开关 & 设置入口 ==========
        boolean soundOn = prefs.getBool("enable_sound", true);
        r.drawText("声音 " + (soundOn ? "开" : "关"), 20, 62, 0.7f, 0.82f, 0.95f, 1f);
        int optX = sw - 100;
        r.drawRect(optX, 56, 80, 28, 0.1f, 0.15f, 0.25f, 0.85f);
        r.drawText("设置", optX + 8, 62, 0.75f, 0.85f, 0.95f, 1f);

        // 存档警告
        if (game.getGameSaveManager() != null && game.getGameSaveManager().hasQuickSave()) {
            r.drawText("开始新游戏将清除快速存档", cardX + cardW/2 - 90, diffRowY - 22, 0.85f, 0.55f, 0.25f, 0.9f);
        }

        // ========== 底部按钮 ==========
        drawBackButton(backX, backY, backW, backH);

        float sr = unlocked ? 0.08f : 0.18f, sg = unlocked ? 0.28f : 0.15f, sb = unlocked ? 0.12f : 0.1f;
        r.drawRect(startX, startY, startW, startH, sr, sg, sb, 0.92f);
        r.drawRect(startX, startY + startH - 2, startW, 2,
                unlocked ? 0.3f : 0.15f, unlocked ? 0.55f : 0.2f, unlocked ? 0.2f : 0.15f, 0.8f);
        r.drawText("开始游戏", startX + startW/2 - 22, startY + 18,
                unlocked ? 0.82f : 0.45f, unlocked ? 0.88f : 0.45f, unlocked ? 0.85f : 0.45f, 1.0f);

        // ========== 快捷键提示 ==========
        r.drawRect(0, 0, r.getScreenWidth(), 22, 0.05f, 0.07f, 0.15f, 0.75f);
        r.drawText("←→ 切换地图  ↑↓ 调节难度  Enter 开始  ESC 返回",
                10, 6, 0.65f, 0.68f, 0.75f, 1f);

        r.end();
    }

    /** 绘制开关组件（方框 + 标签） */
    private void drawToggle(GameRenderer r, int x, int y, boolean on, String label, boolean enabled) {
        float cr = enabled ? (on ? 0.12f : 0.1f) : 0.1f;
        float cg = enabled ? (on ? 0.45f : 0.15f) : 0.1f;
        float cb = enabled ? (on ? 0.18f : 0.25f) : 0.1f;
        r.drawRect(x, y, chkSize, chkSize, cr, cg, cb, 0.85f);
        if (on) {
            r.drawText("ON", x + 6, y + 14, 0.5f, 0.9f, 0.5f, 1.0f);
        }
        float lr = enabled ? 0.75f : 0.4f, lg = enabled ? 0.82f : 0.4f, lb = enabled ? 0.9f : 0.4f;
        r.drawText(label, x + chkSize + 8, y + 14, lr, lg, lb, 1.0f);
    }

    // ==================== 混合器数码选择面板 ====================

    private static void loadDigits(int value, int[] digits) {
        for (int i = DIGIT_COUNT - 1; i >= 0; i--) {
            digits[i] = value % 10;
            value /= 10;
        }
    }

    private static int digitsToValue(int[] digits) {
        int v = 0;
        for (int i = 0; i < DIGIT_COUNT; i++) v = v * 10 + digits[i];
        return v;
    }

    private void randomizeDigits(int[] digits) {
        loadDigits(Math.abs(com.rdefense.core.game.FastRandom.nextInt() % 100000), digits);
    }

    private void drawMixerPanel(com.rdefense.core.platform.GameRenderer r) {
        int px = mixChkX + 40;
        int py = modeRowY - 15;
        int panelW = 280;
        int panelH = 80;
        r.drawRect(px, py, panelW, panelH, 0.05f, 0.08f, 0.12f, 0.92f);
        r.drawText(editingTowerMixer ? "塔混合器种子" : "关卡混合器种子",
                px + 8, py + 4, 0.75f, 0.85f, 0.95f, 1f);

        int[] digits = editingTowerMixer ? towerMixerDigits : mixerDigits;
        for (int i = 0; i < DIGIT_COUNT; i++) {
            int dx = px + 16 + i * 48;
            int dy = py + 22;
            r.drawRect(dx + 8, dy - 14, 24, 16, 0.2f, 0.7f, 0.9f, 1f);
            r.drawText("▲", dx + 10, dy - 6, 0.9f, 0.9f, 0.9f, 1f);
            r.drawText(Integer.toString(digits[i]), dx + 12, dy + 6, 1f, 1f, 0.3f, 1f);
            r.drawRect(dx + 8, dy + 16, 24, 16, 0.2f, 0.7f, 0.9f, 1f);
            r.drawText("▼", dx + 10, dy + 24, 0.9f, 0.9f, 0.9f, 1f);
        }
        int rx = px + panelW - 44;
        r.drawRect(rx, py + 46, 36, 24, 0.15f, 0.5f, 0.15f, 1f);
        r.drawText("随机", rx + 3, py + 54, 0.7f, 1f, 0.7f, 1f);
        r.drawRect(px + 6, py + 46, 60, 24, 0.3f, 0.3f, 0.5f, 1f);
        r.drawText(editingTowerMixer ? "普通" : "塔", px + 12, py + 54, 0.9f, 0.85f, 0.7f, 1f);
    }

    private boolean handleMixerPanelClick(int x, int y) {
        int px = mixChkX + 40;
        int py = modeRowY - 15;
        int[] digits = editingTowerMixer ? towerMixerDigits : mixerDigits;
        int rx = px + 280 - 44;
        // 随机按钮
        if (x >= rx && x <= rx + 36 && y >= py + 46 && y <= py + 70) {
            randomizeDigits(digits); return true;
        }
        // 切换按钮
        if (x >= px + 6 && x <= px + 66 && y >= py + 46 && y <= py + 70) {
            editingTowerMixer = !editingTowerMixer; return true;
        }
        for (int i = 0; i < DIGIT_COUNT; i++) {
            int dx = px + 24 + i * 48;
            if (x >= dx && x <= dx + 18) {
                if (y >= py + 8 && y <= py + 20) { digits[i] = (digits[i] + 1) % 10; return true; }
                if (y >= py + 38 && y <= py + 50) { digits[i] = (digits[i] + 9) % 10; return true; }
            }
        }
        // 点击面板外 → 关闭并保存
        mixerPanelOpen = false;
        prefs.putMixerValue(digitsToValue(mixerDigits));
        prefs.putTowerMixerValue(digitsToValue(towerMixerDigits));
        return true;
    }
}
