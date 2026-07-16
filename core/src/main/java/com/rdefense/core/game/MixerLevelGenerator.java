package com.rdefense.core.game;

import com.rdefense.core.render.GameWorldRenderer.LevelOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * 混合器关卡生成器 — 根据种子码生成随机关卡布局
 * 对应原版 MixerLevelGenerator
 *
 * <p>5位种子码生成规则：
 * <ul>
 *   <li>路径数量 = (|seed| % 10) + 1（1-10条路径）</li>
 *   <li>种子码第5位为5时触发固定路径模式（is_fixed_path=true）</li>
 *   <li>障碍物数量 = 0-10个随机生成</li>
 *   <li>障碍物尺寸：1×1, 1×2, 2×1, 2×2（随机选择）</li>
 * </ul>
 */
public final class MixerLevelGenerator {

    private static final int MAX_OBSTACLES = 10;
    private static final int PATH_MAX_LENGTH = 5;

    // 墙壁方向常量（与原版 Android 一致）
    private static final int WALL_LEFT   = 256;  // 0x0100
    private static final int WALL_TOP    = 512;  // 0x0200
    private static final int WALL_RIGHT  = 768;  // 0x0300
    private static final int WALL_BOTTOM = 1024; // 0x0400
    private static final int WALL_MASK   = 0xFF00;

    private final int width;
    private final int height;
    private int numStartPaths;
    private boolean isFixedPath;
    private final Random rand;

    /**
     * 创建混合器关卡生成器
     * @param mixerSeed 混合器种子码
     * @param width 网格宽度
     * @param height 网格高度
     */
    public MixerLevelGenerator(int mixerSeed, int width, int height) {
        this.width = width;
        this.height = height;
        this.numStartPaths = (Math.abs(mixerSeed) % 10) + 1;
        // 种子码末位为5时触发固定路径模式
        if (this.numStartPaths == 5) {
            this.numStartPaths = 1;
            this.isFixedPath = true;
        } else {
            this.isFixedPath = false;
        }
        this.rand = new Random(mixerSeed / 10);
    }

    public boolean isFixedPath() {
        return isFixedPath;
    }

    // ========== 起终点生成 ==========

    /**
     * 创建起点数组（编码：wall_number << 8 | wall_offset）
     */
    public int[] createStartPaths() {
        int[] pathStart = new int[numStartPaths];
        int created = 0;
        while (created < pathStart.length) {
            created = createOneStartPath(pathStart, created);
        }
        return pathStart;
    }

    /**
     * 创建终点数组（自动生成起点对面的墙壁位置）
     */
    public int[] createEndPaths(int[] pathStart) {
        int[] pathEnd = new int[pathStart.length];
        for (int idx = 0; idx < pathEnd.length; idx++) {
            int wallNumber = pathStart[idx] & WALL_MASK;
            int wallOffset = pathStart[idx] & 0xFF;
            switch (wallNumber) {
                case WALL_LEFT:
                    pathEnd[idx] = wallOffset | WALL_RIGHT;
                    break;
                case WALL_TOP:
                    pathEnd[idx] = wallOffset | WALL_BOTTOM;
                    break;
                case WALL_RIGHT:
                    pathEnd[idx] = wallOffset | WALL_LEFT;
                    break;
                default: // WALL_BOTTOM
                    pathEnd[idx] = wallOffset | WALL_TOP;
                    break;
            }
        }
        return pathEnd;
    }

    /**
     * 创建一个起点，确保不与已有起点重叠
     */
    private int createOneStartPath(int[] pathStart, int idx) {
        int nextIdx = idx + 1;
        int wallRand = rand.nextInt(0xFFFF);
        int wallNumber = ((wallRand >> 8) & 3) << 8; // 0, 256, 512, 768
        wallNumber += 256; // → 256(WALL_LEFT), 512(WALL_TOP), 768(WALL_RIGHT), 1024(WALL_BOTTOM)
        int wallOffset2 = wallRand & 0xFF;
        boolean isHorizontal = isHorizontal(wallNumber);

        int wallOffset;
        if (isHorizontal) {
            wallOffset = (wallOffset2 % (height - 2)) + 1;
        } else {
            wallOffset = (wallOffset2 % (width - 2)) + 1;
        }

        // 检查是否与已有起点的方向+偏移重复
        int i;
        for (i = 0; i < idx; i++) {
            if (isHorizontal == isHorizontal(pathStart[i]) && wallOffset == (pathStart[i] & 0xFF)) {
                nextIdx = idx; // 重复，不增加
                break;
            }
        }

        if (i == idx) {
            pathStart[idx] = wallNumber | wallOffset;
        }
        return nextIdx;
    }

