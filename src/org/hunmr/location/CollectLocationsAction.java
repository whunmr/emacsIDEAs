package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.options.PluginConfig;

import java.io.IOException;

public class CollectLocationsAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null || editor.isDisposed()) {
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
        Project project = e.getProject();
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
}
