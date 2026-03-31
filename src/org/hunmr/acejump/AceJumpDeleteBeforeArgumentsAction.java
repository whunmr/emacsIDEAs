package org.hunmr.acejump;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.DeleteRangeAfterJumpCommand;
import org.hunmr.common.selector.BeforeArgumentsSelector;
import org.hunmr.common.selector.Selector;

public class AceJumpDeleteBeforeArgumentsAction extends AbstractAceJumpArgumentSliceRangeAction {
    @Override
    protected boolean isBeforeSlice() {
        return true;
    }

    @Override
    protected Class<? extends Selector> getSelectorClass() {
        return BeforeArgumentsSelector.class;
    }

    @Override
    protected CommandAroundJump createCommand(Editor editor, Class<? extends Selector> selectorClass) {
        return new DeleteRangeAfterJumpCommand(editor, selectorClass);
    }
}
