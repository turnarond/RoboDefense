package com.rdefense.core.render;

import com.rdefense.core.game.BulletData;
import com.rdefense.core.game.EnemyData;
import com.rdefense.core.game.LevelData;
import com.rdefense.core.game.TowerData;
import com.rdefense.core.platform.GameRenderer;

/**
 * 游戏世界渲染器 — 处理 Y-sorted 渲染、粒子、爆炸、子弹等
 * 对应原版 Display 的 drawEnemiesAndTowers、drawBullets、handleParticleEvents 等逻辑
 */
public class GameWorldRenderer {

    private final GameRenderer renderer;
    private final CameraManager camera;

    // 颜色常量（RGB 0-1 范围）
    private static final float COLOR_ICE_R = 0.3f;
    private static final float COLOR_ICE_G = 0.5f;
    private static final float COLOR_ICE_B = 1.0f;
    private static final float COLOR_ICE_A = 0.4f;

    private static final float COLOR_FIRE_R = 1.0f;
    private static final float COLOR_FIRE_G = 0.3f;
    private static final float COLOR_FIRE_B = 0.0f;
    private static final float COLOR_FIRE_A = 0.4f;

    // 粒子池
    private final Particle[] particles = new Particle[64];
    private int particleCount = 0;

    // 爆炸效果池
    private final Explosion[] explosions = new Explosion[16];
    private int explosionCount = 0;

    public GameWorldRenderer(GameRenderer renderer, CameraManager camera) {
        this.renderer = renderer;
        this.camera = camera;
    }

    /**
     * 渲染游戏世界（塔、敌人、子弹、粒子、爆炸）
     * @param world 游戏世界数据提供器
     * @param stateIndex 当前状态索引（帧）
     */
    public void render(GameWorld world, int stateIndex) {
        renderer.begin();

        // 1. 绘制 underlays（底层背景覆盖物）
        renderUnderlays(world);

        // 2. Y-sorted 渲染塔和敌人
        renderSortedObjects(world, stateIndex);

        // 3. 绘制 overlays（顶层背景覆盖物）
        renderOverlays(world);

        // 4. 绘制粒子效果
        renderParticles(stateIndex);

        // 5. 绘制爆炸效果
        renderExplosions(stateIndex);

        // 6. 绘制子弹
        renderBullets(world, stateIndex);

        // 7. 绘制金钱飘字
        renderMoneyPopups(world, stateIndex);

        renderer.end();
    }

    /**
     * 绘制 underlays
     */
    private void renderUnderlays(GameWorld world) {
        LevelOverlay[] underlays = world.getUnderlays();
        if (underlays != null) {
            for (LevelOverlay overlay : underlays) {
                float screenX = camera.worldToScreenX(overlay.x);
                float screenY = camera.worldToScreenY(overlay.y);
                renderer.drawSprite(overlay.imageName, screenX, screenY);
            }
        }
    }

    /**
     * Y-sorted 渲染塔和敌人
     */
    private void renderSortedObjects(GameWorld world, int stateIndex) {
        GridObject obj = world.getSortedList();
        while (obj != null) {
            if (obj.getClassType() == 1) {
                // 塔
                renderTower((GameTowerObj) obj, stateIndex);
            } else {
                // 敌人
                renderEnemy((EnemyObj) obj, stateIndex);
            }
            obj = obj.next_y;
        }
    }

    /**
     * 渲染单个塔
     * 原版渲染流程：
     * 1. 背景层：绘制 TowerData.image(type) 即 images[0] 底座
     * 2. 前景层：绘制 TowerData.getDirectionImage(type, direction, frame) 即转头帧
     * 
     * 精灵表结构：33 帧（1 底座 + 32 方向转头），横向排列
     */
    private void renderTower(GameTowerObj tower, int stateIndex) {
        int type = tower.getType();
        int direction = tower.getDirection();
        int frameIndex = TowerData.getDirectionFrameIndex(type, direction, stateIndex);
        int totalFrames = TowerData.getTotalFrames(type);
        String sheetName = TowerData.getImageSheetName(type);

        int gridPixelSize = getGridPixelSize();
        float worldX = tower.getGridX() * gridPixelSize;
        float worldY = tower.getGridY() * gridPixelSize;

        float screenX = camera.worldToScreenX(worldX);
        float screenY = camera.worldToScreenY(worldY);

        // 1. 绘制底座（images[0]）
        renderer.drawSpriteFrame(sheetName, 0, totalFrames,
                screenX, screenY, gridPixelSize, gridPixelSize);

        // 2. 绘制转头（images[frameIndex]）
        // 原版 y = gridY * GRID_PIXEL_SIZE - towerHeight(type)
        int towerHeight = getTowerHeight(type);
        float turretWorldY = worldY - towerHeight;
        float turretScreenY = camera.worldToScreenY(turretWorldY);

        renderer.drawSpriteFrame(sheetName, frameIndex, totalFrames,
                screenX, turretScreenY, gridPixelSize, gridPixelSize);
    }

