package com.rdefense.core.platform;

/**
 * 网络客户端接口 — 排行榜、云存档、用户认证
 */
public interface NetworkClient {

    /**
     * 提交积分到排行榜
     * @return 是否提交成功
     */
    boolean submitScore(String mapType, int difficulty, long score);

    /**
     * 查询排行榜
     * @return 排行榜条目列表（JSON 格式）
     */
    String getLeaderboard(String mapType, int difficulty, int limit, int offset);

    /**
     * 上传存档到云端
     * @param slotId 存档槽位 (0-4)
     * @param data 存档数据
     * @return 是否上传成功
     */
    boolean uploadSave(int slotId, byte[] data);

    /**
     * 从云端下载存档
     * @param slotId 存档槽位 (0-4)
     * @return 存档数据，不存在返回 null
     */
    byte[] downloadSave(int slotId);

    /**
     * 用户登录（游客模式）
     */
    boolean loginGuest(String deviceId);

    /**
     * 用户登录（OAuth）
     */
    boolean loginOAuth(String provider, String token);

    /**
     * 登出
     */
    void logout();

    /** 是否已认证 */
    boolean isAuthenticated();

    /** 网络是否可用 */
    boolean isNetworkAvailable();

    /** 释放资源 */
    void dispose();
}
