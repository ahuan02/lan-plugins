package com.ide.plugin.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ide.plugin.client.LanClient;
import com.ide.plugin.client.MessageCallback;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.NotNull;

/**
 * 聊天主面板 — 图片预览 + 文件卡片 + HTTP 文件服务器
 *
 * <pre>
 *  ┌─────────────────────────────────────────────────────────┐
 *  │ 服务器 [192.168.4.66] 聊天 [25030] 文件 [25031] 昵称 [name] │
 *  │ [连接] [断开] [选择文件]              下载目录 [/path 更改] ●│
 *  ├──────────┬──────────────────────────────────────────────┤
 *  │ 在线 (3) │  消息列表（文本 / 图片 / 文件卡片）             │
 *  ├──────────┴──────────────────────────────────────────────┤
 *  │ [输入消息...                                       ] [发送] │
 *  └─────────────────────────────────────────────────────────┘
 * </pre>
 */
public class ChatPanel extends JBPanel<ChatPanel> implements MessageCallback, Disposable {

    // ═══════ 科技感配色（JBColor 自动适配暗/亮主题） ═══════
    // 约定：JBColor(亮色主题色, 暗色主题色)
    private static final Color BG_DARK       = new JBColor(new Color(0xEB, 0xEE, 0xF3), new Color(0x0B, 0x12, 0x1E));
    private static final Color BG_PANEL      = new JBColor(new Color(0xF2, 0xF4, 0xF7), new Color(0x12, 0x1B, 0x2D));
    private static final Color BG_CHAT       = new JBColor(new Color(0xFF, 0xFF, 0xFF), new Color(0x0F, 0x17, 0x24));
    private static final Color ACCENT_BLUE   = new JBColor(new Color(0x00, 0x7A, 0xAD), new Color(0x00, 0xB4, 0xD8));
    private static final Color ACCENT_PURPLE = new JBColor(new Color(0x6D, 0x28, 0xD9), new Color(0x7C, 0x3A, 0xED));
    private static final Color ACCENT_GREEN  = new JBColor(new Color(0x05, 0x96, 0x69), new Color(0x06, 0xD6, 0xA0));
    private static final Color TEXT_PRIMARY  = new JBColor(new Color(0x1F, 0x29, 0x37), new Color(0xE0, 0xE8, 0xF0));
    private static final Color TEXT_SECONDARY= new JBColor(new Color(0x5F, 0x6B, 0x7A), new Color(0x88, 0x96, 0xB0));
    private static final Color TEXT_DIM      = new JBColor(new Color(0x8E, 0x97, 0xA5), new Color(0x58, 0x66, 0x80));

    private static final Color SELF_BUBBLE   = new JBColor(new Color(0xDB, 0xEA, 0xFE), new Color(0x1A, 0x3C, 0x5E));
    private static final Color OTHER_BUBBLE  = new JBColor(new Color(0xEF, 0xF2, 0xF6), new Color(0x18, 0x26, 0x38));
    private static final Color SELF_SENDER   = new JBColor(new Color(0x1D, 0x4E, 0xD8), new Color(0x38, 0xBD, 0xF8));
    private static final Color OTHER_SENDER  = new JBColor(new Color(0x7C, 0x3A, 0xED), new Color(0xA7, 0x8B, 0xFA));
    private static final Color SYS_BG        = new JBColor(new Color(0xFE, 0xF9, 0xC3), new Color(0x16, 0x22, 0x34));
    private static final Color FILE_BG       = new JBColor(new Color(0xEC, 0xFD, 0xF5), new Color(0x1E, 0x2D, 0x1C));
    private static final Color FILE_BORDER   = new JBColor(new Color(0x10, 0xB9, 0x81), new Color(0x4A, 0xD6, 0x6D));
    private static final Color HIGHLIGHT_IN  = new JBColor(new Color(0xA7, 0xF3, 0xD0), new Color(0x06, 0x47, 0x3E));
    private static final Color HIGHLIGHT_OUT = new JBColor(new Color(0xFE, 0xC8, 0xC8), new Color(0x56, 0x1A, 0x1A));
    private static final Color OTHER_BORDER  = new JBColor(new Color(0xD1, 0xD5, 0xDB), new Color(0x2A, 0x36, 0x4C));
    private static final Color SELF_BORDER   = new JBColor(new Color(0x93, 0xC5, 0xFD), new Color(0x1E, 0x5A, 0x8A));
    private static final Color INPUT_BG      = new JBColor(new Color(0xFF, 0xFF, 0xFF), new Color(0x16, 0x22, 0x38));
    private static final Color INPUT_BORDER  = new JBColor(new Color(0xCB, 0xD0, 0xD8), new Color(0x2E, 0x3C, 0x56));
    private static final Color TOOLBAR_BG    = new JBColor(new Color(0xF0, 0xF2, 0xF5), new Color(0x0D, 0x16, 0x26));

    private static final Color BTN_GREEN = new JBColor(new Color(0x38, 0x8E, 0x3C), new Color(0x43, 0xA0, 0x47));
    private static final Color BTN_RED   = new JBColor(new Color(0xD3, 0x2F, 0x2F), new Color(0xE5, 0x39, 0x35));
    private static final Color BUTTON_BG = new JBColor(new Color(0xE0, 0xE5, 0xF0), new Color(0x22, 0x34, 0x50));
    private static final Color STATUS_WARN  = new JBColor(new Color(0xD9, 0x7A, 0x00), new Color(0xF5, 0x9E, 0x0B));
    private static final Color STATUS_ERROR = new JBColor(new Color(0xDC, 0x26, 0x26), new Color(0xEF, 0x44, 0x44));
    private static final Color STATUS_OK    = new JBColor(new Color(0x05, 0x96, 0x69), new Color(0x06, 0xD6, 0xA0));

    // ═══════ 连接组件 ═══════
    private final JBTextField hostField      = new JBTextField("192.168.4.66", 10);
    private final JBTextField chatPortField  = new JBTextField("25030", 6);
    private final JBTextField filePortField  = new JBTextField("25031", 6);
    private final JBTextField nickField      = new JBTextField(getDefaultNickname(), 7);
    private final JButton     connectBtn     = makeColoredBtn("连接", BTN_GREEN, Color.WHITE, 11, JBUI.insets(3, 12, 3, 12));
    private final JButton     disconnBtn     = makeColoredBtn("断开", BTN_RED, Color.WHITE, 11, JBUI.insets(3, 12, 3, 12));
    private final JBLabel     statusDot      = new JBLabel(" ● ");
    private final JBLabel     statusLabel    = new JBLabel("未连接");

    // ═══════ 用户列表 ═══════
    private final LinkedHashMap<String, UserEntry> userMap = new LinkedHashMap<>();
    private final DefaultListModel<UserEntry> userListModel = new DefaultListModel<>();
    private final JBList<UserEntry> userList = new JBList<>(userListModel);
    private final JBLabel userCountLabel = new JBLabel("在线 (0)");
    /** cid → 高亮 alpha (1=最亮, 0=消失) */
    private final Map<String, Float> highlightAlpha = new ConcurrentHashMap<>();
    /** cid → 高亮颜色 (0=离开红, 1=加入绿) */
    private final Map<String, Integer> highlightKind = new ConcurrentHashMap<>();

    // ═══════ 消息区域（改用 JPanel + GridBagLayout，每行独立对齐）═══════
    private final JPanel messagePanel = new JPanel();
    private final JBScrollPane msgScroll;
    private final List<JComponent> messageWidgets = new ArrayList<>();
    private int messageCount = 0;
    private int messageRow = 0;
    private static final int MAX_MSGS = 200;

