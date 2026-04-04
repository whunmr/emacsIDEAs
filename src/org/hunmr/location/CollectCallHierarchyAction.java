package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.DataManager;
import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowserBaseEx;
import com.intellij.ide.hierarchy.HierarchyNodeDescriptor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentManager;
import org.hunmr.options.PluginConfig;

import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.lang.reflect.Method;

public class CollectCallHierarchyAction extends com.intellij.openapi.project.DumbAwareAction {
    private static final String CALL_HIERARCHY_SECTION = "[Call Hierarchy]";

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

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        CallHierarchyView currentView = findCurrentCallHierarchyView(project);
        if (currentView == null) {
            showMessage(editor, project, "Open Call Hierarchy first");
            return;
        }

        String relation = relationForView(currentView.viewType);
        if (relation.isEmpty()) {
            showMessage(editor, project, "Current Hierarchy view is not a Call Hierarchy caller/callee view");
            return;
        }

        JTree tree = currentView.tree;
        if (tree == null || tree.getRowCount() <= 1) {
            showMessage(editor, project, "Current Call Hierarchy view has no collected rows");
            return;
        }

        String existingEntries = CollectedPromptFormatter.withPromptHeader(
                CollectedOutputFileManager.getCurrentText(project),
                PluginConfig.getInstance()._promptHeader
        );
        StringBuilder collectedEntries = new StringBuilder();
        appendTreeRows(project, tree, relation, collectedEntries);

        if (collectedEntries.length() == 0) {
            showMessage(editor, project, "Current Call Hierarchy view has no visible rows to collect");
            return;
        }

        PsiElement hierarchyBase = getHierarchyBase(currentView.browser);
        CollectedLocationContext targetContext = resolveContext(project, hierarchyBase);
        String targetDescription = targetContext.hasSymbol()
                ? targetContext.getSymbolKind() + " `" + targetContext.getSymbolName() + "`"
                : describeTarget(hierarchyBase);
        String block = CollectedCallHierarchyFormatter.formatBlock(
                targetDescription,
                "caller".equals(relation) ? collectedEntries.toString() : "",
                "callee".equals(relation) ? collectedEntries.toString() : ""
        );
        String sectionEntry = CollectedCallHierarchyFormatter.formatSectionBlock(block);
        if (CollectedPromptFormatter.contextSectionContainsLine(existingEntries, CALL_HIERARCHY_SECTION, sectionEntry)) {
            showMessage(editor, project, "Already exists");
            return;
        }

