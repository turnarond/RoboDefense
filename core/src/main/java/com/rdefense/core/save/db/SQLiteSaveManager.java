package com.rdefense.core.save.db;

import com.rdefense.core.game.GameState;
import com.rdefense.core.game.GridObject.GameSaveWriter;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SQLite存档管理器实现
 * 
 * <p>使用SQLite数据库存储游戏存档，支持：
 * <ul>
 *   <li>多存档槽位（1-10）</li>
 *   <li>快速存档（槽位0，兼容原版）</li>
 *   <li>玩家进度永久存储</li>
 *   <li>为未来云存档预留接口</li>
 * </ul>
 */
public class SQLiteSaveManager implements GameSaveManager {

    private static final String TAG = "SQLiteSaveManager";

    /** 存档魔数（与原版一致） */
    private static final int SAVE_EYECATCHER = 1094993222;
    /** 存档版本 */
    private static final int SAVE_VERSION = 1;
    /** 最大存档槽位数 */
    private static final int MAX_SLOTS = 10;
    /** 快速存档槽位 */
    private static final int QUICK_SAVE_SLOT = 0;

    /** 数据库连接 */
    private Connection conn;
    /** 数据库路径 */
    private final String dbPath;
    /** 是否已初始化 */
    private boolean initialized = false;

    /**
     * 创建存档管理器
     * @param dbPath 数据库文件路径（如 "saves/game_saves.db"）
     */
    public SQLiteSaveManager(String dbPath) {
        this.dbPath = dbPath;
        initDatabase();
    }

    /**
     * 初始化数据库连接和表结构
     */
    private void initDatabase() {
        if (initialized) return;

        try {
            // 确保目录存在
            File dbFile = new File(dbPath);
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // 建立连接
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            conn.setAutoCommit(true);

            // 创建表
            createTables();

            initialized = true;
            log("数据库初始化成功: " + dbPath);
        } catch (SQLException e) {
            logError("数据库初始化失败: " + e.getMessage());
        }
    }

    /**
     * 创建数据库表
     */
    private void createTables() throws SQLException {
        Statement stmt = conn.createStatement();

        // 存档槽位表（元数据）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS save_slots (" +
            "slot_id INTEGER PRIMARY KEY, " +
            "save_name TEXT, " +
            "level_type INTEGER NOT NULL, " +
            "level_num INTEGER NOT NULL, " +
            "difficulty INTEGER NOT NULL, " +
            "survival_mode INTEGER DEFAULT 0, " +
            "tower_mixer INTEGER DEFAULT 0, " +
            "money INTEGER NOT NULL, " +
            "score INTEGER NOT NULL, " +
            "health INTEGER NOT NULL, " +
            "play_time_secs INTEGER DEFAULT 0, " +
            "created_at INTEGER NOT NULL, " +
            "updated_at INTEGER NOT NULL, " +
            "thumbnail BLOB, " +
            "is_valid INTEGER DEFAULT 1" +
            ")"
        );

        // 存档状态表（二进制数据）
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS save_states (" +
            "slot_id INTEGER PRIMARY KEY, " +
            "state_data BLOB NOT NULL, " +
            "eyecatcher INTEGER NOT NULL, " +
            "version INTEGER NOT NULL, " +
            "FOREIGN KEY (slot_id) REFERENCES save_slots(slot_id)" +
            ")"
        );

