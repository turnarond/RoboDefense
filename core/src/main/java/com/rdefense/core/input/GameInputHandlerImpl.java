package com.rdefense.core.input;

import com.rdefense.core.game.GameState;
import com.rdefense.core.game.GameTower;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameInputHandler;
import com.rdefense.core.render.CameraManager;
import com.rdefense.core.render.UiRenderer;
import com.rdefense.core.render.UiRenderer.UpgradeOption;

import java.util.ArrayList;
import java.util.List;

/**
 * 游戏输入处理器 — 将输入事件转换为游戏操作
 * 对应原版 GameInput 的核心逻辑
 */
public class GameInputHandlerImpl implements GameInputHandler {

    private final GameState gameState;
    private final CameraManager camera;
    private final UiRenderer uiRenderer;

    // 触摸状态
    private float touchDownX;
    private float touchDownY;
    private boolean isDragging;
    private static final float DRAG_THRESHOLD = 20.0f;

    public GameInputHandlerImpl(GameState gameState, CameraManager camera, UiRenderer uiRenderer) {
        this.gameState = gameState;
        this.camera = camera;
        this.uiRenderer = uiRenderer;
    }

    @Override
    public void onPointerDown(float x, float y, int pointer) {
        // 缩放滑块点击检测（最优先，以免被其他操作拦截）
        if (uiRenderer.handleScaleSliderClick((int) x, (int) y, camera)) return;

        this.touchDownX = x;
        this.touchDownY = y;
        this.isDragging = false;

        // 检查升级对话框点击（优先处理）
        if (uiRenderer.isUpgradeDialogVisible()) {
            if (!uiRenderer.handleUpgradeDialogDown(x, y)) {
                // 点击在对话框外部，关闭对话框并清除状态
                uiRenderer.hideUpgradeDialog();
                uiRenderer.setClickedTower(null);
            }
            return;
        }

        // 检查 UI 按钮点击
        int towerId = uiRenderer.handleTowerButtonClick(x, y);
        if (towerId >= 0) {
            uiRenderer.setActiveTowerId(towerId);
            return;
        }

        // 记录点击的塔（用于 onPointerUp 判断）
        int gridX = camera.screenToGridX(x);
        int gridY = camera.screenToGridY(y, gameState.getLevelData().getGridHeight());
        GameTower clickedTower = gameState.findTowerAt(gridX, gridY);
        uiRenderer.setClickedTower(clickedTower);
        uiRenderer.setHighlightedTower(clickedTower);
    }

    @Override
    public void onPointerDrag(float x, float y, int pointer) {
        // 如果升级对话框可见，处理对话框内的拖拽
        if (uiRenderer.isUpgradeDialogVisible()) {
            uiRenderer.handleUpgradeDialogDrag(x, y);
            return;
        }

        // 检查是否超过拖拽阈值
        float dx = x - touchDownX;
        float dy = y - touchDownY;
        if (!isDragging && (dx * dx + dy * dy) > DRAG_THRESHOLD * DRAG_THRESHOLD) {
            isDragging = true;
        }

        if (isDragging) {
            // 如果有激活的塔，移动预览位置
            int activeTowerId = uiRenderer.getActiveTowerId();
            if (activeTowerId >= 0) {
                int gridX = camera.screenToGridX(x);
                int gridY = camera.screenToGridY(y, gameState.getLevelData().getGridHeight());
                uiRenderer.setActiveTowerPosition(gridX, gridY);
            } else {
                // 平移视图
                camera.pan(-dx, -dy);
                touchDownX = x;
                touchDownY = y;
            }
        }
    }

    @Override
    public void onPointerMove(float x, float y) {
        // 鼠标移动（不按按钮）时，如果有激活的塔，更新预览位置
        int activeTowerId = uiRenderer.getActiveTowerId();
        if (activeTowerId >= 0) {
            int gridX = camera.screenToGridX(x);
            int gridY = camera.screenToGridY(y, gameState.getLevelData().getGridHeight());
            uiRenderer.setActiveTowerPosition(gridX, gridY);
        }
    }