    private static boolean isHorizontal(int dir) {
        return ((dir & WALL_MASK) == WALL_RIGHT) || ((dir & WALL_MASK) == WALL_LEFT);
    }

    // ========== 障碍物生成 ==========

    /**
     * 创建障碍物数组
     * 编码：(x << 24) | (y << 16) | (w << 8) | h
     */
    public int[] createObstacles(LevelData levelData) {
        List<Integer> pathObstacles = new ArrayList<>();

        if (isFixedPath) {
            createFixedPath(levelData, pathObstacles, 0);
        } else {
            MovementGrid[] grid = new MovementGrid[levelData.getPathCount()];
            for (int gridIdx = 0; gridIdx < grid.length; gridIdx++) {
                grid[gridIdx] = new MovementGrid(levelData, gridIdx);
            }
            int maxCount = rand.nextInt(MAX_OBSTACLES + 1); // 0-10
            for (int i = 0; i < maxCount; i++) {
                tryCreateOneObstacle(grid, pathObstacles);
            }
        }

        if (pathObstacles.isEmpty()) {
            return null;
        }
        int[] result = new int[pathObstacles.size()];
        for (int idx = 0; idx < pathObstacles.size(); idx++) {
            result[idx] = pathObstacles.get(idx);
        }
        return result;
    }

    /**
     * 尝试创建一个障碍物（在所有路径上验证有效性）
     */
    private void tryCreateOneObstacle(MovementGrid[] grid, List<Integer> currentList) {
        int w = rand.nextInt(2) + 1;
        int h = rand.nextInt(2) + 1;
        int x = rand.nextInt((width - 2) - w) + 1;
        int y = rand.nextInt((height - 2) - h) + 1;
        int newCode = (x << 24) | (y << 16) | (w << 8) | h;

        // 验证所有路径上放置该障碍物后仍可通行
        int idx = 0;
        while (idx < grid.length && grid[idx].checkObstacleLayout(currentList, newCode)) {
            idx++;
        }
        if (idx == grid.length) {
            currentList.add(newCode);
        }
    }

    // ========== 固定路径生成 ==========

    /**
     * 生成固定路径（曲折的蛇形路径）
     * 路径的每个格子作为 1×1 障碍物标记（在 fixed path 模式下代表路径本身）
     */
    private void createFixedPath(LevelData levelData, List<Integer> pathObstacles, int pathNum) {
        int gridW = levelData.getGridWidth();
        int gridH = levelData.getGridHeight();
        int curX = levelData.getStartX(pathNum);
        int curY = levelData.getStartY(pathNum);
        int endX = -1;
        int endY = -1;
        int dx = 0;
        int dy = 0;

        // 从起点出发确定移动方向
        if (curX == 0) {
            dx = 1;
            endX = levelData.getEndX(pathNum) - 1;
        } else if (curX == gridW - 1) {
            dx = -1;
            endX = levelData.getEndX(pathNum) + 1;
        } else if (curY == 0) {
            dy = 1;
            endY = levelData.getEndY(pathNum) - 1;
        } else {
            dy = -1;
            endY = levelData.getEndY(pathNum) + 1;
        }

        // 先沿主方向走，遇到边界或随机转弯
        int[] grid = new int[gridW * gridH];
        Arrays.fill(grid, 1);

        // 安全限制：最多迭代 gridW * gridH * 2 次，防止死循环
        int maxIterations = gridW * gridH * 2;
        int iteration = 0;
        while (curX != endX && curY != endY && iteration < maxIterations) {
            int length = rand.nextInt(PATH_MAX_LENGTH) + 2;
            iteration++;
            for (int i = 0; i < length; i++) {
                curX += dx;
                curY += dy;
                grid[(gridW * curY) + curX] = 0;

                // 触及边界时提前终止
                if ((dx == 1 && curX == gridW - 2) ||
                    (dx == -1 && curX == 1) ||
                    (dy == 1 && curY == gridH - 2) ||
                    (dy == -1 && curY == 1)) {
                    break;
                }
            }

            // 转弯
            if (dx != 0) {
                dx = 0;
                dy = endY == -1
                    ? (curY == 1 ? 1 : curY == gridH - 2 ? -1 : (rand.nextInt(2) * 2) - 1)
                    : (endY == 1 ? -1 : 1);
            } else {
                dy = 0;
                dx = endX == -1
                    ? (curX == 1 ? 1 : curX == gridW - 2 ? -1 : (rand.nextInt(2) * 2) - 1)
                    : (endX == 1 ? -1 : 1);
            }
        }

        // 最后直接连接到终点
        if (curX == endX) {
            int finalEndY = levelData.getEndY(pathNum);
            int dy2 = curY < finalEndY ? 1 : -1;
            while (curY != finalEndY) {
                curY += dy2;
                grid[(gridW * curY) + curX] = 0;
            }
        } else {
            int finalEndX = levelData.getEndX(pathNum);
            int dx2 = curX < finalEndX ? 1 : -1;
            while (curX != finalEndX) {
                curX += dx2;
                grid[(gridW * curY) + curX] = 0;
            }
        }

        // 将路径标记转换为 1×1 障碍物
        for (int cx = 1; cx < gridW - 1; cx++) {
            for (int cy = 1; cy < gridH - 1; cy++) {
                if (grid[(gridW * cy) + cx] == 0) {
                    pathObstacles.add((cx << 24) | (cy << 16) | (1 << 8) | 1);
                }
            }
        }
    }