    /**
     * 渲染单个敌人
     */
    private void renderEnemy(EnemyObj enemy, int stateIndex) {
        // 计算像素位置
        float worldX = enemy.calcPixelX() + getEnemyDrawShiftX(enemy.getType());
        float worldY = enemy.calcPixelY() + getEnemyDrawShiftY(enemy.getType());

        float screenX = camera.worldToScreenX(worldX);
        float screenY = camera.worldToScreenY(worldY);

        // 检查是否正在死亡
        int deathFrame = enemy.getDeathFrame();
        if (deathFrame != -1) {
            // 死亡动画：透明度渐隐
            int deathFrames = getEnemyDeathFrames(enemy.getType());
            int elapsed = stateIndex - deathFrame;
            float alpha = Math.max(0, 1.0f - (float) elapsed / deathFrames);
            String regionName = getEnemyRegionName(enemy.getType(), enemy.getOrientation(),
                    (enemy.getFirstState() + deathFrame) >> 0);
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY, 1.0f, 1.0f, 1.0f, alpha);
            }
            return;
        }

        // 正常渲染
        int animationFrame = (enemy.getFirstState() + stateIndex) >> 0;
        int slowCounter = enemy.getSlowCounter();
        int flameCounter = enemy.getFlameCount();

        // 根据状态效果选择颜色
        if (slowCounter > 0 && flameCounter > 0) {
            // 冰冻 + 灼烧
            String regionName = getEnemyRegionName(enemy.getType(), enemy.getOrientation(), animationFrame >> 1);
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY, 0.5f, 0.4f, 0.8f, 1.0f);
            }
        } else if (flameCounter > 0) {
            // 灼烧效果
            String regionName = getEnemyRegionName(enemy.getType(), enemy.getOrientation(), animationFrame);
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY, COLOR_FIRE_R, COLOR_FIRE_G, COLOR_FIRE_B, 1.0f);
            }
        } else if (slowCounter > 0) {
            // 冰冻效果：动画减半 + 蓝色着色
            String regionName = getEnemyRegionName(enemy.getType(), enemy.getOrientation(), animationFrame >> 1);
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY, COLOR_ICE_R, COLOR_ICE_G, COLOR_ICE_B, 1.0f);
            }
        } else {
            // 正常状态
            String regionName = getEnemyRegionName(enemy.getType(), enemy.getOrientation(), animationFrame);
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY);
            }
        }

        // 绘制火焰粒子
        renderEnemyFlames(enemy, stateIndex, screenX, screenY);

        // 绘制血条
        renderEnemyHealthBar(enemy, screenX, screenY);
    }

    /**
     * 渲染敌人火焰粒子
     */
    private void renderEnemyFlames(EnemyObj enemy, int stateIndex, float baseScreenX, float baseScreenY) {
        int flameCount = enemy.getFlameCount();
        if (flameCount <= 0) return;

        int effectWidth = getGridPixelSize() - 2;
        int hborder = effectWidth >> 3;
        int width = effectWidth;
        int height = effectWidth;

        for (int i = 0; i < flameCount; i++) {
            int flameOffset = enemy.getFlameFrame(i);
            if (flameOffset >= 0) {
                int flameX = (int) (baseScreenX + hborder + (enemy.getFlameX(i) % (width - hborder * 2)));
                int flameY = (int) (baseScreenY + hborder * 2 + (enemy.getFlameY(i) % (height - hborder * 2)));

                String flameRegion = "fire_" + (Math.abs(stateIndex + flameOffset) % 8);
                renderer.drawSprite(flameRegion, flameX, flameY);
            }
        }
    }

    /**
     * 渲染敌人血条
     */
    private void renderEnemyHealthBar(EnemyObj enemy, float baseScreenX, float baseScreenY) {
        int energyBarWidth = getGridPixelSize() - 6;
        int health = enemy.getHealth();
        int maxHealth = enemy.getMaxHealth();
        if (health >= maxHealth) return;

        int barWidth = (energyBarWidth * health) / maxHealth;
        int offsetX = getEnemyEnergyBarOffset(enemy.getType());

        float screenX = camera.worldToScreenX(baseScreenX + offsetX);
        float screenY = camera.worldToScreenY(baseScreenY + 1);

        // 血条背景（红色）
        renderer.drawRect(screenX, screenY, energyBarWidth, 2, 1.0f, 0.0f, 0.0f, 0.8f);

        // 血条前景（绿色）
        renderer.drawRect(screenX, screenY, barWidth, 2, 0.0f, 1.0f, 0.0f, 0.8f);
    }

    /**
     * 绘制子弹
     */
    private void renderBullets(GameWorld world, int stateIndex) {
        BulletObj bullet = world.getBulletList();
        while (bullet != null) {
            int size = bullet.getSize(stateIndex);
            float worldX = bullet.getX() - size / 2.0f;
            float worldY = bullet.getY() - size / 2.0f;

            float screenX = camera.worldToScreenX(worldX);
            float screenY = camera.worldToScreenY(worldY);

            String regionName = bullet.getDirectionImageName();
            if (regionName != null) {
                renderer.drawSprite(regionName, screenX, screenY, size, size);
            } else {
                // 使用纯色矩形代替
                float r = ((bullet.getColor() >> 16) & 0xFF) / 255.0f;
                float g = ((bullet.getColor() >> 8) & 0xFF) / 255.0f;
                float b = (bullet.getColor() & 0xFF) / 255.0f;
                renderer.drawRect(screenX, screenY, size, size, r, g, b, 1.0f);
            }

            bullet = bullet.next;
        }
    }

    /**
     * 添加粒子效果
     */
    public void addParticle(float worldX, float worldY, int deltaX, int deltaY, int color, int size, int startState, int duration) {
        if (particleCount >= particles.length) return;

        Particle p = particles[particleCount++];
        if (p == null) {
            p = new Particle();
            particles[particleCount - 1] = p;
        }
        p.worldX = worldX;
        p.worldY = worldY;
        p.deltaX = deltaX;
        p.deltaY = deltaY;
        p.color = color;
        p.size = size;
        p.startState = startState;
        p.duration = duration;
    }

    /**
     * 渲染粒子
     */
    private void renderParticles(int stateIndex) {
        for (int i = 0; i < particleCount; i++) {
            Particle p = particles[i];
            if (p == null) continue;

            int elapsed = stateIndex - p.startState;
            if (elapsed < 0 || elapsed >= p.duration) continue;

            float worldX = p.worldX + (p.deltaX * elapsed) / 8.0f;
            float worldY = p.worldY + (p.deltaY * elapsed) / 8.0f;

            float screenX = camera.worldToScreenX(worldX);
            float screenY = camera.worldToScreenY(worldY);

            float alpha = (float) (p.duration - elapsed) / p.duration;
            float r = ((p.color >> 16) & 0xFF) / 255.0f;
            float g = ((p.color >> 8) & 0xFF) / 255.0f;
            float b = (p.color & 0xFF) / 255.0f;

            renderer.drawRect(screenX, screenY, p.size, p.size, r, g, b, alpha);
        }
    }

    /**
     * 添加爆炸效果
     */
    public void addExplosion(float worldX, float worldY, int type, int startState, int duration) {
        if (explosionCount >= explosions.length) return;

        Explosion e = explosions[explosionCount++];
        if (e == null) {
            e = new Explosion();
            explosions[explosionCount - 1] = e;
        }
        e.worldX = worldX;
        e.worldY = worldY;
        e.type = type;
        e.startState = startState;
        e.duration = duration;
    }

    /**
     * 渲染爆炸
     */
    private void renderExplosions(int stateIndex) {
        for (int i = 0; i < explosionCount; i++) {
            Explosion e = explosions[i];
            if (e == null) continue;

            int elapsed = stateIndex - e.startState;
            if (elapsed < 0 || elapsed >= e.duration) continue;

            float screenX = camera.worldToScreenX(e.worldX);
            float screenY = camera.worldToScreenY(e.worldY);

            String regionName = "explosion_" + e.type + "_" + elapsed;
            renderer.drawSprite(regionName, screenX, screenY);
        }
    }

    /**
     * 渲染金钱飘字
     */
    private void renderMoneyPopups(GameWorld world, int stateIndex) {
        MoneyPopup popup = world.getMoneyPopupList();
        while (popup != null) {
            int elapsed = stateIndex - popup.startState;
            if (elapsed >= 0 && elapsed < 10) {
                float worldX = popup.worldX + 5;
                float worldY = popup.worldY + getGridPixelSize() / 2 - elapsed;

                float screenX = camera.worldToScreenX(worldX);
                float screenY = camera.worldToScreenY(worldY);

                String moneyString = "$" + popup.amount;
                // 黑色描边
                renderer.drawText(moneyString, screenX + 1, screenY + 1, 0.0f, 0.0f, 0.0f, 1.0f);
                // 黄色文字
                renderer.drawText(moneyString, screenX, screenY, 1.0f, 1.0f, 0.0f, 1.0f);
            }
            popup = popup.next;
        }
    }

    /**
     * 渲染 overlays
     */
    private void renderOverlays(GameWorld world) {
        LevelOverlay[] overlays = world.getOverlays();
        if (overlays != null) {
            for (LevelOverlay overlay : overlays) {
                float screenX = camera.worldToScreenX(overlay.x);
                float screenY = camera.worldToScreenY(overlay.y);
                renderer.drawSprite(overlay.imageName, screenX, screenY);
            }
        }
    }

    // ========== 辅助方法（需要与游戏数据对接） ==========

    private int getGridPixelSize() {
        // 从游戏配置获取
        return 32;
    }

    /**
     * 获取塔高度（转头的 Y 偏移）
     */
    private int getTowerHeight(int type) {
        return TowerData.towerHeight(type);
    }

    /**
     * 获取塔底座精灵图区域名称（对应 images[0]）
     */
    private String getTowerBaseRegionName(int type) {
        String sheetName = TowerData.getImageSheetName(type);
        return "tower_" + sheetName + "_base";
    }

    /**
     * 获取塔转头精灵图区域名称（对应 images[frameIndex]）
     */
    private String getTowerTurretRegionName(int type, int frameIndex) {
        String sheetName = TowerData.getImageSheetName(type);
        return "tower_" + sheetName + "_dir_" + frameIndex;
    }

    // 保留旧的 getTowerRegionName 用于兼容
    private String getTowerRegionName(int type, int direction, int frameIndex) {
        return "tower_" + type + "_" + direction;
    }

    private int getEnemyDrawShiftX(int type) {
        return 0;
    }

    private int getEnemyDrawShiftY(int type) {
        return 0;
    }

    private int getEnemyDeathFrames(int type) {
        return 10;
    }

    private String getEnemyRegionName(int type, int orientation, int frame) {
        return "enemy_" + type + "_" + orientation + "_" + frame;
    }

    private int getEnemyEnergyBarOffset(int type) {
        return 0;
    }

    /**
     * 清理粒子池（关卡切换时调用）
     */
    public void clear() {
        for (int i = 0; i < particles.length; i++) {
            particles[i] = null;
        }
        particleCount = 0;

        for (int i = 0; i < explosions.length; i++) {
            explosions[i] = null;
        }
        explosionCount = 0;
    }

    // ========== 内部数据类 ==========

    private static class Particle {
        float worldX, worldY;
        int deltaX, deltaY;
        int color;
        int size;
        int startState;
        int duration;
    }

    private static class Explosion {
        float worldX, worldY;
        int type;
        int startState;
        int duration;
    }

    // ========== 游戏世界数据接口 ==========

    /**
     * 游戏世界数据提供器接口 — 隔离渲染器与游戏逻辑
     */
    public interface GameWorld {
        GridObject getSortedList();
        BulletObj getBulletList();
        LevelOverlay[] getUnderlays();
        LevelOverlay[] getOverlays();
        MoneyPopup getMoneyPopupList();
    }

    public static class LevelOverlay {
        public float x, y;
        public String imageName;
    }

    public static abstract class GridObject {
        public GridObject next_y;
        public GridObject prev_y;

        public abstract int getClassType(); // 1=塔, 2=敌人
        public abstract int getGridX();
        public abstract int getGridY();
    }

    public static abstract class GameTowerObj extends GridObject {
        public abstract int getType();
        public abstract int getDirection();

        @Override
        public int getClassType() {
            return 1;
        }
    }

    public static abstract class EnemyObj extends GridObject {
        public abstract int getType();
        public abstract int getOrientation();
        public abstract int getHealth();
        public abstract int getMaxHealth();
        public abstract int getFirstState();
        public abstract int getDeathFrame();
        public abstract int getSlowCounter();
        public abstract int getFlameCount();
        public abstract int getFlameFrame(int index);
        public abstract int getFlameX(int index);
        public abstract int getFlameY(int index);
        public abstract int calcPixelX();
        public abstract int calcPixelY();

        @Override
        public int getClassType() {
            return 2;
        }
    }

    public static abstract class BulletObj {
        public BulletObj next;

        public abstract int getX();
        public abstract int getY();
        public abstract int getSize(int stateIndex);
        public abstract String getDirectionImageName();
        public abstract int getColor();
    }

    public static class MoneyPopup {
        public MoneyPopup next;
        public float worldX, worldY;
        public int amount;
        public int startState;
    }
}
