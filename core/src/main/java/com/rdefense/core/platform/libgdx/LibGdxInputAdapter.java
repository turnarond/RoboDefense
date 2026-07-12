package com.rdefense.core.platform.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.rdefense.core.platform.GameInputHandler;

/**
 * libGDX 输入处理器适配器
 * 将 libGDX 的 InputProcessor 事件转换为 GameInputHandler 接口调用
 *
 * 用法：
 *   Gdx.input.setInputProcessor(new LibGdxInputAdapter(handler));
 */
public class LibGdxInputAdapter extends InputAdapter {

    private final GameInputHandler handler;

    // 记录按下的指针位置（用于判断拖拽）
    private final float[] pointerDownX = new float[10];
    private final float[] pointerDownY = new float[10];
    private final boolean[] pointerDown = new boolean[10];

    public LibGdxInputAdapter(GameInputHandler handler) {
        this.handler = handler;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        float y = flipY(screenY);
        if (pointer < 10) {
            pointerDownX[pointer] = screenX;
            pointerDownY[pointer] = y;
            pointerDown[pointer] = true;
        }
        System.out.println("[InputDebug] touchDown screen=(" + screenX + "," + screenY + ") flippedY=" + y + " pointer=" + pointer + " button=" + button);
        handler.onPointerDown(screenX, y, pointer);
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        float y = flipY(screenY);
        handler.onPointerDrag(screenX, y, pointer);
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        float y = flipY(screenY);
        handler.onPointerMove(screenX, y);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float y = flipY(screenY);
        if (pointer < 10) {
            pointerDown[pointer] = false;
        }
        System.out.println("[InputDebug] touchUp screen=(" + screenX + "," + screenY + ") flippedY=" + y + " pointer=" + pointer + " button=" + button);
        handler.onPointerUp(screenX, y, pointer);
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        handler.onKeyPressed(mapKey(keycode));
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        handler.onKeyReleased(mapKey(keycode));
        return true;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        handler.onScrolled(amountX, amountY);
        return true;
    }

    /**
     * 翻转 Y 坐标（libGDX 原点在左下角，游戏 UI 原点在左上角）
     */
    private float flipY(int screenY) {
        return Gdx.graphics.getHeight() - screenY;
    }

    /**
     * 将 libGDX 按键码映射为游戏按键码
     */
    private int mapKey(int keycode) {
        switch (keycode) {
            case com.badlogic.gdx.Input.Keys.NUM_1:
                return GameInputHandler.Keys.NUM_1;
            case com.badlogic.gdx.Input.Keys.NUM_2:
                return GameInputHandler.Keys.NUM_2;
            case com.badlogic.gdx.Input.Keys.NUM_3:
                return GameInputHandler.Keys.NUM_3;
            case com.badlogic.gdx.Input.Keys.SPACE:
                return GameInputHandler.Keys.SPACE;
            case com.badlogic.gdx.Input.Keys.ESCAPE:
                return GameInputHandler.Keys.ESCAPE;
            case com.badlogic.gdx.Input.Keys.F:
                return GameInputHandler.Keys.F;
            default:
                return keycode;
        }
    }

    /**
     * 获取 libGDX InputProcessor 实例
     */
    public InputProcessor getInputProcessor() {
        return this;
    }
}
