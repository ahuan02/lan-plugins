package com.ide.plugin.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ide.plugin.protocol.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Netty TCP 客户端 — 连接聊天服务端
 *
 * <pre>
 * 新流程（发送文件）:
 *   1. 选文件
 *   2. HTTP POST → 文件服务器 /upload
 *   3. 拿到 downloadUrl
 *   4. 通过聊天通道发送 file_share 消息（只含 URL，不含文件数据）
 * </pre>
 */
public class LanClient {

    private static final Gson gson = new Gson();

    private final MessageCallback callback;

    private EventLoopGroup group;
    private Channel channel;
    private volatile String clientId;
    private String nickname;
    private volatile boolean connected;

    /** 文件上传器（在 connect 时设置 host/filePort） */
    private volatile FileUploader fileUploader;

    public LanClient(MessageCallback callback) {
        this.callback = callback;
    }

    // ==================== 连接 / 断开 ====================

    public void connect(String host, int port, String nickname, int filePort) {
        this.nickname = nickname;
        this.fileUploader = new FileUploader("http://" + host + ":" + filePort);

        group = new NioEventLoopGroup(1);

        MessageEncoder encoder = new MessageEncoder();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast("idle", new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS))
                                .addLast("frameDecoder", new LengthFieldBasedFrameDecoder(
                                        ByteOrder.BIG_ENDIAN, 100 * 1024 * 1024,
                                        0, 4, 0, 4, true))
                                .addLast("messageDecoder", new MessageDecoder())
                                .addLast("messageEncoder", encoder)
                                .addLast("handler", new ClientHandler());
                    }
                });

        bootstrap.connect(host, port).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                channel = future.channel();
                connected = true;
                callback.onConnected();
            } else {
                callback.onError("连接失败: " + future.cause().getMessage());
            }
        });
    }

    public void disconnect() {
        connected = false;
        clientId = null;
        if (channel != null) {
            channel.close();
            channel = null;
        }
        if (group != null) {
            group.shutdownGracefully();
            group = null;
        }
    }

    public boolean isConnected() { return connected && channel != null && channel.isActive(); }
    public String clientId() { return clientId; }
    public String nickname() { return nickname; }
    public FileUploader fileUploader() { return fileUploader; }

    // ==================== 发送 ====================

    public void sendConnect() {
        JsonObject obj = new JsonObject();
        obj.addProperty("msgType", "connect");
        obj.addProperty("nickname", nickname);
        sendJson(obj);
    }

    public void sendText(String content) {
        JsonObject obj = new JsonObject();
        obj.addProperty("msgType", "text");
        obj.addProperty("content", content);
        sendJson(obj);
    }

    /**
     * 上传文件到文件服务器，然后广播 file_share 链接。
     * 调用者应在线程中执行（涉及网络 IO）。
     */
    public UploadResult sendFile(Path filePath) throws Exception {
        if (fileUploader == null) throw new IllegalStateException("未连接");

        // 1. 上传到文件服务器
        FileUploader.UploadResult up = fileUploader.upload(filePath);

        // 2. 广播 file_share
        JsonObject share = new JsonObject();
        share.addProperty("msgType", "file_share");
        share.addProperty("fileName", up.fileName);
        share.addProperty("fileSize", up.fileSize);
        share.addProperty("mimeType", up.mimeType);
        share.addProperty("downloadUrl", up.downloadUrl);
        sendJson(share);

        return new UploadResult(up.fileName, up.fileSize, up.downloadUrl, up.mimeType);
    }

    /** 下载文件 */
    public void downloadFile(String downloadUrl, Path saveTo) throws Exception {
        if (fileUploader == null) throw new IllegalStateException("未连接");
        fileUploader.download(downloadUrl, saveTo);
    }

    private void sendJson(JsonObject obj) {
        if (channel != null && channel.isActive()) {
            channel.writeAndFlush(new JsonMessage(gson.toJson(obj)));
        }
    }

    // ==================== 内部 Handler ====================

    private class ClientHandler extends SimpleChannelInboundHandler<Message> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
            if (msg instanceof JsonMessage) {
                handleJson((JsonMessage) msg);
            }
        }

        private void handleJson(JsonMessage msg) {
            JsonObject obj;
            try {
                obj = gson.fromJson(msg.json(), JsonObject.class);
            } catch (Exception e) { return; }

            String msgType = obj.has("msgType") ? obj.get("msgType").getAsString() : "";

            switch (msgType) {
                case "welcome":
                    clientId = obj.get("clientId").getAsString();
                    callback.onWelcome(clientId);
                    sendConnect();
                    break;
                case "online":
                    callback.onOnlineUsers(obj);
                    break;
                case "join":
                    callback.onUserJoin(
                            obj.get("clientId").getAsString(),
                            obj.get("nickname").getAsString(),
                            obj.has("ip") ? obj.get("ip").getAsString() : "");
                    break;
                case "leave":
                    callback.onUserLeave(
                            obj.get("clientId").getAsString(),
                            obj.get("nickname").getAsString(),
                            obj.has("ip") ? obj.get("ip").getAsString() : "");
                    break;
                case "text":
                    callback.onTextMessage(
                            obj.get("clientId").getAsString(),
                            obj.get("nickname").getAsString(),
                            obj.get("content").getAsString(),
                            obj.has("timestamp") ? obj.get("timestamp").getAsLong() : System.currentTimeMillis());
                    break;
                case "file_share":
                    callback.onFileShare(
                            obj.get("clientId").getAsString(),
                            obj.get("nickname").getAsString(),
                            obj.get("fileName").getAsString(),
                            obj.get("fileSize").getAsLong(),
                            obj.get("mimeType").getAsString(),
                            obj.get("downloadUrl").getAsString(),
                            obj.has("timestamp") ? obj.get("timestamp").getAsLong() : System.currentTimeMillis());
                    break;
                case "error":
                    callback.onError(obj.has("message") ? obj.get("message").getAsString() : "未知错误");
                    break;
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            connected = false;
            callback.onDisconnected();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            callback.onError("连接异常: " + cause.getMessage());
            ctx.close();
        }
    }

    // ==================== 内部类 ====================

    public static class UploadResult {
        public final String fileName, downloadUrl, mimeType;
        public final long fileSize;
        UploadResult(String fileName, long fileSize, String downloadUrl, String mimeType) {
            this.fileName = fileName; this.fileSize = fileSize;
            this.downloadUrl = downloadUrl; this.mimeType = mimeType;
        }
    }
}
