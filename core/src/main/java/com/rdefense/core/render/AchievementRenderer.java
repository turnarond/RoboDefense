package com.rdefense.core.render;

import com.rdefense.core.game.AchievementData;
import com.rdefense.core.platform.GameAudio;
import com.rdefense.core.platform.GameRenderer;

public class AchievementRenderer {

    private static final int STATE_FILL_TL = 0;
    private static final int STATE_FILL_BR = 1;
    private static final int STATE_HOLD = 2;
    private static final int STATE_FADE = 3;
    private static final int STATE_HOLD_OFF = 4;

    private static final int SHINE_FRAMES = 5;
    private static final int HOLD_FRAMES = 50;
    private static final int FADE_FRAMES = 30;
    private static final int HOLD_OFF_FRAMES = 20;

    private static final int PAD = 8;
    private static final int WIDTH = 200;
    private static final int HEIGHT = 60;
    private static final int TITLE_Y_PAD = 20;
    private static final int TITLE_X_PAD = 10;
    private static final int NAME_Y_PAD = 40;
    private static final int NAME_X_PAD = 16;
    private static final int BORDER_WIDTH = 3;

    private int achievementType = -1;
    private int state = STATE_FILL_TL;
    private int frame = 0;
    private boolean soundPlayed = false;

    public void showAchievement(int type) {
        this.achievementType = type;
        this.state = STATE_FILL_TL;
        this.frame = 0;
        this.soundPlayed = false;
    }

    public boolean isAnimating() {
        return achievementType >= 0 && state != STATE_HOLD_OFF;
    }

    public void update(GameAudio audio) {
        if (!isAnimating()) return;

        if (!soundPlayed && audio != null) {
            audio.playSound("achievement", 1.0f);
            soundPlayed = true;
        }

        frame++;

        switch (state) {
            case STATE_FILL_TL:
                if (frame >= SHINE_FRAMES) {
                    frame = 0;
                    state = STATE_FILL_BR;
                }
                break;
            case STATE_FILL_BR:
                if (frame >= SHINE_FRAMES) {
                    frame = 0;
                    state = STATE_HOLD;
                }
                break;
            case STATE_HOLD:
                if (frame >= HOLD_FRAMES) {
                    frame = 0;
                    state = STATE_FADE;
                }
                break;
            case STATE_FADE:
                if (frame >= FADE_FRAMES) {
                    frame = 0;
                    state = STATE_HOLD_OFF;
                }
                break;
            case STATE_HOLD_OFF:
                if (frame >= HOLD_OFF_FRAMES) {
                    achievementType = -1;
                }
                break;
        }
    }

    public void draw(GameRenderer renderer) {
        if (!isAnimating()) return;

        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        int rectLeft = screenWidth - WIDTH - PAD;
        int rectTop = PAD;
        int rectRight = screenWidth - PAD;
        int rectBottom = PAD + HEIGHT;

        int alpha = 255;
        if (state == STATE_FADE) {
            alpha = 255 - ((frame * 255) / FADE_FRAMES);
        }

        drawMain(renderer, rectLeft, rectTop, rectRight, rectBottom, alpha);

        if (state != STATE_HOLD_OFF) {
            float frameRatio = (float) frame / SHINE_FRAMES;
            drawBorder(renderer, rectLeft, rectTop, rectRight, rectBottom, alpha, frameRatio);
        }
    }

    private void drawBorder(GameRenderer renderer, int left, int top, int right, int bottom, int alpha, float frameRatio) {
        float colorAlpha = alpha / 255f;

        switch (state) {
            case STATE_FILL_TL:
                float topWidth = (right - left) * frameRatio;
                float leftHeight = (bottom - top) * frameRatio;
                renderer.drawRect(left, top, topWidth, BORDER_WIDTH, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(left, top, BORDER_WIDTH, leftHeight, 1.0f, 1.0f, 1.0f, colorAlpha);
                break;
            case STATE_FILL_BR:
                renderer.drawRect(left, top, right - left, BORDER_WIDTH, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(left, top, BORDER_WIDTH, bottom - top, 1.0f, 1.0f, 1.0f, colorAlpha);
                float bottomWidth = (right - left) * frameRatio;
                float rightHeight = (bottom - top) * frameRatio;
                renderer.drawRect(left, bottom - BORDER_WIDTH, bottomWidth, BORDER_WIDTH, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(right - BORDER_WIDTH, top, BORDER_WIDTH, rightHeight, 1.0f, 1.0f, 1.0f, colorAlpha);
                break;
            case STATE_HOLD:
            case STATE_FADE:
                renderer.drawRect(left, top, right - left, BORDER_WIDTH, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(left, top, BORDER_WIDTH, bottom - top, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(left, bottom - BORDER_WIDTH, right - left, BORDER_WIDTH, 1.0f, 1.0f, 1.0f, colorAlpha);
                renderer.drawRect(right - BORDER_WIDTH, top, BORDER_WIDTH, bottom - top, 1.0f, 1.0f, 1.0f, colorAlpha);
                break;
        }
    }

    private void drawMain(GameRenderer renderer, int left, int top, int right, int bottom, int alpha) {
        float bgAlpha = (alpha * 208) / 255f;
        renderer.drawRect(left, top, right - left, bottom - top, 0, 0, 0, bgAlpha / 255f);

        float textAlpha = alpha / 255f;

        renderer.drawText("成就达成！", left + TITLE_X_PAD, top + TITLE_Y_PAD, 1.0f, 0.9f, 0.2f, textAlpha);

        // 使用中文名称
        String achievementName = AchievementData.getNameZh(achievementType);
        renderer.drawText(achievementName, left + NAME_X_PAD, top + NAME_Y_PAD, 1.0f, 1.0f, 1.0f, textAlpha);
    }
}