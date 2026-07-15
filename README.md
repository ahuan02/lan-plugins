# 局域网伙伴 (LanPartner) — IntelliJ IDEA 插件

## 项目概述

**局域网伙伴** 是一个 IntelliJ IDEA 插件，让你在 IDE 内部直接进行局域网实时聊天和文件共享，无需切换到外部 IM 工具。

| 属性 | 值                                            |
|---|----------------------------------------------|
| 插件名称 | 局域网伙伴 (LanPartner)                           |
| 插件 ID | `com.ide.plugin`                             |
| 目标 IDE | IntelliJ IDEA 2020.1+ (Community / Ultimate) |
| Java 版本 | JDK 17                                       |
| 协议 | 自定义 TCP 帧协议 + HTTP 文件传输                      |

---

## 功能特性

- **实时文本聊天** — 基于 Netty NIO TCP 长连接，消息气泡式展示（自己右对齐、他人左对齐、系统居中）
- **文件共享** — 拖拽/选择文件上传，自动广播下载链接，支持图片内嵌预览
- **在线用户列表** — 左侧面板实时显示在线用户，新用户加入/离开带高亮动画
- **消息动画** — 渐显 + 水平滑入效果，视觉流畅
- **未读通知** — 面板收起时 IDEA 右下角弹出气泡通知新消息
- **主题自适应** — 通过 JBColor 适配 IDEA 暗色/亮色主题，圆角气泡 + 阴影

---

## 环境要求

| 项目 | 要求                                         |
|---|--------------------------------------------|
| JDK | **11+**                                    |
| Gradle | 8.x（IDEA 内置 Gradle 即可）                     |
| IntelliJ IDEA | 2020.1 ~ 2026.1                            |
| 服务端 | 需要启动 [lan-server](../lan-server/README.md) |

---

## 快速开始

### 1. 启动服务端

先在局域网内某台机器上启动 `lan-server`：

```bash
cd lan-server
gradle runConsole

# 或者带 GUI 监控面板
gradle runUi
```

记下服务端 IP 和端口（默认 `25030`）。

### 2. 构建插件

```bash
# 方式一：Fat JAR（单文件，推荐）
gradle fatJar
# 产物: build/libs/LanPartner-1.0-SNAPSHOT-all.jar

# 方式二：标准插件包（ZIP）
gradle buildPlugin
# 产物: build/distributions/plugins-1.0-SNAPSHOT.zip
```

### 3. 安装插件

#### 方式一：Fat JAR 安装（最快）

将 `LanPartner-1.0-SNAPSHOT-all.jar` 放入 IDEA 插件目录：

