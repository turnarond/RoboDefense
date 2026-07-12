package com.rdefense.core.save;

import com.badlogic.gdx.utils.Json;

/**
 * JSON 存档序列化器
 * 负责 GameState 与 JSON 格式之间的转换
 */
public class JsonSaveSerializer {

    /** 当前存档格式版本 */
    public static final int CURRENT_VERSION = 2;

    private final Json json;

    public JsonSaveSerializer() {
        this.json = new Json();
        this.json.setIgnoreUnknownFields(true);
        this.json.setUsePrototypes(false);
    }

    /**
     * 将游戏存档数据序列化为 JSON 字符串
     */
    public String serialize(GameSaveData data) {
        data.timestamp = System.currentTimeMillis();
        data.version = CURRENT_VERSION;
        return toJson(data);
    }

    /**
     * 从 JSON 字符串反序列化为游戏存档数据
     * 自动处理版本迁移
     */
    public GameSaveData deserialize(String jsonString) {
        GameSaveData data = fromJson(jsonString);
        if (data == null) {
            throw new IllegalArgumentException("无效的存档格式");
        }
        return migrate(data);
    }

    /**
     * 版本迁移：将旧版本存档升级到当前版本
     */
    private GameSaveData migrate(GameSaveData data) {
        switch (data.version) {
            case 1:
                migrateV1ToV2(data);
                // 后续版本迁移在此添加
                break;
            case CURRENT_VERSION:
                // 已是最新版本，无需迁移
                break;
            default:
                if (data.version > CURRENT_VERSION) {
                    throw new IllegalArgumentException(
                        "存档版本过高 (v" + data.version + ")，请更新游戏"
                    );
                }
                break;
        }
        return data;
    }

    /**
     * v1 → v2 迁移
     * 新增字段设置默认值
     */
    private void migrateV1ToV2(GameSaveData data) {
        data.mixerActive = false;
        data.mixerSeed = 0;
        data.noSlowTowersCreated = false;
        data.cheapskate = false;
        data.onlyOneTowerCreated = false;
        data.version = CURRENT_VERSION;
    }

    /**
     * 使用 libGDX Json 进行序列化
     */
    private String toJson(GameSaveData data) {
        return json.toJson(data);
    }

    private GameSaveData fromJson(String jsonString) {
        return json.fromJson(GameSaveData.class, jsonString);
    }
}
