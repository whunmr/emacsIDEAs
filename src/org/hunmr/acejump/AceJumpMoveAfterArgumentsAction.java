package org.hunmr.acejump;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.MoveRangeAfterJumpCommand;
import org.hunmr.common.selector.AfterArgumentsSelector;
import org.hunmr.common.selector.Selector;

public class AceJumpMoveAfterArgumentsAction extends AbstractAceJumpArgumentSliceRangeAction {
    @Override
    protected boolean isBeforeSlice() {
        return false;
    }

    @Override
    protected Class<? extends Selector> getSelectorClass() {
        return AfterArgumentsSelector.class;
    }

    @Override
    protected CommandAroundJump createCommand(Editor editor, Class<? extends Selector> selectorClass) {
        return new MoveRangeAfterJumpCommand(editor, selectorClass);
    }
}
