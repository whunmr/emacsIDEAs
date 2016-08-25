package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class GroupSelector extends BlockSelector {

    public GroupSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange(CommandContext cmdCtx) {
        TextRange r = super.getRange(cmdCtx);

        if (r != null) {
            int start = _document.getLineStartOffset(_document.getLineNumber(r.getStartOffset() - 1));
            int end   = _document.getLineEndOffset(_document.getLineNumber(r.getEndOffset() + 1));

            if (end + 1 < _docText.length()) {
                end++; //extend to include the ending newline char
            }

            return new TextRange(start, end);
        }

        return r;
    }

}
