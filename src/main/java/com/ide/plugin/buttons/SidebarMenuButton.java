package com.ide.plugin.buttons;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;

public class SidebarMenuButton extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        // 获取我们创建的ToolWindow
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("LanPartnerWindow");
        if (toolWindow != null) {
            toolWindow.show(); // 打开右侧面板
        }
    }
}