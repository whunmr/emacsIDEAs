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
import com.intellij.psi.SmartPsiElementPointer;
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
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

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
        StringBuilder debugInfo = new StringBuilder();
        appendTreeRows(project, tree, relation, collectedEntries, debugInfo);

        if (collectedEntries.length() == 0) {
            showMessage(editor, project, "Current Call Hierarchy view has no visible rows to collect; " + debugInfo.toString());
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
                                       StringBuilder builder,
                                       StringBuilder debugInfo) {
        for (int row = 1; row < tree.getRowCount(); row++) {
            TreePath treePath = tree.getPathForRow(row);
            if (treePath == null) {
                appendDebugInfo(debugInfo, "r" + row + ":path=null");
                continue;
            }

            Object rowNode = treePath.getLastPathComponent();
            HierarchyNodeDescriptor descriptor = extractHierarchyDescriptor(rowNode);
            PsiElement element = extractPsiElement(rowNode);
            LocationInfo locationInfo = buildLocationInfo(project, element, descriptor);
            appendDebugInfo(debugInfo, buildRowDebug(row, rowNode, descriptor, element, locationInfo));
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

    private static void appendDebugInfo(StringBuilder debugInfo, String entry) {
        if (debugInfo == null || entry == null || entry.isEmpty()) {
            return;
        }
        int existingCount = 0;
        for (int i = 0; i < debugInfo.length(); i++) {
            if (debugInfo.charAt(i) == ';') {
                existingCount++;
            }
        }
        if (existingCount >= 3) {
            return;
        }
        if (debugInfo.length() > 0) {
            debugInfo.append("; ");
        }
        debugInfo.append(entry);
    }

    private static String buildRowDebug(int row,
                                        Object rowNode,
                                        HierarchyNodeDescriptor descriptor,
                                        PsiElement element,
                                        LocationInfo locationInfo) {
        PsiElement descriptorElement = descriptor == null ? null : coercePsiElement(descriptor.getElement());
        PsiElement normalizedElement = normalizeLocationElement(element);
        PsiElement normalizedDescriptorElement = normalizeLocationElement(descriptorElement);
        PsiFile descriptorFile = descriptor == null ? null : descriptor.getContainingFile();

        StringBuilder builder = new StringBuilder();
        builder.append("r").append(row)
                .append("[n=").append(simpleName(rowNode))
                .append(",d=").append(simpleName(descriptor))
                .append(",e=").append(simpleName(element))
                .append(",ne=").append(simpleName(normalizedElement))
                .append(",de=").append(simpleName(descriptorElement))
                .append(",nde=").append(simpleName(normalizedDescriptorElement))
                .append(",df=").append(descriptorFile != null ? "Y" : "N")
                .append(",loc=").append(locationInfo != null ? "Y" : "N")
                .append("]");
        return builder.toString();
    }

    private static String simpleName(Object value) {
        if (value == null) {
            return "-";
        }
        Class<?> type = value instanceof Class ? (Class<?>) value : value.getClass();
        String simpleName = type.getSimpleName();
        return simpleName == null || simpleName.isEmpty() ? type.getName() : simpleName;
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
        PsiElement direct = coercePsiElement(node);
        if (direct != null) {
            return direct;
        }

        if (node instanceof HierarchyNodeDescriptor) {
            PsiElement descriptorElement = coercePsiElement(((HierarchyNodeDescriptor) node).getElement());
            if (descriptorElement != null) {
                return descriptorElement;
            }
        }

        Object descriptor = invokeNoArgMethod(node, "getDescriptor");
        PsiElement descriptorElement = coercePsiElement(descriptor);
        if (descriptorElement != null) {
            return descriptorElement;
        }
        if (descriptor instanceof HierarchyNodeDescriptor) {
            PsiElement hierarchyDescriptorElement = coercePsiElement(((HierarchyNodeDescriptor) descriptor).getElement());
            if (hierarchyDescriptorElement != null) {
                return hierarchyDescriptorElement;
            }
        }

        Object userObject = invokeNoArgMethod(node, "getUserObject");
        PsiElement userObjectElement = coercePsiElement(userObject);
        if (userObjectElement != null) {
            return userObjectElement;
        }
        if (userObject instanceof HierarchyNodeDescriptor) {
            PsiElement hierarchyUserObjectElement = coercePsiElement(((HierarchyNodeDescriptor) userObject).getElement());
            if (hierarchyUserObjectElement != null) {
                return hierarchyUserObjectElement;
            }
        }

        return coercePsiElement(invokeNoArgMethod(node, "getElement"));
    }

    private static HierarchyNodeDescriptor extractHierarchyDescriptor(Object node) {
        if (node instanceof HierarchyNodeDescriptor) {
            return (HierarchyNodeDescriptor) node;
        }

        Object descriptor = invokeNoArgMethod(node, "getDescriptor");
        if (descriptor instanceof HierarchyNodeDescriptor) {
            return (HierarchyNodeDescriptor) descriptor;
        }

        Object userObject = invokeNoArgMethod(node, "getUserObject");
        if (userObject instanceof HierarchyNodeDescriptor) {
            return (HierarchyNodeDescriptor) userObject;
        }

        return null;
    }

    private static CollectedLocationContext resolveContext(Project project, PsiElement element) {
        if (project == null || element == null) {
            return CollectedLocationContext.EMPTY;
        }

        PsiElement locationElement = normalizeLocationElement(element);
        PsiFile file = element.getContainingFile();
        if (locationElement != null) {
            file = locationElement.getContainingFile();
        }
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        Document document = virtualFile == null ? null : FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null) {
            return CollectedLocationContext.EMPTY;
        }

        int offset = Math.max(0, locationElement == null ? element.getTextOffset() : locationElement.getTextOffset());
        return CollectedLocationContextResolver.resolve(project, document, offset, offset, false);
    }

    private static LocationInfo buildLocationInfo(Project project, PsiElement element, HierarchyNodeDescriptor descriptor) {
        PsiElement locationElement = normalizeLocationElement(element);
        if (locationElement == null && descriptor != null) {
            locationElement = normalizeLocationElement(coercePsiElement(descriptor.getElement()));
        }

        PsiFile file = locationElement == null ? null : locationElement.getContainingFile();
        if (file == null && descriptor != null) {
            file = descriptor.getContainingFile();
        }
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        if (virtualFile == null) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        String absolutePath = getAbsolutePath(virtualFile);
        int lineNumber = 0;
        CollectedLocationContext context = CollectedLocationContext.EMPTY;
        if (document != null) {
            if (locationElement != null) {
                int safeOffset = Math.max(0, Math.min(locationElement.getTextOffset(), Math.max(0, document.getTextLength() - 1)));
                lineNumber = document.getLineNumber(safeOffset) + 1;
                context = CollectedLocationContextResolver.resolve(project, document, safeOffset, safeOffset, false);
            } else if (document.getLineCount() > 0) {
                lineNumber = 1;
            }
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

    private static PsiElement normalizeLocationElement(PsiElement element) {
        PsiElement candidate = coercePsiElement(element);
        if (hasPhysicalLocation(candidate)) {
            return candidate;
        }

        PsiElement navigationElement = candidate == null ? null : coercePsiElement(candidate.getNavigationElement());
        if (hasPhysicalLocation(navigationElement)) {
            return navigationElement;
        }

        PsiElement originalElement = candidate == null ? null : coercePsiElement(candidate.getOriginalElement());
        if (hasPhysicalLocation(originalElement)) {
            return originalElement;
        }

        if (candidate != null && candidate.isValid()) {
            return candidate;
        }
        if (navigationElement != null && navigationElement.isValid()) {
            return navigationElement;
        }
        if (originalElement != null && originalElement.isValid()) {
            return originalElement;
        }
        return null;
    }

    private static boolean hasPhysicalLocation(PsiElement element) {
        if (element == null || !element.isValid()) {
            return false;
        }
        PsiFile file = element.getContainingFile();
        return file != null && file.getVirtualFile() != null;
    }

    private static PsiElement coercePsiElement(Object value) {
        return coercePsiElement(value, new IdentityHashMap<Object, Boolean>());
    }

    private static PsiElement coercePsiElement(Object value, Map<Object, Boolean> visited) {
        if (value == null) {
            return null;
        }
        if (visited.containsKey(value)) {
            return null;
        }
        visited.put(value, Boolean.TRUE);

        if (value instanceof PsiElement) {
            return (PsiElement) value;
        }
        if (value instanceof SmartPsiElementPointer) {
            Object pointerElement = ((SmartPsiElementPointer<?>) value).getElement();
            return coercePsiElement(pointerElement, visited);
        }
        if (value instanceof HierarchyNodeDescriptor) {
            return coercePsiElement(((HierarchyNodeDescriptor) value).getElement(), visited);
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                PsiElement element = coercePsiElement(item, visited);
                if (element != null) {
                    return element;
                }
            }
        }
        if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            for (int i = 0; i < array.length; i++) {
                PsiElement element = coercePsiElement(array[i], visited);
                if (element != null) {
                    return element;
                }
            }
        }

        String[] accessorNames = {"getElement", "getPsiElement", "getTargetElement", "getNavigationElement", "getOriginalElement"};
        for (int i = 0; i < accessorNames.length; i++) {
            Object nested = invokeNoArgMethod(value, accessorNames[i]);
            if (nested == value) {
                continue;
            }
            PsiElement element = coercePsiElement(nested, visited);
            if (element != null) {
                return element;
            }
        }

        return null;
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
            Method method = findNoArgMethod(target.getClass(), methodName);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
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