| 操作系统 | 插件目录 |
|---|---|
| Windows | `%APPDATA%\JetBrains\IntelliJIdea<版本>\plugins\LanPartner\lib\` |
| macOS | `~/Library/Application Support/JetBrains/IntelliJIdea<版本>/plugins/LanPartner/lib/` |
| Linux | `~/.local/share/JetBrains/IntelliJIdea<版本>/plugins/LanPartner/lib/` |

> 如果是绿色版/Portable IDEA，目录在 IDEA 安装路径下的 `plugins/`。

放入后**重启 IDEA** 即可。

#### 方式二：从磁盘安装

1. IDEA → `File` → `Settings` → `Plugins` → 齿轮图标 → `Install Plugin from Disk...`
2. 选择 `build/distributions/plugins-1.0-SNAPSHOT.zip`
3. 重启 IDEA

---

## 使用说明

### 打开聊天面板

- **方式一**：IDEA 右侧工具窗口栏 → 点击「局域网伙伴」图标
- **方式二**：菜单栏 `Help` → `局域网伙伴`

### 连接服务器

```
┌─────────────────────────────┐
│  服务器: 192.168.1.100      │  ← 输入服务端 IP
│  端口:   25030              │  ← 聊天端口
│  昵称:   张三               │  ← 你的显示名
│         [  连接  ]          │
└─────────────────────────────┘
```

连接成功后即可看到在线用户列表和聊天消息。

### 发送消息

- 底部输入框输入文字 → 回车发送
- 支持**拖拽文件**到输入区域上传分享

### 下载文件

- 聊天面板中的文件卡片 → 点击下载按钮
- 图片文件会自动内嵌预览

---

## 打包命令

| 命令 | 产物 | 说明 |
|---|---|---|
| `gradle fatJar` | `build/libs/LanPartner-1.0-SNAPSHOT-all.jar` | 单文件 Fat JAR，含全部依赖 |
| `gradle buildPlugin` | `build/distributions/plugins-1.0-SNAPSHOT.zip` | 标准 IDEA 插件包（ZIP） |
| `gradle build` | `build/libs/plugins-1.0-SNAPSHOT.jar` | 瘦 JAR（仅插件代码，不含依赖） |
| `gradle runIde` | 启动沙箱 IDEA | 在独立 IDEA 实例中调试插件 |

> **注意**：Fat JAR 是 IntelliJ 插件格式（类 + plugin.xml + 依赖全部打包），**不能**用 `java -jar` 运行。它只能放入 IDEA 插件目录使用。

---

## 项目结构

```
plugins/
├── build.gradle.kts          ← 构建配置（含 fatJar 任务）
├── src/main/
│   ├── java/com/ide/plugin/
│   │   ├── buttons/
│   │   │   └── SidebarMenuButton.java    ← Help 菜单入口
│   │   ├── client/
│   │   │   ├── LanClient.java            ← Netty TCP 客户端
│   │   │   ├── FileUploader.java         ← HTTP 文件上传/下载
│   │   │   └── MessageCallback.java      ← 消息回调接口
│   │   ├── factory/
│   │   │   └── MyToolWindowFactory.java  ← 工具窗口工厂
│   │   ├── protocol/
│   │   │   ├── Message.java              ← 消息基类
│   │   │   ├── JsonMessage.java          ← JSON 消息
│   │   │   ├── FileChunkMessage.java     ← 文件分块消息
│   │   │   ├── MessageEncoder.java       ← Netty 编码器
│   │   │   └── MessageDecoder.java       ← Netty 解码器
│   │   └── ui/
│   │       └── ChatPanel.java            ← 聊天主面板（核心 UI）
│   └── resources/META-INF/
│       └── plugin.xml                    ← 插件描述文件
└── gradle.properties
```

## 架构简图

```
IntelliJ IDEA
  │
  ├── ToolWindow (右侧面板 "LanPartnerWindow")
  │     └── MyToolWindowFactory
  │           └── ChatPanel (核心 UI)
  │                 ├── LanClient (Netty TCP)
  │                 │     ├── MessageEncoder / Decoder
  │                 │     └── JsonMessage / FileChunkMessage
  │                 ├── FileUploader (HTTP)
  │                 └── 消息气泡 / 用户列表 / 输入区
  │
  ├── Action → SidebarMenuButton (Help 菜单)
  │     └── 打开 LanPartnerWindow
  │
  └── NotificationGroup → 新消息气泡通知
```

## 通信协议

| 通道 | 协议 | 用途 |
|---|---|---|
| TCP（默认 25030） | 自定义二进制帧 | 聊天消息、用户上线/下线、文件分享通知 |
| HTTP（默认 25031） | HTTP POST/GET | 文件上传 `/upload`、文件下载 `/dl/{fileId}` |

### TCP 帧格式

```
[4 bytes big-endian 总长度] [1 byte type] [N bytes payload]

type:
  0x00 = JSON 消息（聊天/控制指令）
  0x01 = 文件分块（二进制直传）
```

### JSON 消息类型

| msgType | 方向 | 说明 |
|---|---|---|
| `welcome` | 服务端→客户端 | 分配 clientId |
| `connect` | 客户端→服务端 | 注册昵称 |
| `online` | 服务端→客户端 | 在线用户列表 |
| `join` | 服务端→广播 | 有人加入 |
| `leave` | 服务端→广播 | 有人离开 |
| `text` | 双向广播 | 文本聊天 |
| `file_share` | 双向广播 | 文件分享通知 |
| `error` | 服务端→客户端 | 错误消息 |

## 依赖

| 依赖 | 用途 |
|---|---|
| Netty 4.1.x | TCP NIO 网络通信 |
| Gson 2.11 | JSON 序列化 |
| Radiance Animation 8.5 | Trident 动画引擎（消息滑入效果） |
| IntelliJ Platform | IDEA 插件框架（JBColor、JBPanel、ToolWindow 等） |
