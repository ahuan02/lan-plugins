package com.ide.plugin.protocol;

import java.util.Arrays;
import java.util.UUID;

/**
 * 文件分块消息 — 二进制直传
 *
 * <pre>
 * 二进制布局（20 + data）:
 *   [16 bytes: transferId UUID]
 *   [2 bytes:  chunkIndex (unsigned short)]
 *   [2 bytes:  totalChunks (unsigned short)]
 *   [N bytes:  文件数据]
 * </pre>
 */
public class FileChunkMessage extends Message {

    private final UUID transferId;
    private final int chunkIndex;
    private final int totalChunks;
    private final byte[] data;

    public FileChunkMessage(UUID transferId, int chunkIndex, int totalChunks, byte[] data) {
        this.transferId = transferId;
        this.chunkIndex = chunkIndex;
        this.totalChunks = totalChunks;
        this.data = data;
    }

    @Override
    public byte type() { return TYPE_FILE_CHUNK; }

    public UUID transferId() { return transferId; }
    public int chunkIndex() { return chunkIndex; }
    public int totalChunks() { return totalChunks; }
    public byte[] data() { return data; }

    @Override
    public String toString() {
        return "FileChunk{" + transferId + " chunk=" + chunkIndex + "/" + totalChunks + " len=" + data.length + "}";
    }

    // ---------- 编解码 ----------

    public static final int HEADER_SIZE = 20;

    public byte[] encodePayload() {
        byte[] payload = new byte[HEADER_SIZE + data.length];
        long msb = transferId.getMostSignificantBits();
        long lsb = transferId.getLeastSignificantBits();
        for (int i = 0; i < 8; i++) {
            payload[i]     = (byte) (msb >>> (56 - i * 8));
            payload[i + 8] = (byte) (lsb >>> (56 - i * 8));
        }
        payload[16] = (byte) (chunkIndex >>> 8);
        payload[17] = (byte) chunkIndex;
        payload[18] = (byte) (totalChunks >>> 8);
        payload[19] = (byte) totalChunks;
        System.arraycopy(data, 0, payload, HEADER_SIZE, data.length);
        return payload;
    }

    public static FileChunkMessage decodePayload(byte[] payload) {
        long msb = 0, lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (payload[i] & 0xFF);
            lsb = (lsb << 8) | (payload[i + 8] & 0xFF);
        }
        UUID tid = new UUID(msb, lsb);
        int idx  = ((payload[16] & 0xFF) << 8) | (payload[17] & 0xFF);
        int total = ((payload[18] & 0xFF) << 8) | (payload[19] & 0xFF);
        byte[] data = Arrays.copyOfRange(payload, HEADER_SIZE, payload.length);
        return new FileChunkMessage(tid, idx, total, data);
    }
}
