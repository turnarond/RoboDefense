package com.rdefense.core.render;

import com.rdefense.core.game.RewardData;
import com.rdefense.core.platform.GameRenderer;

public class RewardRenderer {

    // 列表式布局 - 每行显示一个奖励
    private static final int ROW_HEIGHT = 48;
    private static final int ROW_PADDING = 4;
    private static final int TEXT_LEFT_MARGIN = 16;
    private static final int BUTTON_WIDTH = 60;
    private static final int BUTTON_HEIGHT = 30;

    private int selectedIndex = -1;

    public void drawRewardGrid(GameRenderer renderer, int scrollOffset, long rewardPoints) {
        int screenWidth = renderer.getScreenWidth();
        int screenHeight = renderer.getScreenHeight();

        int startY = screenHeight - 100 + scrollOffset * 2;

        for (int i = 0; i < RewardData.REWARD_TYPE_COUNT; i++) {
            int y = startY - i * (ROW_HEIGHT + ROW_PADDING);

            // 只渲染可见区域内的行
            if (y > screenHeight + 50 || y < -ROW_HEIGHT) continue;

            CardState state = getCardState(i, rewardPoints);
            drawRow(renderer, 0, y, screenWidth, i, state, rewardPoints);
        }
    }

    private CardState getCardState(int type, long rewardPoints) {
        if (RewardData.isBlocked(type) != null) return CardState.BLOCKED;
        if (RewardData.isMaxLevel(type)) return CardState.MAXED;
        if (RewardData.isUnlockable(type) && RewardData.getLevel(type) > 0) return CardState.UNLOCKED;
        if (RewardData.nextLevelRewardCost(type) > rewardPoints) return CardState.CANNOT_AFFORD;
        return CardState.AVAILABLE;
    }

    private void drawRow(GameRenderer renderer, int x, int y, int width, int type, CardState state, long rewardPoints) {
        float[] bgColor = getBackgroundColor(state);
        renderer.drawRect(x, y, width, ROW_HEIGHT, bgColor[0], bgColor[1], bgColor[2], 0.95f);

        int textY = y + ROW_HEIGHT - 12;

        // 名称
        String name = RewardData.getName(type);
        renderer.drawText(name, x + TEXT_LEFT_MARGIN, textY, 0.85f, 0.9f, 0.9f, 1.0f);

        // 等级信息
        String levelInfo = getLevelInfo(type);
        renderer.drawText(levelInfo, x + 200, textY, 0.65f, 0.7f, 0.7f, 1.0f);

        // 描述（简短）
        String desc = RewardData.rewardString(type);
        if (desc.length() > 25) {
            desc = desc.substring(0, 24) + "..";
        }
        renderer.drawText(desc, x + 320, textY, 0.6f, 0.8f, 0.6f, 1.0f);

        // 锁定原因
        String blockedReason = RewardData.isBlocked(type);
        if (blockedReason != null) {
            String shortReason = blockedReason.length() > 15 ? blockedReason.substring(0, 14) + ".." : blockedReason;
            renderer.drawText("[锁定: " + shortReason + "]", x + width - 200, textY, 0.6f, 0.5f, 0.5f, 1.0f);
        } else if (state == CardState.AVAILABLE) {
            // 成本和升级按钮
            long cost = RewardData.nextLevelRewardCost(type);
            renderer.drawText(formatCost(cost), x + width - 150, textY, 0.7f, 0.8f, 0.9f, 1.0f);

            int buttonX = x + width - BUTTON_WIDTH - 16;
            int buttonY = y + (ROW_HEIGHT - BUTTON_HEIGHT) / 2;
            renderer.drawRect(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 0.15f, 0.5f, 0.15f, 0.9f);
            renderer.drawText("升级", buttonX + 14, buttonY + BUTTON_HEIGHT - 10, 0.7f, 1.0f, 1.0f, 1.0f);
        } else if (state == CardState.MAXED) {
            renderer.drawText("已满级", x + width - 150, textY, 0.6f, 0.6f, 0.6f, 1.0f);
        } else if (state == CardState.UNLOCKED) {
            renderer.drawText("已解锁", x + width - 150, textY, 0.5f, 0.7f, 0.5f, 1.0f);
        } else if (state == CardState.CANNOT_AFFORD) {
            long cost = RewardData.nextLevelRewardCost(type);
            renderer.drawText("需要 " + formatCost(cost), x + width - 180, textY, 0.6f, 0.5f, 0.5f, 1.0f);
        }

        // 行分隔线
        renderer.drawRect(x, y, width, 1, 0.1f, 0.15f, 0.25f, 0.8f);
    }

    private String getLevelInfo(int type) {
        if (RewardData.isUnlockable(type)) {
            return RewardData.getLevel(type) > 0 ? "[已解锁]" : "[未解锁]";
        }
        int level = RewardData.getLevel(type);
        int maxLevel = RewardData.getMaxLevel(type);
        return String.format("[Lv.%d/%d]", level + 1, maxLevel);
    }

    private float[] getBackgroundColor(CardState state) {
        switch (state) {
            case AVAILABLE: return new float[]{0.08f, 0.15f, 0.28f};
            case MAXED: return new float[]{0.12f, 0.12f, 0.12f};
            case CANNOT_AFFORD: return new float[]{0.12f, 0.1f, 0.1f};
            case BLOCKED: return new float[]{0.1f, 0.08f, 0.08f};
            case UNLOCKED: return new float[]{0.08f, 0.2f, 0.15f};
            default: return new float[]{0.08f, 0.12f, 0.22f};
        }
    }

    private String formatCost(long cost) {
        if (cost >= 1000000) {
            return String.format("%.1fM", cost / 1000000.0f);
        } else if (cost >= 1000) {
            return String.format("%dK", cost / 1000);
        }
        return String.valueOf(cost);
    }

    public int getRowAt(int x, int y, int scrollOffset, int screenHeight) {
        int startY = screenHeight - 100 + scrollOffset * 2;
        int row = (startY - y) / (ROW_HEIGHT + ROW_PADDING);
        if (row < 0 || row >= RewardData.REWARD_TYPE_COUNT) return -1;
        return row;
    }

    private enum CardState {
        AVAILABLE,
        MAXED,
        CANNOT_AFFORD,
        UNLOCKED,
        BLOCKED
    }
}
