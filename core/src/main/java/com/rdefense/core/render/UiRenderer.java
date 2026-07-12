package com.rdefense.core.render;

import com.rdefense.core.game.GameTower;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameRenderer;

/**
 * UI 控件渲染器 — 绘制塔选择按钮、升级对话框、暂停/快进/菜单按钮
 */
public class UiRenderer {

    private final GameRenderer renderer;
    private final CameraManager camera;

    private TowerButtonInfo[] towerButtons;
    private int activeTowerId = -1;
    private int activeTowerGridX = 0;
    private int activeTowerGridY = 0;

    private UpgradeDialogState upgradeDialog;
    private GameTower selectedTower;
    private GameTower clickedTower;
    private GameTower highlightedTower;  // 高亮显示的塔（显示攻击范围）

    private boolean pauseButtonPressed;
    private boolean ffButtonPressed;
    private boolean menuButtonPressed;

    private String placementFailureMsg = null;
    private int placementFailureTimer = 0;
    private static final int PLACEMENT_FAILURE_DURATION = 90;
    private String shortMessage = null;
    private int shortMessageTimer = 0;
    private static final int SHORT_MESSAGE_DURATION = 150;

    public UiRenderer(GameRenderer renderer, CameraManager camera) {
        this.renderer = renderer;
        this.camera = camera;
        this.upgradeDialog = new UpgradeDialogState();
    }

    public void initTowerButtons(TowerButtonInfo[] buttons) {
        this.towerButtons = buttons;
    }
    
    public void resize(int screenWidth, int screenHeight) {
        // 重新初始化塔按钮位置
        if (towerButtons != null) {
            for (int i = 0; i < towerButtons.length; i++) {
                TowerButtonInfo button = towerButtons[i];
                if (button != null) {
                    int buttonSize = 64;
                    int buttonGap = 8;
                    int border = 4;
                    button.screenX = screenWidth - border - buttonSize - (buttonSize + buttonGap) * i;
                    button.screenY = screenHeight - border - buttonSize;
                }
            }
        }
    }

    public void renderTowerButtons(int money, int stateIndex) {
        if (towerButtons == null) return;

        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();
        boolean hasActiveTower = activeTowerId >= 0;

        for (TowerButtonInfo button : towerButtons) {
            if (button == null) continue;

            float screenX = button.screenX;
            float screenY = button.screenY;

            boolean canAfford = button.cost <= money;
            boolean isActive = hasActiveTower && activeTowerId == button.towerType;

            if (isActive) {
                renderer.drawRect(screenX, screenY, button.width, button.height, 0.2f, 0.6f, 1.0f, 0.9f);
                renderer.drawRect(screenX - 2, screenY - 2, button.width + 4, 2, 0.4f, 0.8f, 1.0f, 1.0f);
                renderer.drawRect(screenX - 2, screenY + button.height, button.width + 4, 2, 0.4f, 0.8f, 1.0f, 1.0f);
            } else if (canAfford) {
                renderer.drawRect(screenX, screenY, button.width, button.height, 0.25f, 0.3f, 0.35f, 0.8f);
            } else {
                renderer.drawRect(screenX, screenY, button.width, button.height, 0.3f, 0.15f, 0.15f, 0.5f);
            }

            renderer.drawText(button.towerName, screenX + button.width / 2 - 18, screenY + button.height - 12,
                    1.0f, 1.0f, 1.0f, 1.0f);

            String priceText = "$" + button.cost;
            if (button.cost > money) {
                renderer.drawText(priceText, screenX + button.width / 2 - 12, screenY + 8,
                        1.0f, 0.4f, 0.4f, 1.0f);
            } else {
                renderer.drawText(priceText, screenX + button.width / 2 - 12, screenY + 8,
                        0.3f, 1.0f, 0.3f, 1.0f);
            }
        }

        renderer.end();
    }

    public int handleTowerButtonClick(float screenX, float screenY) {
        if (towerButtons == null) return -1;

        for (TowerButtonInfo button : towerButtons) {
            if (button == null) continue;

            if (screenX >= button.screenX && screenX <= button.screenX + button.width
                    && screenY >= button.screenY && screenY <= button.screenY + button.height) {
                return button.towerType;
            }
        }
        return -1;
    }

