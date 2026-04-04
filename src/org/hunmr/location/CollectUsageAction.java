package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import com.intellij.usages.ReadWriteAccessUsage;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.UsageViewManager;
import com.intellij.usages.UsageViewPresentation;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.UsageInFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.hunmr.options.PluginConfig;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectUsageAction extends com.intellij.openapi.project.DumbAwareAction {
    private static final String USAGES_SECTION = "[Usages]";

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
        String existingEntries = CollectedPromptFormatter.withPromptHeader(
                CollectedOutputFileManager.getCurrentText(project),
                PluginConfig.getInstance()._promptHeader
        );
        String nextLabel = CollectedUsageFormatter.nextLabel(existingEntries);
        Map<String, List<String>> groupedEntries = new LinkedHashMap<String, List<String>>();
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
            String usageCategory = classifyUsage(usage, info.context, info.lineContent);
            String formattedEntry = CollectedUsageFormatter.formatEntry(
                    label,
                    usageCategory,
                    info.lineContent,
                    info.context,
                    info.absolutePath,
                    info.lineNumber
            );
            addGroupedEntry(groupedEntries, usageCategory, formattedEntry);
            collectedCount++;
        }

        if (collectedCount == 0) {
            showMessage(e, project, "No active usages to collect");
            return;
        }

        String block = CollectedUsageFormatter.formatBlock(buildUsageBlockTitle(project, usageView), groupedEntries);
        String updatedText = CollectedPromptFormatter.appendToContextSection(
                existingEntries,
                USAGES_SECTION,
                CollectedUsageFormatter.formatSectionBlock(block)
        );
        writeOutput(project, e, updatedText, collectedCount);
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

        JComponent component = content.getComponent();
        return findUsageViewInComponent(component);
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
        Editor editor = e == null ? null : e.getData(CommonDataKeys.EDITOR);
        if (editor != null && !editor.isDisposed()) {
            HintManager.getInstance().showInformationHint(editor, message);
            return;
        }

        if (project != null) {
            ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.FIND, MessageType.INFO, message);
        }
    }

    private static void writeOutput(Project project, AnActionEvent e, String text, int collectedCount) {
        try {
            Editor editor = e == null ? null : e.getData(CommonDataKeys.EDITOR);
            VirtualFile outputFile = CollectedOutputFileManager.replaceAndOpen(project, text, editor);
            String path = outputFile == null ? "tmp output file" : outputFile.getPath();
            showMessage(e, project, "Collected " + collectedCount + " usages into " + path);
        } catch (java.io.IOException exception) {
            showMessage(e, project, "Failed to write collected usages: " + exception.getMessage());
        }
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

    private static Object invokeNoArgMethod(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isEmpty()) {
            return null;
        }

        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
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

    private static void addGroupedEntry(Map<String, List<String>> groupedEntries, String usageCategory, String formattedEntry) {
        String category = usageCategory == null || usageCategory.trim().isEmpty() ? "other" : usageCategory.trim();
        List<String> entries = groupedEntries.get(category);
        if (entries == null) {
            entries = new ArrayList<String>();
            groupedEntries.put(category, entries);
        }
        entries.add(formattedEntry);
    }

    private static String classifyUsage(Usage usage, CollectedLocationContext context, String lineContent) {
        if (usage instanceof ReadWriteAccessUsage) {
            ReadWriteAccessUsage accessUsage = (ReadWriteAccessUsage) usage;
            boolean read = accessUsage.isAccessedForReading();
            boolean write = accessUsage.isAccessedForWriting();
            if (read && write) {
                return "read-write";
            }
            if (write) {
                return "write";
            }
            if (read) {
                return "read";
            }
        }

        UsageType usageType = resolveUsageType(usage);
        if (usageType != null) {
            String usageTypeText = usageType.toString();
            if (usageTypeText != null && !usageTypeText.trim().isEmpty()) {
                return normalizeUsageCategory(usageTypeText);
            }
        }

        if (context != null && context.hasSymbol()) {
            String symbolName = context.getSymbolName();
            if (lineContent != null && symbolName != null && lineContent.contains(symbolName + "(")) {
                return "call";
            }
            if ("method".equals(context.getSymbolKind()) || "function".equals(context.getSymbolKind())) {
                return "reference";
            }
        }

        return "other";
    }

    private static UsageType resolveUsageType(Usage usage) {
        if (!(usage instanceof PsiElementUsage)) {
            return null;
        }

        PsiElement element = ((PsiElementUsage) usage).getElement();
        if (element == null) {
            return null;
        }

        for (UsageTypeProvider provider : UsageTypeProvider.EP_NAME.getExtensionList()) {
            UsageType usageType = provider.getUsageType(element);
            if (usageType != null) {
                return usageType;
            }
        }
        return null;
    }

    private static String normalizeUsageCategory(String text) {
        String normalized = text == null ? "" : text.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return "other";
        }
        normalized = normalized.replace(' ', '-');
        return normalized;
    }

    private static String buildUsageBlockTitle(Project project, UsageView usageView) {
        String targetDescription = resolveUsageTargetDescription(project, usageView);
        if (targetDescription.isEmpty()) {
            return "Usages";
        }
        return "Usages (" + targetDescription + ")";
    }

    private static String resolveUsageTargetDescription(Project project, UsageView usageView) {
        UsageViewPresentation presentation = usageView == null ? null : usageView.getPresentation();
        String presentationDescription = CollectedUsageFormatter.describeUsagePresentation(
                presentation == null ? null : presentation.getTargetsNodeText(),
                presentation == null ? null : presentation.getSearchString(),
                presentation == null ? null : presentation.getTabText(),
                presentation == null ? null : presentation.getTabName()
        );
        if (!presentationDescription.isEmpty()) {
            return presentationDescription;
        }

        UsageTarget[] usageTargets = getUsageTargets(usageView);
        if (usageTargets != null) {
            for (int i = 0; i < usageTargets.length; i++) {
                String description = describeUsageTarget(project, usageTargets[i]);
                if (!description.isEmpty()) {
                    return description;
                }
            }
        }

        if (presentation == null) {
            return "";
        }

        return firstNonEmptyPresentationText(
                presentation.getTargetsNodeText(),
                presentation.getTabText(),
                presentation.getTabName(),
                presentation.getSearchString()
        );
    }

    private static UsageTarget[] getUsageTargets(UsageView usageView) {
        if (usageView == null || usageView.getComponent() == null) {
            return null;
        }
        return UsageView.USAGE_TARGETS_KEY.getData(DataManager.getInstance().getDataContext(usageView.getComponent()));
    }

    private static String describeUsageTarget(Project project, UsageTarget usageTarget) {
        if (usageTarget == null || !usageTarget.isValid()) {
            return "";
        }

        if (usageTarget instanceof PsiElement) {
            PsiElement element = (PsiElement) usageTarget;
            String structured = describePsiElement(project, element);
            if (!structured.isEmpty()) {
                return structured;
            }
        }

        String name = usageTarget.getName();
        if (name != null && !name.trim().isEmpty()) {
            return "`" + name.trim() + "`";
        }

        return normalizeUsagePresentationText(String.valueOf(usageTarget));
    }

    private static String describePsiElement(Project project, PsiElement element) {
        if (element == null || !element.isValid()) {
            return "";
        }

        if (element instanceof PsiNamedElement) {
            String name = ((PsiNamedElement) element).getName();
            String kind = inferKind(element);
            if (name != null && !name.trim().isEmpty() && !kind.isEmpty()) {
                return kind + " `" + name.trim() + "`";
            }
            if (name != null && !name.trim().isEmpty()) {
                return "`" + name.trim() + "`";
            }
        }

        CollectedLocationContext context = resolveUsageTargetContext(project, element);
        if (context.hasSymbol()) {
            return context.getSymbolKind() + " `" + context.getSymbolName() + "`";
        }

        return "";
    }

    private static CollectedLocationContext resolveUsageTargetContext(Project project, PsiElement element) {
        if (project == null || element == null) {
            return CollectedLocationContext.EMPTY;
        }

        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        Document document = virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return CollectedLocationContext.EMPTY;
        }

        int offset = Math.max(0, element.getTextOffset());
        return CollectedLocationContextResolver.resolve(project, document, offset, offset, false);
    }

    private static String inferKind(PsiElement element) {
        String simpleName = element.getClass().getSimpleName().toLowerCase();
        if (simpleName.contains("method")) {
            return "method";
        }
        if (simpleName.contains("function")) {
            return "function";
        }
        if (simpleName.contains("class")) {
            return "class";
        }
        if (simpleName.contains("interface")) {
            return "interface";
        }
        if (simpleName.contains("struct")) {
            return "struct";
        }
        if (simpleName.contains("field")) {
            return "field";
        }
        if (simpleName.contains("type")) {
            return "type";
        }
        return "";
    }

    private static String normalizeUsagePresentationText(String text) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) {
            return "";
        }

        String normalized = safeText;
        normalized = normalized.replaceFirst("(?i)^find\\s+usages\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^usages\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^usage\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^methods?\\s+to\\s+", "");
        normalized = normalized.replaceFirst("(?i)^functions?\\s+to\\s+", "");
        normalized = normalized.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.indexOf('`') >= 0) {
            return normalized;
        }
        return normalized;
    }

    private static String firstNonEmptyPresentationText(String... candidates) {
        if (candidates == null) {
            return "";
        }

        for (int i = 0; i < candidates.length; i++) {
            String normalized = normalizeUsagePresentationText(candidates[i]);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
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
