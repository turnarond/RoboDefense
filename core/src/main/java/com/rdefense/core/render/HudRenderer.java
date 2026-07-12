package com.rdefense.core.render;

import com.rdefense.core.platform.GameRenderer;

/**
 * HUD 渲染器 — 绘制等级、积分、金钱、生命值等 UI 信息
 * 对应原版 GameHud + HudEntry 逻辑
 */
public class HudRenderer {

    private final GameRenderer renderer;

    // HUD 元素位置（屏幕坐标）
    private int bonusX, bonusY;
    private int healthX, healthY;
    private int levelX, levelY;
    private int moneyX, moneyY;
    private int scoreX, scoreY;

    // 本地化文本
    private String levelLabel;
    private String scoreLabel;
    private String bonusLabel;
    private String moneyPrefix;

    // 数值动画
    private int displayMoney;
    private int displayHealth;
    private int displayScore;
    private int lastStateIndex;

    // 字体颜色（RGB）
    private static final float TEXT_R = 1.0f;
    private static final float TEXT_G = 1.0f;
    private static final float TEXT_B = 1.0f;

    public HudRenderer(GameRenderer renderer) {
        this.renderer = renderer;
        this.displayMoney = 0;
        this.displayHealth = 0;
        this.displayScore = 0;
        this.lastStateIndex = 0;
    }

    /**
     * 初始化 HUD（屏幕尺寸变化时调用）
     * @param screenWidth 屏幕宽度
     * @param screenHeight 屏幕高度
     * @param uiPixelSize UI 像素大小
     * @param locale 本地化文本提供器
     */
    public void init(int screenWidth, int screenHeight, int uiPixelSize, TextProvider locale) {
        this.levelLabel = locale.getText("level") + ":";
        this.scoreLabel = locale.getText("score");
        this.bonusLabel = locale.getText("bonus");
        this.moneyPrefix = "$:";

        // 计算 HUD 元素位置
        bonusX = uiPixelSize / 8;
        bonusY = uiPixelSize / 2;
        healthX = screenWidth - (uiPixelSize / 8);
        healthY = uiPixelSize;
        levelX = screenWidth / 2;
        levelY = bonusY;
        moneyX = screenWidth - (uiPixelSize / 4);
        moneyY = bonusY;
        scoreX = bonusX;
        scoreY = healthY;
    }

    /**
     * 绘制 HUD
     * @param gameState 游戏状态
     * @param stateIndex 当前状态索引
     */
    public void render(GameStateInfo gameState, int stateIndex) {
        int frames = stateIndex - lastStateIndex;
        lastStateIndex = stateIndex;

        // 处理积分增加事件
        for (ScoreAddEvent e : gameState.getScoreAddEvents()) {
            if (e.amount > 0) {
                displayScore += e.amount;
                e.amount = 0; // 标记已处理
            }
        }

        // 动画过渡数值
        animateValue(gameState.getMoney(), frames);

        // 绘制 HUD 元素
        renderer.begin();

        // 金钱（右上）
        drawText(moneyPrefix + formatNumber(displayMoney), moneyX, moneyY, true);

        // 生命值（右下）
        drawText(String.valueOf(gameState.getHealth()), healthX, healthY, true);

        // 等级（顶部居中）
        String levelText = levelLabel + " " + gameState.getLevelNum();
        drawText(levelText, levelX, levelY, false);

        // 击杀奖励（左上）
        drawText(bonusLabel + ": " + gameState.getEnemyKillBonus(), bonusX, bonusY, true);

        // 积分（左下）
        drawText(scoreLabel + ": " + formatNumber(displayScore), scoreX, scoreY, true);

        renderer.end();
    }

    /**
     * 绘制文本（带颜色）
     */
    private void drawText(String text, float x, float y, boolean alignRight) {
        if (alignRight) {
            // libGDX BitmapFont 默认左对齐，需要手动计算右对齐位置
            // 这里简化处理，实际应该使用 font.getBoundWidth()
            renderer.drawText(text, x, y, TEXT_R, TEXT_G, TEXT_B, 1.0f);
        } else {
            renderer.drawText(text, x, y, TEXT_R, TEXT_G, TEXT_B, 1.0f);
        }
    }

    /**
     * 数值动画过渡
     */
    private void animateValue(int target, int frames) {
        // 简化实现：直接跳到目标值
        // 原版使用渐进动画，这里先保持一致
        displayMoney = target;
    }

    /**
     * 格式化数字（添加千位分隔符）
     */
    private String formatNumber(int number) {
        if (number < 1000) {
            return String.valueOf(number);
        }
        return String.format("%,d", number);
    }

    /**
     * 绘制消息事件（游戏消息、成就提示等）
     */
    public void renderMessages(GameStateInfo gameState, int stateIndex) {
        renderer.begin();

        // 绘制游戏消息
        int messageIndex = 0;
        for (MessageEvent e : gameState.getMessages()) {
            e.framesRemaining--;
            if (e.framesRemaining > 0) {
                int alpha = Math.min(255, (e.framesRemaining * 255) / 30);
                int yPos = (messageIndex * 15) + 15;
                boolean flash = ((e.framesRemaining >> 1) & 1) == 0;

                float r = flash ? 0.95f : 1.0f;
                float g = flash ? 0.95f : 1.0f;
                float b = flash ? 1.0f : 1.0f;
                float a = alpha / 255.0f;

                renderer.drawText(e.message, 15, yPos, r, g, b, a);
                messageIndex++;
            }
        }

        // 绘制成就提示
        for (AchievementEvent e : gameState.getAchievements()) {
            if (e.active) {
                // 成就提示在右侧显示
                renderer.drawText(e.message, renderer.getScreenWidth() - 200, 50, 1.0f, 1.0f, 0.0f, 1.0f);
            }
        }

        renderer.end();
    }

    /**
     * 本地化文本提供器接口
     */
    public interface TextProvider {
        String getText(String key);
    }

    /**
     * 游戏状态信息接口
     */
    public interface GameStateInfo {
        int getMoney();
        int getHealth();
        int getLevelNum();
        int getDifficultyLevel();
        int getEnemyKillBonus();
        int getScore();
        ScoreAddEvent[] getScoreAddEvents();
        MessageEvent[] getMessages();
        AchievementEvent[] getAchievements();
    }

    public static class ScoreAddEvent {
        public int amount;
    }

    public static class MessageEvent {
        public String message;
        public int framesRemaining;
    }

    public static class AchievementEvent {
        public String message;
        public boolean active;
    }
}
