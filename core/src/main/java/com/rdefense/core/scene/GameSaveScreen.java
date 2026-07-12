package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.platform.GameRenderer;
import com.rdefense.core.save.db.GameSaveManager;
import com.rdefense.core.save.db.SaveSlotInfo;

import java.util.List;

/**
 * 存档选择界面 - 显示存档列表，支持加载/删除/创建存档
 */
public class GameSaveScreen extends GameScreen {

    private static final int MAX_SLOTS = 10;
    private static final int SLOT_HEIGHT = 60;
    private static final int PANEL_PADDING = 20;

    private List<SaveSlotInfo> slots;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private String errorMsg = null;
    private String successMsg = null;
    private int msgTimer = 0;

    /** 操作模式：0=加载，1=保存 */
    private final int mode;
    /** 保存模式下要保存的游戏状态（由GamePlayScreen传入） */
    private com.rdefense.core.game.GameState stateToSave;

    private static final int MODE_LOAD = 0;
    private static final int MODE_SAVE = 1;

    /**
     * 创建加载存档界面
     */
    public GameSaveScreen(RoboDefenseGame game) {
        super(game);
        this.mode = MODE_LOAD;
        refreshSlots();
    }

    /**
     * 创建保存存档界面
     */
    public GameSaveScreen(RoboDefenseGame game, com.rdefense.core.game.GameState state) {
        super(game);
        this.mode = MODE_SAVE;
        this.stateToSave = state;
        refreshSlots();
    }

    private void refreshSlots() {
        GameSaveManager manager = game.getGameSaveManager();
        System.out.println("[GameSaveScreen] refreshSlots - manager=" + manager);
        if (manager != null) {
            slots = manager.getAllSlots();
            System.out.println("[GameSaveScreen] getAllSlots returned " + (slots != null ? slots.size() : "null") + " slots");
            System.out.println("[GameSaveScreen] hasQuickSave=" + manager.hasQuickSave());
        }
        if (slots == null) {
            slots = new java.util.ArrayList<>();
        }
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    protected void update(float delta) {
        // 消息计时器
        if (msgTimer > 0) {
            msgTimer--;
            if (msgTimer == 0) {
                errorMsg = null;
                successMsg = null;
            }
        }

        // ESC返回
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        // 键盘导航
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            selectedIndex = Math.max(0, selectedIndex - 1);
        }
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            selectedIndex = Math.min(slots.size(), selectedIndex + 1);
        }

