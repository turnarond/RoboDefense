package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.config.LevelNames;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.save.PlayerPrefs;

/**
 * 关卡选择场景 - 简洁稳定版
 */
public class LevelSelectScreen extends GameScreen {

    private static final int MAP_COUNT = 7;
    private static final int MAX_DIFFICULTY = 10;
    private static final int LEVELS_PER_MAP = 10;

    private int currentMap = 0;
    private int currentDifficulty = 0;
    private boolean survivalMode = false;
    private boolean towerMixerEnabled = false;
    private int mixerValue = 0;
    private int towerMixerValue = 0;

    private PlayerPrefs prefs;

    public LevelSelectScreen(RoboDefenseGame game) {
        super(game);
        this.prefs = game.getPlayerPrefs();
        if (prefs != null) {
            this.currentMap = prefs.getMap();
            this.currentDifficulty = prefs.getDifficulty();
            this.survivalMode = prefs.isSurvivalMode();
            this.towerMixerEnabled = prefs.isTowerMixerEnabled();
        }
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    private boolean isMapUnlocked(int mapId) {
        if (prefs == null) return true;
        int maxLevelWon = prefs.getMaxLevelWon();
        int unlockMap = maxLevelWon / LEVELS_PER_MAP;
        return mapId <= unlockMap;
    }

    private boolean isMixerUnlocked() {
        if (prefs == null) return false;
        return mixerValue > 0 || towerMixerValue > 0;
    }

    @Override
    protected void update(float delta) {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            onStartPressed();
            return;
        }

        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();

        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = screenHeight - Gdx.input.getY();

            // 左箭头
            if (isPointInRect(x, y, 20, screenHeight - 140, 50, 50)) {
                currentMap = (currentMap + MAP_COUNT - 1) % MAP_COUNT;
                return;
            }
            // 右箭头
            if (isPointInRect(x, y, screenWidth - 70, screenHeight - 140, 50, 50)) {
                currentMap = (currentMap + 1) % MAP_COUNT;
                return;
            }

            // 难度 -
            if (isPointInRect(x, y, screenWidth - 90, screenHeight - 260, 50, 35)) {
                currentDifficulty = Math.max(0, currentDifficulty - 1);
                return;
            }
            // 难度 +
            if (isPointInRect(x, y, screenWidth - 90, screenHeight - 220, 50, 35)) {
                currentDifficulty = Math.min(MAX_DIFFICULTY, currentDifficulty + 1);
                return;
            }

            boolean mapUnlocked = isMapUnlocked(currentMap);
            // 生存模式开关
            if (isPointInRect(x, y, 20, screenHeight - 370, 40, 40)) {
                if (mapUnlocked) survivalMode = !survivalMode;
                return;
            }
            // 塔混合器开关
            if (isPointInRect(x, y, 20, screenHeight - 430, 40, 40)) {
                if (mapUnlocked && isMixerUnlocked()) towerMixerEnabled = !towerMixerEnabled;
                return;
            }

            // 开始按钮
            if (isPointInRect(x, y, screenWidth - 160, 20, 140, 45)) {
                onStartPressed();
                return;
            }
            // 返回按钮
            if (isPointInRect(x, y, 20, 20, 140, 45)) {
                switchScreen(new MainMenuScreen(game));
                return;
            }
        }

