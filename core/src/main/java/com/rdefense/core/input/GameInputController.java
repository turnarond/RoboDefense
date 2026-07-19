package com.rdefense.core.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.rdefense.core.game.GameState;

/**
 * 游戏输入控制 — 处理键盘快捷键（从 GamePlayScreen.update() 提取）
 *
 * 职责：
 * - ESC：暂停/取消暂停/关闭暂停菜单
 * - SPACE：切换快进模式
 * - F9：强制触发 HD 回退（调试用）
 *
 * 通过 GameInputCallbacks 回调与 GamePlayScreen 交互，避免直接依赖场景类。
 */
public class GameInputController {

    /**
     * 游戏输入回调接口 — 由 GamePlayScreen 实现
     */
    public interface GameInputCallbacks {
        void onTogglePause();
        void onToggleFastFwd();
        void onPauseMenuContinue();
        void onPauseMenuSaveAndQuit();
        void onPauseMenuQuitWithoutSave();
        void onForceHdFallback();
    }

    /**
     * 处理键盘输入，触发对应的回调
     *
     * @param gameState      当前游戏状态（用于读取 runState）
     * @param showPauseMenu  暂停菜单是否可见
     * @param callbacks      回调实现
     * @return true 表示 ESC 从运行/快进状态暂停了游戏，调用方应设置 showPauseMenu = true
     */
    public boolean handleKeys(GameState gameState, boolean showPauseMenu, GameInputCallbacks callbacks) {
        boolean shouldShowMenu = false;

        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            int rs = gameState.getRunState();
            if (showPauseMenu) {
                // 菜单可见 → 关闭菜单并取消暂停
                callbacks.onPauseMenuContinue();
            } else if (rs == GameState.GAME_PAUSED) {
                // 暂停中（无菜单）→ 恢复运行
                callbacks.onTogglePause();
            } else if (rs == GameState.GAME_RUNNING || rs == GameState.GAME_FAST_FWD) {
                // 运行/快进 → 暂停并提示调用方显示菜单
                callbacks.onTogglePause();
                shouldShowMenu = true;
            }
        }

        if (Gdx.input.isKeyJustPressed(Keys.SPACE)) {
            callbacks.onToggleFastFwd();
        }

        if (Gdx.input.isKeyJustPressed(Keys.F9)) {
            callbacks.onForceHdFallback();
        }

        return shouldShowMenu;
    }
}
