package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.copycutwithoutselection.selector.LineSelector;

public class ReplaceLineAfterJumpCommand extends PasteAfterJumpCommand {
    public ReplaceLineAfterJumpCommand(Editor editor) {
        super(editor, false);
    }

    public TextRange getTextRangeToReplace()
    {
        return new LineSelector(_editor).getRange(new CommandContext());
    }
}