    /**
     * 计算某个升级选项的屏幕坐标
     */
    private float getOptionY(float dialogY, int index) {
        // 标题栏高度 30, 属性显示 40, 间距 10, 选项间隔 35
        return dialogY + upgradeDialog.height - 30 - 40 - 10 - (index + 1) * 35;
    }

    public void renderUpgradeDialog(int money, int stateIndex) {
        if (!upgradeDialog.visible) return;

        // 重置相机变换，确保使用屏幕坐标系
        renderer.applyCameraTransform(0, 0, 1.0f);

        renderer.begin();

        float dialogX = upgradeDialog.screenX - upgradeDialog.width / 2;
        float dialogY = upgradeDialog.screenY - upgradeDialog.height / 2;

        // 绘制半透明遮罩（覆盖整个屏幕）
        renderer.drawRect(0, 0, renderer.getScreenWidth(), renderer.getScreenHeight(), 0.0f, 0.0f, 0.0f, 0.4f);

        renderer.drawRect(dialogX, dialogY, upgradeDialog.width, upgradeDialog.height,
                0.08f, 0.12f, 0.18f, 0.98f);

        renderer.drawRect(dialogX, dialogY, upgradeDialog.width, 3, 0.3f, 0.6f, 1.0f, 1.0f);
        renderer.drawRect(dialogX, dialogY + upgradeDialog.height - 3, upgradeDialog.width, 3, 0.3f, 0.6f, 1.0f, 1.0f);
        renderer.drawRect(dialogX, dialogY, 3, upgradeDialog.height, 0.3f, 0.6f, 1.0f, 1.0f);
        renderer.drawRect(dialogX + upgradeDialog.width - 3, dialogY, 3, upgradeDialog.height, 0.3f, 0.6f, 1.0f, 1.0f);

        // 顶部标题栏
        float headerY = dialogY + upgradeDialog.height - 30;
        renderer.drawRect(dialogX + 5, headerY, upgradeDialog.width - 10, 25, 0.2f, 0.3f, 0.4f, 0.8f);
        renderer.drawText(getTowerDisplayName(upgradeDialog.towerType), dialogX + 15, headerY + 18, 0.9f, 0.9f, 1.0f, 1.0f);

        // 属性显示区
        float propY = headerY - 40;
        int power = TowerData.power(upgradeDialog.towerType);
        int range = TowerData.attackRadius(upgradeDialog.towerType);
        int delay = TowerData.shotDelay(upgradeDialog.towerType);
        renderer.drawText("攻击:" + power + " 范围:" + range + " 射速:" + (1000/delay), dialogX + 15, propY, 0.7f, 0.7f, 0.7f, 1.0f);

        // 等级显示（移到右边，确保不重叠）
        String levelText = "等级 " + getUpgradeLevel(upgradeDialog.towerType);
        renderer.drawText(levelText, dialogX + upgradeDialog.width - 80, propY, 0.7f, 0.7f, 0.7f, 1.0f);

        // 升级选项区（从上到下绘制，索引0在最上面）
        for (int i = 0; i < upgradeDialog.options.length; i++) {
            UpgradeOption option = upgradeDialog.options[i];
            if (option == null) continue;

            float optionY = getOptionY(dialogY, i);
            boolean canAfford = option.cost <= money;

            // 选项背景
            if (canAfford) {
                renderer.drawRect(dialogX + 10, optionY, upgradeDialog.width - 20, 30, 0.15f, 0.3f, 0.15f, 0.7f);
            } else {
                renderer.drawRect(dialogX + 10, optionY, upgradeDialog.width - 20, 30, 0.25f, 0.15f, 0.15f, 0.5f);
            }

            // 选项名称
            renderer.drawText(option.name, dialogX + 15, optionY + 20, 0.9f, 0.9f, 0.9f, 1.0f);

            // 属性加成
            float bonusX = dialogX + 120;
            if (option.powerBonus != 0) {
                renderer.drawText("攻击+" + option.powerBonus, bonusX, optionY + 20, 0.6f, 0.8f, 0.6f, 1.0f);
                bonusX += 55;
            }
            if (option.rangeBonus != 0) {
                renderer.drawText("范围+" + option.rangeBonus, bonusX, optionY + 20, 0.6f, 0.8f, 0.6f, 1.0f);
            }

            // 价格
            String priceText = "$" + option.cost;
            if (canAfford) {
                renderer.drawText(priceText, dialogX + upgradeDialog.width - 50, optionY + 20, 0.3f, 1.0f, 0.3f, 1.0f);
            } else {
                renderer.drawText(priceText, dialogX + upgradeDialog.width - 50, optionY + 20, 1.0f, 0.3f, 0.3f, 1.0f);
            }
        }

        // 出售按钮（底部）
        float sellY = dialogY + 10;
        renderer.drawRect(dialogX + 10, sellY, upgradeDialog.width - 20, 30, 0.3f, 0.2f, 0.1f, 0.8f);
        renderer.drawRect(dialogX + 10, sellY, upgradeDialog.width - 20, 2, 0.5f, 0.4f, 0.3f, 1.0f);
        renderer.drawRect(dialogX + 10, sellY + 28, upgradeDialog.width - 20, 2, 0.5f, 0.4f, 0.3f, 1.0f);
        renderer.drawText("出售 $" + upgradeDialog.sellValue, dialogX + upgradeDialog.width / 2 - 30, sellY + 20, 1.0f, 0.9f, 0.5f, 1.0f);

        renderer.end();
    }

