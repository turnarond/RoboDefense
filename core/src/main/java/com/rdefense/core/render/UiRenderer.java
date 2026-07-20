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

    private int[] scaleSliderRect;

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

            // 纯色状态编码：钢板蓝(可买) / 暗深红(买不起) / 全息青框(选中)
            float br, bg, bb, ba;
            if (isActive) {
                br = 0.06f; bg = 0.16f; bb = 0.28f; ba = 0.94f;  // 钢板蓝底
            } else if (canAfford) {
                br = 0.08f; bg = 0.14f; bb = 0.26f; ba = 0.85f;
            } else {
                br = 0.16f; bg = 0.06f; bb = 0.08f; ba = 0.6f;   // 暗深红
            }
            renderer.drawRect(screenX, screenY, button.width, button.height, br, bg, bb, ba);

            // 选中态：全息青窄边框
            if (isActive) {
                renderer.drawRect(screenX, screenY, button.width, 2, 0.05f, 0.94f, 0.94f, 0.85f);
                renderer.drawRect(screenX, screenY + button.height - 2, button.width, 2, 0.05f, 0.94f, 0.94f, 0.85f);
                renderer.drawRect(screenX, screenY, 2, button.height, 0.05f, 0.94f, 0.94f, 0.85f);
                renderer.drawRect(screenX + button.width - 2, screenY, 2, button.height, 0.05f, 0.94f, 0.94f, 0.85f);
            }


            // 塔名/价格由 drawTowerButtonIcons 的底座+转头图标替代
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
        // 与 renderUpgradeDialog 中的布局一致：divY - 8 - (i+1)*30
        // divY = propY - 10, propY = headerY - 22, headerY = dialogY + height - 30
        float headerY = dialogY + upgradeDialog.height - 30;
        float propY = headerY - 22;
        float divY = propY - 10;
        return divY - 8 - (index + 1) * 30;
    }

    public void renderUpgradeDialog(int money, int stateIndex) {
        if (!upgradeDialog.visible) return;

        // 重置相机变换，确保使用屏幕坐标系
        renderer.applyCameraTransform(0, 0, 1.0f);

        renderer.begin();

        float dialogX = upgradeDialog.screenX - upgradeDialog.width / 2;
        float dialogY = upgradeDialog.screenY - upgradeDialog.height / 2;

        // 轻遮罩——游戏仍然可见
        renderer.drawRect(0, 0, renderer.getScreenWidth(), renderer.getScreenHeight(), 0f, 0f, 0f, 0.22f);

        // 数据卡片面板（半透明，游戏可见）
        renderer.drawRect(dialogX, dialogY, upgradeDialog.width, upgradeDialog.height,
                0.04f, 0.06f, 0.14f, 0.82f);
        renderer.drawRect(dialogX, dialogY + upgradeDialog.height - 1, upgradeDialog.width, 2,
                0.83f, 0.67f, 0.16f, 0.6f);

        // 标题：塔名 + 等级
        float headerY = dialogY + upgradeDialog.height - 30;
        renderer.drawText(getTowerDisplayName(upgradeDialog.towerType),
                dialogX + 14, headerY + 18, 0.83f, 0.78f, 0.7f, 1.0f);
        renderer.drawText("Lv." + getUpgradeLevel(upgradeDialog.towerType),
                dialogX + upgradeDialog.width - 56, headerY + 18, 0.4f, 0.8f, 0.5f, 0.85f);

        // 属性行
        float propY = headerY - 22;
        int power = TowerData.power(upgradeDialog.towerType);
        int range = TowerData.attackRadius(upgradeDialog.towerType);
        int delay = TowerData.shotDelay(upgradeDialog.towerType);
        renderer.drawText("攻" + power + "  范" + range + "  速" + delay,
                dialogX + 14, propY, 0.5f, 0.55f, 0.6f, 0.85f);

        // 细分隔线
        float divY = propY - 10;
        renderer.drawRect(dialogX + 12, divY, upgradeDialog.width - 24, 1, 0.25f, 0.3f, 0.35f, 0.4f);

        // 升级选项
        for (int i = 0; i < upgradeDialog.options.length; i++) {
            UpgradeOption option = upgradeDialog.options[i];
            if (option == null) continue;
            float optionY = divY - 8 - (i + 1) * 30;
            boolean canAfford = option.cost <= money;
            float oR = canAfford ? 0.06f : 0.14f, oG = canAfford ? 0.16f : 0.08f, oB = canAfford ? 0.1f : 0.1f;
            renderer.drawRect(dialogX + 12, optionY, upgradeDialog.width - 24, 26, oR, oG, oB, 0.78f);
            renderer.drawText(option.name, dialogX + 18, optionY + 16, 0.78f, 0.82f, 0.88f, 1.0f);
            StringBuilder bonus = new StringBuilder();
            if (option.powerBonus != 0) bonus.append("攻+").append(option.powerBonus).append(" ");
            if (option.rangeBonus != 0) bonus.append("范+").append(option.rangeBonus);
            if (bonus.length() > 0)
                renderer.drawText(bonus.toString(), dialogX + 110, optionY + 16, 0.4f, 0.7f, 0.4f, 0.9f);
            renderer.drawText("$" + option.cost, dialogX + upgradeDialog.width - 50, optionY + 16,
                    canAfford ? 0.4f : 0.85f, canAfford ? 0.85f : 0.25f, canAfford ? 0.35f : 0.2f, 1.0f);
        }

        // 出售按钮（锈红底，细边框）
        float sellY = dialogY + 10;
        renderer.drawRect(dialogX + 12, sellY, upgradeDialog.width - 24, 26, 0.32f, 0.1f, 0.1f, 0.78f);
        renderer.drawRect(dialogX + 12, sellY, upgradeDialog.width - 24, 1, 0.5f, 0.2f, 0.18f, 0.5f);
        renderer.drawText("出售 $" + upgradeDialog.sellValue, dialogX + upgradeDialog.width / 2 - 30,
                sellY + 16, 0.9f, 0.7f, 0.3f, 1.0f);

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
        // 键盘脚注条已在 HUD 中显示——文字标签冗余，移除
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

    /** 绘制缩放滑块条（顶部，受选项 6 控制，对应原版 DisplayScaleUI） */
    public void renderScaleSlider(com.rdefense.core.config.OptionsData options,
                                  com.rdefense.core.render.CameraManager camera) {
        if (options == null || !options.optionValue(6)) return;
        int screenW = renderer.getScreenWidth();
        int screenH = renderer.getScreenHeight();
        int barX = 16; // 左侧，避开 PTS 和状态文字
        int barY = screenH - 38;
        int barW = 150;
        int barH = 14;
        renderer.begin();
        renderer.drawRect(barX, barY, barW, barH, 0.15f, 0.15f, 0.2f, 0.8f);
        float scale = camera.getScale();
        float pct = (scale - 0.5f) / (2.0f - 0.5f);
        if (pct < 0f) pct = 0f; if (pct > 1f) pct = 1f;
        int knobX = barX + (int)(pct * (barW - 10));
        renderer.drawRect(knobX, barY + 1, 10, barH - 2, 0.3f, 0.7f, 0.95f, 1f);
        renderer.end();
        this.scaleSliderRect = new int[]{barX, barY, barW, barH};
    }

    /** 检查点击是否在滑块上，若是则更新缩放 */
    public boolean handleScaleSliderClick(int clickX, int clickY,
                                          com.rdefense.core.render.CameraManager camera) {
        if (scaleSliderRect == null) return false;
        int barX = scaleSliderRect[0], barY = scaleSliderRect[1],
            barW = scaleSliderRect[2], barH = scaleSliderRect[3];
        if (clickX < barX || clickX > barX + barW || clickY < barY - 6 || clickY > barY + barH + 6)
            return false;
        float pct = (float)(clickX - barX) / barW;
        float newScale = 0.5f + pct * 1.5f;
        camera.setScale(Math.max(0.5f, Math.min(2.0f, newScale)));
        return true;
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

    /**
     * 关卡结算明细（消费 EVENT_SCORE_SAVED，对应原版 ScoreOverlay 的简化实现：
     * 五行明细逐行显示 + 合计，帧驱动淡入）
     */
    public void renderScoreOverlay(com.rdefense.core.game.GameState gameState) {
        com.rdefense.core.game.GameEvent e =
                gameState.getGameEventList(com.rdefense.core.game.GameEvent.EVENT_SCORE_SAVED);
        if (e == null) return;

        int frame = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_FRAME_INDEX]++;
        int scoreAdd = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_ADD];
        int wonBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_WON_BONUS];
        int healthBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_HEALTH_BONUS];
        int perfectBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_PERFECT_BONUS];
        int moneyBonus = e.var[com.rdefense.core.game.GameEvent.VAR_SCORE_MONEY_BONUS];
        long total = (long) scoreAdd + wonBonus + healthBonus + perfectBonus + moneyBonus;

        int cx = renderer.getScreenWidth() / 2 - 110;
        int cy = renderer.getScreenHeight() / 2 - 90;
        renderer.begin();
        renderer.drawRect(cx - 20, cy - 30, 280, 190, 0f, 0f, 0f, 0.75f);
        String[] labels = {"得分", "胜利奖励", "生命奖励", "完美奖励", "金钱奖励"};
        int[] values = {scoreAdd, wonBonus, healthBonus, perfectBonus, moneyBonus};
        int shown = Math.min(labels.length, frame / 12 + 1);
        for (int i = 0; i < shown; i++) {
            renderer.drawText(labels[i], cx, cy + i * 24, 0.75f, 0.85f, 0.95f, 1.0f);
            renderer.drawText("+" + values[i], cx + 150, cy + i * 24, 1.0f, 0.9f, 0.2f, 1.0f);
        }
        if (shown >= labels.length) {
            renderer.drawText("合计积分", cx, cy + 132, 1.0f, 1.0f, 1.0f, 1.0f);
            renderer.drawText("+" + total, cx + 150, cy + 132, 0.2f, 1.0f, 0.6f, 1.0f);
        }
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
        // 提示显示在商店左侧，玩家视线在右下方时可立即看到
        int msgX = renderer.getScreenWidth() - 260;
        int msgY = renderer.getScreenHeight() / 2;
        renderer.drawText(placementFailureMsg, msgX, msgY, 1.0f, 0.35f, 0.2f, alpha);
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
