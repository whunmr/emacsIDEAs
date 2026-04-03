package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.ide.hierarchy.CallHierarchyBrowserBase;
import com.intellij.ide.hierarchy.HierarchyBrowser;
import com.intellij.ide.hierarchy.HierarchyProvider;
import com.intellij.ide.hierarchy.HierarchyTreeStructure;
import com.intellij.ide.hierarchy.LanguageCallHierarchy;
import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;
import org.hunmr.options.PluginConfig;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class CollectCallHierarchyAction extends com.intellij.openapi.project.DumbAwareAction {
    private static final int MAX_COLLECTED_ITEMS = 40;

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null && e.getData(CommonDataKeys.EDITOR) != null);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (project == null || editor == null || editor.isDisposed()) {
            return;
        }

        PsiElement target = findTargetElement(project, editor, e);
        if (target == null) {
            showMessage(editor, project, "No call hierarchy target at caret");
            return;
        }

        HierarchyProvider provider = LanguageCallHierarchy.INSTANCE.forLanguage(resolveLanguage(target));
        if (provider == null) {
            showMessage(editor, project, "Call hierarchy is not available for this language");
            return;
        }

        HierarchyBrowser browser = provider.createHierarchyBrowser(target);
        if (browser == null) {
            showMessage(editor, project, "Failed to create call hierarchy browser");
            return;
        }
        provider.browserActivated(browser);

        HierarchyTreeStructure incomingTree = createHierarchyTreeStructure(browser, CallHierarchyBrowserBase.getCallerType(), target);
        HierarchyTreeStructure outgoingTree = createHierarchyTreeStructure(browser, CallHierarchyBrowserBase.getCalleeType(), target);
        if (incomingTree == null && outgoingTree == null) {
            showMessage(editor, project, "No call hierarchy data available");
            return;
        }

        String existingEntries = CollectedOutputFileManager.getCurrentText(project);
        String nextLabel = CollectedCallHierarchyFormatter.nextLabel(existingEntries);
        int nextIndex = decodeLabel(nextLabel);
        int configuredDepth = Math.max(1, PluginConfig.getInstance()._collectCallHierarchyDepth);
        StringBuilder incomingEntries = new StringBuilder();
        nextIndex = appendHierarchyEntries(project, incomingTree, "caller", configuredDepth, nextIndex, incomingEntries);
        StringBuilder outgoingEntries = new StringBuilder();
        nextIndex = appendHierarchyEntries(project, outgoingTree, "callee", configuredDepth, nextIndex, outgoingEntries);

        if (incomingEntries.length() == 0 && outgoingEntries.length() == 0) {
            showMessage(editor, project, "No call hierarchy items to collect");
            return;
        }

        CollectedLocationContext targetContext = resolveContext(project, target);
        String targetDescription = targetContext.hasSymbol()
                ? targetContext.getSymbolKind() + " `" + targetContext.getSymbolName() + "`"
                : describeTarget(target);
        String block = CollectedCallHierarchyFormatter.formatBlock(
                targetDescription,
                incomingEntries.toString(),
                outgoingEntries.toString()
        );
        String updatedText = CollectedPromptFormatter.appendToContext(existingEntries, block);
        try {
            VirtualFile outputFile = CollectedOutputFileManager.replaceAndOpen(project, updatedText);
            String path = outputFile == null ? "tmp output file" : outputFile.getPath();
            showMessage(editor, project, "Collected call hierarchy into " + path);
        } catch (IOException exception) {
            showMessage(editor, project, "Failed to write collected call hierarchy: " + exception.getMessage());
        }
    }

    private static int appendHierarchyEntries(Project project,
                                              HierarchyTreeStructure treeStructure,
                                              String relation,
                                              int maxDepth,
                                              int nextIndex,
                                              StringBuilder builder) {
        if (treeStructure == null) {
            return nextIndex;
        }

        Object root = treeStructure.getRootElement();
        if (root == null) {
            return nextIndex;
        }

        TraversalState state = new TraversalState(nextIndex);
        Set<String> visited = new HashSet<String>();
        appendHierarchyEntries(project, treeStructure, root, relation, 1, maxDepth, state, builder, visited);
        return state.nextIndex;
    }

    private static void appendHierarchyEntries(Project project,
                                               HierarchyTreeStructure treeStructure,
                                               Object parentNode,
                                               String relation,
                                               int depth,
                                               int maxDepth,
                                               TraversalState state,
                                               StringBuilder builder,
                                               Set<String> visited) {
        if (treeStructure == null || parentNode == null || depth > maxDepth || state.remaining <= 0) {
            return;
        }

        Object[] children = treeStructure.getChildElements(parentNode);
        if (children == null) {
            return;
        }

        for (int i = 0; i < children.length && state.remaining > 0; i++) {
            Object child = children[i];
            PsiElement element = extractPsiElement(child);
            if (element == null || !element.isValid()) {
                continue;
            }

            String visitKey = buildVisitKey(element, relation);
            if (!visited.add(visitKey)) {
                continue;
            }

            LocationInfo locationInfo = buildLocationInfo(project, element);
            if (locationInfo == null) {
                continue;
            }

            String label = encodeLabel(state.nextIndex++);
            builder.append(CollectedCallHierarchyFormatter.formatEntry(
                    label,
                    relation + " depth-" + depth,
                    locationInfo.context,
                    locationInfo.absolutePath,
                    locationInfo.lineNumber
            )).append('\n');
            state.remaining--;

            appendHierarchyEntries(project, treeStructure, child, relation, depth + 1, maxDepth, state, builder, visited);
        }
    }

    private static HierarchyTreeStructure createHierarchyTreeStructure(HierarchyBrowser browser, String viewType, PsiElement target) {
        if (browser == null || viewType == null || target == null) {
            return null;
        }

        try {
            Method method = findMethod(browser.getClass(), "createHierarchyTreeStructure", String.class, PsiElement.class);
            if (method == null) {
                return null;
            }
            method.setAccessible(true);
            Object value = method.invoke(browser, viewType, target);
            if (value instanceof HierarchyTreeStructure) {
                return (HierarchyTreeStructure) value;
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private static Method findMethod(Class<?> type, String name, Class<?> firstArg, Class<?> secondArg) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, firstArg, secondArg);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static PsiElement extractPsiElement(Object node) {
        if (node instanceof PsiElement) {
            return (PsiElement) node;
        }

        try {
            Method method = node.getClass().getMethod("getElement");
            Object value = method.invoke(node);
            if (value instanceof PsiElement) {
                return (PsiElement) value;
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private static PsiElement findTargetElement(Project project, Editor editor, AnActionEvent e) {
        PsiElement directTarget = resolveTargetFromProvider(project, e);
        if (directTarget != null) {
            return directTarget;
        }

        Document document = editor.getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.commitDocument(document);
        PsiFile psiFile = documentManager.getPsiFile(document);
        if (psiFile == null || psiFile.getTextLength() <= 0) {
            return null;
        }

        int offset = Math.max(0, Math.min(editor.getCaretModel().getOffset(), psiFile.getTextLength() - 1));
        PsiElement element = psiFile.findElementAt(offset);
        if (element == null && offset > 0) {
            element = psiFile.findElementAt(offset - 1);
        }

        PsiElement current = element;
        while (current != null) {
            if (current instanceof PsiNamedElement) {
                return current;
            }
            current = current.getParent();
        }

        return null;
    }

    private static PsiElement resolveTargetFromProvider(Project project, AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null || editor.isDisposed()) {
            return null;
        }

        Document document = editor.getDocument();
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.commitDocument(document);
        PsiFile psiFile = documentManager.getPsiFile(document);
        if (psiFile == null) {
            return null;
        }

        HierarchyProvider provider = LanguageCallHierarchy.INSTANCE.forLanguage(psiFile.getLanguage());
        if (provider == null) {
            return null;
        }

        return provider.getTarget(e.getDataContext());
    }

    private static Language resolveLanguage(PsiElement target) {
        PsiFile containingFile = target == null ? null : target.getContainingFile();
        if (containingFile != null) {
            return containingFile.getLanguage();
        }
        return target == null ? Language.ANY : target.getLanguage();
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
        if (canonicalPath != null && !canonicalPath.isEmpty()) {
            return canonicalPath;
        }
        return virtualFile.getPath();
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
        if (editor != null) {
            HintManager.getInstance().showInformationHint(editor, message);
            return;
        }
        Messages.showInfoMessage(project, message, "emacsJump");
    }

    private static int decodeLabel(String label) {
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

    private static String encodeLabel(int index) {
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

    private static String buildVisitKey(PsiElement element, String relation) {
        PsiFile file = element.getContainingFile();
        VirtualFile virtualFile = file == null ? null : file.getVirtualFile();
        String path = virtualFile == null ? "" : getAbsolutePath(virtualFile);
        return relation + ":" + path + ":" + element.getTextOffset();
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

    private static final class TraversalState {
        private int nextIndex;
        private int remaining = MAX_COLLECTED_ITEMS;

        private TraversalState(int nextIndex) {
            this.nextIndex = nextIndex;
        }
    }
}
