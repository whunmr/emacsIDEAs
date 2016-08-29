package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.EditorUtils;

public class ReplaceRangeAfterJumpCommand extends PasteAfterJumpCommand {
    private final Class<? extends Selector> _selectorClass;

    public ReplaceRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }

    public TextRange getTextRangeToReplace()
    {
        return EditorUtils.getRangeOf(_selectorClass, _se);
    }
}