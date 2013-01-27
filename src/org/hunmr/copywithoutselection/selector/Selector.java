package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public abstract class Selector {

    protected Editor _editor;
    protected final String _docText;
    protected final Document _document;

    public Selector(Editor editor) {
        _editor = editor;
        _document = _editor.getDocument();
        _docText = _document.getText();
    }

    public abstract TextRange getRange(CommandContext cmdCtx);

    protected boolean isCaretBetweenSpaces(Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();
        int textLength = _docText.length();

        return caretOffset - 1 > 0
                && caretOffset + 1 < textLength
                && Character.isWhitespace(charAt(caretOffset - 1))
                && Character.isWhitespace(charAt(caretOffset + 1));
    }

    protected int getNearestStringEndOffset(Editor editor) {
        int offset = editor.getCaretModel().getOffset();

        if (isCaretBetweenSpaces(editor)) {
            while (offset > 0 && Character.isWhitespace(charAt(offset - 1))) {
                offset--;
            }
        }

        return offset;
    }

    protected char charAt(int index) {
        return _docText.charAt(index);
    }
}