    // ═══════ 输入区域 ═══════
    private final JBTextArea inputArea = new JBTextArea(3, 40);
    private final JButton sendBtn  = makeColoredBtn("▶ 发送", BTN_GREEN, Color.WHITE, 10, JBUI.insets(2, 8, 2, 8));
    private final JButton clearBtn = makeBtn("清屏");

    // ═══════ 文件 / 下载 ═══════
    private final JButton  fileChooseBtn = makeBtn("选择文件");
    private final JBLabel  dlDirLabel    = new JBLabel("");
    private final JButton  dlDirBtn      = makeBtn("更改");
    private Path downloadDir;

    // ═══════ 客户端 ═══════
    private LanClient client;
    private volatile String myClientId;
    private boolean isConnected = false;

    /** cid → 最新昵称（用于消息气泡显示历史消息时展示最新昵称） */
    private final Map<String, String> latestNicknames = new ConcurrentHashMap<>();

    /** 昵称输入防抖 Timer */
    private Timer nickDebounceTimer;
    private static final int NICK_DEBOUNCE_MS = 600;

    // ═══════ 未读消息 & 通知 ═══════
    private Project myProject;
    private ToolWindow myToolWindow;
    private int unreadCount = 0;
    private Notification lastUnreadNotification;

    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm");

    // ════════════════════════════════════
    //  构造
    // ════════════════════════════════════

    public ChatPanel() {
        super(new BorderLayout(0, 0));
        setBackground(BG_DARK);
        setBorder(JBUI.Borders.empty());

        // 下载目录
        downloadDir = Paths.get(System.getProperty("user.home"), "LanDownloads");
        try { Files.createDirectories(downloadDir); } catch (Exception ignored) {}
        dlDirLabel.setText(truncPath(downloadDir));
        dlDirLabel.setFont(dlDirLabel.getFont().deriveFont(11f));
        dlDirLabel.setForeground(TEXT_DIM);

        // 消息列表
        messagePanel.setLayout(new GridBagLayout());
        messagePanel.setBackground(BG_CHAT);
        messagePanel.setBorder(JBUI.Borders.empty(4, 4, 4, 4));

        // ScrollableWrapper 保证在 JScrollPane 中水平占满视口宽度
        JPanel msgWrapper = new ScrollableWrapper(new BorderLayout());
        msgWrapper.setBackground(BG_CHAT);
        msgWrapper.add(messagePanel, BorderLayout.NORTH);

        msgScroll = new JBScrollPane(msgWrapper);
        msgScroll.setBorder(JBUI.Borders.empty());
        msgScroll.getVerticalScrollBar().setUnitIncrement(16);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildMainSplit(), BorderLayout.CENTER);

