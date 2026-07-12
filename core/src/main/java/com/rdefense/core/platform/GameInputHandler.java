package com.rdefense.core.platform;

/**
 * 输入处理器接口 — 统一触摸/鼠标/键盘事件
 */
public interface GameInputHandler {

    /** 指针按下（触摸或鼠标点击） */
    void onPointerDown(float x, float y, int pointer);

    /** 指针拖拽移动 */
    void onPointerDrag(float x, float y, int pointer);

    /** 指针释放 */
    void onPointerUp(float x, float y, int pointer);

    /** 鼠标移动（不按按钮） */
    void onPointerMove(float x, float y);

    /** 按键按下 */
    void onKeyPressed(int keyCode);

    /** 按键释放 */
    void onKeyReleased(int keyCode);

    /** 鼠标滚轮滚动 */
    void onScrolled(float amountX, float amountY);

    /** 键盘类型代码常量 */
    class Keys {
        public static final int NUM_1 = 1;
        public static final int NUM_2 = 2;
        public static final int NUM_3 = 3;
        public static final int SPACE = 32;
        public static final int ESCAPE = 256;
        public static final int F = 70;
    }
}