        // Enter确认
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ENTER)) {
            handleSelection();
        }

        // 鼠标点击
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            handleClick(x, y);
        }
    }

    private void handleSelection() {
        if (selectedIndex < 0) return;

        GameSaveManager manager = game.getGameSaveManager();
        if (manager == null) return;

        if (mode == MODE_LOAD) {
            // 加载模式：选中已有存档
            if (selectedIndex < slots.size()) {
                SaveSlotInfo slot = slots.get(selectedIndex);
                loadSlot(slot.slotId);
            }
        } else {
            // 保存模式
            if (selectedIndex < slots.size()) {
                // 覆盖已有存档
                SaveSlotInfo slot = slots.get(selectedIndex);
                saveToSlot(slot.slotId);
            } else {
                // 新建存档
                int newSlot = manager.getNextFreeSlot();
                if (newSlot > 0) {
                    saveToSlot(newSlot);
                } else {
                    showError("存档槽位已满");
                }
            }
        }
    }

    private void handleClick(int x, int y) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();

        // 计算存档列表区域
        int listY = h - 80;
        int listH = h - 160;

        // 检查点击的是哪个存档项
        for (int i = 0; i <= slots.size(); i++) {
            int itemY = listY - i * SLOT_HEIGHT - scrollOffset;
            if (y >= itemY - SLOT_HEIGHT && y < itemY) {
                if (x >= PANEL_PADDING && x < w - PANEL_PADDING) {
                    selectedIndex = i;
                    handleSelection();
                    return;
                }
            }
        }

        // 返回按钮
        if (y >= 20 && y < 65 && x >= 20 && x < 160) {
            switchScreen(new MainMenuScreen(game));
        }
    }

    private void loadSlot(int slotId) {
        GameSaveManager manager = game.getGameSaveManager();
        if (manager == null) return;

        com.rdefense.core.game.GameState state = new com.rdefense.core.game.GameState(1, 0, false);
        if (manager.loadSave(slotId, state)) {
            GamePlayScreen gps = new GamePlayScreen(game, true);
            switchScreen(gps);
            showSuccess("存档加载成功");
        } else {
            showError("存档加载失败");
        }
    }

    private void saveToSlot(int slotId) {
        GameSaveManager manager = game.getGameSaveManager();
        if (manager == null || stateToSave == null) return;

        if (manager.createSave(slotId, stateToSave, null)) {
            showSuccess("存档保存成功");
            refreshSlots();
        } else {
            showError("存档保存失败");
        }
    }

    private void deleteSlot(int slotId) {
        GameSaveManager manager = game.getGameSaveManager();
        if (manager == null) return;

        if (manager.deleteSave(slotId)) {
            showSuccess("存档已删除");
            refreshSlots();
            selectedIndex = Math.min(selectedIndex, slots.size() - 1);
        } else {
            showError("删除失败");
        }
    }

    private void showError(String msg) {
        errorMsg = msg;
        msgTimer = 120;
    }

    private void showSuccess(String msg) {
        successMsg = msg;
        msgTimer = 120;
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

        // 标题
        String title = mode == MODE_LOAD ? "加载存档" : "保存游戏";
        renderer.drawText(title, 20, h - 20, 1.0f, 0.9f, 0.2f, 1.0f);

        // 存档列表区域
        int listY = h - 80;
        int listH = h - 160;
        renderer.drawRect(PANEL_PADDING, listY - listH, w - PANEL_PADDING * 2, listH,
                0.08f, 0.1f, 0.15f, 0.9f);

        // 绘制存档项
        int itemY = listY - 10;
        for (int i = 0; i < slots.size(); i++) {
            SaveSlotInfo slot = slots.get(i);
            boolean selected = (i == selectedIndex);

            // 选中高亮
            if (selected) {
                renderer.drawRect(PANEL_PADDING + 5, itemY - SLOT_HEIGHT + 5,
                        w - PANEL_PADDING * 2 - 10, SLOT_HEIGHT - 10,
                        0.2f, 0.3f, 0.5f, 0.8f);
            }

            // 存档信息
            renderer.drawText(slot.getDisplayName(), PANEL_PADDING + 15, itemY - 10,
                    1.0f, 1.0f, 1.0f, 1.0f);
            renderer.drawText(slot.getMapName() + " 关卡" + slot.levelNum + " 难度" + slot.difficulty,
                    PANEL_PADDING + 15, itemY - 30, 0.7f, 0.7f, 0.7f, 1.0f);
            renderer.drawText("金币:" + slot.money + " 积分:" + slot.score + " " + slot.getPlayTimeFormatted(),
                    PANEL_PADDING + 15, itemY - 50, 0.6f, 0.6f, 0.6f, 1.0f);
            renderer.drawText(slot.getUpdateTimeFormatted(), w - 150, itemY - 30,
                    0.5f, 0.5f, 0.5f, 1.0f);

            itemY -= SLOT_HEIGHT;
        }

        // 保存模式下显示"新建存档"选项
        if (mode == MODE_SAVE) {
            boolean selected = (selectedIndex == slots.size());
            if (selected) {
                renderer.drawRect(PANEL_PADDING + 5, itemY - SLOT_HEIGHT + 5,
                        w - PANEL_PADDING * 2 - 10, SLOT_HEIGHT - 10,
                        0.2f, 0.3f, 0.5f, 0.8f);
            }
            renderer.drawText("+ 新建存档", PANEL_PADDING + 15, itemY - 30,
                    0.8f, 1.0f, 0.8f, 1.0f);
        }

        // 提示信息
        if (errorMsg != null) {
            renderer.drawText(errorMsg, w / 2 - 80, h - 50, 1.0f, 0.3f, 0.3f, 1.0f);
        } else if (successMsg != null) {
            renderer.drawText(successMsg, w / 2 - 80, h - 50, 0.3f, 1.0f, 0.3f, 1.0f);
        }

        // 底部提示
        renderer.drawRect(0, 0, w, 40, 0.0f, 0.0f, 0.0f, 0.6f);
        renderer.drawText("↑↓选择  Enter确认  ESC返回", 20, 28, 0.6f, 0.6f, 0.6f, 1.0f);

        // 返回按钮
        renderer.drawRect(20, 20, 140, 45, 0.25f, 0.25f, 0.35f, 0.9f);
        renderer.drawText("返回", 60, 48, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }
}