        initClient();
        setConnectedState(false);
    }

    // ════════════════════════════════════
    //  UI 构建
    // ════════════════════════════════════

    private JComponent buildToolbar() {
        JBPanel<?> bar = new JBPanel<>();
        bar.setLayout(new GridLayout(3, 1, 0, 2));
        bar.setBorder(JBUI.Borders.empty(8, 10, 8, 10));
        bar.setBackground(TOOLBAR_BG);

        // 行1: 服务器 + 昵称（自适应宽度）
        JBPanel<?> row1 = new JBPanel<>(new GridLayout(1, 4, 4, 0));
        row1.setOpaque(false);
        JBLabel srvLbl = new JBLabel("服务器");
        srvLbl.setForeground(TEXT_SECONDARY);
        JBLabel nickLbl = new JBLabel("昵称");
        nickLbl.setForeground(TEXT_SECONDARY);
        row1.add(srvLbl);
        row1.add(hostField);
        row1.add(nickLbl);
        row1.add(nickField);

        // 行2: 端口（自适应宽度）
        JBPanel<?> row2 = new JBPanel<>(new GridLayout(1, 4, 4, 0));
        row2.setOpaque(false);
        JBLabel chatLbl = new JBLabel("聊天端口");
        chatLbl.setForeground(TEXT_SECONDARY);
        JBLabel fileLbl = new JBLabel("文件端口");
        fileLbl.setForeground(TEXT_SECONDARY);
        row2.add(chatLbl);
        row2.add(chatPortField);
        row2.add(fileLbl);
        row2.add(filePortField);

        // 行3: 按钮 + 状态
        JBPanel<?> row3 = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row3.setOpaque(false);
        connectBtn.addActionListener(e -> doConnect());
        disconnBtn.addActionListener(e -> doDisconnect());
        row3.add(connectBtn);
        row3.add(disconnBtn);
        row3.add(Box.createHorizontalStrut(20));
        row3.add(statusDot);
        row3.add(statusLabel);

        bar.add(row1);
        bar.add(row2);
        bar.add(row3);
        return bar;
    }

    private JComponent buildMainSplit() {
        // 左侧用户列表
        userList.setCellRenderer(new UserRenderer());
        userList.setFixedCellHeight(30);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBorder(JBUI.Borders.empty());
        userList.setBackground(BG_PANEL);
        userList.setForeground(TEXT_PRIMARY);
        JBScrollPane userScroll = new JBScrollPane(userList);
        userScroll.setBorder(JBUI.Borders.customLine(INPUT_BORDER, 0, 1, 0, 1));

        JBPanel<?> left = new JBPanel<>(new BorderLayout());
        left.setBackground(BG_PANEL);
        userCountLabel.setBorder(JBUI.Borders.empty(6, 10, 6, 10));
        userCountLabel.setFont(userCountLabel.getFont().deriveFont(Font.BOLD, 12f));
        userCountLabel.setForeground(ACCENT_BLUE);
        left.add(userCountLabel, BorderLayout.NORTH);
        left.add(userScroll, BorderLayout.CENTER);
        left.setPreferredSize(new Dimension(170, 0));
        left.setMinimumSize(new Dimension(130, 0));

        // 右侧消息区
        JBPanel<?> right = new JBPanel<>(new BorderLayout(0, 0));
        right.setBackground(BG_DARK);
        right.setOpaque(false);
        right.add(msgScroll, BorderLayout.CENTER);
        right.add(buildInputArea(), BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(165);
        split.setDividerSize(2);
        split.setResizeWeight(0.0);
        split.setContinuousLayout(true);  // 拖动分割条时实时跟随
        split.setBorder(JBUI.Borders.empty());
        return split;
    }

    private JComponent buildInputArea() {
        fileChooseBtn.addActionListener(e -> chooseAndSendFile());
        dlDirBtn.addActionListener(e -> changeDownloadDir());
        clearBtn.addActionListener(e -> clearMessages());
        sendBtn.addActionListener(e -> sendMessage());

        // ── 工具栏行：左=操作按钮 | 右=下载目录 ──
        JBPanel<?> barLeft = new JBPanel<>(new FlowLayout(FlowLayout.LEFT, 6, 0));
        barLeft.setOpaque(false);
        barLeft.add(fileChooseBtn);
        barLeft.add(clearBtn);

        JBPanel<?> barRight = new JBPanel<>(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        barRight.setOpaque(false);
        JBLabel dlLbl = new JBLabel("下载:");
        dlLbl.setForeground(TEXT_DIM);
        dlLbl.setFont(dlLbl.getFont().deriveFont(11f));
        barRight.add(dlLbl);
        barRight.add(dlDirLabel);
        barRight.add(dlDirBtn);

        JBPanel<?> topRow = new JBPanel<>(new BorderLayout(0, 0));
        topRow.setBorder(JBUI.Borders.empty(4, 8, 2, 8));
        topRow.setOpaque(false);
        topRow.add(barLeft, BorderLayout.WEST);
        topRow.add(barRight, BorderLayout.EAST);

        // 输入行：深色背景 + 圆角容器
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBackground(INPUT_BG);
        inputArea.setForeground(TEXT_PRIMARY);
        inputArea.setCaretColor(ACCENT_BLUE);
        inputArea.setBorder(JBUI.Borders.empty(6, 10, 6, 10));
        inputArea.setFont(inputArea.getFont().deriveFont(13f));
        inputArea.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });
        // (监听器已在 buildInputArea 开头注册)

        // 输入框用圆角面板包裹
        RoundedPanel inputWrap = new RoundedPanel(INPUT_BG, INPUT_BORDER, 10);
        inputWrap.setLayout(new BorderLayout());
        inputWrap.setBorder(JBUI.Borders.empty(2, 2, 2, 2));

        JBScrollPane inputScroll = new JBScrollPane(inputArea);
        inputScroll.setBorder(JBUI.Borders.empty());
        inputScroll.getViewport().setBackground(INPUT_BG);
        inputWrap.add(inputScroll, BorderLayout.CENTER);

        JBPanel<?> inputRow = new JBPanel<>(new BorderLayout(6, 0));
        inputRow.setBorder(JBUI.Borders.empty(4, 8, 6, 8));
        inputRow.setOpaque(false);
        inputRow.add(inputWrap, BorderLayout.CENTER);

        JBPanel<?> btnCol = new JBPanel<>();
        btnCol.setLayout(new BoxLayout(btnCol, BoxLayout.Y_AXIS));
        btnCol.setOpaque(false);
        btnCol.add(sendBtn);
        inputRow.add(btnCol, BorderLayout.EAST);

        JBPanel<?> wrapper = new JBPanel<>(new BorderLayout(0, 0));
        wrapper.setBackground(BG_PANEL);
        wrapper.add(topRow, BorderLayout.NORTH);
        wrapper.add(inputRow, BorderLayout.CENTER);
        wrapper.setBorder(JBUI.Borders.customLine(INPUT_BORDER, 1, 0, 0, 0));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        return wrapper;
    }

    // ════════════════════════════════════
    //  连接
    // ════════════════════════════════════

    private void initClient() { client = new LanClient(this); }

    /**
     * 由 {@link com.ide.plugin.factory.MyToolWindowFactory} 调用，
     * 注入项目上下文 &amp; 工具窗口引用。
     * 负责：监听窗口可见性 → 重置未读计数
     */
    public void initToolWindow(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.myProject = project;
        this.myToolWindow = toolWindow;

        // 监听工具窗口展开/收起
        project.getMessageBus().connect(this).subscribe(
                ToolWindowManagerListener.TOPIC,
                new ToolWindowManagerListener() {
                    @Override
                    public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                        ToolWindow tw = toolWindowManager.getToolWindow("LanPartnerWindow");
                        if (tw != null && tw.isVisible()) {
                            resetUnreadCount();
                        }
                    }
                }
        );
    }

    /**
     * 收到"他人"的消息或文件分享时调用，
     * 若当前窗口处于收起状态 → 累加未读计数 + 弹 IDEA 右下角气泡
     */
    private void onIncomingMessage(String preview) {
        if (myToolWindow == null || myToolWindow.isVisible()) return;
        unreadCount++;
        showUnreadNotification(preview);
    }

    private void showUnreadNotification(String preview) {
        // 先关闭旧气泡（保证只存在一个气泡）
        if (lastUnreadNotification != null) {
            lastUnreadNotification.expire();
        }

        String shortPreview = preview.length() > 30 ? preview.substring(0, 28) + "…" : preview;
        String content;
        if (unreadCount <= 1) {
            content = shortPreview;
        } else {
            content = "共 " + unreadCount + " 条新消息\n" + shortPreview;
        }

        lastUnreadNotification = new Notification(
                "LanPartner.Notifications",
                "💬 局域网伙伴",
                content,
                NotificationType.INFORMATION
        );

        // 点击气泡 → 打开聊天面板
        lastUnreadNotification.addAction(new AnAction("打开查看") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (myToolWindow != null) {
                    myToolWindow.show();
                    resetUnreadCount();
                }
            }
        });

        if (myProject != null) {
            Notifications.Bus.notify(lastUnreadNotification, myProject);
        }
    }

    private void resetUnreadCount() {
        unreadCount = 0;
        if (lastUnreadNotification != null) {
            lastUnreadNotification.expire();
            lastUnreadNotification = null;
        }
    }

    private void setConnectedState(boolean c) {
        isConnected = c;
        hostField.setEnabled(!c); chatPortField.setEnabled(!c);
        filePortField.setEnabled(!c);
        // 昵称框连接后不禁用，允许随时修改
        connectBtn.setEnabled(!c);
        disconnBtn.setEnabled(c);
        inputArea.setEnabled(c);
        sendBtn.setEnabled(c);
        fileChooseBtn.setEnabled(c);

        // 连接后为昵称框加防抖监听，断开时取消
        if (c) {
            setupNickDebounce();
        } else {
            cancelNickDebounce();
        }
    }

    private void doConnect() {
        if (isConnected) return;
        String host = hostField.getText().trim();
        String nick = nickField.getText().trim();
        if (host.isEmpty()) { addSys("请先输入服务器地址"); return; }
        // 昵称为空或纯空白时自动填充"未命名"
        if (nick.isEmpty()) {
            nick = "未命名";
            nickField.setText(nick);
        }
        int chatPort, filePort;
        try { chatPort = Integer.parseInt(chatPortField.getText().trim()); }
        catch (NumberFormatException e) { addSys("聊天端口号格式不正确"); return; }
        try { filePort = Integer.parseInt(filePortField.getText().trim()); }
        catch (NumberFormatException e) { addSys("文件端口号格式不正确"); return; }

        setStatus("⏳ 正在连接 " + host + ":" + chatPort + " …", STATUS_WARN);
        String finalNick = nick;
        new Thread(() -> client.connect(host, chatPort, finalNick, filePort), "lan-connect").start();
    }

    private void doDisconnect() {
        if (!isConnected) return;
        cancelNickDebounce();
        client.disconnect();
        myClientId = null;
        userMap.clear(); rebuildUserList();
        clearMessages();
        setConnectedState(false);
        setStatus("已断开", TEXT_DIM);
    }

    // ════════════════════════════════════
    //  昵称防抖（连接后自动发送更新到服务端）
    // ════════════════════════════════════

    private void setupNickDebounce() {
        cancelNickDebounce();
        nickField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { onNickChanged(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { onNickChanged(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onNickChanged(); }
        });
    }

    private void onNickChanged() {
        cancelNickDebounce();
        nickDebounceTimer = new Timer(NICK_DEBOUNCE_MS, e -> {
            if (client == null || !client.isConnected()) return;
            String newNick = nickField.getText().trim();
            if (newNick.isEmpty()) {
                newNick = "未命名";
                nickField.setText(newNick);
            }
            // 没变化不发
            if (newNick.equals(client.nickname())) return;
            String oldNick = client.nickname();
            client.sendNicknameUpdate(newNick);
            // 本地也更新 userMap 里自己的昵称
            if (myClientId != null) {
                UserEntry me = userMap.get(myClientId);
                if (me != null) {
                    userMap.put(myClientId, new UserEntry(me.cid, newNick, me.ip, true));
                    rebuildUserList();
                }
            }
            // 更新昵称映射
            if (myClientId != null) {
                latestNicknames.put(myClientId, newNick);
            }
            // 刷新所有历史消息气泡中的昵称显示
            refreshMessageSenders();
        });
        nickDebounceTimer.setRepeats(false);
        nickDebounceTimer.start();
    }

    private void cancelNickDebounce() {
        if (nickDebounceTimer != null) {
            nickDebounceTimer.stop();
            nickDebounceTimer = null;
        }
    }

    /** 刷新消息列表中所有发送者昵称（用自己的最新昵称替换旧的"我"标签） */
    private void refreshMessageSenders() {
        if (messageWidgets.isEmpty()) return;
        for (JComponent w : messageWidgets) {
            refreshSenderInWidget(w);
        }
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void refreshSenderInWidget(JComponent comp) {
        if (comp instanceof ChatPanel.AnimatedWrapper) {
            ChatPanel.AnimatedWrapper aw = (ChatPanel.AnimatedWrapper) comp;
            if (aw.getComponentCount() > 0) {
                refreshBlockPanels(aw.getComponent(0));
            }
        }
    }

    private void refreshBlockPanels(Component outer) {
        if (!(outer instanceof JPanel)) return;
        JPanel wrapper = (JPanel) outer;
        for (Component c : wrapper.getComponents()) {
            if (c instanceof JPanel) {
                JPanel block = (JPanel) c;
                if (block.getLayout() instanceof BoxLayout) {
                    for (Component cc : block.getComponents()) {
                        if (cc instanceof JPanel) {
                            JPanel maybeMeta = (JPanel) cc;
                            if (maybeMeta.getLayout() instanceof FlowLayout) {
                                refreshMetaRow(maybeMeta);
                            }
                        }
                    }
                }
            }
        }
    }

    private void refreshMetaRow(JPanel metaRow) {
        for (Component c : metaRow.getComponents()) {
            if (c instanceof JLabel) {
                JLabel lbl = (JLabel) c;
                if ("我".equals(lbl.getText())) {
                    String newName = (client != null) ? client.nickname() : "我";
                    lbl.setText(newName);
                }
            }
        }
    }

    /** 把历史消息中某人的旧昵称全部替换为新昵称 */
    private void refreshSendersByName(String oldName, String newName) {
        if (oldName == null || oldName.isEmpty()) return;
        for (JComponent w : messageWidgets) {
            replaceSenderInWidget(w, oldName, newName);
        }
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void replaceSenderInWidget(JComponent comp, String oldName, String newName) {
        if (comp instanceof ChatPanel.AnimatedWrapper) {
            ChatPanel.AnimatedWrapper aw = (ChatPanel.AnimatedWrapper) comp;
            if (aw.getComponentCount() > 0) {
                replaceInBlock(aw.getComponent(0), oldName, newName);
            }
        }
    }

    private void replaceInBlock(Component outer, String oldName, String newName) {
        if (!(outer instanceof JPanel wrapper)) return;
        for (Component c : wrapper.getComponents()) {
            if (c instanceof JPanel block && block.getLayout() instanceof BoxLayout) {
                for (Component cc : block.getComponents()) {
                    if (cc instanceof JPanel maybeMeta && maybeMeta.getLayout() instanceof FlowLayout) {
                        for (Component lc : maybeMeta.getComponents()) {
                            if (lc instanceof JLabel lbl && oldName.equals(lbl.getText())) {
                                lbl.setText(newName);
                            }
                        }
                    }
                }
            }
        }
    }

    // ════════════════════════════════════
    //  发送
    // ════════════════════════════════════

    private void sendMessage() {
        String text = inputArea.getText().trim();
        if (text.isEmpty() || !client.isConnected()) return;
        client.sendText(text);
        inputArea.setText("");
    }

    private void chooseAndSendFile() {
        if (!client.isConnected()) return;

        // 用 IntelliJ 原生文件选择器
        FileChooserDescriptor desc = new FileChooserDescriptor(true, false,
                false, false, false, true)
                .withTitle("选择要发送的文件");

        Project project = getProject();
        ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.vfs.VirtualFile[] vfs =
                    FileChooser.chooseFiles(desc, project, null);
            if (vfs.length == 0) return;

            for (com.intellij.openapi.vfs.VirtualFile vf : vfs) {
                Path file = Paths.get(vf.getPath());
                addSys("正在发送: " + file.getFileName() + " (" + fmtSize(toFileSize(file)) + ")");
                new Thread(() -> {
                    try {
                        LanClient.UploadResult up = client.sendFile(file);
                        appendSelfUploadCard(up.fileName, up.fileSize, up.downloadUrl);
                    } catch (Exception ex) {
                        onError("发送失败: " + ex.getMessage());
                    }
                }, "lan-upload").start();
            }
        });
    }

    private void changeDownloadDir() {
        FileChooserDescriptor desc = FileChooserDescriptorFactory.createSingleFolderDescriptor()
                .withTitle("选择下载目录");
        Project project = getProject();
        ApplicationManager.getApplication().invokeLater(() -> {
            com.intellij.openapi.vfs.VirtualFile vf =
                    FileChooser.chooseFile(desc, project, null);
            if (vf != null) {
                downloadDir = Paths.get(vf.getPath());
                dlDirLabel.setText(truncPath(downloadDir));
                try { Files.createDirectories(downloadDir); } catch (Exception ignored) {}
            }
        });
    }

    // ════════════════════════════════════
    //  MessageCallback
    // ════════════════════════════════════

    @Override public void onWelcome(String cid) {
        myClientId = cid;
        SwingUtilities.invokeLater(() -> {
            setConnectedState(true);
            setStatus("连接成功，ID：" + cid, ACCENT_GREEN);
            clearMessages();
            addSys("✅ 已成功连接到聊天服务器");
        });
    }

    @Override public void onOnlineUsers(JsonObject msg) {
        SwingUtilities.invokeLater(() -> {
            userMap.clear();
            Set<String> seenIps = new HashSet<>();
            JsonArray arr = msg.getAsJsonArray("users");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject u = arr.get(i).getAsJsonObject();
                String cid  = u.get("clientId").getAsString();
                String name = u.get("nickname").getAsString();
                String ip   = u.has("ip") ? u.get("ip").getAsString() : "";
                String ipHost = extractHost(ip);
                // 同一IP只保留第一次出现的用户，跳过重复
                if (!ipHost.isEmpty() && !seenIps.add(ipHost)) continue;
                userMap.put(cid, new UserEntry(cid, name, ip, cid.equals(myClientId)));
                // 更新昵称映射
                latestNicknames.put(cid, name);
            }
            rebuildUserList();
        });
    }

    @Override public void onUserJoin(String cid, String name, String ip) {
        SwingUtilities.invokeLater(() -> {
            if (userMap.containsKey(cid)) return;
            String ipHost = extractHost(ip);
            // 同一IP已存在则跳过（可能同一台机器多开了窗口）
            if (!ipHost.isEmpty() && findUserByIpHost(ipHost) != null) return;
            userMap.put(cid, new UserEntry(cid, name, ip, cid.equals(myClientId)));
            latestNicknames.put(cid, name);
            rebuildUserList();
            highlightUser(cid, true);
            addSys("👋 " + name + " 加入了聊天");
        });
    }

    @Override public void onUserLeave(String cid, String name, String ip) {
        SwingUtilities.invokeLater(() -> {
            if (!userMap.containsKey(cid)) return;
            highlightUser(cid, false);
            new Timer(800, ev -> { userMap.remove(cid); rebuildUserList(); }) {{ setRepeats(false); start(); }};
            addSys("👋 " + name + " 离开了聊天");

            // 面板收起/隐藏时，右下角气泡通知谁离开了
            if (myToolWindow == null || !myToolWindow.isVisible()) {
                showLeaveNotification(name);
            }
        });
    }

    /** 面板收起时右下角气泡：通知有人离开 */
    private void showLeaveNotification(String name) {
        // 如果已有未读消息气泡，先关掉避免重叠
        if (lastUnreadNotification != null) {
            lastUnreadNotification.expire();
            lastUnreadNotification = null;
        }
        // 未读数一并清零 — 用户点"打开查看"时只需看到离开消息
        unreadCount = 0;

        Notification n = new Notification(
                "LanPartner.Notifications",
                "👋 有人离开了",
                name + " 离开了聊天",
                NotificationType.INFORMATION
        );
        n.addAction(new AnAction("打开查看") {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                if (myToolWindow != null) {
                    myToolWindow.show();
                }
            }
        });
        if (myProject != null) {
            Notifications.Bus.notify(n, myProject);
        }
    }

    @Override public void onTextMessage(String cid, String name, String content, long ts) {
        SwingUtilities.invokeLater(() -> {
            // 用历史记录中最新的昵称来显示
            String displayName = latestNicknames.getOrDefault(cid, name);
            latestNicknames.put(cid, displayName.isEmpty() ? name : displayName);
            appendTextBubble(cid, displayName, content, ts);
            if (!cid.equals(myClientId)) onIncomingMessage(displayName + ": " + content);
        });
    }

    @Override public void onFileShare(String cid, String name, String fn, long size,
                                       String mime, String url, long ts) {
        SwingUtilities.invokeLater(() -> {
            String displayName = latestNicknames.getOrDefault(cid, name);
            latestNicknames.put(cid, displayName.isEmpty() ? name : displayName);
            if (isImageMime(mime)) {
                appendImagePreview(cid, displayName, fn, size, url, ts);
            } else {
                appendFileCard(cid, displayName, fn, size, mime, url, ts);
            }
            if (!cid.equals(myClientId)) onIncomingMessage(displayName + " 分享了文件 " + fn);
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            myClientId = null; userMap.clear(); rebuildUserList();
            setConnectedState(false);
            setStatus("连接已断开", STATUS_ERROR);
            addSys("🔌 与服务器断开连接");
        });
    }
    @Override public void onError(String msg) {
        SwingUtilities.invokeLater(() -> addSys("! " + msg));
    }

    @Override public void onNicknameUpdate(String cid, String oldNick, String newNick) {
        SwingUtilities.invokeLater(() -> {
            latestNicknames.put(cid, newNick);
            // 更新用户列表中的昵称
            UserEntry ue = userMap.get(cid);
            if (ue != null) {
                userMap.put(cid, new UserEntry(ue.cid, newNick, ue.ip, ue.isMe));
                rebuildUserList();
            }
            // 刷新他人历史消息气泡中的旧昵称 → 新昵称
            if (!cid.equals(myClientId)) {
                refreshSendersByName(oldNick, newNick);
            } else {
                refreshMessageSenders(); // 自己的更新"我" → 新昵称
            }
            String oldName = (oldNick != null && !oldNick.isEmpty()) ? oldNick : "用户";
            addSys("✏ " + oldName + " 改名为 " + newNick);
        });
    }

    // ════════════════════════════════════
    //  消息组件
    // ════════════════════════════════════

    private int calcBubbleWidth() {
        int viewW = msgScroll.getViewport().getWidth();
        if (viewW <= 0) viewW = msgScroll.getWidth();
        if (viewW <= 0) viewW = 250;
        return Math.min(viewW - 40, 320);
    }

    private JPanel buildMessageMeta(boolean mine, String name, String time, Color senderC) {
        JPanel metaRow = new JPanel(new FlowLayout(
                mine ? FlowLayout.RIGHT : FlowLayout.LEFT, 6, 0));
        metaRow.setOpaque(false);
        metaRow.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        AvatarCircle av = new AvatarCircle(name);
        av.setPreferredSize(new Dimension(28, 28));

        JLabel senderLbl = new JLabel(mine ? "我" : esc(name));
        senderLbl.setFont(senderLbl.getFont().deriveFont(Font.BOLD, 11.5f));
        senderLbl.setForeground(senderC);

        JLabel timeLbl = new JLabel(time);
        timeLbl.setFont(timeLbl.getFont().deriveFont(Font.PLAIN, 9.5f));
        timeLbl.setForeground(TEXT_DIM);

        if (mine) {
            metaRow.add(timeLbl);
            metaRow.add(senderLbl);
            metaRow.add(av);
        } else {
            metaRow.add(av);
            metaRow.add(senderLbl);
            metaRow.add(timeLbl);
        }

        return metaRow;
    }

    private void appendTextBubble(String cid, String name, String text, long ts) {
        boolean mine = cid.equals(myClientId);
        String time = TIME_FMT.format(new Date(ts));
        Color bg = mine ? SELF_BUBBLE : OTHER_BUBBLE;
        Color borderC = mine ? SELF_BORDER : OTHER_BORDER;
        Color senderC = mine ? SELF_SENDER : OTHER_SENDER;
        int bubbleWidth = calcBubbleWidth();

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        block.add(buildMessageMeta(mine, name, time, senderC));

        // 圆角气泡面板
        BubblePanel bubble = new BubblePanel(bg, borderC, mine, 12);
        bubble.setLayout(new BorderLayout());
        bubble.setBorder(JBUI.Borders.empty(7, 12, 7, 12));

        // 用 JTextArea 替代 JLabel+HTML：原生换行，无 HTML 渲染限制
        JTextArea textArea = new JTextArea(text);
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setFont(textArea.getFont().deriveFont(Font.PLAIN, 13f));
        textArea.setForeground(TEXT_PRIMARY);
        textArea.setBorder(null);
        textArea.setFocusable(false);

        // 先测内容自然宽度（不换行），再按最大宽度限制重新算高度
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        int naturalW = textArea.getPreferredSize().width;
        int finalW   = Math.min(naturalW, bubbleWidth);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setSize(finalW, Short.MAX_VALUE);
        int finalH = textArea.getPreferredSize().height;
        textArea.setPreferredSize(new Dimension(finalW, finalH));
        bubble.add(textArea, BorderLayout.CENTER);

        bubble.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        block.add(bubble);
        block.add(Box.createVerticalStrut(6));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        if (mine) wrapper.add(block, BorderLayout.EAST);
        else wrapper.add(block, BorderLayout.WEST);

        appendWidget(wrapper, mine);
    }

    private void appendImagePreview(String cid, String name, String fn,
                                     long size, String url, long ts) {
        boolean mine = cid.equals(myClientId);
        String time = TIME_FMT.format(new Date(ts));
        Color senderC = mine ? SELF_SENDER : OTHER_SENDER;

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        block.add(buildMessageMeta(mine, name, time, senderC));

        // 图片预览区用圆角气泡
        BubblePanel imgBubble = new BubblePanel(
                mine ? SELF_BUBBLE : OTHER_BUBBLE,
                mine ? SELF_BORDER : OTHER_BORDER,
                mine, 12);
        imgBubble.setLayout(new BorderLayout());
        imgBubble.setBorder(JBUI.Borders.empty(6, 6, 6, 6));
        imgBubble.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        JLabel imgPlaceholder = new JLabel("📷 加载中...");
        imgPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
        imgPlaceholder.setPreferredSize(new Dimension(200, 100));
        imgPlaceholder.setMinimumSize(new Dimension(200, 100));
        imgPlaceholder.setForeground(TEXT_DIM);
        imgPlaceholder.setFont(imgPlaceholder.getFont().deriveFont(11f));

        imgBubble.add(imgPlaceholder, BorderLayout.CENTER);
        block.add(imgBubble);

        // 底部文件名 + 下载
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        bottom.setOpaque(false);
        bottom.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        JLabel fnLbl = new JLabel(fn + " · " + fmtSize(size));
        fnLbl.setFont(fnLbl.getFont().deriveFont(Font.PLAIN, 9.5f));
        fnLbl.setForeground(TEXT_DIM);
        JButton dlBtn = makeSmallBtn("下载");
        dlBtn.addActionListener(e -> startDownload(url, fn));
        bottom.add(fnLbl);
        bottom.add(dlBtn);
        block.add(bottom);

        block.add(Box.createVerticalStrut(6));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        if (mine) wrapper.add(block, BorderLayout.EAST);
        else wrapper.add(block, BorderLayout.WEST);

        appendWidget(wrapper, mine);

        new Thread(() -> {
            try {
                if (client == null || !client.isConnected()) return;
                Path tmp = Files.createTempFile("lan-img-", ".dat");
                client.downloadFile(url, tmp);
                BufferedImage full = ImageIO.read(tmp.toFile());
                if (full != null) {
                    int maxW = 220;
                    int w = full.getWidth(), h = full.getHeight();
                    if (w > maxW) { h = h * maxW / w; w = maxW; }
                    Image scaled = full.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                    BufferedImage thumb = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    thumb.getGraphics().drawImage(scaled, 0, 0, null);
                    int finalW = w;
                    int finalH = h;
                    SwingUtilities.invokeLater(() -> {
                        imgPlaceholder.setIcon(new ImageIcon(thumb));
                        imgPlaceholder.setText("");
                        imgPlaceholder.setPreferredSize(new Dimension(finalW + 4, finalH + 4));
                        imgPlaceholder.setMinimumSize(new Dimension(finalW + 4, finalH + 4));
                        block.revalidate(); block.repaint();
                    });
                }
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            } catch (Exception ignored) {
                SwingUtilities.invokeLater(() -> imgPlaceholder.setText("📷 加载失败"));
            }
        }, "lan-img-dl").start();
    }

    private void appendFileCard(String cid, String name, String fn,
                                 long size, String mime, String url, long ts) {
        boolean mine = cid.equals(myClientId);
        String time = TIME_FMT.format(new Date(ts));
        Color senderC = mine ? SELF_SENDER : OTHER_SENDER;
        int cardW = Math.min(calcBubbleWidth(), 260);

        JPanel block = new JPanel();
        block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
        block.setOpaque(false);
        block.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        block.add(buildMessageMeta(mine, name, time, senderC));

        // 文件卡片用 BubblePanel 包裹，圆角+阴影
        BubblePanel card = new BubblePanel(FILE_BG, FILE_BORDER, mine, 10);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(JBUI.Borders.empty(8, 12, 8, 12));
        card.setMaximumSize(new Dimension(cardW, 52));

        JLabel iconLbl = new JLabel(iconForFile(fn));
        iconLbl.setFont(iconLbl.getFont().deriveFont(22f));
        iconLbl.setBorder(JBUI.Borders.empty(0, 0, 0, 2));

        JLabel infoLbl = new JLabel("<html><b style='color:#E0E8F0;font-size:11px;'>" + esc(fn) + "</b><br>"
                + "<span style='color:#8896B0;font-size:10px;'>" + fmtSize(size) + "</span></html>");
        infoLbl.setFont(infoLbl.getFont().deriveFont(Font.PLAIN, 11f));

        JButton dlBtn = makeSmallBtn("下载");
        dlBtn.addActionListener(e -> startDownload(url, fn));

        card.add(iconLbl, BorderLayout.WEST);
        card.add(infoLbl, BorderLayout.CENTER);
        card.add(dlBtn, BorderLayout.EAST);
        card.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        block.add(card);

        block.add(Box.createVerticalStrut(6));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setAlignmentX(mine ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);
        if (mine) wrapper.add(block, BorderLayout.EAST);
        else wrapper.add(block, BorderLayout.WEST);

        appendWidget(wrapper, mine);
    }

    /** 自己发送文件成功后追加一个"已发送"卡片 */
    private void appendSelfUploadCard(String fn, long size, String url) {
        SwingUtilities.invokeLater(() -> {
            JPanel block = new JPanel();
            block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS));
            block.setOpaque(false);
            block.setAlignmentX(Component.RIGHT_ALIGNMENT);

            block.add(buildMessageMeta(true, "我", TIME_FMT.format(new Date()), SELF_SENDER));

            int cardW = Math.min(calcBubbleWidth(), 260);
            BubblePanel card = new BubblePanel(FILE_BG, FILE_BORDER, true, 10);
            card.setLayout(new BorderLayout(8, 0));
            card.setBorder(JBUI.Borders.empty(8, 12, 8, 12));
            card.setMaximumSize(new Dimension(cardW, 46));

            JLabel iconLbl = new JLabel(iconForFile(fn));
            iconLbl.setFont(iconLbl.getFont().deriveFont(20f));
            JLabel infoLbl = new JLabel("<html><b style='font-size:11px;color:#E0E8F0;'>" + esc(fn) + "</b>&nbsp;"
                    + "<span style='color:#06D6A0;font-size:10px;'>✓ 已发送</span><br>"
                    + "<span style='color:#8896B0;font-size:10px;'>" + fmtSize(size) + "</span></html>");
            infoLbl.setFont(infoLbl.getFont().deriveFont(Font.PLAIN, 11f));
            card.add(iconLbl, BorderLayout.WEST);
            card.add(infoLbl, BorderLayout.CENTER);
            card.setAlignmentX(Component.RIGHT_ALIGNMENT);

            block.add(card);
            block.add(Box.createVerticalStrut(6));

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.setAlignmentX(Component.RIGHT_ALIGNMENT);
            wrapper.add(block, BorderLayout.EAST);

            appendWidget(wrapper, true);
        });
    }

    private void addSys(String msg) {
        SwingUtilities.invokeLater(() -> {
            BubblePanel bubble = new BubblePanel(SYS_BG, null, false, 8);
            bubble.setLayout(new BorderLayout());
            bubble.setBorder(JBUI.Borders.empty(3, 12, 3, 12));

            String html = msg.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            JLabel lbl = new JLabel("<html><div style='width:300px; text-align:center;'>" + html + "</div></html>");
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN, 10.5f));
            lbl.setForeground(TEXT_DIM);

            bubble.add(lbl, BorderLayout.CENTER);

            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
            wrapper.setOpaque(false);
            wrapper.add(bubble);
            appendWidget(wrapper, null);
        });
    }

    private void startDownload(String url, String fn) {
        Path dest = downloadDir.resolve(fn);
        // 去重
        if (Files.exists(dest)) {
            String base = fn.replaceFirst("(\\.[^.]+)$", "");
            String ext  = fn.contains(".") ? fn.substring(fn.lastIndexOf('.')) : "";
            int n = 1;
            while (Files.exists(downloadDir.resolve(base + "(" + n + ")" + ext))) n++;
            dest = downloadDir.resolve(base + "(" + n + ")" + ext);
        }
        Path finalDest = dest;
        new Thread(() -> {
            try {
                client.downloadFile(url, finalDest);
                SwingUtilities.invokeLater(() -> addSys("下载完成 → " + finalDest));
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> addSys("下载失败: " + ex.getMessage()));
            }
        }, "lan-dl").start();
    }

    // ════════════════════════════════════
    //  消息列表管理
    // ════════════════════════════════════

    /**
     * @param w    消息组件
     * @param mine true=自己发送（右→左）, false=接收（左→右）, null=系统消息（纯淡入）
     */
    private void appendWidget(JComponent w, Boolean mine) {
        if (messageCount >= MAX_MSGS) {
            if (!messageWidgets.isEmpty()) {
                JComponent old = messageWidgets.remove(0);
                messagePanel.remove(old);
                messageCount--;
            }
        }

        AnimatedWrapper animWrap = new AnimatedWrapper(w, mine);
        messageWidgets.add(animWrap);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = messageRow++;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = (mine == null) ? GridBagConstraints.CENTER
                : (mine ? GridBagConstraints.EAST : GridBagConstraints.WEST);
        gbc.insets = JBUI.insets(4, 8, 4, 8);
        messagePanel.add(animWrap, gbc);
        messageCount++;
        messagePanel.revalidate();
        messagePanel.repaint();

        // 滚动到底部
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = msgScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });

        animWrap.startAnimation();
    }

    private void clearMessages() {
        messagePanel.removeAll();
        messageWidgets.clear();
        messageCount = 0;
        messageRow = 0;
        messagePanel.revalidate();
        messagePanel.repaint();
    }

    // ════════════════════════════════════
    //  用户列表
    // ════════════════════════════════════

    private void rebuildUserList() {
        userListModel.clear();
        for (UserEntry e : userMap.values()) userListModel.addElement(e);
        userCountLabel.setText("在线 (" + userMap.size() + ")");
    }

    private void highlightUser(String cid, boolean join) {
        new HighlightAnimator(cid).start(join);
    }

    public class HighlightAnimator {
        private final String cid;

        HighlightAnimator(String cid) { this.cid = cid; }

        void start(boolean join) {
            highlightKind.put(cid, join ? 1 : 0);
            highlightAlpha.put(cid, 1f);

            final int durationMs = join ? 1800 : 900;
            final int fps = 30;
            final int interval = 1000 / fps;
            final int totalFrames = Math.max(1, durationMs / interval);
            final int[] frameHolder = {0};

            Timer timer = new Timer(interval, e -> {
                int frame = ++frameHolder[0];
                float raw = Math.min(1f, (float) frame / totalFrames);
                float alpha = 1f - raw;
                highlightAlpha.put(cid, alpha);
                userList.repaint();

                if (frame >= totalFrames) {
                    highlightAlpha.remove(cid);
                    highlightKind.remove(cid);
                    userList.repaint();
                    ((Timer) e.getSource()).stop();
                }
            });
            timer.setInitialDelay(0);
            timer.start();
        }
    }

    private class UserRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int idx,
                                                       boolean sel, boolean focus) {
            UserEntry e = (UserEntry) value;
            JBPanel<?> row = new JBPanel<>(new BorderLayout(6, 0));
            row.setBorder(JBUI.Borders.empty(2, 8, 2, 6));
            row.setOpaque(true);

            Float alpha = highlightAlpha.get(e.cid);
            if (alpha != null && alpha > 0.01f) {
                Integer kind = highlightKind.get(e.cid);
                Color hl = (kind != null && kind == 1) ? HIGHLIGHT_IN : HIGHLIGHT_OUT;
                Color bg = list.getBackground();
                row.setBackground(blend(bg, hl, alpha));
            } else if (sel) {
                row.setBackground(list.getSelectionBackground());
            } else {
                row.setBackground(list.getBackground());
            }

            AvatarCircle av = new AvatarCircle(e.name);
            av.setPreferredSize(new Dimension(24, 24));
            JBPanel<?> avWrap = new JBPanel<>(new BorderLayout());
            avWrap.setOpaque(false);
            avWrap.add(av, BorderLayout.CENTER);

            JBLabel nl = new JBLabel(e.name + (e.isMe ? " (我)" : ""));
            nl.setFont(nl.getFont().deriveFont(Font.PLAIN, 12f));
            nl.setForeground(e.isMe ? ACCENT_GREEN : TEXT_PRIMARY);

            JBPanel<?> nameRow = new JBPanel<>(new BorderLayout(4, 0));
            nameRow.setOpaque(false);
            nameRow.add(nl, BorderLayout.CENTER);
            if (e.ip != null && !e.ip.isEmpty()) {
                JBLabel ipLabel = new JBLabel(e.ip);
                ipLabel.setFont(ipLabel.getFont().deriveFont(Font.PLAIN, 11f));
                ipLabel.setForeground(ACCENT_BLUE);
                nameRow.add(ipLabel, BorderLayout.EAST);
            }

            row.add(avWrap, BorderLayout.WEST);
            row.add(nameRow, BorderLayout.CENTER);
            return row;
        }

        private Color blend(Color bg, Color hl, float a) {
            a = Math.max(0, Math.min(1, a));
            return new Color(
                    (int)(bg.getRed()   + (hl.getRed()   - bg.getRed())   * a),
                    (int)(bg.getGreen() + (hl.getGreen() - bg.getGreen()) * a),
                    (int)(bg.getBlue()  + (hl.getBlue()  - bg.getBlue())  * a));
        }
    }

    private static class AvatarCircle extends JComponent {
        private final String letter;
        private static final Color[] CS = {
            new Color(0x3B,0x82,0xF6), new Color(0x06,0xB6,0xD4),
            new Color(0x8B,0x5C,0xF6), new Color(0xEF,0x44,0x44),
            new Color(0xF5,0x9E,0x0B), new Color(0x10,0xB9,0x81),
        };
        private final Color bg;
        AvatarCircle(String name) {
            letter = name.isEmpty() ? "?" : name.substring(0,1).toUpperCase();
            bg = CS[Math.abs(name.hashCode()) % CS.length];
            Font f = getFont();
            if (f == null) f = new Font(Font.SANS_SERIF, Font.BOLD, 12);
            setFont(f.deriveFont(Font.BOLD, 12f));
        }
        @Override protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // subtle shadow
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillOval(2, 2, w - 2, h - 2);
            g2.setColor(bg);
            g2.fillOval(1, 1, w - 3, h - 3);
            g2.setColor(Color.WHITE);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(letter, (w-fm.stringWidth(letter))/2f, (h-fm.getHeight())/2f+fm.getAscent());
            g2.dispose();
        }
    }

    /**
     * 圆角容器 — 输入框等场景
     */
    private static class RoundedPanel extends JPanel {
        private final Color fill, stroke;
        private final int radius;

        RoundedPanel(Color fill, Color stroke, int radius) {
            this.fill = fill; this.stroke = stroke; this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = radius * 2;
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc));
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 2, arc, arc));
            }
            g2.dispose();
        }
    }

    /**
     * 圆角气泡面板 — 绘制圆角矩形背景 + 微阴影 + 可选描边
     */
    private static class BubblePanel extends JPanel {
        private final Color fill;
        private final Color stroke;
        private final int radius;

        BubblePanel(Color fill, Color stroke, boolean mine, int radius) {
            this.fill = fill;
            this.stroke = stroke;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth(), h = getHeight();
            int arc = radius * 2;

            // 阴影（深色背景下用亮边代替暗影更佳）
            g2.setColor(new Color(0, 0, 0, 40));
            g2.fill(new RoundRectangle2D.Float(1, 3, w - 2, h - 2, arc, arc));

            // 填充
            g2.setColor(fill);
            g2.fill(new RoundRectangle2D.Float(0, 0, w - 1, h - 2, arc, arc));

            // 描边
            if (stroke != null) {
                g2.setColor(stroke);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 2, h - 3, arc, arc));
            }

            g2.dispose();
        }
    }

    /**
     * 动画包裹器 — 渐显 + 水平滑入（nanoTime 插值 + 仅重绘自身，不触发父容器重布局）
     * <ul>
     *   <li>mine=true  → 从右滑入 (translateX: 40 → 0)</li>
     *   <li>mine=false → 从左滑入 (translateX: -40 → 0)</li>
     *   <li>mine=null  → 纯淡入不滑动</li>
     * </ul>
     */
    public static class AnimatedWrapper extends JPanel {
        private float alpha;
        private int translateX;
        private final Boolean mine;
        private final int startX;
        private final int absStartX;
        private boolean animDone;

        AnimatedWrapper(JComponent content, Boolean mine) {
            this.mine = mine;
            this.animDone = false;
            if (mine == null) {
                startX = 0; absStartX = 0; translateX = 0; alpha = 1f;
            } else if (mine) {
                startX = 40; absStartX = 40; translateX = 40; alpha = 0f;
            } else {
                startX = -40; absStartX = 40; translateX = -40; alpha = 0f;
            }
            setOpaque(false);
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
        }

        @Override
        public void paint(Graphics g) {
            if (animDone) { super.paint(g); return; }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
            if (mine != null) g2.translate(translateX, 0);
            super.paint(g2);
            g2.dispose();
        }

        void startAnimation() {
            if (animDone) return;
            animate(mine == null ? 150 : 250, mine != null);
        }

        // ── nanoTime 驱动，补偿 EDT 延迟，丝滑无帧差 ──
        private void animate(int durationMs, boolean slide) {
            final long t0 = System.nanoTime();
            final long durNs = durationMs * 1_000_000L;
            // interval=10ms 目标 ~100fps，实际受 EDT 限制 ~16ms，但 nanoTime 插值保证视觉平滑
            Timer timer = new Timer(10, null);
            timer.addActionListener(e -> {
                long elapsed = System.nanoTime() - t0;
                float raw = Math.min(1f, (float) elapsed / durNs);
                // ease-out quart：丝滑减速到停
                float t = 1f - raw;
                float eased = 1f - t * t * t * t;

                alpha = eased;
                if (slide) translateX = (int) (startX * t);

                if (raw >= 1f) {
                    alpha = 1f; translateX = 0; animDone = true;
                    timer.stop();
                    // 动画结束只需重绘，GridBagLayout 不依赖最大尺寸
                    SwingUtilities.invokeLater(() -> {
                        AnimatedWrapper.this.repaint();
                    });
                }
                // 关键：只重绘自身，不触发 revalidate / 不搅动父布局
                repaint();
            });
            timer.setInitialDelay(0);
            timer.start();
        }
    }

    /**
     * 实现 Scrollable，强制在 JScrollPane 中水平占满视口宽度，防止视图居中缩窄
     */
    private static class ScrollableWrapper extends JPanel implements Scrollable {
        ScrollableWrapper(LayoutManager lm) {
            super(lm);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;   // 关键：宽度 = 视口宽度
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;  // 高度由内容决定，可垂直滚动
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height / 2;
        }
    }

    // ════════════════════════════════════
    //  工具
    // ════════════════════════════════════

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
        statusDot.setForeground(color);
    }

    private static boolean isImageMime(String mime) {
        return mime != null && (mime.startsWith("image/png") ||
                mime.startsWith("image/jpeg") || mime.startsWith("image/gif") ||
                mime.startsWith("image/webp") || mime.startsWith("image/bmp"));
    }

    private static String iconForFile(String fn) {
        String l = fn.toLowerCase();
        if (l.endsWith(".zip")||l.endsWith(".jar")) return "📦";
        if (l.endsWith(".mp4")||l.endsWith(".mov"))   return "🎬";
        if (l.endsWith(".mp3")||l.endsWith(".wav"))   return "🎵";
        if (l.endsWith(".pdf")) return "📄";
        if (l.endsWith(".java")||l.endsWith(".kt"))   return "📝";
        return "📎";
    }

    private static String esc(String s) {
        return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");
    }

    private static String fmtSize(long n) {
        if (n < 1024) return n + " B";
        if (n < 1048576) return String.format("%.1f KB", n / 1024.0);
        if (n < 1073741824L) return String.format("%.1f MB", n / 1048576.0);
        return String.format("%.2f GB", n / 1073741824.0);
    }

    private static long toFileSize(Path p) {
        try { return Files.size(p); } catch (Exception e) { return 0; }
    }

    private static String getDefaultNickname() {
        String user = System.getProperty("user.name", "");
        // 若系统用户名为纯英文/数字，给一个中文友好的默认名
        if (user.isEmpty() || user.matches("[a-zA-Z0-9_]+")) {
            return "未命名";
        }
        return user;
    }

    private static String truncPath(Path p) {
        String s = p.toString();
        return s.length() > 28 ? "..." + s.substring(s.length() - 25) : s;
    }

    private static Project getProject() {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        return projects.length > 0 ? projects[0] : null;
    }

    /** 自定义底色按钮，禁用态不跟随 Darcula 灰色，而是保持原色变淡 */
    private static JButton makeColoredBtn(String text, Color bg, Color fg,
                                          int fontSize, Insets margin) {
        @SuppressWarnings("serial")
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c;
                if (!isEnabled()) {
                    c = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 100);
                } else if (getModel().isPressed()) {
                    c = bg.darker();
                } else if (getModel().isRollover()) {
                    c = bg.brighter();
                } else {
                    c = bg;
                }
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(b.getFont().deriveFont(Font.PLAIN, (float) fontSize));
        b.setForeground(fg);
        b.setMargin(margin);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton makeBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 11f));
        b.setBackground(BUTTON_BG);
        b.setForeground(TEXT_PRIMARY);
        b.setMargin(JBUI.insets(3, 12, 3, 12));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static JButton makeSmallBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 10f));
        b.setBackground(BUTTON_BG);
        b.setForeground(TEXT_PRIMARY);
        b.setMargin(JBUI.insets(1, 8, 1, 8));
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static Component gap(int w) {
        return Box.createRigidArea(new Dimension(w, 0));
    }

    @Override
    public void dispose() {
        highlightAlpha.clear();
        highlightKind.clear();
        resetUnreadCount();
        cancelNickDebounce();
        if (client != null) client.disconnect();
    }

    /** 从 "ip:port" 中提取纯 IP 地址 */
    private static String extractHost(String ipAddr) {
        if (ipAddr == null || ipAddr.isEmpty()) return "";
        int colon = ipAddr.lastIndexOf(':');
        return colon > 0 ? ipAddr.substring(0, colon) : ipAddr;
    }

    /** 在 userMap 中根据纯 IP 查找用户（忽略端口） */
    private UserEntry findUserByIpHost(String ipHost) {
        for (UserEntry e : userMap.values()) {
            if (extractHost(e.ip).equals(ipHost)) return e;
        }
        return null;
    }

    // ════════════════════════════════════
    //  内部类
    // ════════════════════════════════════

    private static class UserEntry {
        final String cid, name, ip;
        final boolean isMe;
        UserEntry(String c, String n, String ip, boolean me) { cid=c; name=n; this.ip=ip; isMe=me; }
    }
}
