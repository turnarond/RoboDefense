package com.rdefense.core.platform.libgdx;

import com.rdefense.core.platform.NetworkClient;

/**
 * 无操作网络客户端 — 离线/未登录时使用
 * 所有操作静默失败，不阻塞游戏运行
 */
public class NoOpNetworkClient implements NetworkClient {

    @Override
    public boolean submitScore(String mapType, int difficulty, long score) {
        return false;
    }

    @Override
    public String getLeaderboard(String mapType, int difficulty, int limit, int offset) {
        return "[]";
    }

    @Override
    public boolean uploadSave(int slotId, byte[] data) {
        return false;
    }

    @Override
    public byte[] downloadSave(int slotId) {
        return null;
    }

    @Override
    public boolean loginGuest(String deviceId) {
        return false;
    }

    @Override
    public boolean loginOAuth(String provider, String token) {
        return false;
    }

    @Override
    public void logout() {
    }

    @Override
    public boolean isAuthenticated() {
        return false;
    }

    @Override
    public boolean isNetworkAvailable() {
        return false;
    }

    @Override
    public void dispose() {
    }
}
