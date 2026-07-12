package com.rdefense.core.scene;

import com.badlogic.gdx.Gdx;
import com.rdefense.core.RoboDefenseGame;
import com.rdefense.core.game.AchievementData;
import com.rdefense.core.platform.GameRenderer;

import java.util.ArrayList;
import java.util.List;

public class AchievementScreen extends GameScreen {

    private static final int TITLE_HEIGHT = 60;
    private static final int ROW_HEIGHT = 52;  // 增加行高
    private static final int ROW_PADDING = 4;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_HEIGHT = 40;
    private static final int SEARCH_BOX_HEIGHT = 36;
    private static final int SEARCH_BOX_WIDTH = 200;

    private String searchText = "";
    private FilterMode filterMode = FilterMode.ALL;
    private AchievementData.AchievementCategory selectedCategory = null;  // 分类筛选
    private int scrollOffset = 0;
    private List<Integer> filteredAchievements;

    private enum FilterMode {
        ALL("全部"),
        EARNED("已获得"),
        PENDING("未获得");

        final String label;
        FilterMode(String label) {
            this.label = label;
        }
    }

    public AchievementScreen(RoboDefenseGame game) {
        super(game);
        updateFilteredList();
    }

    @Override
    protected void init() {
        Gdx.input.setCursorCatched(false);
    }

    private void updateFilteredList() {
        filteredAchievements = new ArrayList<>();
        String lowerSearch = searchText.toLowerCase();

        for (int i = 0; i < AchievementData.ACHIEVEMENT_TYPE_COUNT; i++) {
            // 使用中文名称搜索
            String name = AchievementData.getNameZh(i);
            
            if (!lowerSearch.isEmpty() && !name.toLowerCase().contains(lowerSearch)) {
                continue;
            }

            // 分类筛选
            if (selectedCategory != null && AchievementData.getCategory(i) != selectedCategory) {
                continue;
            }

            boolean achieved = AchievementData.isAchieved(i);
            switch (filterMode) {
                case ALL:
                    filteredAchievements.add(i);
                    break;
                case EARNED:
                    if (achieved) filteredAchievements.add(i);
                    break;
                case PENDING:
                    if (!achieved) filteredAchievements.add(i);
                    break;
            }
        }
    }

    @Override
    protected void update(float delta) {
        handleKeyboardInput();
        handleMouseInput();
    }

