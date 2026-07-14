package com.ide.plugin.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * 消息编码器 — 输出帧:
 *   [4 bytes: payload总长度(big-endian)] [1 byte: type] [payload]
 */
@ChannelHandler.Sharable
public class MessageEncoder extends MessageToByteEncoder<Message> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) {
        byte[] payload;
        byte type = msg.type();

        if (msg instanceof JsonMessage jm) {
            payload = jm.json().getBytes(StandardCharsets.UTF_8);
        } else if (msg instanceof FileChunkMessage fcm) {
            payload = fcm.encodePayload();
        } else {
            throw new IllegalArgumentException("不支持的消息类型: " + msg.getClass());
        }

        out.writeInt(1 + payload.length);  // type + payload 总长
        out.writeByte(type);
        out.writeBytes(payload);
    }
}
