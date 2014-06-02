package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class ToParagraphEndSelector extends ParagraphSelector {
    public ToParagraphEndSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        final int caretOffset = getNearestStringEndOffset(_editor);

        int paraEnd = getParagraphEndOffset(caretOffset);
        return paraEnd > caretOffset ? new TextRange(caretOffset, paraEnd) : null;
    }
}
