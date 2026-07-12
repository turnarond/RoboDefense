package com.rdefense.core.platform.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.rdefense.core.platform.GameRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * libGDX 平台渲染器实现
 * 支持两种模式：
 * 1. TextureAtlas 模式：通过 atlas 查找 region
 * 2. 散装纹理模式：按文件名直接从 assets/images/ 加载 PNG
 */
public class LibGdxRenderer implements GameRenderer {

    private final SpriteBatch batch;
    private final ShapeRenderer shapeRenderer;
    private final BitmapFont font;
    private final TextureAtlas atlas;
    private final OrthographicCamera camera;
    private final Vector3 tmpVec = new Vector3();

    // 散装纹理缓存（当 atlas 为空或找不到 region 时使用）
    private final Map<String, Texture> textureCache = new HashMap<String, Texture>();

    // 精灵表帧缓存：key = "name_N" (N=总帧数), value = TextureRegion[] 每帧的区域
    private final Map<String, TextureRegion[]> spriteSheetCache = new HashMap<String, TextureRegion[]>();
    private boolean useLinearFiltering = true;
    private boolean use16BitBackground = false;
    private boolean useHQGraphics = false;
    private boolean oomTriggered = false;
    private HdFallbackListener hdFallbackListener;

    public static interface HdFallbackListener {
        void onHdFallback(String reason);
    }

    /**
     * 使用 TextureAtlas 创建渲染器
     */
    public LibGdxRenderer(TextureAtlas atlas, BitmapFont font) {
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = font;
        this.atlas = atlas;
        this.camera = new OrthographicCamera();
        updateCamera();
    }

    /**
     * 不使用 TextureAtlas 创建渲染器（散装纹理模式）
     */
    public LibGdxRenderer(BitmapFont font) {
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
        this.font = font;
        this.atlas = null;
        this.camera = new OrthographicCamera();
        updateCamera();
    }

