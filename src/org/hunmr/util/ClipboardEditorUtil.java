package org.hunmr.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.util.TextRange;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;

public final class ClipboardEditorUtil {
    private ClipboardEditorUtil() {
    }

    public static TextRange[] pasteFromClipboard(Editor editor) {
        if (editor == null || editor.isDisposed()) {
            return new TextRange[0];
        }

        String text = CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor);
        if (text == null || text.isEmpty()) {
            return new TextRange[0];
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        int startOffset = selectionModel.hasSelection()
                ? selectionModel.getSelectionStart()
                : editor.getCaretModel().getOffset();
        int endOffset = selectionModel.hasSelection()
                ? selectionModel.getSelectionEnd()
                : startOffset;

        if (selectionModel.hasSelection()) {
            editor.getDocument().replaceString(startOffset, endOffset, text);
            selectionModel.removeSelection();
        } else {
            editor.getDocument().insertString(startOffset, text);
        }

        int pastedEndOffset = startOffset + text.length();
        editor.getCaretModel().moveToOffset(pastedEndOffset);
        return new TextRange[] {new TextRange(startOffset, pastedEndOffset)};
    }

    public static void copyToClipboard(String text) {
        if (text == null) {
            return;
        }

        CopyPasteManager.getInstance().setContents(new StringSelection(text));
    }
}
