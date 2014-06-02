package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class StringSelector extends Selector {
    public StringSelector(Editor editor) {
        super(editor);
    }

    private int getStringStartOffset(final int caretOffset) {
        int offset = caretOffset;
        while (offset > 0 && !Character.isWhitespace(charAt(offset - 1))) {
            offset--;
        }

        return offset;
    }

    private int getStringEndOffset(final int caretOffset) {
        int offset = caretOffset;
        while (offset < _docText.length() && !Character.isWhitespace(charAt(offset))) {
            offset++;
        }

        return offset;
    }

    public TextRange getRange(CommandContext cmdCtx) {
        final int caretOffset = getNearestStringEndOffset(_editor);

        int strStart = getStringStartOffset(caretOffset);
        int strEnd = getStringEndOffset(caretOffset);

        return strEnd > strStart ? new TextRange(strStart, strEnd) : null;
    }
}
