package org.hunmr.location;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiNamedElement;

import java.lang.reflect.Method;
import java.util.Locale;

public final class CollectedLocationContextResolver {
    private CollectedLocationContextResolver() {
    }

    public static CollectedLocationContext resolve(Project project,
                                                   Editor editor,
                                                   int rangeStart,
                                                   int rangeEnd,
                                                   boolean hasSelection) {
        if (project == null || editor == null || editor.isDisposed()) {
            return CollectedLocationContext.EMPTY;
        }

        return resolve(project, editor.getDocument(), hasSelection ? rangeStart : editor.getCaretModel().getOffset(), rangeEnd, hasSelection);
    }

    public static CollectedLocationContext resolve(Project project,
                                                   Document document,
                                                   int rangeStart,
                                                   int rangeEnd,
                                                   boolean hasSelection) {
        if (project == null || document == null) {
            return CollectedLocationContext.EMPTY;
        }

        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        documentManager.commitDocument(document);
        PsiFile psiFile = documentManager.getPsiFile(document);
        if (psiFile == null || psiFile.getTextLength() <= 0) {
            return CollectedLocationContext.EMPTY;
        }

        PsiElement anchor = findAnchorElement(psiFile, rangeStart);
        if (anchor == null) {
            return CollectedLocationContext.EMPTY;
        }

        SymbolInfo symbol = findNearestSymbol(anchor);
        if (symbol == null && hasSelection && rangeEnd > rangeStart) {
            symbol = findNearestSymbol(findAnchorElement(psiFile, Math.max(rangeStart, rangeEnd - 1)));
        }
        if (symbol == null) {
            return CollectedLocationContext.EMPTY;
        }

        SymbolInfo container = findBestContainer(symbol);
        if (container == null) {
            return new CollectedLocationContext(symbol.kind, symbol.name, "", "");
        }

        return new CollectedLocationContext(symbol.kind, symbol.name, container.kind, container.name);
    }

    private static PsiElement findAnchorElement(PsiFile psiFile, int offset) {
        int textLength = psiFile.getTextLength();
        if (textLength <= 0) {
            return null;
        }

        int safeOffset = Math.max(0, Math.min(offset, textLength - 1));
        PsiElement element = psiFile.findElementAt(safeOffset);
        if (element == null && safeOffset > 0) {
            element = psiFile.findElementAt(safeOffset - 1);
        }
        return element;
    }

    private static SymbolInfo findNearestSymbol(PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            SymbolInfo info = toSymbolInfo(current);
            if (info != null) {
                return info;
            }
            current = current.getParent();
        }
        return null;
    }

    private static SymbolInfo findContainerSymbol(PsiElement element) {
        PsiElement current = element;
        while (current != null) {
            SymbolInfo info = toSymbolInfo(current);
            if (info != null) {
                return info;
            }
            current = current.getParent();
        }
        return null;
    }

    private static SymbolInfo findBestContainer(SymbolInfo symbol) {
        if ("method".equals(symbol.kind)) {
            SymbolInfo receiverContainer = findReceiverContainer(symbol.element);
            if (receiverContainer != null) {
                return receiverContainer;
            }
        }

        SymbolInfo parentContainer = findContainerSymbol(symbol.element.getParent());
        if (parentContainer != null) {
            return parentContainer;
        }

        return findPackageContainer(symbol.element.getContainingFile());
    }

    private static SymbolInfo toSymbolInfo(PsiElement element) {
        if (!(element instanceof PsiNamedElement)) {
            return null;
        }

        String kind = inferKind(element);
        if (kind.isEmpty()) {
            return null;
        }

        String name = ((PsiNamedElement) element).getName();
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        return new SymbolInfo(element, kind, name.trim());
    }

    private static String inferKind(PsiElement element) {
        String simpleName = element.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (simpleName.isEmpty()) {
            return "";
        }

        if (simpleName.contains("constructor")) {
            return "constructor";
        }
        if (simpleName.contains("method")) {
            return "method";
        }
        if (simpleName.contains("function")) {
            return "function";
        }
        if (simpleName.contains("annotation")) {
            return "annotation";
        }
        if (simpleName.contains("interface")) {
            return "interface";
        }
        if (simpleName.contains("struct")) {
            return "struct";
        }
        if (simpleName.contains("record")) {
            return "record";
        }
        if (simpleName.contains("class")) {
            return "class";
        }
        if (simpleName.contains("enum")) {
            return "enum";
        }
        if (simpleName.contains("module")) {
            return "module";
        }
        if (simpleName.contains("package")) {
            return "package";
        }
        if (simpleName.contains("namespace")) {
            return "namespace";
        }
        if (simpleName.contains("attribute")) {
            return "attribute";
        }
        if (simpleName.contains("tag")) {
            return "tag";
        }
        if (simpleName.contains("typedef") || simpleName.contains("type")) {
            return "type";
        }
        if (simpleName.contains("property")) {
            return "property";
        }
        if (simpleName.contains("field")) {
            return "field";
        }
        if (simpleName.contains("parameter") || simpleName.contains("param")) {
            return "parameter";
        }
        if (simpleName.contains("receiver")) {
            return "receiver";
        }
        if (simpleName.contains("constant") || simpleName.contains("const")) {
            return "constant";
        }
        if (simpleName.contains("variable") || simpleName.contains("var")) {
            return "variable";
        }
        if (simpleName.contains("file")) {
            return "file";
        }
        return "";
    }

    private static SymbolInfo findReceiverContainer(PsiElement element) {
        String[] receiverMethodNames = {
                "getReceiverType",
                "getReceiverTypeReference",
                "getReceiverTypeRef",
                "getReceiver",
                "getReceiverIdentifier"
        };
        for (int i = 0; i < receiverMethodNames.length; i++) {
            String name = extractName(invokeNoArgMethod(element, receiverMethodNames[i]));
            if (!name.isEmpty()) {
                return new SymbolInfo(element, "type", name);
            }
        }
        return null;
    }

    private static SymbolInfo findPackageContainer(PsiFile psiFile) {
        if (psiFile == null) {
            return null;
        }

        String packageName = extractName(invokeNoArgMethod(psiFile, "getPackageName"));
        if (!packageName.isEmpty()) {
            return new SymbolInfo(psiFile, "package", packageName);
        }

        Object packageClause = invokeNoArgMethod(psiFile, "getPackageClause");
        String clauseName = extractName(packageClause);
        if (!clauseName.isEmpty()) {
            return new SymbolInfo(psiFile, "package", clauseName);
        }

        return null;
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

    private static String extractName(Object value) {
        if (value == null) {
            return "";
        }

        if (value instanceof PsiNamedElement) {
            String name = ((PsiNamedElement) value).getName();
            return name == null ? "" : name.trim();
        }
        if (value instanceof String) {
            return ((String) value).trim();
        }
        if (value instanceof PsiElement) {
            String text = ((PsiElement) value).getText();
            return text == null ? "" : text.trim();
        }
        return "";
    }

    private static final class SymbolInfo {
        private final PsiElement element;
        private final String kind;
        private final String name;

        private SymbolInfo(PsiElement element, String kind, String name) {
            this.element = element;
            this.kind = kind;
            this.name = name;
        }
    }
}
