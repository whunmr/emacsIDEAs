package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

public class LineSelector extends Selector {
    public LineSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange() {

        int lineNumber = _document.getLineNumber(_editor.getCaretModel().getOffset());
        int lineStart = _document.getLineStartOffset(lineNumber);
        int lineEnd = _document.getLineEndOffset(lineNumber);

        return lineEnd > lineStart ? new TextRange(lineStart, lineEnd) : null;
    }
}
