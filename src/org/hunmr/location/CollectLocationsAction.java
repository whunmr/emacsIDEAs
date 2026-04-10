package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.options.PluginConfig;

import java.io.IOException;

public class CollectLocationsAction extends SimpleEditorAction {
    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        e.getPresentation().setEnabled(project != null && resolveCollectTarget(project, e) != CollectTarget.NONE);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        CollectTarget target = resolveCollectTarget(project, e);
        if (target == CollectTarget.USAGES) {
            new CollectUsageAction().performCollect(e);
            return;
        }
        if (target == CollectTarget.CALL_HIERARCHY) {
            new CollectCallHierarchyAction().performCollect(e);
            return;
        }

        Editor editor = getActiveEditor(project, e);
        if (editor == null || editor.isDisposed()) {
            showMessage(project, null, "Focus the editor, Find Usages, or Call Hierarchy window first");
            return;
        }

        Document document = editor.getDocument();
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        String absolutePath = getAbsolutePath(virtualFile);
        if (absolutePath == null || absolutePath.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "No file path for current editor");
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String existingEntries = CollectedPromptFormatter.withPromptHeader(
                CollectedOutputFileManager.getCurrentText(project),
                PluginConfig.getInstance()._promptHeader
        );
        String label = CollectedLocationFormatter.nextLabel(existingEntries);
        String entry;

        if (!selectionModel.hasSelection()) {
            int lineNumber = document.getLineNumber(editor.getCaretModel().getOffset()) + 1;
            CollectedLocationContext context = CollectedLocationContextResolver.resolve(
                    project,
                    editor,
                    editor.getCaretModel().getOffset(),
                    editor.getCaretModel().getOffset(),
                    false
            );
            entry = CollectedLocationFormatter.formatEntry(label, context, "", absolutePath, lineNumber, lineNumber, false);
        } else {
            int selectionStart = selectionModel.getSelectionStart();
            int selectionEnd = selectionModel.getSelectionEnd();
            int startLine = document.getLineNumber(selectionStart) + 1;
            int endLine = document.getLineNumber(getSelectionEndForLineNumber(selectionStart, selectionEnd)) + 1;
            boolean fullFileSelection = selectionStart == 0 && selectionEnd == document.getTextLength();
            CollectedLocationContext context = CollectedLocationContextResolver.resolve(
                    project,
                    editor,
                    selectionStart,
                    selectionEnd,
                    true
            );
            entry = CollectedLocationFormatter.formatEntry(
                    label,
                    context,
                    selectionModel.getSelectedText(),
                    absolutePath,
                    startLine,
                    endLine,
                    fullFileSelection
            );
        }

        if (CollectedLocationFormatter.containsDuplicate(existingEntries, entry)) {
            HintManager.getInstance().showInformationHint(editor, "Already exists");
            return;
        }

        String updatedText = CollectedPromptFormatter.appendToContext(
                existingEntries,
                CollectedLocationFormatter.appendEntry("", entry)
        );
        writeOutput(project, editor, updatedText);
    }

    private static CollectTarget resolveCollectTarget(Project project, AnActionEvent e) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        String activeToolWindowId = toolWindowManager == null ? null : toolWindowManager.getActiveToolWindowId();

        if (ToolWindowId.FIND.equals(activeToolWindowId) && CollectUsageAction.hasActiveUsageView(project, e)) {
            return CollectTarget.USAGES;
        }

        if (ToolWindowId.HIERARCHY.equals(activeToolWindowId) && CollectCallHierarchyAction.hasActiveCallHierarchyView(project, e)) {
            return CollectTarget.CALL_HIERARCHY;
        }

        if ((toolWindowManager != null && toolWindowManager.isEditorComponentActive())
                || isUsableEditor(e == null ? null : e.getData(CommonDataKeys.EDITOR))) {
            return isUsableEditor(getActiveEditor(project, e)) ? CollectTarget.EDITOR : CollectTarget.NONE;
        }

        return CollectTarget.NONE;
    }

    private static Editor getActiveEditor(Project project, AnActionEvent e) {
        Editor editor = e == null ? null : e.getData(CommonDataKeys.EDITOR);
        if (isUsableEditor(editor)) {
            return editor;
        }

        if (project == null) {
            return null;
        }

        Editor selectedEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        return isUsableEditor(selectedEditor) ? selectedEditor : null;
    }

    private static boolean isUsableEditor(Editor editor) {
        return editor != null && !editor.isDisposed();
    }

    private static int getSelectionEndForLineNumber(int selectionStart, int selectionEnd) {
        if (selectionEnd <= selectionStart) {
            return selectionStart;
        }

        return selectionEnd - 1;
    }

    private static String getAbsolutePath(VirtualFile virtualFile) {
        if (virtualFile == null) {
            return null;
        }

        String canonicalPath = virtualFile.getCanonicalPath();
        if (canonicalPath != null && !canonicalPath.isEmpty()) {
            return canonicalPath;
        }

        return virtualFile.getPath();
    }

    static void writeOutput(Project project, Editor editor, String text) {
        try {
            VirtualFile outputFile = CollectedOutputFileManager.replaceAndOpen(project, text, editor);
            String path = outputFile == null ? "tmp output file" : outputFile.getPath();
            HintManager.getInstance().showInformationHint(editor, "Collected into " + path);
        } catch (IOException exception) {
            HintManager.getInstance().showInformationHint(editor, "Failed to write collected output: " + exception.getMessage());
        }
    }

    private static void showMessage(Project project, Editor editor, String message) {
        if (editor != null && !editor.isDisposed()) {
            HintManager.getInstance().showInformationHint(editor, message);
            return;
        }

        if (project != null) {
            ToolWindowManager.getInstance(project).notifyByBalloon(ToolWindowId.FIND, MessageType.INFO, message);
        }
    }

    private enum CollectTarget {
        NONE,
        EDITOR,
        USAGES,
        CALL_HIERARCHY
    }
}
