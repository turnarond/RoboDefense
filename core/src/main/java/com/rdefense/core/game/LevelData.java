package com.rdefense.core.game;

/**
 * 关卡数据类 — 管理关卡配置和生成
 * 对应原版 LevelData（简化版）
 */
public final class LevelData {

    // 关卡类型常量
    public static final int BASIC_LEVEL = 0;
    public static final int RUINS_LEVEL = 1;
    public static final int FACTORY_LEVEL = 2;
    public static final int COURTYARD_LEVEL = 3;
    public static final int MIXER_LEVEL = 4;
    public static final int ROADWAY_LEVEL = 5;
    public static final int SKYTOWER_LEVEL = 6;

    // 存档eyecatcher
    private static final int level_eye = 1279612492;

    // 关卡参数
    private int level_type;
    private int level_num;
    private int level_seed;
    private int mixer_seed;
    private int index;
    private int sub_index;
    private int frame_index;
    private int unit_type;
    private int path_num;
    private int level_score_multiplier;
    private boolean is_fixed_path;
    private Starfield starfield;
    
    // 缓存字段，避免重复生成关卡
    private int generated_seed = -1;
    private int generated_diff_level = -1;

    // 路径配置
    private int[] path_start;
    private int[] path_end;
    private int[] path_obstacles;
    private int[] path_start_orientation;  // 起点朝向

    // 关卡数据
    private int[] level_data;
    private int width;
    private int height;

    /**
     * 创建关卡数据
     */
    public LevelData(int mixer_seed) {
        this.mixer_seed = mixer_seed;
    }

    /**
     * 初始化关卡（带难度等级）
     */
    public void init(int level_type, int difficulty_level) {
        this.level_type = level_type;
        this.index = 0;
        this.sub_index = 0;
        this.frame_index = 0;
        this.unit_type = -1;
        this.path_num = -1;
        this.level_num = Math.max(1, difficulty_level);
        this.level_seed = (int) System.currentTimeMillis();

        // 根据关卡类型设置路径
        initLevelParms();

        // 如果路径无效，使用默认关卡
        if (path_start == null) {
            this.level_type = BASIC_LEVEL;
            initLevelParms();
        }

        // 自动生成关卡数据
        generateLevel(difficulty_level, false);
    }

    /**
     * 初始化关卡（默认难度1）
     */
    public void init(int level_type) {
        init(level_type, 1);
    }

    /**
     * 生成关卡数据
     */
    public void generateLevel(int difficulty_level, boolean survival_mode) {
        if (level_data == null || this.level_seed != this.generated_seed || difficulty_level != this.generated_diff_level) {
            level_data = LevelDataGenerator.generate(
                    this.level_seed, difficulty_level, getPathCount(), survival_mode);
            this.generated_seed = this.level_seed;
            this.generated_diff_level = difficulty_level;
        }
    }

    /**
     * 更新关卡状态（每帧调用）
     * 事件编码格式（匹配 LevelDataGenerator）：
     *   bits 10-14: 敌人类型 (unit_type)
     *   bits 3-9:   敌人数量 (unit_count)
     *   bits 24-30: 波次间隔 (unit_delay)
     *   bits 15-23: 首波间隔
     *   bits 0-2:   路径号 (path_num)
     */
    public void nextState() {
        this.unit_type = -1;
        this.path_num = -1;

        if (level_data == null || index >= level_data.length) {
            return;
        }

        int val = level_data[index];
        if (val == 0) {
            // 终止符或等待标记：不增加 frame_index（与原版一致）
            return;
        }

        this.frame_index++;

        // 解析事件
        int unit_type_val = (val >> 10) & 31;    // bits 10-14: 敌人类型
        int unit_count = (val >> 3) & 127;        // bits 3-9: 敌人数量
        int unit_delay;
        if (this.sub_index == 0) {
            unit_delay = (val >> 15) & 511;       // bits 15-23: 首波间隔
        } else {
            unit_delay = (val >> 24) & 127;       // bits 24-30: 波次间隔
        }
        int path_num_val = val & 7;               // bits 0-2: 路径号

        // 等待足够帧数后生成敌人
        if (this.frame_index >= unit_delay) {
            this.frame_index = 0;
            this.unit_type = unit_type_val;
            this.path_num = path_num_val;
            this.sub_index++;

            // 生成完所有敌人后移动到下一个事件
            if (this.sub_index >= unit_count) {
                this.sub_index = 0;
                this.index++;
            }
        }
    }

