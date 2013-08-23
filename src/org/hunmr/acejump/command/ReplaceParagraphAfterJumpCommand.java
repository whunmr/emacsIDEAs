package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.copycutwithoutselection.selector.ParagraphSelector;

public class ReplaceParagraphAfterJumpCommand extends PasteAfterJumpCommand {
    public ReplaceParagraphAfterJumpCommand(Editor editor) {
        super(editor, false);
    }

    public TextRange getTextRangeToReplace()
    {
        return new ParagraphSelector(_editor).getRange(new CommandContext());
    }
}
