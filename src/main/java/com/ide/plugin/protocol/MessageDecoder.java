package com.ide.plugin.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 消息解码器 — pipeline 中 LengthFieldBasedFrameDecoder 已拆帧
 * 输入: [1 byte type][payload]
 */
public class MessageDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < 1) return;

        byte type = in.readByte();
        int len = in.readableBytes();
        byte[] payload = new byte[len];
        in.readBytes(payload);

        if (type == Message.TYPE_JSON) {
            String json = new String(payload, StandardCharsets.UTF_8);
            out.add(new JsonMessage(json));
        } else if (type == Message.TYPE_FILE_CHUNK) {
            out.add(FileChunkMessage.decodePayload(payload));
        }
    }
}