    private int getUpgradeLevel(int towerType) {
        if (towerType == 1 || towerType == 4 || towerType == 7 || towerType == 10 ||
            towerType == 12 || towerType == 14 || towerType == 16 || towerType == 18 || towerType == 20) {
            return 1;
        } else if (towerType == 2 || towerType == 5 || towerType == 8 || towerType == 11 ||
                   towerType == 13 || towerType == 15 || towerType == 17 || towerType == 19 || towerType == 21) {
            return 2;
        } else {
            return 3;
        }
    }

    private String getTowerDisplayName(int towerType) {
        switch (towerType) {
            case 1: case 2: case 3: return "机枪塔";
            case 4: case 5: case 6: return "减速塔";
            case 7: case 8: case 9: return "火箭塔";
            case 10: case 11: return "火焰塔";
            case 12: case 13: return "防空塔";
            case 14: case 15: return "导弹塔";
            case 16: case 17: return "迫击炮";
            case 18: case 19: return "传送塔";
            case 20: case 21: return "地雷塔";
            case 22: return "照明弹";
            default: return "塔";
        }
    }

    public void renderControlButtons(int runState) {
        renderer.begin();

        int pauseX = renderer.getScreenWidth() - 100;
        int pauseY = renderer.getScreenHeight() - 30;
        String pauseIcon = (runState == 0 || runState == 4) ? "icon_pause" : "icon_play";
        renderer.drawSprite(pauseIcon, pauseX, pauseY);

        int ffX = renderer.getScreenWidth() - 60;
        int ffY = renderer.getScreenHeight() - 30;
        String ffIcon = (runState == 4) ? "icon_ff_active" : "icon_ff";
        renderer.drawSprite(ffIcon, ffX, ffY);

        int menuX = renderer.getScreenWidth() - 30;
        int menuY = renderer.getScreenHeight() - 30;
        renderer.drawSprite("icon_menu", menuX, menuY);

        renderer.end();
    }

    public void renderActiveTowerPreview(int gridX, int gridY, int towerType, float attackRange) {
        renderer.begin();

        int worldX = gridX * 32;
        int worldY = gridY * 32;

        float screenX = camera.worldToScreenX(worldX);
        float screenY = camera.worldToScreenY(worldY);

        float screenRange = attackRange * camera.getScale();
        renderer.drawCircle(screenX, screenY, screenRange, 0.5f, 0.5f, 1.0f, 0.3f);

        String region = "tower_" + towerType + "_preview";
        renderer.drawSprite(region, screenX, screenY, 0.5f, 0.5f, 1.0f, 0.5f);

        renderer.end();
    }

    public void renderPauseOverlay(String pauseText) {
        renderer.begin();

        renderer.drawRect(0, 0, renderer.getScreenWidth(), renderer.getScreenHeight(),
                0.0f, 0.0f, 0.0f, 0.65f);

        float centerX = renderer.getScreenWidth() / 2.0f;
        float centerY = renderer.getScreenHeight() / 2.0f;
        renderer.drawText(pauseText, centerX, centerY, 1.0f, 1.0f, 0.8f, 1.0f);
        renderer.drawText("按空格继续", centerX, centerY - 40, 0.7f, 0.7f, 0.7f, 0.9f);

        renderer.end();
    }

