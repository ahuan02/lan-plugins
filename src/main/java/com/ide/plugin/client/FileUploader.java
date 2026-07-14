package com.ide.plugin.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;

/**
 * HTTP 文件上传/下载客户端 — 对接 LAN 文件服务器
 */
public class FileUploader {

    private static final Gson gson = new Gson();
    private static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public final String fileServerBase; // e.g. "http://192.168.4.66:25031"

    public FileUploader(String fileServerBase) {
        this.fileServerBase = fileServerBase.replaceAll("/$", "");
    }

    // ==================== 上传 ====================

    public UploadResult upload(Path filePath) throws IOException, InterruptedException {
        String fileName = filePath.getFileName().toString();
        String mime = probeMime(fileName);
        long size = Files.size(filePath);
        byte[] data  = Files.readAllBytes(filePath);

        String disposition = buildDisposition(fileName);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(fileServerBase + "/upload"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", mime)
                .header("Content-Disposition", disposition)
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("上传失败 HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
        return new UploadResult(
                obj.get("fileId").getAsString(),
                obj.get("fileName").getAsString(),
                obj.get("fileSize").getAsLong(),
                obj.get("mimeType").getAsString(),
                obj.get("downloadUrl").getAsString()
        );
    }

    // ==================== 下载 ====================

    public void download(String downloadUrl, Path saveTo) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofMinutes(5))
                .GET()
                .build();

        HttpResponse<InputStream> resp = http.send(req, HttpResponse.BodyHandlers.ofInputStream());
        if (resp.statusCode() != 200) {
            throw new IOException("下载失败 HTTP " + resp.statusCode());
        }

        Files.createDirectories(saveTo.getParent());
        try (InputStream in = resp.body()) {
            Files.copy(in, saveTo, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 下载到临时文件（供图片预览用） */
    public Path downloadToTemp(String downloadUrl) throws IOException, InterruptedException {
        Path tmp = Files.createTempFile("lan-dl-", ".tmp");
        download(downloadUrl, tmp);
        return tmp;
    }

    // ==================== 工具 ====================

    private static String buildDisposition(String fileName) {
        // ASCII 可打印字符 (32-126)，不含双引号
        String ascii = fileName.replaceAll("[^\\x20-\\x7E]", "_").replace("\"", "_");
        String encoded = java.net.URLEncoder.encode(fileName, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + ascii + "\"; filename*=UTF-8''" + encoded;
    }

    private static String probeMime(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif"))  return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp"))  return "image/bmp";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".mp4"))  return "video/mp4";
        if (lower.endsWith(".mp3"))  return "audio/mpeg";
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".zip"))  return "application/zip";
        if (lower.endsWith(".txt"))  return "text/plain";
        if (lower.endsWith(".java") || lower.endsWith(".kt")) return "text/plain";
        return "application/octet-stream";
    }

    // ==================== 内部类 ====================

    public static class UploadResult {
        public final String fileId, fileName, mimeType, downloadUrl;
        public final long fileSize;
        UploadResult(String fileId, String fileName, long fileSize, String mimeType, String downloadUrl) {
            this.fileId = fileId; this.fileName = fileName;
            this.fileSize = fileSize; this.mimeType = mimeType;
            this.downloadUrl = downloadUrl;
        }
    }
}