    @Override
    public void onPointerUp(float x, float y, int pointer) {
        // 如果升级对话框可见，处理释放事件
        if (uiRenderer.isUpgradeDialogVisible()) {
            int result = uiRenderer.handleUpgradeDialogUp(x, y);
            if (result >= 0) {
                // result = 0: 出售, result > 0: 升级类型
                GameTower tower = uiRenderer.getSelectedTower();
                if (tower != null) {
                    if (result == 0) {
                        gameState.sellTower(tower);
                    } else {
                        gameState.upgradeTower(tower, result);
                    }
                }
                uiRenderer.hideUpgradeDialog();
            }
            // 如果 result == -1（未点击任何按钮），保持对话框打开
            return;
        }

        if (!isDragging) {
            int activeTowerId = uiRenderer.getActiveTowerId();
            if (activeTowerId >= 0) {
                // 放置塔：优先使用预览坐标（如果存在）
                int gridX, gridY;
                if (uiRenderer.hasActiveTowerPreview()) {
                    gridX = uiRenderer.getActiveTowerGridX();
                    gridY = uiRenderer.getActiveTowerGridY();
                } else {
                    gridX = camera.screenToGridX(x);
                    gridY = camera.screenToGridY(y, gameState.getLevelData().getGridHeight());
                }
                System.out.println("[TowerDebug] place request id=" + activeTowerId +
                        " preview=(" + uiRenderer.getActiveTowerGridX() + "," + uiRenderer.getActiveTowerGridY() + ")" +
                        " clickGrid=(" + gridX + "," + gridY + ")");
                int result = gameState.tryPlaceTower(activeTowerId, gridX, gridY);
                if (result == 0) {
                    GameTower placedTower = gameState.findTowerAt(gridX, gridY);
                    System.out.println("[TowerDebug] placement result=0 actualTowerGrid=(" +
                            (placedTower != null ? placedTower.getGridX() : -1) + "," +
                            (placedTower != null ? placedTower.getGridY() : -1) + ")");
                } else {
                    uiRenderer.showPlacementFailure(result);
                }
                uiRenderer.setActiveTowerId(-1);
            } else {
                // 检查是否点击了已有的塔 -> 显示升级对话框
                GameTower clickedTower = uiRenderer.getClickedTower();
                if (clickedTower != null) {
                    showUpgradeDialogForTower(clickedTower);
                }
            }
        }

        isDragging = false;
    }

    /**
     * 显示塔的升级对话框
     */
    private void showUpgradeDialogForTower(GameTower tower) {
        int towerType = tower.getType();
        int sellValue = TowerData.sellValue(towerType);
        
        List<UpgradeOption> options = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int upgradeType = TowerData.upgradeType(towerType, i);
            if (upgradeType > 0) {
                UpgradeOption option = new UpgradeOption();
                option.name = getUpgradeName(upgradeType);
                option.cost = TowerData.cost(upgradeType);
                option.upgradeTypeId = upgradeType;
                option.powerBonus = TowerData.power(upgradeType) - TowerData.power(towerType);
                option.rangeBonus = TowerData.attackRadius(upgradeType) - TowerData.attackRadius(towerType);
                option.hovered = false;
                options.add(option);
            }
        }
        
        float worldX = tower.getGridX() * 32 + 16;
        float worldY = tower.getGridY() * 32 + 16;
        
        uiRenderer.showUpgradeDialog(worldX, worldY, towerType, options.toArray(new UpgradeOption[0]), sellValue);
        uiRenderer.setSelectedTower(tower);
    }

    /**
     * 获取升级选项名称
     */
    private String getUpgradeName(int towerType) {
        switch (towerType) {
            case 2: return "机枪塔 II";
            case 3: return "机枪塔 III";
            case 5: return "减速塔 II";
            case 6: return "减速塔 III";
            case 8: return "火箭塔 II";
            case 9: return "火箭塔 III";
            case 10: return "火焰塔";
            case 11: return "火焰塔 II";
            case 12: return "防空塔";
            case 13: return "防空塔 II";
            case 14: return "导弹塔";
            case 15: return "导弹塔 II";
            case 16: return "迫击炮";
            case 17: return "火炮";
            case 19: return "传送塔";
            case 21: return "地雷";
            default: return "升级";
        }
    }

    @Override
    public void onKeyPressed(int keyCode) {
        switch (keyCode) {
            case Keys.SPACE:
                gameState.togglePause();
                break;
            case Keys.F:
                gameState.toggleFastFwd();
                break;
            case Keys.NUM_1:
                uiRenderer.setActiveTowerId(TowerData.GUN_TOWER);
                break;
            case Keys.NUM_2:
                uiRenderer.setActiveTowerId(TowerData.SLOW_TOWER);
                break;
            case Keys.NUM_3:
                uiRenderer.setActiveTowerId(TowerData.ROCKET_TOWER);
                break;
            case Keys.ESCAPE:
                uiRenderer.setActiveTowerId(-1);
                uiRenderer.hideUpgradeDialog();
                break;
        }
    }

    @Override
    public void onKeyReleased(int keyCode) {
        // 暂无需要处理的按键释放事件
    }

    @Override
    public void onScrolled(float amountX, float amountY) {
        // 鼠标滚轮缩放
        float scaleFactor = amountY > 0 ? 0.9f : 1.1f;
        camera.zoom(scaleFactor);
    }
}