    private void updateCamera() {
        // yDown=true 表示 Y 轴向下（左上角原点），与 Android Canvas 一致
        camera.setToOrtho(true, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();
    }

    @Override
    public int getScreenWidth() {
        return Gdx.graphics.getWidth();
    }

    @Override
    public int getScreenHeight() {
        return Gdx.graphics.getHeight();
    }

    @Override
    public void begin() {
        batch.begin();
    }

    @Override
    public void end() {
        batch.end();
    }

    @Override
    public void drawSprite(String regionName, float x, float y) {
        TextureRegion region = findRegion(regionName);
        if (region != null) {
            batch.draw(region, x, y);
        }
    }

    @Override
    public void drawSprite(String regionName, float x, float y, float width, float height) {
        TextureRegion region = findRegion(regionName);
        if (region != null) {
            batch.draw(region, x, y, width, height);
        }
    }

    @Override
    public void drawSprite(String regionName, float x, float y, float width, float height, float rotation) {
        TextureRegion region = findRegion(regionName);
        if (region != null) {
            batch.draw(region, x, y, width / 2, height / 2, width, height, 1, 1, rotation);
        }
    }

    @Override
    public void drawSprite(String regionName, float x, float y, float r, float g, float b, float a) {
        TextureRegion region = findRegion(regionName);
        if (region != null) {
            float prevR = batch.getColor().r;
            float prevG = batch.getColor().g;
            float prevB = batch.getColor().b;
            float prevA = batch.getColor().a;
            batch.setColor(r, g, b, a);
            batch.draw(region, x, y);
            batch.setColor(prevR, prevG, prevB, prevA);
        }
    }

    @Override
    public void drawSprite(String regionName, float x, float y, float width, float height, float r, float g, float b, float a) {
        TextureRegion region = findRegion(regionName);
        if (region != null) {
            float prevR = batch.getColor().r;
            float prevG = batch.getColor().g;
            float prevB = batch.getColor().b;
            float prevA = batch.getColor().a;
            batch.setColor(r, g, b, a);
            batch.draw(region, x, y, width, height);
            batch.setColor(prevR, prevG, prevB, prevA);
        }
    }

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
    public void drawSpriteFrame(String imageName, int frameIndex, int totalFrames,
                                float x, float y, float width, float height) {
        TextureRegion[] frames = findSpriteSheetFrames(imageName, totalFrames);
        if (frames == null || frameIndex < 0 || frameIndex >= frames.length) {
            return;
        }
        batch.draw(frames[frameIndex], x, y, width, height);
    }

    /**
     * 绘制精灵表的特定帧（带颜色 tint）
     */
    public void drawSpriteFrame(String imageName, int frameIndex, int totalFrames,
                                float x, float y, float width, float height,
                                float r, float g, float b, float a) {
        TextureRegion[] frames = findSpriteSheetFrames(imageName, totalFrames);
        if (frames == null || frameIndex < 0 || frameIndex >= frames.length) {
            return;
        }
        float prevR = batch.getColor().r;
        float prevG = batch.getColor().g;
        float prevB = batch.getColor().b;
        float prevA = batch.getColor().a;
        batch.setColor(r, g, b, a);
        batch.draw(frames[frameIndex], x, y, width, height);
        batch.setColor(prevR, prevG, prevB, prevA);
    }

    /**
     * 获取精灵图帧的宽度
     * @param imageName 精灵图名称
     * @param totalFrames 总帧数
     * @return 帧宽度，如果无法获取返回 0
     */
    public int getSpriteFrameWidth(String imageName, int totalFrames) {
        TextureRegion[] frames = findSpriteSheetFrames(imageName, totalFrames);
        if (frames != null && frames.length > 0) {
            return frames[0].getRegionWidth();
        }
        return 0;
    }

    /**
     * 获取精灵图帧的高度
     * @param imageName 精灵图名称
     * @param totalFrames 总帧数
     * @return 帧高度，如果无法获取返回 0
     */
    public int getSpriteFrameHeight(String imageName, int totalFrames) {
        TextureRegion[] frames = findSpriteSheetFrames(imageName, totalFrames);
        if (frames != null && frames.length > 0) {
            return frames[0].getRegionHeight();
        }
        return 0;
    }

    @Override
    public void drawRect(float x, float y, float width, float height, float r, float g, float b, float a) {
        batch.end();
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(r, g, b, a);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.end();
        batch.begin();
    }

    @Override
    public void drawCircle(float x, float y, float radius, float r, float g, float b, float a) {
        batch.end();
        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(r, g, b, a);
        int segments = Math.max(16, (int) (6 * (float) Math.cbrt(radius)));
        shapeRenderer.circle(x, y, radius, segments);
        shapeRenderer.end();
        batch.begin();
    }

    @Override
    public void drawText(String text, float x, float y) {
        font.draw(batch, text, x, y);
    }

    @Override
    public void drawText(String text, float x, float y, float r, float g, float b, float a) {
        com.badlogic.gdx.graphics.Color color = font.getColor();
        font.setColor(r, g, b, a);
        font.draw(batch, text, x, y);
        font.setColor(color);
    }

    @Override
    public void setTransformMatrix(float[] matrix) {
        if (matrix != null && matrix.length >= 16) {
            Matrix4 m = new Matrix4(matrix);
            batch.setTransformMatrix(m);
        }
    }

    /**
     * 应用相机变换到 SpriteBatch（与原版 Android Canvas matrix 行为一致）
     * 
     * 原版 Display.scrollView 中的矩阵：
     *   matrix_array[0] = scalef, matrix_array[2] = -xbase
     *   matrix_array[4] = scalef, matrix_array[5] = -ybase
     * 
     * 这是一个 3x3 仿射变换，需要扩展为 4x4 齐次矩阵给 libGDX：
     *   | scale  0     0  -xbase |
     *   | 0      scale  0  -ybase |
     *   | 0      0      1   0     |
     *   | 0      0      0   1     |
     *
     * @param xBase 世界坐标 X 偏移
     * @param yBase 世界坐标 Y 偏移
     * @param scale 缩放比例
     */
    public void applyCameraTransform(float xBase, float yBase, float scale) {
        // xBase = camera.position.x - (viewportWidth / 2) * zoom
        // yBase = camera.position.y - (viewportHeight / 2) * zoom
        // 我们需要构建一个变换矩阵，让世界坐标正确显示在屏幕上
        
        Matrix4 m = new Matrix4();
        // 变换：先缩放，再平移
        // screen_x = (world_x - xBase) * (1/scale)
        // screen_y = (world_y - yBase) * (1/scale)
        // 矩阵表示：
        //   [ 1/scale   0        0    -xBase/scale ]
        //   [  0     1/scale    0    -yBase/scale ]
        //   [  0        0        1         0       ]
        //   [  0        0        0         1       ]
        
        float invScale = 1.0f / scale;
        m.val[Matrix4.M00] = invScale;       // X 缩放
        m.val[Matrix4.M11] = invScale;       // Y 缩放
        m.val[Matrix4.M22] = 1.0f;           // Z 缩放
        m.val[Matrix4.M03] = -xBase * invScale;  // X 平移
        m.val[Matrix4.M13] = -yBase * invScale;  // Y 平移
        m.val[Matrix4.M33] = 1.0f;           // 齐次坐标
        batch.setTransformMatrix(m);
    }

    @Override
    public float[] screenToWorld(float screenX, float screenY) {
        tmpVec.set(screenX, screenY, 0);
        camera.unproject(tmpVec);
        return new float[]{tmpVec.x, tmpVec.y};
    }

    @Override
    public float[] worldToScreen(float worldX, float worldY) {
        tmpVec.set(worldX, worldY, 0);
        camera.project(tmpVec);
        return new float[]{tmpVec.x, tmpVec.y};
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        // 释放散装纹理缓存
        for (Texture tex : textureCache.values()) {
            tex.dispose();
        }
        textureCache.clear();
        // atlas 和 font 由外部管理生命周期
    }

    /**
     * 查找纹理区域：优先从 atlas 查找，找不到则从散装文件加载
     */
    private TextureRegion findRegion(String regionName) {
        // 1. 尝试从 atlas 查找
        if (atlas != null) {
            TextureRegion region = atlas.findRegion(regionName);
            if (region != null) {
                return region;
            }
            // atlas 中可能存有 HQ 变体
            if (useHQGraphics) {
                String[] hqNames = {regionName + "_2x", regionName + "_large", regionName + "_hd"};
                for (String hn : hqNames) {
                    region = atlas.findRegion(hn);
                    if (region != null) return region;
                }
            }
        }

        // 2. 尝试从散装纹理缓存查找
        Texture tex = textureCache.get(regionName);
        if (tex != null) {
            return new TextureRegion(tex);
        }

        // 3. 尝试加载散装 PNG 文件（若启用 HQ，则优先尝试 _2x/_large 变体）
        String[] candidateNames;
        if (useHQGraphics) {
            candidateNames = new String[]{regionName + "_2x", regionName + "_large", regionName + "_hd", regionName};
        } else {
            candidateNames = new String[]{regionName};
        }

        String[] extensions = new String[]{".png", ".jpg", ""};
        java.util.List<String> pathsList = new java.util.ArrayList<String>();
        for (String n : candidateNames) {
            for (String ext : extensions) {
                pathsList.add("images/" + n + ext);
            }
            for (String ext : extensions) {
                pathsList.add(n + ext);
            }
        }
        String[] paths = pathsList.toArray(new String[0]);

        Texture.TextureFilter filter = useLinearFiltering
                ? Texture.TextureFilter.Linear
                : Texture.TextureFilter.Nearest;
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                try {
                    if (use16BitBackground && isBackgroundName(regionName)) {
                        com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal(path);
                        com.badlogic.gdx.graphics.Pixmap src = new com.badlogic.gdx.graphics.Pixmap(fh);
                        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(src.getWidth(), src.getHeight(), com.badlogic.gdx.graphics.Pixmap.Format.RGB565);
                        pm.drawPixmap(src, 0, 0, src.getWidth(), src.getHeight(), 0, 0, src.getWidth(), src.getHeight());
                        src.dispose();
                        tex = new Texture(pm, false);
                        pm.dispose();
                    } else {
                        tex = new Texture(Gdx.files.internal(path));
                    }
                    tex.setFilter(filter, filter);
                    textureCache.put(regionName, tex);
                    return new TextureRegion(tex);
                } catch (OutOfMemoryError oom) {
                    oomTriggered = true;
                    // 清理并回退到 SD 变体（基本名）
                    textureCache.values().forEach(t -> { try { t.dispose(); } catch (Exception ignored) {} });
                    textureCache.clear();
                    if (!regionName.equals(regionName.replaceAll("_2x|_large|_hd", ""))) {
                        // 如果当前是 HQ 名称则尝试基本名
                        String base = regionName.replaceAll("_2x|_large|_hd", "");
                        if (!base.equals(regionName)) {
                            if (hdFallbackListener != null) {
                                try { hdFallbackListener.onHdFallback("OOM loading " + regionName); } catch (Throwable ignored) {}
                            }
                            return findRegion(base);
                        }
                    }
                    // 无法回退，继续尝试下一路径
                } catch (Exception e) {
                    // 加载失败，尝试下一个路径
                }
            }
        }

