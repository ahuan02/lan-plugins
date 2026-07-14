package com.ide.plugin.protocol;

/**
 * 消息基类 — 与服务端协议一致
 */
public abstract class Message {

    /** JSON 消息（文本聊天、控制指令） */
    public static final byte TYPE_JSON = 0x00;

    /** 二进制文件分块 */
    public static final byte TYPE_FILE_CHUNK = 0x01;

    public abstract byte type();
}