    private void handleKeyboardInput() {
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.ESCAPE)) {
            switchScreen(new MainMenuScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.UP)) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            int maxScroll = Math.max(0, filteredAchievements.size() - getVisibleRowCount());
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F1)) {
            filterMode = FilterMode.ALL;
            updateFilteredList();
            scrollOffset = 0;
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F2)) {
            filterMode = FilterMode.EARNED;
            updateFilteredList();
            scrollOffset = 0;
            return;
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F3)) {
            filterMode = FilterMode.PENDING;
            updateFilteredList();
            scrollOffset = 0;
        }
    }

    private void handleMouseInput() {
        if (Gdx.input.justTouched()) {
            int x = Gdx.input.getX();
            int y = Gdx.graphics.getHeight() - Gdx.input.getY();
            int screenWidth = Gdx.graphics.getWidth();
            int screenHeight = Gdx.graphics.getHeight();

            int backX = screenWidth - BUTTON_WIDTH - 16;
            int backY = 16;
            if (x >= backX && x <= backX + BUTTON_WIDTH && y >= backY && y <= backY + BUTTON_HEIGHT) {
                switchScreen(new MainMenuScreen(game));
                return;
            }

            int searchX = 16;
            int searchY = screenHeight - 90;
            if (x >= searchX && x <= searchX + SEARCH_BOX_WIDTH &&
                    y >= searchY && y <= searchY + SEARCH_BOX_HEIGHT) {
                return;
            }

            int filterStartX = searchX + SEARCH_BOX_WIDTH + 10;
            int filterWidth = 80;
            for (FilterMode mode : FilterMode.values()) {
                if (x >= filterStartX && x <= filterStartX + filterWidth &&
                        y >= searchY && y <= searchY + SEARCH_BOX_HEIGHT) {
                    filterMode = mode;
                    updateFilteredList();
                    scrollOffset = 0;
                    return;
                }
                filterStartX += filterWidth + 5;
            }

            // 分类标签点击处理
            int tabY = screenHeight - 130;
            int tabWidth = 60;
            int tabHeight = 32;
            int tabStartX = 16;

            if (y >= tabY && y <= tabY + tabHeight) {
                // "全部" 标签
                if (x >= tabStartX && x <= tabStartX + tabWidth) {
                    selectedCategory = null;
                    updateFilteredList();
                    scrollOffset = 0;
                    return;
                }
                tabStartX += tabWidth + 5;

                // 各分类标签
                for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
                    if (x >= tabStartX && x <= tabStartX + tabWidth) {
                        selectedCategory = cat;
                        updateFilteredList();
                        scrollOffset = 0;
                        return;
                    }
                    tabStartX += tabWidth + 5;
                }
            }

            int listTop = screenHeight - TITLE_HEIGHT - SEARCH_BOX_HEIGHT - 70;  // 调整列表位置
            int listBottom = BUTTON_HEIGHT + 30;
            if (y >= listBottom && y <= listTop) {
                int rowHeight = ROW_HEIGHT + ROW_PADDING;
                int clickRow = (listTop - y) / rowHeight + scrollOffset;
                if (clickRow >= 0 && clickRow < filteredAchievements.size()) {
                    int achievementId = filteredAchievements.get(clickRow);
                }
            }

            // 滚动条点击处理 - 只在点击时触发
            int scrollBarX = screenWidth - 12;
            int scrollBarY = listBottom;
            int scrollBarHeight = listTop - listBottom;
            if (x >= scrollBarX - 8 && x <= scrollBarX + 8 && y >= scrollBarY && y <= scrollBarY + scrollBarHeight) {
                if (filteredAchievements.size() > getVisibleRowCount()) {
                    float ratio = (float) (listTop - y) / scrollBarHeight;
                    int maxScroll = filteredAchievements.size() - getVisibleRowCount();
                    scrollOffset = (int) (ratio * maxScroll);
                    scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
                }
            }
        }

        // 鼠标滚轮滚动处理
        int scrollAmount = Gdx.input.getDeltaY();
        if (Math.abs(scrollAmount) > 2) {  // 添加阈值，避免鼠标移动误触发
            int maxScroll = Math.max(0, filteredAchievements.size() - getVisibleRowCount());
            // 根据滚动量调整滚动速度，使滚轮更灵敏
            int scrollDelta = scrollAmount > 0 ? -2 : 2;
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset + scrollDelta));
        }
    }

    private int getVisibleRowCount() {
        int screenHeight = Gdx.graphics.getHeight();
        int availableHeight = screenHeight - TITLE_HEIGHT - SEARCH_BOX_HEIGHT - BUTTON_HEIGHT - 100;  // 调整以适应分类标签栏
        return availableHeight / (ROW_HEIGHT + ROW_PADDING);
    }

    @Override
    protected void draw(float delta) {
        GameRenderer renderer = game.getServices().getRenderer();
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        renderer.applyCameraTransform(0, 0, 1.0f);
        renderer.begin();

        renderer.drawRect(0, 0, screenWidth, screenHeight, 0.02f, 0.02f, 0.06f, 1.0f);

        renderer.drawText("成就", 24, screenHeight - 24, 1.0f, 0.9f, 0.7f, 1.0f);

        int earnedCount = AchievementData.totalCount();
        int totalCount = AchievementData.ACHIEVEMENT_TYPE_COUNT;
        renderer.drawText(String.format("%d / %d", earnedCount, totalCount), screenWidth - 80, screenHeight - 24, 0.8f, 0.8f, 0.8f, 1.0f);

        drawSearchBox(renderer, screenWidth, screenHeight);
        drawFilterButtons(renderer, screenWidth, screenHeight);
        drawCategoryTabs(renderer, screenWidth, screenHeight);  // 分类标签栏
        drawAchievementList(renderer, screenWidth, screenHeight);

        int backX = screenWidth - BUTTON_WIDTH - 16;
        int backY = 16;
        renderer.drawRect(backX, backY, BUTTON_WIDTH, BUTTON_HEIGHT, 0.2f, 0.2f, 0.5f, 0.9f);
        renderer.drawText("返回", backX + 24, backY + BUTTON_HEIGHT - 14, 1.0f, 1.0f, 1.0f, 1.0f);

        renderer.end();
    }

    private void drawSearchBox(GameRenderer renderer, int screenWidth, int screenHeight) {
        int x = 16;
        int y = screenHeight - 90;

        renderer.drawRect(x, y, SEARCH_BOX_WIDTH, SEARCH_BOX_HEIGHT, 0.1f, 0.1f, 0.2f, 0.9f);
        
        String displayText = searchText.isEmpty() ? "搜索成就..." : searchText;
        float textColor = searchText.isEmpty() ? 0.5f : 1.0f;
        renderer.drawText(displayText, x + 8, y + SEARCH_BOX_HEIGHT - 12, textColor, textColor, textColor, 1.0f);
    }

    private void drawFilterButtons(GameRenderer renderer, int screenWidth, int screenHeight) {
        int x = 16 + SEARCH_BOX_WIDTH + 10;
        int y = screenHeight - 90;
        int buttonWidth = 80;

        for (FilterMode mode : FilterMode.values()) {
            float r = filterMode == mode ? 0.3f : 0.1f;
            float g = filterMode == mode ? 0.4f : 0.1f;
            float b = filterMode == mode ? 0.7f : 0.2f;
            renderer.drawRect(x, y, buttonWidth, SEARCH_BOX_HEIGHT, r, g, b, filterMode == mode ? 0.9f : 0.6f);
            renderer.drawText(mode.label, x + 12, y + SEARCH_BOX_HEIGHT - 12, 
                    filterMode == mode ? 1.0f : 0.6f, 
                    filterMode == mode ? 1.0f : 0.6f, 
                    filterMode == mode ? 1.0f : 0.6f, 1.0f);
            x += buttonWidth + 5;
        }
    }

    private void drawCategoryTabs(GameRenderer renderer, int screenWidth, int screenHeight) {
        int tabY = screenHeight - 130;
        int tabWidth = 60;
        int tabHeight = 32;
        int startX = 16;

        // "全部" 标签
        boolean allSelected = selectedCategory == null;
        renderer.drawRect(startX, tabY, tabWidth, tabHeight,
                allSelected ? 0.3f : 0.1f, allSelected ? 0.4f : 0.1f, allSelected ? 0.6f : 0.2f,
                allSelected ? 0.9f : 0.6f);
        renderer.drawText("全部", startX + 14, tabY + tabHeight - 10,
                allSelected ? 1.0f : 0.7f, allSelected ? 1.0f : 0.7f, allSelected ? 1.0f : 0.7f, 1.0f);
        startX += tabWidth + 5;

        // 各分类标签
        for (AchievementData.AchievementCategory cat : AchievementData.AchievementCategory.values()) {
            boolean selected = selectedCategory == cat;
            renderer.drawRect(startX, tabY, tabWidth, tabHeight,
                    selected ? 0.3f : 0.1f, selected ? 0.4f : 0.1f, selected ? 0.6f : 0.2f,
                    selected ? 0.9f : 0.6f);
            String label = cat.label.replace("成就", "");
            int textX = startX + (tabWidth - label.length() * 8) / 2;
            renderer.drawText(label, textX, tabY + tabHeight - 10,
                    selected ? 1.0f : 0.7f, selected ? 1.0f : 0.7f, selected ? 1.0f : 0.7f, 1.0f);
            startX += tabWidth + 5;
        }
    }

    private void drawAchievementList(GameRenderer renderer, int screenWidth, int screenHeight) {
        // 调整列表位置以适应分类标签栏
        int listTop = screenHeight - TITLE_HEIGHT - SEARCH_BOX_HEIGHT - 70;
        int listBottom = BUTTON_HEIGHT + 30;
        int listLeft = 16;
        int listRight = screenWidth - 32;

        renderer.drawRect(listLeft, listBottom, listRight - listLeft, listTop - listBottom, 0.05f, 0.05f, 0.1f, 0.5f);

        int rowHeight = ROW_HEIGHT + ROW_PADDING;
        int y = listTop - rowHeight;

        for (int i = scrollOffset; i < Math.min(scrollOffset + getVisibleRowCount(), filteredAchievements.size()); i++) {
            int achievementId = filteredAchievements.get(i);
            boolean achieved = AchievementData.isAchieved(achievementId);

            renderer.drawRect(listLeft, y, listRight - listLeft, ROW_HEIGHT, 
                    achieved ? 0.15f : 0.08f, 
                    achieved ? 0.25f : 0.08f, 
                    achieved ? 0.4f : 0.1f, 0.8f);

            // 使用中文名称和描述
            String name = AchievementData.getNameZh(achievementId);
            String desc = AchievementData.getDescription(achievementId);

            float textR = achieved ? 1.0f : 0.6f;
            float textG = achieved ? 0.9f : 0.6f;
            float textB = achieved ? 0.7f : 0.6f;

            // 标题在上，使用稍大字体
            renderer.drawText(name, listLeft + 12, y + ROW_HEIGHT - 14, textR, textG, textB, 1.0f);

            // 描述在下，使用较小字体，灰色显示
            if (!desc.isEmpty()) {
                // 描述文字右对齐，避免超出边界
                int maxDescWidth = listRight - listLeft - 80;  // 减去图标和边距
                renderer.drawText(desc, listLeft + 12, y + 6, 0.4f, 0.4f, 0.5f, 0.8f);
            }

            // 状态图标：已获得显示绿色勾选，未获得显示灰色锁
            int statusIconX = listRight - 40;
            int statusIconY = y + 12;
            if (achieved) {
                // 绿色背景 + 勾选标记
                renderer.drawRect(statusIconX, statusIconY, 26, 22, 0.15f, 0.6f, 0.25f, 0.9f);
                renderer.drawText("✓", statusIconX + 8, statusIconY + 18, 1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                // 灰色背景 + 锁图标
                renderer.drawRect(statusIconX, statusIconY, 26, 22, 0.25f, 0.25f, 0.3f, 0.7f);
                renderer.drawText("🔒", statusIconX + 4, statusIconY + 18, 0.6f, 0.6f, 0.6f, 1.0f);
            }

            y -= rowHeight;
        }

        int totalItems = filteredAchievements.size();
        int visibleItems = getVisibleRowCount();
        if (totalItems > visibleItems) {
            int scrollBarX = screenWidth - 20;
            int scrollBarY = listBottom;
            int scrollBarHeight = listTop - listBottom;
            int scrollHandleHeight = Math.max(20, (scrollBarHeight * visibleItems) / totalItems);
            int scrollHandleY = scrollBarY + (scrollBarHeight - scrollHandleHeight) * scrollOffset / (totalItems - visibleItems);

            renderer.drawRect(scrollBarX - 4, scrollBarY, 8, scrollBarHeight, 0.2f, 0.2f, 0.2f, 0.5f);
            renderer.drawRect(scrollBarX - 4, scrollHandleY, 8, scrollHandleHeight, 0.5f, 0.5f, 0.7f, 0.9f);
        }
    }
}