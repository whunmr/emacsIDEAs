package org.hunmr.location;

import com.goide.psi.GoAnonymousFieldDefinition;
import com.goide.psi.GoFieldDeclaration;
import com.goide.psi.GoFieldDefinition;
import com.goide.psi.GoFile;
import com.goide.psi.GoInterfaceType;
import com.goide.psi.GoNamedElement;
import com.goide.psi.GoParameterDeclaration;
import com.goide.psi.GoReferenceExpression;
import com.goide.psi.GoSpecType;
import com.goide.psi.GoStructType;
import com.goide.psi.GoType;
import com.goide.psi.GoTypeReferenceExpression;
import com.goide.psi.GoTypeSpec;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.util.PsiTreeUtil;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.options.PluginConfig;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CollectTypesInSelectionAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        Project project = e.getProject();
        if (project == null || editor == null || editor.isDisposed()) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        if (!selectionModel.hasSelection()) {
            HintManager.getInstance().showInformationHint(editor, "Select Go code first");
            return;
        }

        PsiElement selectionRoot = findSelectionRoot(project, editor, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
        if (selectionRoot == null) {
            HintManager.getInstance().showInformationHint(editor, "No Go PSI available for selection");
            return;
        }

        Collection<PsiElement> elements = collectSelectionElements(
                selectionRoot,
                selectionModel.getSelectionStart(),
                selectionModel.getSelectionEnd()
        );

        Map<String, TypeLocation> collectedTypes = new LinkedHashMap<String, TypeLocation>();
        for (PsiElement element : elements) {
            collectFromElement(project, element, collectedTypes);
        }

        PluginConfig config = PluginConfig.getInstance();
        List<String> includeFilters = splitLines(config._collectTypesInSelectionInclude);
        List<String> excludeFilters = splitLines(config._collectTypesInSelectionExclude);

        String existingEntries = CollectedPromptFormatter.withPromptHeader(
                CollectedOutputFileManager.getCurrentText(project),
                config._promptHeader
        );
        String nextLabel = CollectedLocationFormatter.nextLabel(existingEntries);
        int nextIndex = decodeLabel(nextLabel);
        StringBuilder contextBlock = new StringBuilder();
        StringBuilder duplicateProbe = new StringBuilder(existingEntries);
        boolean matchedAnyType = false;
        boolean skippedDuplicate = false;

        for (TypeLocation location : collectedTypes.values()) {
            if (!matchesProjectFilter(project, config._collectTypesInSelectionProjectOnly, location.absolutePath)) {
                continue;
            }
            if (!matchesFilters(location.importPath, includeFilters, excludeFilters)) {
                continue;
            }
            matchedAnyType = true;

            String label = encodeLabel(nextIndex++);
            String entry = CollectedLocationFormatter.formatEntry(
                    label,
                    location.context,
                    location.context.getSymbolName(),
                    location.absolutePath,
                    location.lineNumber,
                    location.lineNumber,
                    false
            );
            StringBuilder lineBuilder = new StringBuilder(entry);
            if (!entry.contains("`" + location.importPath + "`")) {
                lineBuilder.append(" \\n import: `").append(location.importPath).append("`");
            }
            for (String reason : location.reasons) {
                lineBuilder.append(" \\n reason: ").append(reason);
            }
            String finalEntry = lineBuilder.toString();
            if (CollectedLocationFormatter.containsDuplicate(duplicateProbe.toString(), finalEntry)) {
                skippedDuplicate = true;
                continue;
            }

            contextBlock.append(finalEntry).append('\n');
            duplicateProbe.append('\n').append(finalEntry);
        }

        if (contextBlock.length() == 0) {
            HintManager.getInstance().showInformationHint(editor, matchedAnyType && skippedDuplicate
                    ? "Already exists"
                    : "No Go types matched selection filters");
            return;
        }

        String updatedText = CollectedPromptFormatter.appendToContext(existingEntries, contextBlock.toString());
        CollectLocationsAction.writeOutput(project, editor, updatedText);
    }

    private static PsiElement findSelectionRoot(Project project, Editor editor, int start, int end) {
        Document document = editor.getDocument();
        com.intellij.psi.PsiDocumentManager documentManager = com.intellij.psi.PsiDocumentManager.getInstance(project);
        documentManager.commitDocument(document);
        com.intellij.psi.PsiFile psiFile = documentManager.getPsiFile(document);
        if (!(psiFile instanceof GoFile) || psiFile.getTextLength() <= 0) {
            return null;
        }

        int safeStart = Math.max(0, Math.min(start, psiFile.getTextLength() - 1));
        int safeEnd = Math.max(safeStart, Math.min(Math.max(start, end - 1), psiFile.getTextLength() - 1));
        PsiElement startElement = psiFile.findElementAt(safeStart);
        PsiElement endElement = psiFile.findElementAt(safeEnd);
        if (startElement == null || endElement == null) {
            return psiFile;
        }
        PsiElement common = PsiTreeUtil.findCommonParent(startElement, endElement);
        return common != null ? common : psiFile;
    }

    private static Collection<PsiElement> collectSelectionElements(PsiElement selectionRoot, int selectionStart, int selectionEnd) {
        Set<PsiElement> elements = new LinkedHashSet<PsiElement>();
        if (selectionRoot == null) {
            return elements;
        }

        com.intellij.psi.PsiFile psiFile = selectionRoot.getContainingFile();
        if (psiFile == null || psiFile.getTextLength() <= 0) {
            return elements;
        }

        int safeStart = Math.max(0, Math.min(selectionStart, psiFile.getTextLength() - 1));
        int safeEnd = Math.max(safeStart, Math.min(Math.max(selectionStart, selectionEnd - 1), psiFile.getTextLength() - 1));
        addNearestCollectableAnchor(psiFile.findElementAt(safeStart), selectionRoot, selectionStart, selectionEnd, elements);
        addNearestCollectableAnchor(psiFile.findElementAt(safeEnd), selectionRoot, selectionStart, selectionEnd, elements);

        Collection<PsiElement> descendants = PsiTreeUtil.findChildrenOfAnyType(
                selectionRoot,
                GoTypeSpec.class,
                GoTypeReferenceExpression.class,
                GoReferenceExpression.class,
                GoNamedElement.class,
                GoParameterDeclaration.class
        );
        for (PsiElement element : descendants) {
            if (intersectsSelection(element, selectionStart, selectionEnd)) {
                elements.add(element);
            }
        }

        return elements;
    }

    private static void addNearestCollectableAnchor(PsiElement anchor,
                                                    PsiElement selectionRoot,
                                                    int selectionStart,
                                                    int selectionEnd,
                                                    Set<PsiElement> elements) {
        PsiElement current = anchor;
        while (current != null) {
            if (isCollectableElement(current) && intersectsSelection(current, selectionStart, selectionEnd)) {
                elements.add(current);
                return;
            }
            if (current == selectionRoot) {
                return;
            }
            current = current.getParent();
        }
    }

    private static boolean isCollectableElement(PsiElement element) {
        return element instanceof GoTypeSpec
                || element instanceof GoTypeReferenceExpression
                || element instanceof GoReferenceExpression
                || element instanceof GoNamedElement
                || element instanceof GoParameterDeclaration;
    }

    private static boolean intersectsSelection(PsiElement element, int selectionStart, int selectionEnd) {
        if (element == null || selectionEnd <= selectionStart) {
            return false;
        }

        TextRange textRange = element.getTextRange();
        if (textRange == null) {
            return false;
        }

        return textRange.getStartOffset() < selectionEnd && textRange.getEndOffset() > selectionStart;
    }

    private static void collectFromElement(Project project, PsiElement element, Map<String, TypeLocation> collectedTypes) {
        if (element instanceof GoTypeSpec) {
            addResolvedType(project, (GoTypeSpec) element, "selected type declaration", collectedTypes);
            return;
        }

        if (element instanceof GoTypeReferenceExpression) {
            PsiElement resolved = ((GoTypeReferenceExpression) element).getReference() == null ? null : ((GoTypeReferenceExpression) element).getReference().resolve();
            if (resolved instanceof GoTypeSpec) {
                addResolvedType(project, (GoTypeSpec) resolved, "referenced type", collectedTypes);
            }
            return;
        }

        if (element instanceof GoReferenceExpression) {
            PsiElement resolved = ((GoReferenceExpression) element).getReference() == null ? null : ((GoReferenceExpression) element).getReference().resolve();
            collectResolvedElementType(project, resolved, "referenced variable type", collectedTypes);
            return;
        }

        if (element instanceof GoParameterDeclaration) {
            GoType type = ((GoParameterDeclaration) element).getType();
            collectFromGoType(project, type, "parameter type", collectedTypes);
            return;
        }

        if (element instanceof GoNamedElement) {
            collectResolvedElementType(project, element, "named element type", collectedTypes);
        }
    }

    private static void collectResolvedElementType(Project project,
                                                   PsiElement resolved,
                                                   String reason,
                                                   Map<String, TypeLocation> collectedTypes) {
        if (resolved instanceof GoTypeSpec) {
            addResolvedType(project, (GoTypeSpec) resolved, reason, collectedTypes);
            return;
        }

        if (resolved instanceof GoNamedElement) {
            GoNamedElement namedElement = (GoNamedElement) resolved;
            GoType siblingType = namedElement.findSiblingType();
            collectFromGoType(project, siblingType, reason, collectedTypes);
            GoType declaredType = namedElement.getGoType(ResolveState.initial());
            collectFromGoType(project, declaredType, reason, collectedTypes);
        }
    }

    private static void collectFromGoType(Project project,
                                          GoType type,
                                          String reason,
                                          Map<String, TypeLocation> collectedTypes) {
        if (type == null) {
            return;
        }

        Collection<GoTypeReferenceExpression> references = PsiTreeUtil.findChildrenOfType(type, GoTypeReferenceExpression.class);
        for (GoTypeReferenceExpression reference : references) {
            if (reference.getReference() == null) {
                continue;
            }
            PsiElement resolved = reference.getReference().resolve();
            if (resolved instanceof GoTypeSpec) {
                addResolvedType(project, (GoTypeSpec) resolved, reason, collectedTypes);
            }
        }

        if (type instanceof GoSpecType) {
            GoTypeSpec typeSpec = resolveTypeSpec(type);
            if (typeSpec != null) {
                addResolvedType(project, typeSpec, reason, collectedTypes);
            }
        }
    }

    private static GoTypeSpec resolveTypeSpec(GoType type) {
        if (type == null) {
            return null;
        }

        try {
            Method method = type.getClass().getMethod("getTypeSpec");
            Object value = method.invoke(type);
            if (value instanceof GoTypeSpec) {
                return (GoTypeSpec) value;
            }
        } catch (Exception ignored) {
        }

        return PsiTreeUtil.getParentOfType(type, GoTypeSpec.class, false);
    }

    private static void addResolvedType(Project project,
                                        GoTypeSpec typeSpec,
                                        String reason,
                                        Map<String, TypeLocation> collectedTypes) {
        if (typeSpec == null || !typeSpec.isValid()) {
            return;
        }

        GoFile file = typeSpec.getContainingFile();
        if (file == null) {
            return;
        }

        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return;
        }

        String importPath = file.getImportPath(true);
        if (importPath == null || importPath.trim().isEmpty()) {
            importPath = file.getPackageName();
        }
        String symbolName = typeSpec.getName();
        if (symbolName == null || symbolName.trim().isEmpty()) {
            return;
        }

        String uniqueKey = importPath + "#" + symbolName;
        TypeLocation existing = collectedTypes.get(uniqueKey);
        if (existing != null) {
            existing.reasons.add(reason);
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
        if (document == null || document.getTextLength() <= 0) {
            return;
        }

        int safeOffset = Math.max(0, Math.min(typeSpec.getTextOffset(), document.getTextLength() - 1));
        int lineNumber = document.getLineNumber(safeOffset) + 1;
        CollectedLocationContext baseContext = CollectedLocationContextResolver.resolve(project, document, safeOffset, safeOffset, false);
        CollectedLocationContext enrichedContext = enrichContext(baseContext, typeSpec, importPath);
        String absolutePath = virtualFile.getCanonicalPath();
        if (absolutePath == null || absolutePath.isEmpty()) {
            absolutePath = virtualFile.getPath();
        }

        TypeLocation location = new TypeLocation(importPath, absolutePath, lineNumber, enrichedContext);
        location.reasons.add(reason);
        collectedTypes.put(uniqueKey, location);

        collectFieldTypes(project, typeSpec, location, collectedTypes);
    }

    private static void collectFieldTypes(Project project,
                                          GoTypeSpec typeSpec,
                                          TypeLocation ownerLocation,
                                          Map<String, TypeLocation> collectedTypes) {
        GoSpecType specType = typeSpec.getSpecType();
        GoType innerType = specType == null ? null : specType.getType();
        if (innerType instanceof GoStructType) {
            GoStructType structType = (GoStructType) innerType;
            for (GoFieldDeclaration fieldDeclaration : structType.getFieldDeclarationList()) {
                for (GoFieldDefinition fieldDefinition : fieldDeclaration.getFieldDefinitionList()) {
                    collectFromGoType(project, fieldDeclaration.getType(),
                            "field type of `" + ownerLocation.context.getSymbolName() + "." + fieldDefinition.getName() + "`",
                            collectedTypes);
                }
                GoAnonymousFieldDefinition anonymousField = fieldDeclaration.getAnonymousFieldDefinition();
                if (anonymousField != null) {
                    collectFromGoType(project, anonymousField.getType(),
                            "embedded field type of `" + ownerLocation.context.getSymbolName() + "`",
                            collectedTypes);
                }
            }
        } else if (innerType instanceof GoInterfaceType) {
            GoInterfaceType interfaceType = (GoInterfaceType) innerType;
            List<GoTypeReferenceExpression> baseTypes = interfaceType.getBaseTypesReferences();
            for (GoTypeReferenceExpression baseType : baseTypes) {
                if (baseType.getReference() == null) {
                    continue;
                }
                PsiElement resolved = baseType.getReference().resolve();
                if (resolved instanceof GoTypeSpec) {
                    addResolvedType(project, (GoTypeSpec) resolved,
                            "embedded interface type of `" + ownerLocation.context.getSymbolName() + "`",
                            collectedTypes);
                }
            }
        }
    }

    private static CollectedLocationContext enrichContext(CollectedLocationContext baseContext,
                                                          GoTypeSpec typeSpec,
                                                          String importPath) {
        String symbolKind = inferTypeKind(typeSpec);
        String symbolName = baseContext.hasSymbol() ? baseContext.getSymbolName() : typeSpec.getName();
        return new CollectedLocationContext(symbolKind, symbolName, "package", importPath);
    }

    private static String inferTypeKind(GoTypeSpec typeSpec) {
        if (typeSpec == null) {
            return "type";
        }
        if (typeSpec.isTypeAlias()) {
            return "type alias";
        }

        GoSpecType specType = typeSpec.getSpecType();
        GoType innerType = specType == null ? null : specType.getType();
        if (innerType instanceof GoInterfaceType) {
            return "interface";
        }
        if (innerType instanceof GoStructType) {
            return "struct";
        }
        return "type";
    }

    private static boolean matchesProjectFilter(Project project, boolean projectOnly, String absolutePath) {
        if (!projectOnly) {
            return true;
        }
        String basePath = project == null ? null : project.getBasePath();
        if (basePath == null || basePath.isEmpty() || absolutePath == null || absolutePath.isEmpty()) {
            return false;
        }
        return absolutePath.startsWith(new File(basePath).getAbsolutePath());
    }

    private static boolean matchesFilters(String importPath, List<String> includeFilters, List<String> excludeFilters) {
        String safeImportPath = importPath == null ? "" : importPath;
        boolean included = includeFilters.isEmpty() || containsAny(safeImportPath, includeFilters);
        if (!included) {
            return false;
        }
        return !containsAny(safeImportPath, excludeFilters);
    }

    private static boolean containsAny(String text, List<String> filters) {
        for (String filter : filters) {
            if (!filter.isEmpty() && text.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitLines(String text) {
        List<String> filters = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return filters;
        }

        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                filters.add(trimmed);
            }
        }
        return filters;
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
        return builder.toString();
    }

    private static final class TypeLocation {
        private final String importPath;
        private final String absolutePath;
        private final int lineNumber;
        private final CollectedLocationContext context;
        private final Set<String> reasons = new LinkedHashSet<String>();

        private TypeLocation(String importPath, String absolutePath, int lineNumber, CollectedLocationContext context) {
            this.importPath = importPath == null ? "" : importPath;
            this.absolutePath = absolutePath;
            this.lineNumber = lineNumber;
            this.context = context;
        }
    }
}
