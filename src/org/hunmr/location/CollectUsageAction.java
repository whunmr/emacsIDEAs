package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.UsageInFile;
import org.hunmr.util.ClipboardEditorUtil;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class CollectUsageAction extends DumbAwareAction {
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        UsageView usageView = findUsageView(project, e);
        if (usageView == null) {
            showMessage(e, project, "No Find Usages result window found");
            return;
        }

        List<Usage> sortedUsages = usageView.getSortedUsages();
        Set<Usage> excludedUsages = usageView.getExcludedUsages();
        String clipboardText = ClipboardEditorUtil.getClipboardText();
        String nextLabel = CollectedUsageFormatter.nextLabel(clipboardText);
        StringBuilder builder = new StringBuilder();
        int nextIndex = decodeUsageLabel(nextLabel);
        int collectedCount = 0;

        for (int i = 0; i < sortedUsages.size(); i++) {
            Usage usage = sortedUsages.get(i);
            if (usage == null || !usage.isValid() || excludedUsages.contains(usage)) {
                continue;
            }

            UsageLineInfo info = collectUsageLineInfo(project, usage);
            if (info == null) {
                continue;
            }

            String label = encodeUsageLabel(nextIndex++);
            builder.append(CollectedUsageFormatter.formatEntry(
                    label,
                    info.lineContent,
                    info.context,
                    info.absolutePath,
                    info.lineNumber
            )).append('\n');
            collectedCount++;
        }

        if (collectedCount == 0) {
            showMessage(e, project, "No active usages to collect");
            return;
        }

        ClipboardEditorUtil.copyToClipboard(CollectedLocationFormatter.appendEntry(clipboardText, builder.toString()));
        showMessage(e, project, "Collected " + collectedCount + " usages to clipboard");
    }

    private static UsageView findUsageView(Project project, AnActionEvent e) {
        UsageView currentUsageView = UsageView.USAGE_VIEW_KEY.getData(e.getDataContext());
        if (currentUsageView != null) {
            return currentUsageView;
        }

        UsageView selectedUsageView = UsageViewManager.getInstance(project).getSelectedUsageView();
        if (selectedUsageView != null) {
            return selectedUsageView;
        }

        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.FIND);
        if (toolWindow != null) {
            ContentManager contentManager = toolWindow.getContentManagerIfCreated();
            if (contentManager != null) {
                UsageView selected = getUsageViewFromContent(contentManager.getSelectedContent());
                if (selected != null) {
                    return selected;
                }

                Content[] contents = contentManager.getContents();
                for (int i = contents.length - 1; i >= 0; i--) {
                    UsageView usageView = getUsageViewFromContent(contents[i]);
                    if (usageView != null) {
                        return usageView;
                    }
                }
            }
        }

        return UsageViewManager.getInstance(project).getSelectedUsageView();
    }

    private static UsageView getUsageViewFromContent(Content content) {
        if (content == null) {
            return null;
        }

        JComponent actionsContextComponent = content.getActionsContextComponent();
        UsageView actionUsageView = findUsageViewInComponent(actionsContextComponent);
        if (actionUsageView != null) {
            return actionUsageView;
        }

        return findUsageViewInComponent(content.getComponent());
    }

    private static UsageView findUsageViewInComponent(Component component) {
        if (component == null) {
            return null;
        }

        UsageView directUsageView = UsageView.USAGE_VIEW_KEY.getData(DataManager.getInstance().getDataContext(component));
        if (directUsageView != null) {
            return directUsageView;
        }

        if (component instanceof Container) {
            Component[] children = ((Container) component).getComponents();
            for (int i = 0; i < children.length; i++) {
                UsageView usageView = findUsageViewInComponent(children[i]);
                if (usageView != null) {
                    return usageView;
                }
            }
        }

        return null;
    }

    private static UsageLineInfo collectUsageLineInfo(Project project, Usage usage) {
        VirtualFile virtualFile = getVirtualFile(usage);
        if (virtualFile == null || !virtualFile.isValid()) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        int navigationOffset = Math.max(0, getNavigationOffset(usage));
        int lineNumber = getLineNumber(usage, document, navigationOffset);
        String absolutePath = getAbsolutePath(virtualFile);
        String lineContent = getLineContent(usage, document, navigationOffset, lineNumber);
        CollectedLocationContext context = CollectedLocationContextResolver.resolve(
                project,
                document,
                navigationOffset,
                navigationOffset,
                false
        );

        return new UsageLineInfo(lineContent, context, absolutePath, lineNumber);
    }

    private static VirtualFile getVirtualFile(Usage usage) {
        if (usage instanceof UsageInFile) {
            return ((UsageInFile) usage).getFile();
        }
        if (usage instanceof UsageInfo2UsageAdapter) {
            return ((UsageInfo2UsageAdapter) usage).getFile();
        }
        return null;
    }

    private static int getNavigationOffset(Usage usage) {
        int reflectedOffset = invokeIntMethod(usage, "getNavigationOffset");
        if (reflectedOffset >= 0) {
            return reflectedOffset;
        }

        if (usage instanceof PsiElementUsage) {
            PsiElement element = ((PsiElementUsage) usage).getElement();
            if (element != null) {
                return Math.max(0, element.getTextOffset());
            }
        }

        return 0;
    }

    private static int getLineNumber(Usage usage, Document document, int navigationOffset) {
        if (document == null) {
            if (usage instanceof com.intellij.usages.UsageInfoAdapter) {
                return ((com.intellij.usages.UsageInfoAdapter) usage).getLine() + 1;
            }
            return 0;
        }

        if (document.getTextLength() <= 0) {
            return 1;
        }

        int safeOffset = Math.max(0, Math.min(navigationOffset, document.getTextLength() - 1));
        return document.getLineNumber(safeOffset) + 1;
    }

    private static String getLineContent(Usage usage, Document document, int navigationOffset, int lineNumber) {
        if (document != null && lineNumber > 0) {
            int lineIndex = lineNumber - 1;
            if (lineIndex >= 0 && lineIndex < document.getLineCount()) {
                int startOffset = document.getLineStartOffset(lineIndex);
                int endOffset = document.getLineEndOffset(lineIndex);
                return document.getText(new TextRange(startOffset, endOffset)).trim();
            }

            if (document.getTextLength() > 0) {
                int safeOffset = Math.max(0, Math.min(navigationOffset, document.getTextLength() - 1));
                int currentLine = document.getLineNumber(safeOffset);
                int startOffset = document.getLineStartOffset(currentLine);
                int endOffset = document.getLineEndOffset(currentLine);
                return document.getText(new TextRange(startOffset, endOffset)).trim();
            }
        }

        String plainText = usage.getPresentation() == null ? "" : usage.getPresentation().getPlainText();
        return plainText == null ? "" : plainText.trim();
    }

    private static String getAbsolutePath(VirtualFile virtualFile) {
        String canonicalPath = virtualFile.getCanonicalPath();
        if (canonicalPath != null && !canonicalPath.isEmpty()) {
            return canonicalPath;
        }
        return virtualFile.getPath();
    }

    private static void showMessage(AnActionEvent e, Project project, String message) {
        if (e != null && e.getData(CommonDataKeys.EDITOR) != null) {
            HintManager.getInstance().showInformationHint(e.getData(CommonDataKeys.EDITOR), message);
            return;
        }

        Messages.showInfoMessage(project, message, "emacsJump");
    }

    private static int invokeIntMethod(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isEmpty()) {
            return -1;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            Object value = method.invoke(target);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (Exception ignored) {
            return -1;
        }

        return -1;
    }

    private static int decodeUsageLabel(String label) {
        if (label == null || label.isEmpty()) {
            return 0;
        }

        int value = 0;
        for (int i = 0; i < label.length(); i++) {
            char current = label.charAt(i);
            if (current < 'a' || current > 'z') {
                return 0;
            }
            value = value * 26 + (current - 'a' + 1);
        }
        return value - 1;
    }

    private static String encodeUsageLabel(int index) {
        int safeIndex = Math.max(0, index);
        StringBuilder builder = new StringBuilder();
        int current = safeIndex;
        do {
            builder.insert(0, (char) ('a' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);

        while (builder.length() < 2) {
            builder.insert(0, 'a');
        }
        return builder.toString();
    }

    private static final class UsageLineInfo {
        private final String lineContent;
        private final CollectedLocationContext context;
        private final String absolutePath;
        private final int lineNumber;

        private UsageLineInfo(String lineContent,
                              CollectedLocationContext context,
                              String absolutePath,
                              int lineNumber) {
            this.lineContent = lineContent;
            this.context = context;
            this.absolutePath = absolutePath;
            this.lineNumber = lineNumber;
        }
    }
}
