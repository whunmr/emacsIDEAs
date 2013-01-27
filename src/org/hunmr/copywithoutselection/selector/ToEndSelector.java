package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class ToEndSelector extends Selector {
    public ToEndSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        final int startOffset = _editor.getCaretModel().getOffset();
        int endOffset;

        if (cmdCtx.getLastCmdKey() == 'E') {
            endOffset = _document.getTextLength();
        } else {
            endOffset = _document.getLineEndOffset(_document.getLineNumber(startOffset));
        }

        return endOffset > startOffset ? new TextRange(startOffset, endOffset) : null;
    }
}
