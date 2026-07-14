package com.ide.plugin.factory;

import com.ide.plugin.ui.ChatPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class MyToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        ChatPanel chatPanel = new ChatPanel();

        Content content = toolWindow.getContentManager().getFactory()
                .createContent(chatPanel, "聊天", false);

        // 关闭工具窗口时自动断开连接、清理资源
        content.setDisposer(chatPanel);

        toolWindow.getContentManager().addContent(content);

        // 窗口关闭后不保留空白内容
        toolWindow.setToHideOnEmptyContent(false);

        // 注入项目上下文 + 工具窗口引用，用于监听可见性 & 未读消息通知
        chatPanel.initToolWindow(project, toolWindow);
    }

    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        return true;
    }
}