    public void renderGameOverOverlay(String title, String subtitle) {
        renderer.begin();

        renderer.drawRect(0, 0, renderer.getScreenWidth(), renderer.getScreenHeight(),
                0.0f, 0.0f, 0.0f, 0.72f);

        float centerX = renderer.getScreenWidth() / 2.0f;
        float centerY = renderer.getScreenHeight() / 2.0f;
        renderer.drawText(title, centerX, centerY, 1.0f, 1.0f, 0.8f, 1.0f);
        renderer.drawText(subtitle, centerX, centerY - 40, 0.7f, 0.7f, 0.7f, 0.95f);

        renderer.end();
    }

    public void showUpgradeDialog(float worldX, float worldY, int towerType, UpgradeOption[] options, int sellValue) {
        upgradeDialog.visible = true;
        upgradeDialog.worldX = worldX;
        upgradeDialog.worldY = worldY;
        // 将世界坐标转换为屏幕坐标，用于对话框定位
        upgradeDialog.screenX = camera.worldToScreenX(worldX);
        upgradeDialog.screenY = camera.worldToScreenY(worldY);
        upgradeDialog.width = 240;
        upgradeDialog.height = 180 + options.length * 35;
        upgradeDialog.towerType = towerType;
        upgradeDialog.options = options;
        upgradeDialog.sellValue = sellValue;
    }

    public void hideUpgradeDialog() {
        upgradeDialog.visible = false;
        selectedTower = null;
    }

    public boolean isUpgradeDialogVisible() {
        return upgradeDialog.visible;
    }

    public void setSelectedTower(GameTower tower) {
        this.selectedTower = tower;
    }

    public GameTower getSelectedTower() {
        return this.selectedTower;
    }

    public void setClickedTower(GameTower tower) {
        this.clickedTower = tower;
    }

    public GameTower getClickedTower() {
        return this.clickedTower;
    }

    public boolean handleUpgradeDialogDown(float screenX, float screenY) {
        if (!upgradeDialog.visible) return false;

        // 直接使用屏幕坐标
        float dialogX = upgradeDialog.screenX - upgradeDialog.width / 2;
        float dialogY = upgradeDialog.screenY - upgradeDialog.height / 2;

        if (screenX < dialogX || screenX > dialogX + upgradeDialog.width ||
            screenY < dialogY || screenY > dialogY + upgradeDialog.height) {
            return false;
        }

        // 检查升级选项
        for (int i = 0; i < upgradeDialog.options.length; i++) {
            UpgradeOption option = upgradeDialog.options[i];
            if (option == null) continue;
            float optionY = getOptionY(dialogY, i);
            if (screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                screenY >= optionY && screenY <= optionY + 30) {
                option.hovered = true;
            } else {
                option.hovered = false;
            }
        }

        // 检查出售按钮
        float sellY = dialogY + 10;
        upgradeDialog.sellHovered = (screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                                   screenY >= sellY && screenY <= sellY + 30);

        return true;
    }

    public void handleUpgradeDialogDrag(float screenX, float screenY) {
        if (!upgradeDialog.visible) return;

        // 直接使用屏幕坐标
        float dialogX = upgradeDialog.screenX - upgradeDialog.width / 2;
        float dialogY = upgradeDialog.screenY - upgradeDialog.height / 2;

        for (int i = 0; i < upgradeDialog.options.length; i++) {
            UpgradeOption option = upgradeDialog.options[i];
            if (option == null) continue;
            float optionY = getOptionY(dialogY, i);
            option.hovered = (screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                            screenY >= optionY && screenY <= optionY + 30);
        }

        float sellY = dialogY + 10;
        upgradeDialog.sellHovered = (screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                                   screenY >= sellY && screenY <= sellY + 30);
    }