    /**
     * 下一关
     * @return true 如果还有下一关
     */
    public boolean nextLevel() {
        if (this.index >= level_data.length - 1 || level_data[this.index] != 0) {
            return false;
        }
        this.index++;
        this.level_num++;
        this.sub_index = 0;
        this.frame_index = 0;
        this.unit_type = -1;
        this.path_num = -1;
        return true;
    }

    /**
     * 初始化关卡参数
     * path_start/path_end 编码：(type << 8) | coord
     * type: 1=右边界, 2=上边界, 3=左边界, 4=下边界
     * 起点朝向：从该边界进入后应该往哪个方向走
     *   type=1(右) → 朝向左(3)
     *   type=2(上) → 朝向下(4)
     *   type=3(左) → 朝向右(1)
     *   type=4(下) → 朝向上(2)
     */
    private void initLevelParms() {
        this.is_fixed_path = false;
        switch (this.level_type) {
            case BASIC_LEVEL:
                path_start = new int[]{774};
                path_end = new int[]{262};
                path_start_orientation = new int[]{1};
                path_obstacles = null;
                width = 20;
                height = 12;
                break;
            case RUINS_LEVEL:
                path_start = new int[]{774, 522};
                path_end = new int[]{262, 1034};
                path_start_orientation = new int[]{1, 4};
                path_obstacles = null;
                width = 20;
                height = 12;
                break;
            case FACTORY_LEVEL:
                path_start = new int[]{771, 774, 777};
                path_end = new int[]{259, 262, 265};
                path_start_orientation = new int[]{1, 1, 1};
                path_obstacles = null;
                width = 20;
                height = 12;
                break;
            case COURTYARD_LEVEL:
                path_start = new int[]{774, 516, 1034, 527};
                path_end = new int[]{262, 1028, 522, 1039};
                path_start_orientation = new int[]{1, 4, 2, 4};
                path_obstacles = new int[]{67371521, 235143681, 67568129, 235340289};
                width = 20;
                height = 12;
                break;
            case ROADWAY_LEVEL:
                path_start = new int[]{516};
                path_end = new int[]{1028};
                path_obstacles = new int[]{50397441, 100729090, 33751297, 84082945, 50594305, 33882369, 50725377, 101057025, 34013441, 84345089, 17367041, 67698945, 101253377, 34210049, 84541697, 34276353, 101449985, 34407425, 34472193, 84803841, 17760513, 51314945, 101646593, 34668801, 85000449, 51511553, 101843457, 34799873, 68354561, 51642625, 34931713, 102105347, 18285057, 35127553};
                width = 9;
                height = 26;
                this.starfield = new Starfield(level_seed);
                break;
            case SKYTOWER_LEVEL:
                path_start = new int[]{771, 264};
                path_end = new int[]{259, 776};
                path_start_orientation = new int[]{1, 3};
                path_obstacles = new int[]{50397953, 234947329, 67240194, 251789570,
                        67436802, 151323138, 251986178, 67633410, 252182786, 50987777, 235537153};
                width = 20;
                height = 12;
                this.starfield = new Starfield(level_seed);
                break;
            case MIXER_LEVEL:
                // 混合器模式：使用种子码动态生成布局
                {
                    MixerLevelGenerator mixer = new MixerLevelGenerator(mixer_seed, 20, 12);
                    width = 20;
                    height = 12;
                    int[] starts = mixer.createStartPaths();
                    int[] ends = mixer.createEndPaths(starts);
                    path_start = starts;
                    path_end = ends;
                    // 根据 start wall_number 推导朝向
                    path_start_orientation = new int[starts.length];
                    for (int i = 0; i < starts.length; i++) {
                        path_start_orientation[i] = wallToOrientation(starts[i] & 0xFF00);
                    }
                    // 先设置 is_fixed_path，因为 createObstacles 依赖它
                    this.is_fixed_path = mixer.isFixedPath();
                    path_obstacles = mixer.createObstacles(this);
                }
                break;
            default:
                path_start = new int[]{774};
                path_end = new int[]{262};
                path_start_orientation = new int[]{1};
                path_obstacles = null;
                width = 20;
                height = 12;
                break;
        }

        // 原版逐图倍率：basic=100, ruins=125, factory=140, courtyard=160,
        // mixer=180, roadway=100, skytower=125
        final int[] SCORE_MULTIPLIERS = {100, 125, 140, 160, 180, 100, 125};
        this.level_score_multiplier = (this.level_type >= 0
                && this.level_type < SCORE_MULTIPLIERS.length)
                ? SCORE_MULTIPLIERS[this.level_type] : 100;

        // 星空粒子背景（仅公路/宇宙关卡）
        if (this.level_type == ROADWAY_LEVEL || this.level_type == SKYTOWER_LEVEL) {
            this.starfield = new Starfield(this.level_seed);
        } else {
            this.starfield = null;
        }
    }

