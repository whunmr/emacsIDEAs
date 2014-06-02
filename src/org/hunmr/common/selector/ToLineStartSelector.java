package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class ToLineStartSelector extends Selector {
    public ToLineStartSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        int startOffset;
        int endOffset = _editor.getCaretModel().getOffset();

        if (cmdCtx.getLastCmdKey() == 'A') {
            startOffset = 0;
        } else {
            startOffset = _document.getLineStartOffset(_document.getLineNumber(endOffset));
        }

        return endOffset > startOffset ? new TextRange(startOffset, endOffset) : null;
    }
}