        String updatedText = CollectedPromptFormatter.appendToContextSection(
                existingEntries,
                CALL_HIERARCHY_SECTION,
                CollectedCallHierarchyFormatter.formatSectionBlock(block)
        );
        try {
            VirtualFile outputFile = CollectedOutputFileManager.replaceAndOpen(project, updatedText);
            String path = outputFile == null ? "tmp output file" : outputFile.getPath();
            showMessage(editor, project, "Collected call hierarchy into " + path);
        } catch (IOException exception) {
            showMessage(editor, project, "Failed to write collected call hierarchy: " + exception.getMessage());
        }
    }

    private static void appendTreeRows(Project project,
                                       JTree tree,
                                       String relation,
                                       StringBuilder builder) {
        for (int row = 1; row < tree.getRowCount(); row++) {
            TreePath treePath = tree.getPathForRow(row);
            if (treePath == null) {
                continue;
            }

            PsiElement element = extractPsiElement(treePath.getLastPathComponent());
            if (element == null || !element.isValid()) {
                continue;
            }

            LocationInfo locationInfo = buildLocationInfo(project, element);
            if (locationInfo == null) {
                continue;
            }

            int depth = Math.max(1, treePath.getPathCount() - 1);
            builder.append(CollectedCallHierarchyFormatter.formatEntry(
                    relation + " depth-" + depth,
                    locationInfo.context,
                    locationInfo.absolutePath,
                    locationInfo.lineNumber
            )).append('\n');
        }
    }

    private static CallHierarchyView findCurrentCallHierarchyView(Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.HIERARCHY);
        if (toolWindow == null) {
            return null;
        }

        ContentManager contentManager = toolWindow.getContentManagerIfCreated();
        if (contentManager != null) {
            Content selectedContent = contentManager.getSelectedContent();
            CallHierarchyView selectedView = toCallHierarchyView(selectedContent);
            if (selectedView != null) {
                return selectedView;
            }

            Content[] contents = contentManager.getContents();
            for (int i = 0; i < contents.length; i++) {
                if (contents[i] == selectedContent) {
                    continue;
                }

                CallHierarchyView contentView = toCallHierarchyView(contents[i]);
                if (contentView != null) {
                    return contentView;
                }
            }
        }

        HierarchyBrowserBaseEx browser = findHierarchyBrowserInComponent(getToolWindowComponent(toolWindow));
        if (browser instanceof CallHierarchyBrowserBase) {
            JTree tree = getCurrentTree(browser);
            if (tree != null) {
                return new CallHierarchyView((CallHierarchyBrowserBase) browser, tree, getCurrentViewType(browser));
            }
        }

        return null;
    }

    private static CallHierarchyView toCallHierarchyView(Content content) {
        if (content == null) {
            return null;
        }

        HierarchyBrowserBaseEx browser = getHierarchyBrowser(content);
        if (!(browser instanceof CallHierarchyBrowserBase)) {
            return null;
        }

        JTree tree = getCurrentTree(browser);
        if (tree == null) {
            return null;
        }

        return new CallHierarchyView((CallHierarchyBrowserBase) browser, tree, getCurrentViewType(browser));
    }

    private static HierarchyBrowserBaseEx getHierarchyBrowser(Content content) {
        JComponent actionsContextComponent = content.getActionsContextComponent();
        HierarchyBrowserBaseEx browser = findHierarchyBrowserInComponent(actionsContextComponent);
        if (browser != null) {
            return browser;
        }

        return findHierarchyBrowserInComponent(content.getComponent());
    }

    private static HierarchyBrowserBaseEx findHierarchyBrowserInComponent(Component component) {
        if (component == null) {
            return null;
        }

        if (component instanceof HierarchyBrowserBaseEx) {
            return (HierarchyBrowserBaseEx) component;
        }

        HierarchyBrowserBaseEx browser = HierarchyBrowserBaseEx.HIERARCHY_BROWSER.getData(
                DataManager.getInstance().getDataContext(component)
        );
        if (browser != null) {
            return browser;
        }

        if (component instanceof Container) {
            Component[] children = ((Container) component).getComponents();
            for (int i = 0; i < children.length; i++) {
                HierarchyBrowserBaseEx nested = findHierarchyBrowserInComponent(children[i]);
                if (nested != null) {
                    return nested;
                }
            }
        }

        return null;
    }

    private static Component getToolWindowComponent(ToolWindow toolWindow) {
        Object value = invokeNoArgMethod(toolWindow, "getComponent");
        return value instanceof Component ? (Component) value : null;
    }

    private static JTree getCurrentTree(HierarchyBrowserBaseEx browser) {
        Object value = invokeNoArgMethod(browser, "getCurrentTree");
        return value instanceof JTree ? (JTree) value : null;
    }

    private static String getCurrentViewType(HierarchyBrowserBaseEx browser) {
        Object value = invokeNoArgMethod(browser, "getCurrentViewType");
        return value instanceof String ? (String) value : "";
    }

    private static PsiElement getHierarchyBase(HierarchyBrowserBaseEx browser) {
        Object value = invokeNoArgMethod(browser, "getHierarchyBase");
        return value instanceof PsiElement ? (PsiElement) value : null;
    }

    private static String relationForView(String viewType) {
        if (CallHierarchyBrowserBase.getCallerType().equals(viewType)) {
            return "caller";
        }
        if (CallHierarchyBrowserBase.getCalleeType().equals(viewType)) {
            return "callee";
        }
        return "";
    }

    private static PsiElement extractPsiElement(Object node) {
        if (node instanceof PsiElement) {
            return (PsiElement) node;
        }

        if (node instanceof HierarchyNodeDescriptor) {
            Object value = ((HierarchyNodeDescriptor) node).getElement();
            if (value instanceof PsiElement) {
                return (PsiElement) value;
            }
        }

        Object descriptor = invokeNoArgMethod(node, "getDescriptor");
        if (descriptor instanceof HierarchyNodeDescriptor) {
            Object value = ((HierarchyNodeDescriptor) descriptor).getElement();
            if (value instanceof PsiElement) {
                return (PsiElement) value;
            }
        }

        Object userObject = invokeNoArgMethod(node, "getUserObject");
        if (userObject instanceof HierarchyNodeDescriptor) {
            Object value = ((HierarchyNodeDescriptor) userObject).getElement();
            if (value instanceof PsiElement) {
                return (PsiElement) value;
            }
        }

        Object value = invokeNoArgMethod(node, "getElement");
        return value instanceof PsiElement ? (PsiElement) value : null;
    }

    private static CollectedLocationContext resolveContext(Project project, PsiElement element) {
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

    private static LocationInfo buildLocationInfo(Project project, PsiElement element) {
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        String absolutePath = getAbsolutePath(virtualFile);
        int lineNumber = 0;
        CollectedLocationContext context = CollectedLocationContext.EMPTY;
        if (document != null) {
            int safeOffset = Math.max(0, Math.min(element.getTextOffset(), Math.max(0, document.getTextLength() - 1)));
            lineNumber = document.getLineNumber(safeOffset) + 1;
            context = CollectedLocationContextResolver.resolve(project, document, safeOffset, safeOffset, false);
        }

        return new LocationInfo(absolutePath, lineNumber, context);
    }

    private static String getAbsolutePath(VirtualFile virtualFile) {
        String canonicalPath = virtualFile.getCanonicalPath();
        return canonicalPath != null && !canonicalPath.isEmpty() ? canonicalPath : virtualFile.getPath();
    }

    private static String describeTarget(PsiElement target) {
        if (target instanceof PsiNamedElement) {
            String name = ((PsiNamedElement) target).getName();
            if (name != null && !name.isEmpty()) {
                return '`' + name + '`';
            }
        }
        return "`<unknown>`";
    }

    private static void showMessage(Editor editor, Project project, String message) {
        if (editor != null && !editor.isDisposed()) {
            HintManager.getInstance().showInformationHint(editor, message);
            return;
        }
        if (project != null) {
            ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.HIERARCHY, MessageType.INFO, message);
        }
    }

    private static Object invokeNoArgMethod(Object target, String methodName) {
        if (target == null || methodName == null || methodName.isEmpty()) {
            return null;
        }

        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static final class LocationInfo {
        private final String absolutePath;
        private final int lineNumber;
        private final CollectedLocationContext context;

        private LocationInfo(String absolutePath, int lineNumber, CollectedLocationContext context) {
            this.absolutePath = absolutePath;
            this.lineNumber = lineNumber;
            this.context = context;
        }
    }

    private static final class CallHierarchyView {
        private final CallHierarchyBrowserBase browser;
        private final JTree tree;
        private final String viewType;

        private CallHierarchyView(CallHierarchyBrowserBase browser, JTree tree, String viewType) {
            this.browser = browser;
            this.tree = tree;
            this.viewType = viewType == null ? "" : viewType;
        }
    }
}
