package com.ide.plugin.protocol;

/**
 * JSON 消息 — 所有文本聊天和控制指令
 */
public class JsonMessage extends Message {

    private final String json;

    public JsonMessage(String json) {
        this.json = json;
    }

    @Override
    public byte type() {
        return TYPE_JSON;
    }

    public String json() {
        return json;
    }

    @Override
    public String toString() {
        return json.length() > 200 ? "JsonMessage{json=" + json.substring(0, 200) + "...}" : "JsonMessage{json=" + json + "}";
    }
}
