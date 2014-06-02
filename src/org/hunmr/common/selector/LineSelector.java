package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class LineSelector extends Selector {
    public LineSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange(CommandContext cmdCtx) {
        int lineNumber = _document.getLineNumber(_editor.getCaretModel().getOffset());
        int lineEnd = _document.getLineEndOffset(lineNumber);

        int lineStart = _document.getLineStartOffset(lineNumber);
        if (needCopyNewLine(cmdCtx) && lineStart > 0) {
            lineStart--;
        }

        return lineEnd > lineStart ? new TextRange(lineStart, lineEnd) : null;
    }

    private boolean needCopyNewLine(CommandContext cmdCtx) {
        return cmdCtx.getLastCmdKey() == 'L';
    }
}
