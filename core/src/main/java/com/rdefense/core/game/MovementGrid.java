package com.rdefense.core.game;

import java.util.List;

/**
 * 移动网格 — 计算敌人移动路径
 * 对应原版 MovementGrid，使用从终点反向BFS的路径finding算法
 */
public final class MovementGrid {

    // 网格值常量（与原版一致）
    private static final byte CELL_EMPTY = 0;      // 空格子
    private static final byte CELL_BORDER = 6;     // 边界
    private static final byte CELL_OBSTACLE = 6;   // 障碍物
    private static final byte CELL_TOWER = 5;      // 塔
    private static final byte CELL_EXIT = 7;       // 出口
    private static final byte CELL_FIXED = 8;      // 固定路径

    // 方向常量
    private static final byte DIR_RIGHT = 1;  // 右
    private static final byte DIR_UP = 2;     // 上
    private static final byte DIR_LEFT = 3;   // 左
    private static final byte DIR_DOWN = 4;   // 下

    private final int pathNum;
    private final int gridW;
    private final int gridH;
    private final int startX;
    private final int startY;
    private final int endX;
    private final int endY;
    private final boolean isFixedPath;
    private final LevelData levelData;

    private byte[] grid;           // 一维数组：grid[y * gridW + x]
    private int[] pendingNodes;    // BFS 待处理节点队列
    private int nextIdx;           // 队列头索引
    private int endIdx;            // 队列尾索引

    // 缓存
    private boolean cachedTowerResult;
    private int cachedTowerX = -1;
    private int cachedTowerY = -1;

    public MovementGrid(LevelData level_data, int pathNum) {
        this.levelData = level_data;
        this.pathNum = pathNum;
        this.gridW = level_data.getGridWidth();
        this.gridH = level_data.getGridHeight();
        this.startX = level_data.getStartX(pathNum);
        this.startY = level_data.getStartY(pathNum);
        this.endX = level_data.getEndX(pathNum);
        this.endY = level_data.getEndY(pathNum);
        this.isFixedPath = level_data.isFixedPath();

        int gridSize = gridW * gridH;
        this.grid = new byte[gridSize];
        // BFS 队列需要足够大，最坏情况下每个格子入队一次
        // 使用 2 倍大小防止边界情况溢出
        this.pendingNodes = new int[gridSize * 2];

        calcPaths(null);
    }

    /**
     * 计算路径（当塔变化时重新计算）
     */
    public void calcPaths(GameTower tower_list) {
        cachedTowerX = -1;
        cachedTowerY = -1;
        initGrid(grid, tower_list);

        // 起点标记为0（未访问）
        grid[startY * gridW + startX] = CELL_EMPTY;

        // 从终点周围开始BFS
        if (endX == 0) {
            startPendingNodes(1, endY, DIR_LEFT);
        } else if (endX == gridW - 1) {
            startPendingNodes(gridW - 2, endY, DIR_RIGHT);
        } else if (endY == 0) {
            startPendingNodes(endX, 1, DIR_UP);
        } else if (endY == gridH - 1) {
            startPendingNodes(endX, gridH - 2, DIR_DOWN);
        } else {
            // 终点在中间，从四个方向开始
            startPendingNodes(endX + 1, endY, DIR_LEFT);
            startPendingNodes(endX - 1, endY, DIR_RIGHT);
            startPendingNodes(endX, endY - 1, DIR_DOWN);
            startPendingNodes(endX, endY + 1, DIR_UP);
        }

        // BFS主循环：从终点反向扩散到起点
        while (nextIdx < endIdx) {
            int pending = pendingNodes[nextIdx++];
            int x = pending >> 16;
            int y = pending & 0xFFFF;

            // 到达起点，标记起点的初始朝向
            if (x == startX && y == startY) {
                byte dir;
                if (startX == 0) {
                    dir = DIR_RIGHT;
                } else if (startX == gridW - 1) {
                    dir = DIR_LEFT;
                } else if (startY == 0) {
                    dir = DIR_DOWN;
                } else {
                    dir = DIR_UP;
                }
                grid[gridW * y + x] = dir;
            } else {
                pushPendingNodes(x, y);
            }
        }
    }

