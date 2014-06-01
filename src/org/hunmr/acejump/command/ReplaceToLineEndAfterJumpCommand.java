package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.copycutwithoutselection.selector.ToLineEndSelector;

public class ReplaceToLineEndAfterJumpCommand extends PasteAfterJumpCommand {
    public ReplaceToLineEndAfterJumpCommand(Editor editor) {
        super(editor, false);
    }

    public TextRange getTextRangeToReplace()
    {
        return new ToLineEndSelector(_editor).getRange(new CommandContext());
    }
}
