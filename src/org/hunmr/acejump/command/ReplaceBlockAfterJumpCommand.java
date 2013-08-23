package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.copycutwithoutselection.selector.BlockSelector;

public class ReplaceBlockAfterJumpCommand extends PasteAfterJumpCommand {
    public ReplaceBlockAfterJumpCommand(Editor editor) {
        super(editor, false);
    }

    public TextRange getTextRangeToReplace()
    {
        return new BlockSelector(_editor).getRange(new CommandContext());
    }
}