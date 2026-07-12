package com.rdefense.core.game;

/**
 * 碰撞网格 — 用于塔检测范围内的敌人
 * 对应原版 CollisionGrid
 * 
 * 注意：原版使用 CGRID_PIXEL_SIZE = GRID_PIXEL_SIZE * 3 = 96 像素作为碰撞网格的大小，
 * 而不是使用游戏逻辑的 32 像素网格。这是为了减少网格单元格数量，提高查找效率。
 */
public final class CollisionGrid {

    private static final int CGRID_PIXEL_SIZE = 96; // GRID_PIXEL_SIZE * 3
    
    private final int gridW;
    private final int gridH;
    private Enemy[] cells; // 一维数组，按行优先存储

    public CollisionGrid(int levelGridW, int levelGridH) {
        // 碰撞网格比游戏逻辑网格小（96 像素一格 vs 32 像素一格）
        int pixelW = levelGridW * 32;
        int pixelH = levelGridH * 32;
        this.gridW = (pixelW + CGRID_PIXEL_SIZE - 1) / CGRID_PIXEL_SIZE;
        this.gridH = (pixelH + CGRID_PIXEL_SIZE - 1) / CGRID_PIXEL_SIZE;
        this.cells = new Enemy[gridW * gridH];
    }

    /**
     * 重置网格（每帧开始时调用）
     */
    public void reset() {
        java.util.Arrays.fill(cells, null);
    }

    /**
     * 添加敌人到网格
     * 使用敌人的像素坐标除以 CGRID_PIXEL_SIZE 来确定所在的碰撞网格单元
     */
    public void add(Enemy enemy) {
        int cx = enemy.calcPixelX() / CGRID_PIXEL_SIZE;
        int cy = enemy.calcPixelY() / CGRID_PIXEL_SIZE;
        if (cx >= 0 && cx < gridW && cy >= 0 && cy < gridH) {
            int idx = cy * gridW + cx;
            enemy.grid_next = cells[idx];
            cells[idx] = enemy;
        }
    }

    /**
     * 获取指定格子的敌人链表
     */
    public Enemy getList(int x, int y) {
        if (x >= 0 && x < gridW && y >= 0 && y < gridH) {
            return cells[y * gridW + x];
        }
        return null;
    }

    /**
     * 为塔设置碰撞范围
     * 使用像素坐标计算，然后转换为碰撞网格坐标
     * 
     * 注意：与原版一致，使用格子左上角作为基准
     * 因为 TowerData.attackRadius 也是基于格子坐标计算的
     */
    public void setupGrid(GameTower tower) {
        int towerX = tower.getGridX() * 32; // 格子左上角的像素 X
        int towerY = tower.getGridY() * 32; // 格子左上角的像素 Y
        int radius = TowerData.attackRadius(tower.getType());
        
        int left = (towerX - radius) / CGRID_PIXEL_SIZE;
        if (left < 0) left = 0;
        
        int right = (towerX + radius) / CGRID_PIXEL_SIZE;
        if (right >= gridW) right = gridW - 1;
        
        int top = (towerY - radius) / CGRID_PIXEL_SIZE;
        if (top < 0) top = 0;
        
        int bottom = (towerY + radius) / CGRID_PIXEL_SIZE;
        if (bottom >= gridH) bottom = gridH - 1;

        tower.setCollisionRange(left, top, right, bottom);
    }

    public int getGridW() { return gridW; }
    public int getGridH() { return gridH; }
}
