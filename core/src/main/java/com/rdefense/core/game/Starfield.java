package com.rdefense.core.game;

/** 星空粒子背景（对应原版 Starfield），用于宇宙/公路类关卡 */
public final class Starfield {

    private static final int NUM_STARS = 80;
    private static final int FIELD_WIDTH = 640;
    private static final int FIELD_HEIGHT = 384;
    private static final int SCROLL_SPEED = 1;

    private final short[] starX = new short[NUM_STARS];
    private final short[] starY = new short[NUM_STARS];
    private final byte[] starAlpha = new byte[NUM_STARS];

    public Starfield(int seed) {
        FastRandom.init();
        for (int i = 0; i < NUM_STARS; i++) {
            starX[i] = (short) (FastRandom.nextInt(FIELD_WIDTH));
            starY[i] = (short) (FastRandom.nextInt(FIELD_HEIGHT));
            starAlpha[i] = (byte) (60 + FastRandom.nextInt(196));
        }
    }

    /** 每帧推进：所有星向下滚动，出屏者循环到顶部 */
    public void update() {
        for (int i = 0; i < NUM_STARS; i++) {
            int y = (starY[i] & 0xFFFF) + SCROLL_SPEED;
            if (y >= FIELD_HEIGHT) {
                y -= FIELD_HEIGHT;
                starX[i] = (short) (FastRandom.nextInt(FIELD_WIDTH));
                starAlpha[i] = (byte) (60 + FastRandom.nextInt(196));
            }
            starY[i] = (short) y;
        }
    }

    /** 绘制星空（坐标已按相机变换） */
    public void draw(com.rdefense.core.platform.GameRenderer renderer, int offsetX, int offsetY) {
        for (int i = 0; i < NUM_STARS; i++) {
            int sx = (starX[i] & 0xFFFF) + offsetX;
            int sy = (starY[i] & 0xFFFF) + offsetY;
            float a = (starAlpha[i] & 0xFF) / 255f;
            renderer.drawRect(sx, sy, 2, 2, 1f, 1f, 1f, a);
        }
    }
}