        // 玩家进度表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS player_progress (" +
            "key TEXT PRIMARY KEY, " +
            "value TEXT, " +
            "int_value INTEGER, " +
            "updated_at INTEGER NOT NULL" +
            ")"
        );

        // 成就表
        stmt.execute(
            "CREATE TABLE IF NOT EXISTS achievements (" +
            "id TEXT PRIMARY KEY, " +
            "unlocked_at INTEGER, " +
            "progress INTEGER DEFAULT 0" +
            ")"
        );

        // 创建索引
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_slots_updated ON save_slots(updated_at DESC)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_slots_valid ON save_slots(is_valid)");

        stmt.close();
    }

    // ========== 存档槽位管理 ==========

    @Override
    public List<SaveSlotInfo> getAllSlots() {
        List<SaveSlotInfo> slots = new ArrayList<>();
        if (!initialized) {
            logError("getAllSlots: 未初始化");
            return slots;
        }

        try {
            // 查询所有有效存档（包括槽位0的快速存档）
            PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_id, save_name, level_type, level_num, difficulty, " +
                "survival_mode, tower_mixer, money, score, health, play_time_secs, " +
                "created_at, updated_at, is_valid " +
                "FROM save_slots WHERE is_valid = 1 ORDER BY updated_at DESC"
            );
            ResultSet rs = ps.executeQuery();
            log("getAllSlots: 查询到 " + rs.getFetchSize() + " 行");
            while (rs.next()) {
                SaveSlotInfo info = new SaveSlotInfo();
                info.slotId = rs.getInt("slot_id");
                info.saveName = rs.getString("save_name");
                // 如果是快速存档且没有名称，设为"快速存档"
                if (info.slotId == 0 && (info.saveName == null || info.saveName.isEmpty())) {
                    info.saveName = "快速存档";
                }
                info.levelType = rs.getInt("level_type");
                info.levelNum = rs.getInt("level_num");
                info.difficulty = rs.getInt("difficulty");
                info.survivalMode = rs.getInt("survival_mode") != 0;
                info.towerMixer = rs.getInt("tower_mixer") != 0;
                info.money = rs.getInt("money");
                info.score = rs.getInt("score");
                info.health = rs.getInt("health");
                info.playTimeSecs = rs.getInt("play_time_secs");
                info.createdAt = rs.getLong("created_at");
                info.updatedAt = rs.getLong("updated_at");
                info.isValid = rs.getInt("is_valid") != 0;
                log("  存档: slot=" + info.slotId + " map=" + info.levelType + " level=" + info.levelNum);
                slots.add(info);
            }
            rs.close();
            ps.close();
            log("getAllSlots: 返回 " + slots.size() + " 个存档");
        } catch (SQLException e) {
            logError("获取存档列表失败: " + e.getMessage());
        }
        return slots;
    }

    @Override
    public SaveSlotInfo getSlot(int slotId) {
        if (!initialized) return null;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT slot_id, save_name, level_type, level_num, difficulty, " +
                "survival_mode, tower_mixer, money, score, health, play_time_secs, " +
                "created_at, updated_at, is_valid " +
                "FROM save_slots WHERE slot_id = ?"
            );
            ps.setInt(1, slotId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                SaveSlotInfo info = new SaveSlotInfo();
                info.slotId = rs.getInt("slot_id");
                info.saveName = rs.getString("save_name");
                info.levelType = rs.getInt("level_type");
                info.levelNum = rs.getInt("level_num");
                info.difficulty = rs.getInt("difficulty");
                info.survivalMode = rs.getInt("survival_mode") != 0;
                info.towerMixer = rs.getInt("tower_mixer") != 0;
                info.money = rs.getInt("money");
                info.score = rs.getInt("score");
                info.health = rs.getInt("health");
                info.playTimeSecs = rs.getInt("play_time_secs");
                info.createdAt = rs.getLong("created_at");
                info.updatedAt = rs.getLong("updated_at");
                info.isValid = rs.getInt("is_valid") != 0;
                rs.close();
                ps.close();
                return info;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            logError("获取存档失败: " + e.getMessage());
        }
        return null;
    }

    @Override
    public boolean createSave(int slotId, GameState state, String name) {
        if (!initialized || state == null) return false;
        if (slotId < 0 || slotId > MAX_SLOTS) return false;

        try {
            // 序列化游戏状态
            byte[] stateData = serializeState(state);
            if (stateData == null) return false;

            long now = System.currentTimeMillis();

            // 插入/更新存档元数据
            PreparedStatement psSlot = conn.prepareStatement(
                "INSERT OR REPLACE INTO save_slots " +
                "(slot_id, save_name, level_type, level_num, difficulty, " +
                "survival_mode, tower_mixer, money, score, health, play_time_secs, " +
                "created_at, updated_at, is_valid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, 1)"
            );
            psSlot.setInt(1, slotId);
            psSlot.setString(2, name);
            psSlot.setInt(3, state.getLevelData().getLevelType());
            psSlot.setInt(4, state.getLevelData().getLevelNum());
            psSlot.setInt(5, state.getDifficultyLevel());
            psSlot.setInt(6, state.isSurvivalMode() ? 1 : 0);
            psSlot.setInt(7, 0); // tower_mixer
            psSlot.setInt(8, state.getMoney());
            psSlot.setInt(9, state.getScore());
            psSlot.setInt(10, state.getHealth());
            psSlot.setLong(11, now); // created_at
            psSlot.setLong(12, now); // updated_at
            psSlot.executeUpdate();
            psSlot.close();

            // 插入/更新存档状态数据
            PreparedStatement psState = conn.prepareStatement(
                "INSERT OR REPLACE INTO save_states " +
                "(slot_id, state_data, eyecatcher, version) VALUES (?, ?, ?, ?)"
            );
            psState.setInt(1, slotId);
            psState.setBytes(2, stateData);
            psState.setInt(3, SAVE_EYECATCHER);
            psState.setInt(4, SAVE_VERSION);
            psState.executeUpdate();
            psState.close();

            log("存档保存成功: 槽位" + slotId);
            return true;
        } catch (SQLException e) {
            logError("保存存档失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean loadSave(int slotId, GameState state) {
        if (!initialized) {
            logError("loadSave: 未初始化");
            return false;
        }
        if (state == null) {
            logError("loadSave: state为null");
            return false;
        }
        log("loadSave: slotId=" + slotId);

        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT state_data, eyecatcher, version FROM save_states WHERE slot_id = ?"
            );
            ps.setInt(1, slotId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int eyecatcher = rs.getInt("eyecatcher");
                int version = rs.getInt("version");
                log("loadSave: 查询到数据 - eyecatcher=" + eyecatcher + ", version=" + version);
                
                byte[] data = rs.getBytes("state_data");
                if (data == null) {
                    logError("loadSave: state_data为null");
                    rs.close();
                    ps.close();
                    return false;
                }
                log("loadSave: state_data长度=" + data.length);
                
                if (eyecatcher != SAVE_EYECATCHER || version != SAVE_VERSION) {
                    logError("loadSave: 版本不匹配 - 期望eyecatcher=" + SAVE_EYECATCHER + ",实际=" + eyecatcher + 
                             ", 期望version=" + SAVE_VERSION + ",实际=" + version);
                    rs.close();
                    ps.close();
                    return false;
                }
                
                rs.close();
                ps.close();
                return deserializeState(data, state);
            } else {
                logError("loadSave: 未找到存档数据");
                rs.close();
                ps.close();
                return false;
            }
        } catch (SQLException e) {
            logError("加载存档失败(SQLException): " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteSave(int slotId) {
        if (!initialized) return false;
        try {
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM save_states WHERE slot_id = ?");
            ps1.setInt(1, slotId);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM save_slots WHERE slot_id = ?");
            ps2.setInt(1, slotId);
            int rows = ps2.executeUpdate();
            ps2.close();

            log("存档删除成功: 槽位" + slotId);
            return rows > 0;
        } catch (SQLException e) {
            logError("删除存档失败: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean hasSave(int slotId) {
        if (!initialized) return false;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT 1 FROM save_slots WHERE slot_id = ? AND is_valid = 1"
            );
            ps.setInt(1, slotId);
            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();
            rs.close();
            ps.close();
            return exists;
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public int getNextFreeSlot() {
        for (int i = 1; i <= MAX_SLOTS; i++) {
            if (!hasSave(i)) return i;
        }
        return -1; // 无空闲槽位
    }

    @Override
    public int getSaveCount() {
        if (!initialized) return 0;
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT COUNT(*) FROM save_slots WHERE slot_id > 0 AND is_valid = 1"
            );
            int count = rs.next() ? rs.getInt(1) : 0;
            rs.close();
            stmt.close();
            return count;
        } catch (SQLException e) {
            return 0;
        }
    }

    // ========== 快速存档 ==========

    @Override
    public boolean quickSave(GameState state) {
        return createSave(QUICK_SAVE_SLOT, state, "快速存档");
    }

    @Override
    public boolean quickLoad(GameState state) {
        return loadSave(QUICK_SAVE_SLOT, state);
    }

    @Override
    public boolean hasQuickSave() {
        return hasSave(QUICK_SAVE_SLOT);
    }

    @Override
    public void clearQuickSave() {
        deleteSave(QUICK_SAVE_SLOT);
    }

    // ========== 玩家进度 ==========

    @Override
    public int getMaxLevelWon() {
        return getProgressInt("max_level_won", 0);
    }

    @Override
    public void setMaxLevelWon(int level) {
        setProgressInt("max_level_won", level);
    }

    @Override
    public long getRewardPoints() {
        return getProgressLong("reward_points", 0L);
    }

    @Override
    public void setRewardPoints(long points) {
        setProgressLong("reward_points", points);
    }

    private int getProgressInt(String key, int def) {
        if (!initialized) return def;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT int_value FROM player_progress WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            int val = rs.next() ? rs.getInt(1) : def;
            rs.close();
            ps.close();
            return val;
        } catch (SQLException e) {
            return def;
        }
    }

    private void setProgressInt(String key, int value) {
        if (!initialized) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_progress (key, int_value, updated_at) VALUES (?, ?, ?)"
            );
            ps.setString(1, key);
            ps.setInt(2, value);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("保存进度失败: " + e.getMessage());
        }
    }

    private long getProgressLong(String key, long def) {
        if (!initialized) return def;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM player_progress WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            long val = rs.next() ? Long.parseLong(rs.getString(1)) : def;
            rs.close();
            ps.close();
            return val;
        } catch (Exception e) {
            return def;
        }
    }

    private void setProgressLong(String key, long value) {
        if (!initialized) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_progress (key, value, updated_at) VALUES (?, ?, ?)"
            );
            ps.setString(1, key);
            ps.setString(2, Long.toString(value));
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("保存进度失败: " + e.getMessage());
        }
    }

    // ========== 偏好设置实现 ==========

    @Override
    public String getPreference(String key, String defaultValue) {
        if (!initialized) return defaultValue;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM player_progress WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            String val = rs.next() ? rs.getString(1) : defaultValue;
            rs.close();
            ps.close();
            return val;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPreference(String key, String value) {
        if (!initialized) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_progress (key, value, updated_at) VALUES (?, ?, ?)"
            );
            ps.setString(1, key);
            ps.setString(2, value);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("保存偏好失败: " + e.getMessage());
        }
    }

    @Override
    public int getPreferenceInt(String key, int defaultValue) {
        if (!initialized) return defaultValue;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT int_value FROM player_progress WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            int val = rs.next() ? rs.getInt(1) : defaultValue;
            rs.close();
            ps.close();
            return val;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPreferenceInt(String key, int value) {
        if (!initialized) return;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_progress (key, int_value, updated_at) VALUES (?, ?, ?)"
            );
            ps.setString(1, key);
            ps.setInt(2, value);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            logError("保存偏好失败: " + e.getMessage());
        }
    }

    @Override
    public boolean getPreferenceBool(String key, boolean defaultValue) {
        String val = getPreference(key, defaultValue ? "true" : "false");
        return Boolean.parseBoolean(val);
    }

    @Override
    public void setPreferenceBool(String key, boolean value) {
        setPreference(key, Boolean.toString(value));
    }

    @Override
    public float getPreferenceFloat(String key, float defaultValue) {
        if (!initialized) return defaultValue;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM player_progress WHERE key = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            float val = rs.next() ? Float.parseFloat(rs.getString(1)) : defaultValue;
            rs.close();
            ps.close();
            return val;
        } catch (SQLException e) {
            return defaultValue;
        }
    }

    @Override
    public void setPreferenceFloat(String key, float value) {
        setPreference(key, Float.toString(value));
    }

    // ========== 序列化/反序列化 ==========

    private byte[] serializeState(GameState state) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(SAVE_EYECATCHER);
            dos.writeInt(SAVE_VERSION);
            log("序列化开始调用GameState.saveState...");
            
            // 使用适配器将 DataOutputStream 包装为 GameSaveWriter
            com.rdefense.core.game.GridObject.GameSaveWriter writer = new com.rdefense.core.game.GridObject.GameSaveWriter() {
                @Override
                public void writeInt(int value) throws Exception {
                    dos.writeInt(value);
                }
                @Override
                public void writeBoolean(boolean value) throws Exception {
                    dos.writeBoolean(value);
                }
            };
            
            state.saveState(writer);
            dos.flush();
            dos.close();
            byte[] result = baos.toByteArray();
            log("序列化完成: 总长度=" + result.length);
            return result;
        } catch (IOException e) {
            logError("序列化失败(IOException): " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            logError("序列化失败(Exception): " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            return null;
        }
    }

    private boolean deserializeState(byte[] data, GameState state) {
        if (data == null) {
            logError("反序列化失败: data为null");
            return false;
        }
        if (data.length == 0) {
            logError("反序列化失败: data长度为0");
            return false;
        }
        log("反序列化开始: 数据长度=" + data.length);
        
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            DataInputStream dis = new DataInputStream(bais);
            
            int eyecatcher = dis.readInt();
            log("读取eyecatcher: " + eyecatcher + " (期望: " + SAVE_EYECATCHER + ")");
            if (eyecatcher != SAVE_EYECATCHER) {
                logError("反序列化失败: eyecatcher不匹配");
                dis.close();
                return false;
            }
            
            int version = dis.readInt();
            log("读取version: " + version + " (期望: " + SAVE_VERSION + ")");
            if (version != SAVE_VERSION) {
                logError("反序列化失败: version不匹配");
                dis.close();
                return false;
            }
            
            // 验证最小数据长度：基本字段需要 9*int + 1*boolean + 3*int = 12*4 + 1 = 49字节
            int minLength = 49; // 基本字段 + level_type + level_num + score_multiplier
            if (bais.available() < minLength) {
                logError("反序列化失败: 数据不足，需要至少" + minLength + "字节，剩余" + bais.available());
                dis.close();
                return false;
            }
            
            // 使用适配器将 DataInputStream 包装为 GameSaveReader
            final int[] readCount = {0};
            com.rdefense.core.game.GridObject.GameSaveReader reader = new com.rdefense.core.game.GridObject.GameSaveReader() {
                @Override
                public int readInt() throws Exception {
                    int val = dis.readInt();
                    readCount[0]++;
                    log("  readInt[" + readCount[0] + "]: " + val + ", 剩余: " + bais.available());
                    return val;
                }
                @Override
                public boolean readBoolean() throws Exception {
                    boolean val = dis.readBoolean();
                    readCount[0]++;
                    log("  readBoolean[" + readCount[0] + "]: " + val + ", 剩余: " + bais.available());
                    return val;
                }
            };
            
            log("开始调用GameState.loadState...");
            boolean ok = state.loadState(reader);
            log("GameState.loadState返回: " + ok + ", 共读取 " + readCount[0] + " 个值");
            
            // 检查是否有未读取完的数据（正常应该读完）
            if (bais.available() > 0) {
                log("反序列化完成后剩余" + bais.available() + "字节未读取");
            }
            
            dis.close();
            return ok;
        } catch (IOException e) {
            logError("反序列化失败(IOException): " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            logError("反序列化失败(Exception): " + (e.getMessage() != null ? e.getMessage() : "null"));
            e.printStackTrace();
            return false;
        }
    }

    // ========== 成就系统实现 ==========

    @Override
    public String getAchievement(String key) {
        if (!initialized) return null;
        try {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT progress FROM achievements WHERE id = ?"
            );
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            String val = rs.next() ? rs.getString(1) : null;
            rs.close();
            ps.close();
            return val;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public void saveAchievements(String achievements) {
        if (!initialized || achievements == null || achievements.isEmpty()) return;
        
        try {
            String[] pairs = achievements.split(";");
            long now = System.currentTimeMillis();
            
            for (String pair : pairs) {
                String[] parts = pair.split("=");
                if (parts.length != 2) continue;
                
                String key = parts[0].trim();
                int value;
                try {
                    value = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    continue;
                }
                
                PreparedStatement ps = conn.prepareStatement(
                    "INSERT OR REPLACE INTO achievements (id, progress, unlocked_at) VALUES (?, ?, ?)"
                );
                ps.setString(1, key);
                ps.setInt(2, value);
                ps.setLong(3, value >= 1 ? now : 0);
                ps.executeUpdate();
                ps.close();
            }
            
            log("成就保存成功: " + pairs.length + " 条记录");
        } catch (SQLException e) {
            logError("保存成就失败: " + e.getMessage());
        }
    }

    // ========== 工具方法 ==========

    @Override
    public void close() {
        if (conn != null) {
            try {
                conn.close();
                log("数据库连接已关闭");
            } catch (SQLException e) {
                logError("关闭数据库失败: " + e.getMessage());
            }
            conn = null;
            initialized = false;
        }
    }

    @Override
    public String getDatabasePath() {
        return dbPath;
    }

    private void log(String msg) {
        System.out.println("[" + TAG + "] " + msg);
    }

    private void logError(String msg) {
        System.err.println("[" + TAG + "] " + msg);
    }
}