        // 键盘导航
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            currentMap = (currentMap + MAP_COUNT - 1) % MAP_COUNT;
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            currentMap = (currentMap + 1) % MAP_COUNT;
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            currentDifficulty = Math.min(MAX_DIFFICULTY, currentDifficulty + 1);
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            currentDifficulty = Math.max(0, currentDifficulty - 1);
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.S)) {
            if (isMapUnlocked(currentMap)) survivalMode = !survivalMode;
        } else if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.M)) {
            if (isMapUnlocked(currentMap) && isMixerUnlocked()) towerMixerEnabled = !towerMixerEnabled;
        }
    }

    private boolean isPointInRect(int px, int py, int rx, int ry, int rw, int rh) {
        return px >= rx && px <= rx + rw && py >= ry && py <= ry + rh;
    }

    private void onStartPressed() {
        if (!isMapUnlocked(currentMap)) {
            Gdx.app.log("LevelSelectScreen", "Map not unlocked: " + currentMap);
            return;
        }
        if (prefs != null) {
            prefs.putMap(currentMap);
            prefs.putDifficulty(currentDifficulty);
            prefs.putSurvivalMode(survivalMode);
            prefs.putTowerMixerEnabled(towerMixerEnabled);
        }
        GamePlayScreen gps = new GamePlayScreen(game, false);
        gps.configureLevel(currentMap, currentDifficulty);
        switchScreen(gps);
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        int w = renderer.getScreenWidth();
        int h = renderer.getScreenHeight();

        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        // 背景
        renderer.drawRect(0, 0, w, h, 0.03f, 0.05f, 0.1f, 1.0f);

        int y = h - 20;

        // 标题
        renderer.drawText("关卡选择", 20, y, 1.0f, 0.9f, 0.2f, 1.0f);
        y -= 40;

        // 地图选择区域
        renderer.drawRect(20, y - 100, w - 40, 100, 0.08f, 0.1f, 0.15f, 0.9f);
        // 左箭头
        renderer.drawRect(20, y - 75, 50, 50, 0.2f, 0.25f, 0.35f, 0.9f);
        renderer.drawText("<", 38, y - 43, 1.0f, 1.0f, 1.0f, 1.0f);
        // 右箭头
        renderer.drawRect(w - 70, y - 75, 50, 50, 0.2f, 0.25f, 0.35f, 0.9f);
        renderer.drawText(">", w - 52, y - 43, 1.0f, 1.0f, 1.0f, 1.0f);

        boolean mapUnlocked = isMapUnlocked(currentMap);
        String mapName = LevelNames.getName(currentMap);
        String mapEn = LevelNames.getEnglishName(currentMap);
        renderer.drawText("地图 " + currentMap, w / 2 - 40, y - 30, 0.7f, 0.7f, 0.7f, 1.0f);
        if (mapUnlocked) {
            renderer.drawText(mapName, w / 2 - 60, y - 55, 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            renderer.drawText(mapName, w / 2 - 60, y - 55, 0.4f, 0.4f, 0.4f, 1.0f);
        }
        renderer.drawText(mapEn, w / 2 - 50, y - 80, 0.5f, 0.5f, 0.5f, 1.0f);
        if (!mapUnlocked) {
            int maxLevelWon = (prefs != null) ? prefs.getMaxLevelWon() : 0;
            int requiredLevel = currentMap * LEVELS_PER_MAP;
            renderer.drawText("通关 " + requiredLevel + " 关解锁（当前: " + maxLevelWon + "）", 
                    w / 2 - 150, y - 105, 0.9f, 0.4f, 0.4f, 1.0f);
        }
        y -= 120;

        // 难度区域
        renderer.drawRect(20, y - 90, w - 40, 90, 0.08f, 0.1f, 0.15f, 0.9f);
        renderer.drawText("难度设置", 30, y - 20, 0.8f, 0.8f, 0.8f, 1.0f);
        // 数字显示
        renderer.drawText(String.valueOf(currentDifficulty), w / 2 - 15, y - 40, 1.2f, 1.2f, 1.2f, 1.0f);
        renderer.drawText("/ " + MAX_DIFFICULTY, w / 2 + 10, y - 40, 0.7f, 0.7f, 0.7f, 1.0f);
        // 进度条
        int sliderW = w - 200;
        float filledW = (float) currentDifficulty / MAX_DIFFICULTY * sliderW;
        renderer.drawRect(30, y - 70, sliderW, 12, 0.2f, 0.2f, 0.3f, 0.8f);
        renderer.drawRect(30, y - 70, filledW, 12, 0.3f, 0.6f, 0.3f, 0.9f);
        // - 按钮
        renderer.drawRect(w - 90, y - 80, 50, 35, 0.2f, 0.2f, 0.4f, 0.9f);
        renderer.drawText("-", w - 75, y - 58, 1.0f, 0.5f, 0.5f, 1.0f);
        // + 按钮
        renderer.drawRect(w - 90, y - 40, 50, 35, 0.2f, 0.4f, 0.2f, 0.9f);
        renderer.drawText("+", w - 75, y - 18, 0.5f, 1.0f, 0.5f, 1.0f);
        y -= 110;

        // 模式区域（增大面板高度，避免重叠）
        renderer.drawRect(20, y - 120, w - 40, 120, 0.08f, 0.1f, 0.15f, 0.9f);
        renderer.drawText("游戏模式", 30, y - 15, 0.8f, 0.8f, 0.8f, 1.0f);

        // 生存模式（向下移动，与标题保持足够间距）
        int survY = y - 50;
        if (survivalMode && mapUnlocked) {
            renderer.drawRect(20, survY - 25, 40, 40, 0.15f, 0.5f, 0.15f, 0.9f);
            renderer.drawText("OK", 30, survY - 5, 1.0f, 1.0f, 1.0f, 1.0f);
        } else {
            renderer.drawRect(20, survY - 25, 40, 40, 0.2f, 0.25f, 0.35f, 0.9f);
        }
        renderer.drawText("生存模式", 70, survY - 5, 
                mapUnlocked ? 0.8f : 0.4f, mapUnlocked ? 0.8f : 0.4f, mapUnlocked ? 0.8f : 0.4f, 1.0f);

        // 塔混合器（向下移动，与生存模式保持足够间距）
        int mixY = y - 100;
        if (isMixerUnlocked()) {
            if (towerMixerEnabled && mapUnlocked) {
                renderer.drawRect(20, mixY - 25, 40, 40, 0.15f, 0.5f, 0.15f, 0.9f);
                renderer.drawText("OK", 30, mixY - 5, 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                renderer.drawRect(20, mixY - 25, 40, 40, 0.2f, 0.25f, 0.35f, 0.9f);
            }
            renderer.drawText("塔混合器", 70, mixY - 5, 
                    mapUnlocked ? 0.8f : 0.4f, mapUnlocked ? 0.8f : 0.4f, mapUnlocked ? 0.8f : 0.4f, 1.0f);
        } else {
            renderer.drawRect(20, mixY - 25, 40, 40, 0.2f, 0.2f, 0.2f, 0.6f);
            renderer.drawText("?", 35, mixY - 5, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.drawText("塔混合器（未解锁）", 70, mixY - 5, 0.5f, 0.5f, 0.5f, 1.0f);
            renderer.drawText("通关 VR Training 地图解锁", 70, mixY - 25, 0.4f, 0.4f, 0.4f, 1.0f);
        }
        y -= 140;

        // 存档提示（移到快捷键提示上方）
        if (game.getGameSaveManager() != null && game.getGameSaveManager().hasQuickSave()) {
            renderer.drawText("注意：开始新游戏将清除现有快速存档", 
                    20, y - 10, 0.9f, 0.6f, 0.3f, 1.0f);
            y -= 30;
        }

        // 提示区（继续下移）
        renderer.drawRect(20, y - 50, w - 40, 50, 0.05f, 0.05f, 0.08f, 0.7f);
        renderer.drawText("方向键: 切换地图/调整难度  S: 生存模式  M: 塔混合器", 
                30, y - 20, 0.5f, 0.5f, 0.5f, 1.0f);
        renderer.drawText("Enter: 开始游戏  ESC: 返回主菜单", 
                w - 220, y - 20, 0.5f, 0.5f, 0.5f, 1.0f);
        y -= 80;

        // 底部按钮
        // 返回
        renderer.drawRect(20, 20, 140, 45, 0.25f, 0.25f, 0.35f, 0.9f);
        renderer.drawText("返回", 60, 48, 1.0f, 1.0f, 1.0f, 1.0f);
        // 开始
        boolean canStart = isMapUnlocked(currentMap);
        float sR = canStart ? 0.15f : 0.3f;
        float sG = canStart ? 0.55f : 0.3f;
        float sB = canStart ? 0.15f : 0.3f;
        renderer.drawRect(w - 160, 20, 140, 45, sR, sG, sB, 0.9f);
        renderer.drawText("开始游戏", w - 125, 48, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }
}
