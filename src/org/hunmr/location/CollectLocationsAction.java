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
import org.hunmr.util.ClipboardEditorUtil;

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
        String clipboardText = ClipboardEditorUtil.getClipboardText();
        String label = CollectedLocationFormatter.nextLabel(clipboardText);
        Project project = e.getProject();
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

        ClipboardEditorUtil.copyToClipboard(CollectedLocationFormatter.appendEntry(clipboardText, entry));
        HintManager.getInstance().showInformationHint(editor, CollectedLocationFormatter.toHintHtml(entry));
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
}