    /**
     * 关卡是否结束
     * 原版逻辑：检查当前事件值是否为 0（终止符）
     */
    public boolean levelHasEnded() {
        if (level_data == null) return true;
        return index >= level_data.length || level_data[index] == 0;
    }

    /**
     * 解码网格坐标
     * 原版编码格式：(type << 8) | coord
     * type: 1=右边界, 2=上边界, 3=左边界, 4=下边界
     * coord: 在该边界上的坐标值
     */
    private static int decodeType(int val) {
        return (val >> 8) & 0xFF;
    }

    private static int decodeCoord(int val) {
        return val & 0xFF;
    }

    // ========== Getter/Setter 方法 ==========

    public int getLevelType() { return level_type; }
    public int getLevelNum() { return level_num; }
    public void setLevelNum(int num) { this.level_num = num; }
    public int getLevelSeed() { return level_seed; }
    public void setLevelSeed(int seed) { this.level_seed = seed; }
    public int getGridWidth() { return width; }
    public int getGridHeight() { return height; }
    public int getUnitType() { return unit_type; }
    public int getUnitPathNum() { return path_num; }
    public int getScoreMultiplier() { return level_score_multiplier; }
    public boolean isFixedPath() { return is_fixed_path; }
    public Starfield getStarfield() { return starfield; }
    
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }
    public int getSubIndex() { return sub_index; }
    public void setSubIndex(int sub_index) { this.sub_index = sub_index; }
    public int getFrameIndex() { return frame_index; }
    public void setFrameIndex(int frame_index) { this.frame_index = frame_index; }
    public void setUnitType(int unit_type) { this.unit_type = unit_type; }
    public void setPathNum(int path_num) { this.path_num = path_num; }
    public int getMixerSeed() { return mixer_seed; }
    public void setMixerSeed(int mixer_seed) { this.mixer_seed = mixer_seed; }

    // ========== 存档方法 ==========

    /**
     * 保存关卡状态
     */
    public void saveState(com.rdefense.core.game.GridObject.GameSaveWriter out) throws Exception {
        out.writeInt(level_eye);
        out.writeInt(this.level_type);
        out.writeInt(this.index);
        out.writeInt(this.sub_index);
        out.writeInt(this.frame_index);
        out.writeInt(this.unit_type);
        out.writeInt(this.path_num);
        out.writeInt(this.level_num);
        out.writeInt(this.level_seed);
        out.writeInt(this.mixer_seed);
    }

    /**
     * 加载关卡状态
     */
    public boolean loadState(com.rdefense.core.game.GridObject.GameSaveReader in) throws Exception {
        int eye = in.readInt();
        if (eye != level_eye) {
            return false;
        }
        
        this.level_type = in.readInt();
        this.index = in.readInt();
        this.sub_index = in.readInt();
        this.frame_index = in.readInt();
        this.unit_type = in.readInt();
        this.path_num = in.readInt();
        this.level_num = in.readInt();
        this.level_seed = in.readInt();
        this.mixer_seed = in.readInt();
        
        if (this.level_type < 0 || this.index < 0 || this.sub_index < 0 || 
            this.frame_index < 0 || this.level_num < 0) {
            return false;
        }
        
        initLevelParms();
        return true;
    }

    /**
     * 墙壁编号 → 起始朝向
     * WALL_LEFT(256) → 朝右(1), WALL_TOP(512) → 朝下(4)
     * WALL_RIGHT(768) → 朝左(3), WALL_BOTTOM(1024) → 朝上(2)
     */
    private static int wallToOrientation(int wallNumber) {
        switch (wallNumber) {
            case 256: return 1;   // 左墙→朝右
            case 512: return 4;   // 上墙→朝下
            case 768: return 3;   // 右墙→朝左
            case 1024: return 2;  // 下墙→朝上
            default: return 1;
        }
    }

    public int getPathCount() {
        return path_start != null ? path_start.length : 1;
    }

    public int getStartX(int pathNum) {
        if (path_start == null || pathNum >= path_start.length) return 0;
        int val = path_start[pathNum];
        int type = decodeType(val);
        int coord = decodeCoord(val);
        switch (type) {
            case 1: return width - 1;
            case 2: return coord;
            case 3: return 0;
            case 4: return coord;
            default: return coord;
        }
    }

    public int getStartY(int pathNum) {
        if (path_start == null || pathNum >= path_start.length) return 0;
        int val = path_start[pathNum];
        int type = decodeType(val);
        int coord = decodeCoord(val);
        switch (type) {
            case 1: return coord;
            case 2: return 0;
            case 3: return coord;
            case 4: return height - 1;
            default: return coord;
        }
    }

    public int getStartOrientation(int pathNum) {
        if (path_start_orientation != null && pathNum < path_start_orientation.length) {
            return path_start_orientation[pathNum];
        }
        if (path_start != null && pathNum < path_start.length) {
            int val = path_start[pathNum];
            int type = (val >> 8) & 0xFF;
            switch (type) {
                case 1: return 3;
                case 2: return 4;
                case 3: return 1;
                case 4: return 2;
            }
        }
        return 1;
    }

    public int getObstacleCount() {
        return path_obstacles != null ? path_obstacles.length : 0;
    }

    public int getObstacleX(int idx) {
        return (path_obstacles[idx] >> 24) & 0xFF;
    }

    public int getObstacleY(int idx) {
        return (path_obstacles[idx] >> 16) & 0xFF;
    }

    public int getObstacleWidth(int idx) {
        return (path_obstacles[idx] >> 8) & 0xFF;
    }

    public int getObstacleHeight(int idx) {
        return path_obstacles[idx] & 0xFF;
    }

    public int getEndX(int pathNum) {
        if (path_end == null || pathNum >= path_end.length) return width - 1;
        int val = path_end[pathNum];
        int type = decodeType(val);
        int coord = decodeCoord(val);
        switch (type) {
            case 1: return width - 1;
            case 2: return coord;
            case 3: return 0;
            case 4: return coord;
            default: return coord;
        }
    }

    public int getEndY(int pathNum) {
        if (path_end == null || pathNum >= path_end.length) return height - 1;
        int val = path_end[pathNum];
        int type = decodeType(val);
        int coord = decodeCoord(val);
        switch (type) {
            case 1: return coord;
            case 2: return 0;
            case 3: return coord;
            case 4: return height - 1;
            default: return coord;
        }
    }
}
