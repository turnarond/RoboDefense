package com.rdefense.core.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

/**
 * 相机管理器 — 处理视图平移、缩放、坐标转换
 * 使用 libgdx 的 OrthographicCamera 进行坐标管理
 * 对应原版 Display 的 matrix/scrollView/screen2Grid 逻辑
 */
public class CameraManager {

    private final OrthographicCamera camera;
    private final Vector3 tmpVec = new Vector3();

    // 世界坐标边界（像素）
    private float worldWidth;
    private float worldHeight;

    // 网格尺寸（用于坐标转换）
    private int gridPixelSize;

    /**
     * 初始化相机
     * @param worldWidth 游戏世界宽度（像素）
     * @param worldHeight 游戏世界高度（像素）
     * @param screenWidth 屏幕宽度（像素）
     * @param screenHeight 屏幕高度（像素）
     * @param gridPixelSize 网格像素大小
     */
    public CameraManager() {
        this.camera = new OrthographicCamera();
        // yDown=true 表示 Y 轴向下（左上角原点），与 Android Canvas 一致
        this.camera.setToOrtho(true);
    }

    public void init(float worldWidth, float worldHeight, int screenWidth, int screenHeight, int gridPixelSize) {
        this.worldWidth = worldWidth;
        this.worldHeight = worldHeight;
        this.gridPixelSize = gridPixelSize;
        
        // 设置相机视口
        camera.viewportWidth = screenWidth;
        camera.viewportHeight = screenHeight;
        camera.position.set(screenWidth / 2.0f, screenHeight / 2.0f, 0);
        camera.zoom = 1.0f;
        camera.update();
        
        clampPosition();
    }

    /**
     * 平移视图
     * @param xDelta X 方向移动量（像素）
     * @param yDelta Y 方向移动量（像素）
     */
    public void pan(float xDelta, float yDelta) {
        camera.position.x -= xDelta;
        camera.position.y -= yDelta;
        camera.update();
        clampPosition();
    }

    /**
     * 缩放视图
     * @param scaleFactor 缩放因子（>1 放大，<1 缩小）
     */
    public void zoom(float scaleFactor) {
        float oldZoom = camera.zoom;
        float newZoom = oldZoom * scaleFactor;
        newZoom = Math.max(0.25f, Math.min(3.0f, newZoom));
        
        // 以屏幕中心为缩放原点
        float centerX = camera.viewportWidth / 2.0f;
        float centerY = camera.viewportHeight / 2.0f;
        
        // 先将屏幕中心转换为世界坐标
        tmpVec.set(centerX, centerY, 0);
        camera.unproject(tmpVec);
        
        // 更新缩放
        camera.zoom = newZoom;
        camera.update();
        
        // 重新定位，让原来的屏幕中心保持在屏幕中心
        camera.position.set(tmpVec.x, tmpVec.y, 0);
        camera.update();
        clampPosition();
    }

    /**
     * 设置缩放比例（绝对值）
     */
    public void setScale(float scale) {
        // 以屏幕中心为缩放原点
        float centerX = camera.viewportWidth / 2.0f;
        float centerY = camera.viewportHeight / 2.0f;
        
        // 先将屏幕中心转换为世界坐标
        tmpVec.set(centerX, centerY, 0);
        camera.unproject(tmpVec);
        
        // 更新缩放
        camera.zoom = Math.max(0.25f, Math.min(3.0f, scale));
        camera.update();
        
        // 重新定位
        camera.position.set(tmpVec.x, tmpVec.y, 0);
        camera.update();
        clampPosition();
    }

    /**
     * 获取当前缩放比例
     */
    public float getScale() {
        return camera.zoom;
    }

    /**
     * 屏幕 X 转世界 X
     */
    public float screenToWorldX(float screenX) {
        tmpVec.set(screenX, 0, 0);
        camera.unproject(tmpVec);
        return tmpVec.x;
    }

    /**
     * 屏幕 Y 转世界 Y
     */
    public float screenToWorldY(float screenY) {
        tmpVec.set(0, screenY, 0);
        camera.unproject(tmpVec);
        return tmpVec.y;
    }

    /**
     * 世界 X 转屏幕 X
     */
    public float worldToScreenX(float worldX) {
        tmpVec.set(worldX, 0, 0);
        camera.project(tmpVec);
        return tmpVec.x;
    }

    /**
     * 世界 Y 转屏幕 Y
     */
    public float worldToScreenY(float worldY) {
        tmpVec.set(0, worldY, 0);
        camera.project(tmpVec);
        return tmpVec.y;
    }

    /**
     * 屏幕坐标转网格 X
     */
    public int screenToGridX(float screenX) {
        float worldX = screenToWorldX(screenX);
        int gridX = (int) (worldX / gridPixelSize);
        int gridWidth = (int) (worldWidth / gridPixelSize);
        return Math.max(0, Math.min(gridWidth - 1, gridX));
    }

    /**
     * 屏幕坐标转网格 Y
     */
    public int screenToGridY(float screenY, int gridHeight) {
        float worldY = screenToWorldY(screenY);
        int gridY = (int) (worldY / gridPixelSize);
        return Math.max(0, Math.min(gridHeight - 1, gridY));
    }

    /**
     * 查找合适的初始视角位置（对准起点）
     * @param startGridX 起点网格 X
     * @param startGridY 起点网格 Y
     */
    public void findGoodView(int startGridX, int startGridY) {
        float centerX = startGridX * gridPixelSize + gridPixelSize / 2.0f;
        float centerY = startGridY * gridPixelSize + gridPixelSize / 2.0f;
        camera.position.set(centerX, centerY, 0);
        camera.update();
        clampPosition();
    }

    /**
     * 获取 X 偏移（用于 applyCameraTransform）
     */
    public float getXBase() {
        return camera.position.x - camera.viewportWidth / 2.0f * camera.zoom;
    }

    /**
     * 获取 Y 偏移（用于 applyCameraTransform）
     */
    public float getYBase() {
        return camera.position.y - camera.viewportHeight / 2.0f * camera.zoom;
    }

    /**
     * 获取屏幕宽度
     */
    public int getScreenWidth() {
        return (int) camera.viewportWidth;
    }

    /**
     * 获取屏幕高度
     */
    public int getScreenHeight() {
        return (int) camera.viewportHeight;
    }

    /**
     * 更新屏幕尺寸（窗口 resize 时调用）
     */
    public void updateScreenSize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        clampPosition();
    }

    /**
     * 限制偏移位置，防止移出地图边界
     */
    private void clampPosition() {
        // 可见范围
        float visibleWidth = camera.viewportWidth * camera.zoom;
        float visibleHeight = camera.viewportHeight * camera.zoom;
        
        // 相机位置范围
        float minX = visibleWidth / 2.0f;
        float maxX = worldWidth - visibleWidth / 2.0f;
        float minY = visibleHeight / 2.0f;
        float maxY = worldHeight - visibleHeight / 2.0f;
        
        // 如果可见范围大于世界，居中显示
        if (visibleWidth > worldWidth) {
            camera.position.x = worldWidth / 2.0f;
        } else {
            camera.position.x = Math.max(minX, Math.min(maxX, camera.position.x));
        }
        
        if (visibleHeight > worldHeight) {
            camera.position.y = worldHeight / 2.0f;
        } else {
            camera.position.y = Math.max(minY, Math.min(maxY, camera.position.y));
        }
        
        camera.update();
    }
}
