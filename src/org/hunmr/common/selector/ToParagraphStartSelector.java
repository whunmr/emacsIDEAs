package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class ToParagraphStartSelector extends ParagraphSelector {
    public ToParagraphStartSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        final int caretOffset = getNearestStringEndOffset(_editor);
        int paraStart = getParagraphStartOffset(caretOffset);
        return caretOffset > paraStart ? new TextRange(paraStart, caretOffset) : null;
    }
}
