package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.copycutwithoutselection.selector.WordSelector;

public class ReplaceWordAfterJumpCommand extends PasteAfterJumpCommand {
    public ReplaceWordAfterJumpCommand(Editor editor) {
        super(editor, false);
    }

    @Override
    public void afterJump(int jumpTargetOffset) {
        TextRange tr = new WordSelector(_editor).getRange(new CommandContext());
        _editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
        super.afterJump(jumpTargetOffset);
    }
}