    public int handleUpgradeDialogUp(float screenX, float screenY) {
        if (!upgradeDialog.visible) return -1;

        // 直接使用屏幕坐标
        float dialogX = upgradeDialog.screenX - upgradeDialog.width / 2;
        float dialogY = upgradeDialog.screenY - upgradeDialog.height / 2;

        int result = -1;

        // 检查升级选项
        for (int i = 0; i < upgradeDialog.options.length; i++) {
            UpgradeOption option = upgradeDialog.options[i];
            if (option == null) continue;
            float optionY = getOptionY(dialogY, i);
            if (option.hovered && screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                screenY >= optionY && screenY <= optionY + 30) {
                result = option.upgradeTypeId;
            }
            option.hovered = false;
        }

        // 检查出售按钮
        if (result == -1) {
            float sellY = dialogY + 10;
            if (upgradeDialog.sellHovered && screenX >= dialogX + 10 && screenX <= dialogX + upgradeDialog.width - 10 &&
                screenY >= sellY && screenY <= sellY + 30) {
                result = 0;
            }
            upgradeDialog.sellHovered = false;
        }

        return result;
    }

    public int getActiveTowerId() {
        return activeTowerId;
    }

    public TowerButtonInfo[] getTowerButtons() {
        return towerButtons;
    }

    public void setActiveTowerId(int towerId) {
        this.activeTowerId = towerId;
    }

    public void setActiveTowerPosition(int gridX, int gridY) {
        this.activeTowerGridX = gridX;
        this.activeTowerGridY = gridY;
    }

    public int getActiveTowerGridX() {
        return activeTowerGridX;
    }

    public int getActiveTowerGridY() {
        return activeTowerGridY;
    }

    public boolean hasActiveTowerPreview() {
        return activeTowerId >= 0 && (activeTowerGridX != 0 || activeTowerGridY != 0);
    }

    public static class TowerButtonInfo {
        public int towerType;
        public String towerName;
        public int cost;
        public float screenX, screenY;
        public int width, height;
        public boolean selected;
    }

    public static class UpgradeOption {
        public String name;
        public int cost;
        public int upgradeTypeId;
        public int powerBonus;
        public int rangeBonus;
        public boolean hovered;
    }

    private static class UpgradeDialogState {
        boolean visible = false;
        float worldX, worldY;
        float screenX, screenY;
        int width, height;
        int towerType;
        UpgradeOption[] options;
        int sellValue;
        boolean sellHovered;
    }

    public void showPlacementFailure(int reason) {
        switch (reason) {
            case 1: placementFailureMsg = "金币不足"; break;
            case 2: placementFailureMsg = "超出地图范围"; break;
            case 3: placementFailureMsg = "此处已有防御塔"; break;
            case 4: placementFailureMsg = "不能阻挡路径"; break;
            default: placementFailureMsg = "无法放置"; break;
        }
        placementFailureTimer = PLACEMENT_FAILURE_DURATION;
    }

    public void updatePlacementFailure() {
        if (placementFailureTimer > 0) {
            placementFailureTimer--;
            if (placementFailureTimer <= 0) {
                placementFailureMsg = null;
            }
        }
        if (shortMessageTimer > 0) {
            shortMessageTimer--;
            if (shortMessageTimer <= 0) {
                shortMessage = null;
            }
        }
    }

    public void renderPlacementFailure() {
        if (placementFailureMsg == null || placementFailureTimer <= 0) return;

        float alpha = Math.min(1.0f, (float) placementFailureTimer / 30.0f);
        renderer.begin();
        renderer.drawText(placementFailureMsg, 10, renderer.getScreenHeight() - 80, 1.0f, 0.3f, 0.3f, alpha);
        renderer.end();
    }

    public void showShortMessage(String msg) {
        this.shortMessage = msg;
        this.shortMessageTimer = SHORT_MESSAGE_DURATION;
    }

    public void renderShortMessage() {
        if (shortMessage == null || shortMessageTimer <= 0) return;
        float alpha = Math.min(1.0f, (float) shortMessageTimer / SHORT_MESSAGE_DURATION);
        renderer.begin();
        float x = renderer.getScreenWidth() / 2.0f;
        float y = renderer.getScreenHeight() - 20;
        renderer.drawText(shortMessage, x, y, 1.0f, 0.9f, 0.3f, alpha);
        renderer.end();
    }

    public GameTower getHighlightedTower() {
        return highlightedTower;
    }

    public void setHighlightedTower(GameTower tower) {
        this.highlightedTower = tower;
    }
}
