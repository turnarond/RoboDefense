package com.rdefense.core.platform;

/**
 * 渲染器接口 — 隔离 libGDX SpriteBatch/Canvas 等平台差异
 */
public interface GameRenderer {

    /** 获取屏幕宽度（像素） */
    int getScreenWidth();

    /** 获取屏幕高度（像素） */
    int getScreenHeight();

    /** 开始渲染批次 */
    void begin();

    /** 结束渲染批次 */
    void end();

    /**
     * 绘制精灵
     * @param regionName 精灵名称（TextureAtlas 中的 region key）
     * @param x 屏幕 X 坐标
     * @param y 屏幕 Y 坐标
     */
    void drawSprite(String regionName, float x, float y);

    /**
     * 绘制拉伸精灵
     */
    void drawSprite(String regionName, float x, float y, float width, float height);

    /**
     * 绘制旋转精灵
     */
    void drawSprite(String regionName, float x, float y, float width, float height, float rotation);

    /**
     * 绘制着色精灵（用于冰冻、灼烧等状态效果）
     */
    void drawSprite(String regionName, float x, float y, float r, float g, float b, float a);

    /**
     * 绘制指定尺寸的着色精灵
     */
    void drawSprite(String regionName, float x, float y, float width, float height, float r, float g, float b, float a);

    /**
     * 绘制精灵表的特定帧（从横向精灵表中提取）
     * @param imageName 精灵表名称（不含扩展名）
     * @param frameIndex 帧索引（从 0 开始）
     * @param totalFrames 总帧数
     * @param x 绘制 X 坐标
     * @param y 绘制 Y 坐标
     * @param width 绘制宽度
     * @param height 绘制高度
     */
    default void drawSpriteFrame(String imageName, int frameIndex, int totalFrames,
                                  float x, float y, float width, float height) {
        // 默认实现：回退到 drawSprite（由具体平台实现覆盖）
        drawSprite(imageName, x, y, width, height);
    }

    /**
     * 绘制精灵表的特定帧（带颜色 tint）
     */
    default void drawSpriteFrame(String imageName, int frameIndex, int totalFrames,
                                  float x, float y, float width, float height,
                                  float r, float g, float b, float a) {
        // 默认实现：回退到 drawSprite
        drawSprite(imageName, x, y, width, height, r, g, b, a);
    }

    /**
     * 绘制矩形（用于攻击范围圈等）
     */
    void drawRect(float x, float y, float width, float height, float r, float g, float b, float a);

    /**
     * 绘制圆形
     */
    void drawCircle(float x, float y, float radius, float r, float g, float b, float a);

    /**
     * 绘制文本
     */
    void drawText(String text, float x, float y);

    /**
     * 绘制着色文本
     */
    void drawText(String text, float x, float y, float r, float g, float b, float a);

    /**
     * 设置视口变换矩阵（用于缩放/平移）
     */
    void setTransformMatrix(float[] matrix);

    /**
     * 应用相机变换（与原版 Android Canvas matrix 行为一致）
     * 直接修改底层 SpriteBatch 的变换矩阵，使所有绘制使用世界坐标
     * @param xBase 世界坐标 X 偏移
     * @param yBase 世界坐标 Y 偏移
     * @param scale 缩放比例
     */
    void applyCameraTransform(float xBase, float yBase, float scale);

    /**
     * 获取精灵图帧的宽度
     * @param imageName 精灵图名称
     * @param totalFrames 总帧数
     * @return 帧宽度，如果无法获取返回 0
     */
    default int getSpriteFrameWidth(String imageName, int totalFrames) {
        return 0;
    }

    /**
     * 获取精灵图帧的高度
     * @param imageName 精灵图名称
     * @param totalFrames 总帧数
     * @return 帧高度，如果无法获取返回 0
     */
    default int getSpriteFrameHeight(String imageName, int totalFrames) {
        return 0;
    }

    /**
     * 屏幕坐标转世界坐标
     */
    float[] screenToWorld(float screenX, float screenY);

    /**
     * 世界坐标转屏幕坐标
     */
    float[] worldToScreen(float worldX, float worldY);

    /** 释放资源 */
    void dispose();
}