    // ========== 装饰层生成 ==========

    /**
     * 创建装饰层（入口、出口、障碍物贴图）
     */
    public LevelOverlay[] createUnderlays(LevelData levelData) {
        int overlayCount = (levelData.getPathCount() * 2) + levelData.getObstacleCount();
        LevelOverlay[] overlays = new LevelOverlay[overlayCount];
        int overlayIdx = 0;

        // 入口和出口标记
        for (int i = 0; i < levelData.getPathCount(); i++) {
            overlays[overlayIdx++] = makeEntrancePathUnderlay(
                    levelData.getStartX(i), levelData.getStartY(i));
            overlays[overlayIdx++] = makeExitPathUnderlay(
                    levelData.getEndX(i), levelData.getEndY(i));
        }

        // 障碍物贴图
        for (int j = 0; j < levelData.getObstacleCount(); j++) {
            if (isFixedPath) {
                overlays[overlayIdx++] = makeFixedPathUnderlay(
                        levelData.getObstacleX(j), levelData.getObstacleY(j));
            } else {
                overlays[overlayIdx++] = makeObstacleUnderlay(
                        levelData.getObstacleX(j), levelData.getObstacleY(j),
                        levelData.getObstacleWidth(j), levelData.getObstacleHeight(j));
            }
        }

        return overlays;
    }

    // ========== 装饰层工厂方法 ==========

    private LevelOverlay makeEntrancePathUnderlay(int x, int y) {
        String imageName;
        if (x == 0) {
            imageName = "mixer_entrance_w";
        } else if (y == 0) {
            imageName = "mixer_entrance_n";
        } else if (x == width - 1) {
            imageName = "mixer_entrance_e";
        } else {
            imageName = "mixer_entrance_s";
        }
        return newOverlay(imageName, x, y);
    }

    private LevelOverlay makeExitPathUnderlay(int x, int y) {
        String imageName;
        if (x == 0) {
            imageName = "mixer_exit_w";
        } else if (y == 0) {
            imageName = "mixer_exit_n";
        } else if (x == width - 1) {
            imageName = "mixer_exit_e";
        } else {
            imageName = "mixer_exit_s";
        }
        return newOverlay(imageName, x, y);
    }

    private LevelOverlay makeObstacleUnderlay(int x, int y, int w, int h) {
        String imageName;
        int key = (w << 8) | h;
        switch (key) {
            case 0x0101: // 1×1
                imageName = "mixer_1x1";
                break;
            case 0x0102: // 1×2
                imageName = "mixer_1x2";
                break;
            case 0x0201: // 2×1
                imageName = "mixer_2x1";
                break;
            default:     // 2×2
                imageName = "mixer_2x2";
                break;
        }
        return newOverlay(imageName, x, y);
    }

    private LevelOverlay makeFixedPathUnderlay(int x, int y) {
        return newOverlay("mixer_path", x, y);
    }

    private LevelOverlay newOverlay(String imageName, int x, int y) {
        LevelOverlay overlay = new LevelOverlay();
        overlay.imageName = imageName;
        overlay.x = 32 * x; // GRID_PIXEL_SIZE = 32
        overlay.y = 32 * y;
        return overlay;
    }
}