    /**
     * 获取指定格子的朝向
     */
    public int getOrientation(int x, int y) {
        if (x >= 0 && x < gridW && y >= 0 && y < gridH) {
            return grid[gridW * y + x];
        }
        return 0;
    }

    /**
     * 检查塔放置是否阻挡路径
     */
    /**
     * 检查障碍物布局是否有效（混合器模式使用）
     * 在已有的障碍物列表基础上，验证新增障碍物后路径仍可达
     *
     * @param currentList 已有的障碍物列表
     * @param newPosition 新障碍物的编码 (x<<24 | y<<16 | w<<8 | h)
     * @return true 如果路径仍然可达
     */
    public boolean checkObstacleLayout(List<Integer> currentList, int newPosition) {
        byte[] trialGrid = new byte[grid.length];
        boolean ok = initGridTrial(trialGrid);

        if (ok) {
            for (int i = 0; ok && i < currentList.size(); i++) {
                ok = addObstacleToTrial(trialGrid, currentList.get(i));
            }
        }
        if (ok) {
            ok = addObstacleToTrial(trialGrid, newPosition);
        }
        if (ok) {
            // 终点标记为可通过
            trialGrid[gridW * endY + endX] = CELL_EMPTY;
            return findPathGreedy(trialGrid, false);
        }
        return false;
    }

    /**
     * 初始化试验网格（用于 checkObstacleLayout，不需要塔列表参数）
     */
    private boolean initGridTrial(byte[] trialGrid) {
        int offset = gridW * (gridH - 1);

        if (isFixedPath) {
            for (int i = 0; i < trialGrid.length; i++) {
                trialGrid[i] = CELL_FIXED;
            }
        } else {
            for (int i = 0; i < trialGrid.length; i++) {
                trialGrid[i] = CELL_EMPTY;
            }
        }

        // 标记边界
        for (int x = 0; x < gridW; x++) {
            trialGrid[x] = CELL_BORDER;
            trialGrid[offset + x] = CELL_BORDER;
        }
        int offset2 = gridW - 1;
        for (int y = 1; y < gridH; y++) {
            trialGrid[gridW * y] = CELL_BORDER;
            trialGrid[gridW * y + offset2] = CELL_BORDER;
        }

        // 标记关卡障碍物
        int obsCount = levelData.getObstacleCount();
        for (int i = 0; i < obsCount; i++) {
            initObstacle(trialGrid, i, isFixedPath ? CELL_EMPTY : CELL_OBSTACLE);
        }

        // 标记出口
        trialGrid[endY * gridW + endX] = CELL_EXIT;
        return true;
    }

