package com.rdefense.core.game;

/**
 * 网格对象排序类 — 按 Y 坐标排序实现正确的遮挡渲染
 * 对应原版 GridObjectOrder
 */
public final class GridObjectOrder {

    private GridObject obj_list;

    /**
     * 获取排序后的链表头
     */
    public GridObject getSortedList() {
        return obj_list;
    }

    /**
     * 插入对象到链表头部
     */
    public void insertObject(GridObject obj) {
        if (this.obj_list != null) {
            this.obj_list.prev_y = obj;
        }
        obj.prev_y = null;
        obj.next_y = this.obj_list;
        this.obj_list = obj;
    }

    /**
     * 从链表中删除对象
     */
    public void deleteObject(GridObject obj) {
        if (obj == null) return;
        
        if (obj == this.obj_list) {
            this.obj_list = this.obj_list.next_y;
            if (this.obj_list != null) {
                this.obj_list.prev_y = null;
            }
        } else {
            // 防御性检查：如果 prev_y 为 null 但 obj 不是链表头，
            // 说明对象已不在链表中（可能已被删除或从未插入）
            if (obj.prev_y == null) {
                // 尝试在整个链表中查找并删除
                GridObject current = this.obj_list;
                GridObject prev = null;
                while (current != null) {
                    if (current == obj) {
                        if (prev != null) {
                            prev.next_y = obj.next_y;
                        }
                        if (obj.next_y != null) {
                            obj.next_y.prev_y = prev;
                        }
                        obj.prev_y = null;
                        obj.next_y = null;
                        return;
                    }
                    prev = current;
                    current = current.next_y;
                }
                // 对象不在链表中
                return;
            }
            obj.prev_y.next_y = obj.next_y;
        }
        if (obj.next_y != null) {
            obj.next_y.prev_y = obj.prev_y;
        }
        obj.prev_y = null;
        obj.next_y = null;
    }

    /**
     * 确保链表按 Y 坐标排序
     */
    public void ensureSorted() {
        GridObject obj = this.obj_list;
        while (obj != null) {
            int objy = calcy(obj);
            GridObject next_obj = obj.next_y;
            if (next_obj != null && objy > calcy(next_obj)) {
                shiftUp(obj);
            } else if (obj.prev_y != null && objy < calcy(obj.prev_y)) {
                next_obj = obj;
                shiftDown(obj);
            }
            obj = next_obj;
        }
    }

    /**
     * 清空链表
     */
    public void clear() {
        GridObject obj = this.obj_list;
        while (obj != null) {
            GridObject next_obj = obj.next_y;
            obj.prev_y = null;
            obj.next_y = null;
            obj = next_obj;
        }
        this.obj_list = null;
    }

    /**
     * 计算对象的 Y 排序值
     */
    private int calcy(GridObject obj) {
        if (obj.getClassType() == 1) {
            // 塔：按网格Y + 塔高度排序
            return (obj.getGridY() * 32) - 15;
        }
        // 敌人：按像素Y排序
        Enemy e = (Enemy) obj;
        return EnemyData.isFlyer(e.getType()) ? e.calcPixelY() + 100000 : e.calcPixelY();
    }

    /**
     * 向后移动（对象Y值太大）
     */
    private void shiftUp(GridObject obj) {
        GridObject next_obj;
        GridObject iter_obj = obj;
        while (true) {
            next_obj = iter_obj.next_y;
            if (next_obj == null || calcy(iter_obj) <= calcy(next_obj)) {
                break;
            }
            iter_obj = next_obj;
        }
        if (iter_obj != obj) {
            deleteObject(obj);
            iter_obj.next_y = obj;
            obj.prev_y = iter_obj;
            if (next_obj != null) {
                obj.next_y = next_obj;
                next_obj.prev_y = obj;
            } else {
                obj.next_y = null;
            }
        }
    }

    /**
     * 向前移动（对象Y值太小）
     */
    private void shiftDown(GridObject obj) {
        GridObject prev_obj;
        GridObject iter_obj = obj;
        while (true) {
            prev_obj = iter_obj.prev_y;
            if (prev_obj == null || calcy(iter_obj) >= calcy(prev_obj)) {
                break;
            }
            iter_obj = prev_obj;
        }
        if (iter_obj != obj) {
            deleteObject(obj);
            iter_obj.prev_y = obj;
            obj.next_y = iter_obj;
            if (prev_obj != null) {
                obj.prev_y = prev_obj;
                prev_obj.next_y = obj;
            } else {
                obj.prev_y = null;
                this.obj_list = obj;
            }
        }
    }
}
