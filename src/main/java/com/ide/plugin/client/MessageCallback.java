package com.ide.plugin.client;

import com.google.gson.JsonObject;

/**
 * 消息回调接口 — 由 UI 层实现
 */
public interface MessageCallback {

    /** 收到服务端的 welcome（分配 clientId） */
    void onWelcome(String clientId);

    /** 收到在线用户列表 */
    void onOnlineUsers(JsonObject msg);

    /** 有人加入 */
    void onUserJoin(String clientId, String nickname, String ip);

    /** 有人离开 */
    void onUserLeave(String clientId, String nickname, String ip);

    /** 收到聊天文本 */
    void onTextMessage(String clientId, String nickname, String content, long timestamp);

    /**
     * 收到文件分享通知（包含下载链接，不走聊天服务器传文件）
     */
    void onFileShare(String clientId, String nickname, String fileName, long fileSize,
                     String mimeType, String downloadUrl, long timestamp);

    /** 连接已建立 */
    void onConnected();

    /** 连接已断开 */
    void onDisconnected();

    /** 连接出错 */
    void onError(String message);
}