    /**
     * 在试验网格中添加一个障碍物
     */
    private boolean addObstacleToTrial(byte[] trialGrid, int position) {
        int sx = (position >> 24) & 0xFF;
        int sy = (position >> 16) & 0xFF;
        int sw = (position >> 8) & 0xFF;
        int sh = position & 0xFF;
        int x2 = sx + sw;
        int y2 = sy + sh;
        for (int y = sy; y < y2; y++) {
            for (int x = sx; x < x2; x++) {
                if (trialGrid[gridW * y + x] == CELL_EMPTY) {
                    trialGrid[gridW * y + x] = CELL_OBSTACLE;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean checkTowerPlacement(GameTower tower_list, Enemy enemy_list,
                                        int towerX, int towerY, int pathIdx, boolean isBlocking) {
        if (!isBlocking) return true;

        if (towerX != cachedTowerX || towerY != cachedTowerY || enemy_list != null) {
            cachedTowerX = towerX;
            cachedTowerY = towerY;
            cachedTowerResult = searchGreedy(tower_list, towerX, towerY, true, enemy_list != null);

            // 注意：原代码中有一个敌人位置检查，但逻辑有误
            // （在 BFS 之前检查 trialGrid[enemyPos] == 0，导致总是返回 false）
            // 已移除该检查，因为 searchGreedy 已经正确检查了路径是否可达
        }
        return cachedTowerResult;
    }

    /**
     * 初始化网格（设置障碍物、塔、边界等）
     */
    private void initGrid(byte[] activeGrid, GameTower tower_list) {
        int offset = gridW * (gridH - 1);

        if (isFixedPath) {
            for (int i = 0; i < activeGrid.length; i++) {
                activeGrid[i] = CELL_FIXED;
            }
        } else {
            for (int i = 0; i < activeGrid.length; i++) {
                activeGrid[i] = CELL_EMPTY;
            }
        }

        nextIdx = 0;
        endIdx = 0;

        // 标记上下边界（跳过起点和终点）
        for (int x = 0; x < gridW; x++) {
            if (!isStartOrEnd(x, 0) && !isStartOrEnd(x, gridH - 1)) {
                activeGrid[x] = CELL_BORDER;
                activeGrid[offset + x] = CELL_BORDER;
            }
        }

        // 标记左右边界（跳过起点和终点）
        int offset2 = gridW - 1;
        for (int y = 1; y < gridH; y++) {
            if (!isStartOrEnd(0, y)) {
                activeGrid[gridW * y] = CELL_BORDER;
            }
            if (!isStartOrEnd(offset2, y)) {
                activeGrid[gridW * y + offset2] = CELL_BORDER;
            }
        }

        // 标记阻挡型塔
        for (GameTower tower = tower_list; tower != null; tower = tower.next) {
            if (TowerData.isBlocking(tower.getType())) {
                activeGrid[tower.getGridY() * gridW + tower.getGridX()] = CELL_TOWER;
            }
        }

        // 标记关卡障碍物
        int obsCount = levelData.getObstacleCount();
        for (int i = 0; i < obsCount; i++) {
            initObstacle(activeGrid, i, isFixedPath ? 0 : CELL_OBSTACLE);
        }

        // 标记出口
        activeGrid[endY * gridW + endX] = CELL_EXIT;
    }

    /**
     * 检查位置是否为起点或终点
     */
    private boolean isStartOrEnd(int x, int y) {
        return (x == startX && y == startY) || (x == endX && y == endY);
    }

    /**
     * 初始化单个障碍物
     */
    private void initObstacle(byte[] activeGrid, int idx, int fillValue) {
        int x1 = getObstacleX(idx);
        int y1 = getObstacleY(idx);
        int x2 = x1 + getObstacleWidth(idx);
        int y2 = y1 + getObstacleHeight(idx);

        for (int y = y1; y < y2; y++) {
            for (int x = x1; x < x2; x++) {
                activeGrid[gridW * y + x] = (byte) fillValue;
            }
        }
    }

    /**
     * 从指定位置开始BFS，标记方向
     */
    private void startPendingNodes(int xpos, int ypos, byte dir) {
        grid[gridW * ypos + xpos] = dir;
        pushToQueue(xpos, ypos);
    }

    /**
     * 将相邻的空格子加入BFS队列
     * 注意：只检查格子值是否为空（0），边界和障碍物已被标记为非0值
     */
    private void pushPendingNodes(int xpos, int ypos) {
        // 上方 (y - 1)
        if (ypos > 0) {
            int idx = gridW * (ypos - 1) + xpos;
            if (grid[idx] == CELL_EMPTY) {
                grid[idx] = DIR_DOWN;
                pushToQueue(xpos, ypos - 1);
            }
        }

        // 下方 (y + 1)
        if (ypos < gridH - 1) {
            int idx = gridW * (ypos + 1) + xpos;
            if (grid[idx] == CELL_EMPTY) {
                grid[idx] = DIR_UP;
                pushToQueue(xpos, ypos + 1);
            }
        }

        // 左方 (x - 1, y)
        if (xpos > 0) {
            int idx = gridW * ypos + (xpos - 1);
            if (grid[idx] == CELL_EMPTY) {
                grid[idx] = DIR_RIGHT;
                pushToQueue(xpos - 1, ypos);
            }
        }

        // 右方 (x + 1, y)
        if (xpos < gridW - 1) {
            int idx = gridW * ypos + (xpos + 1);
            if (grid[idx] == CELL_EMPTY) {
                grid[idx] = DIR_LEFT;
                pushToQueue(xpos + 1, ypos);
            }
        }
    }

    /**
     * 加入BFS队列
     */
    private void pushToQueue(int x, int y) {
        if (endIdx < pendingNodes.length) {
            pendingNodes[endIdx++] = (x << 16) | (y & 0xFFFF);
        }
    }

    /**
     * 贪婪搜索：检查放置塔后是否仍有路径
     */
    private boolean searchGreedy(GameTower tower_list, int towerX, int towerY,
                                  boolean blockingTower, boolean iterateAll) {
        byte[] trialGrid = createTrialGrid(tower_list, towerX, towerY);

        if (blockingTower) {
            trialGrid[gridW * towerY + towerX] = CELL_TOWER;
            return findPathGreedy(trialGrid, iterateAll);
        }
        return true;
    }

    /**
     * 创建试验网格
     */
    private byte[] createTrialGrid(GameTower tower_list, int towerX, int towerY) {
        byte[] trialGrid = new byte[grid.length];
        initGrid(trialGrid, tower_list);
        if (towerX >= 0 && towerX < gridW && towerY >= 0 && towerY < gridH) {
            trialGrid[gridW * towerY + towerX] = CELL_TOWER;
        }
        return trialGrid;
    }

    /**
     * 贪婪路径查找：从起点开始找是否能到达终点
     */
    private boolean findPathGreedy(byte[] trialGrid, boolean iterateAll) {
        int x, y;

        trialGrid[gridW * endY + endX] = CELL_EMPTY;

        // 从起点旁边开始
        if (startX == 0) {
            x = 1;
            y = startY;
        } else if (startX == gridW - 1) {
            x = startX - 1;
            y = startY;
        } else if (startY == 0) {
            x = startX;
            y = 1;
        } else {
            x = startX;
            y = startY - 1;
        }

        if (trialGrid[gridW * y + x] != CELL_EMPTY) {
            return false;
        }

        // 简化的BFS：只检查连通性
        int[] queue = new int[grid.length];
        int head = 0;
        int tail = 0;
        queue[tail++] = (x << 16) | (y & 0xFFFF);
        trialGrid[gridW * y + x] = 1; // 标记已访问

        boolean success = false;

        while (head < tail) {
            int pending = queue[head++];
            int cx = pending >> 16;
            int cy = pending & 0xFFFF;

            if (cx == endX && cy == endY) {
                success = true;
                if (!iterateAll) break;
            }

            // 四个方向
            if (tryAddNode(trialGrid, queue, tail, cx - 1, cy)) tail++;
            if (tryAddNode(trialGrid, queue, tail, cx + 1, cy)) tail++;
            if (tryAddNode(trialGrid, queue, tail, cx, cy - 1)) tail++;
            if (tryAddNode(trialGrid, queue, tail, cx, cy + 1)) tail++;
        }

        return success;
    }

    private boolean tryAddNode(byte[] trialGrid, int[] queue, int tail, int x, int y) {
        if (x >= 0 && x < gridW && y >= 0 && y < gridH && trialGrid[gridW * y + x] == CELL_EMPTY) {
            queue[tail] = (x << 16) | (y & 0xFFFF);
            trialGrid[gridW * y + x] = 1;
            return true;
        }
        return false;
    }

    public int getGridW() { return gridW; }
    public int getGridH() { return gridH; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }

    // ========== 障碍物信息获取（从 LevelData 读取） ==========

    private int getObstacleX(int idx) {
        return levelData.getObstacleX(idx);
    }

    private int getObstacleY(int idx) {
        return levelData.getObstacleY(idx);
    }

    private int getObstacleWidth(int idx) {
        return levelData.getObstacleWidth(idx);
    }

    private int getObstacleHeight(int idx) {
        return levelData.getObstacleHeight(idx);
    }
}