        // 没找到
        return null;
    }

    /**
     * 获取精灵表的所有帧（横向精灵表自动分割）
     * @param imageName 精灵表名称（不含扩展名）
     * @param totalFrames 总帧数
     * @return TextureRegion 数组，每帧一个区域；加载失败返回 null
     */
    private TextureRegion[] findSpriteSheetFrames(String imageName, int totalFrames) {
        String cacheKey = imageName + "_" + totalFrames;

        // 检查缓存
        TextureRegion[] frames = spriteSheetCache.get(cacheKey);
        if (frames != null) {
            return frames;
        }

        // 加载精灵表纹理（优先 HQ 变体）
        Texture tex = null;
        String[] candidateNames;
        if (useHQGraphics) {
            candidateNames = new String[]{imageName + "_2x", imageName + "_large", imageName + "_hd", imageName};
        } else {
            candidateNames = new String[]{imageName};
        }
        String[] extensions = new String[]{".png", ".jpg", ""};
        java.util.List<String> pathsList = new java.util.ArrayList<String>();
        for (String n : candidateNames) {
            for (String ext : extensions) {
                pathsList.add("images/" + n + ext);
            }
            for (String ext : extensions) {
                pathsList.add(n + ext);
            }
        }
        String[] paths = pathsList.toArray(new String[0]);

        Texture.TextureFilter filter = useLinearFiltering
                ? Texture.TextureFilter.Linear
                : Texture.TextureFilter.Nearest;
        for (String path : paths) {
            if (Gdx.files.internal(path).exists()) {
                try {
                    if (use16BitBackground && isBackgroundName(imageName)) {
                        com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal(path);
                        com.badlogic.gdx.graphics.Pixmap src = new com.badlogic.gdx.graphics.Pixmap(fh);
                        com.badlogic.gdx.graphics.Pixmap pm = new com.badlogic.gdx.graphics.Pixmap(src.getWidth(), src.getHeight(), com.badlogic.gdx.graphics.Pixmap.Format.RGB565);
                        pm.drawPixmap(src, 0, 0, src.getWidth(), src.getHeight(), 0, 0, src.getWidth(), src.getHeight());
                        src.dispose();
                        tex = new Texture(pm, false);
                        pm.dispose();
                    } else {
                        tex = new Texture(Gdx.files.internal(path));
                    }
                    tex.setFilter(filter, filter);
                    textureCache.put(imageName, tex);
                    break;
                } catch (OutOfMemoryError oom) {
                    oomTriggered = true;
                    textureCache.values().forEach(t -> { try { t.dispose(); } catch (Exception ignored) {} });
                    textureCache.clear();
                    String base = imageName.replaceAll("_2x|_large|_hd", "");
                    if (!base.equals(imageName)) {
                        // 尝试基本名并通知回退
                        if (hdFallbackListener != null) {
                            try { hdFallbackListener.onHdFallback("OOM loading " + imageName); } catch (Throwable ignored) {}
                        }
                        try {
                            Texture fallback = new Texture(Gdx.files.internal("images/" + base + ".png"));
                            fallback.setFilter(filter, filter);
                            textureCache.put(base, fallback);
                            tex = fallback;
                            break;
                        } catch (Throwable t) {
                            tex = null;
                        }
                    }
                    tex = null;
                } catch (Exception e) {
                    tex = null;
                }
            }
        }

        if (tex == null) {
            return null;
        }

        // 分割精灵表为帧
        int texWidth = tex.getWidth();
        int texHeight = tex.getHeight();
        int frameWidth = texWidth / totalFrames;
        int frameHeight = texHeight;

        frames = new TextureRegion[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            int frameX = i * frameWidth;
            // 最后一帧可能因为舍入略有不同，使用剩余宽度
            int actualWidth = (i == totalFrames - 1) ? (texWidth - frameX) : frameWidth;
            frames[i] = new TextureRegion(tex, frameX, 0, actualWidth, frameHeight);
        }

        spriteSheetCache.put(cacheKey, frames);
        return frames;
    }

    public void setUseLinearFiltering(boolean useLinearFiltering) {
        this.useLinearFiltering = useLinearFiltering;
    }

    public void setUse16BitBackground(boolean use16BitBackground) {
        this.use16BitBackground = use16BitBackground;
    }

    public void setUseHQGraphics(boolean useHQGraphics) {
        this.useHQGraphics = useHQGraphics;
    }

    public void setHdFallbackListener(HdFallbackListener listener) {
        this.hdFallbackListener = listener;
    }

    /**
     * 调试用：强制触发 HD 回退通知（不执行实际纹理回退逻辑）
     */
    public void forceHdFallback(String reason) {
        if (hdFallbackListener != null) {
            try {
                hdFallbackListener.onHdFallback(reason != null ? reason : "forced debug fallback");
            } catch (Throwable ignored) { }
        }
    }

    private boolean isBackgroundName(String name) {
        if (name == null) return false;
        return name.contains("level") || name.contains("background");
    }

    public OrthographicCamera getCamera() { return camera; }
    public SpriteBatch getBatch() { return batch; }
    public TextureAtlas getAtlas() { return atlas; }
    public BitmapFont getFont() { return font; }
}
