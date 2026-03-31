package org.hunmr.acejump;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.ReplaceAfterJumpCommand;
import org.hunmr.common.selector.AfterArgumentsSelector;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.EditorUtils;

public class AceJumpAndReplaceAfterArgumentsAction extends AbstractAceJumpArgumentSliceRangeAction {
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
        return new ReplaceAfterJumpCommand(editor, selectorClass);
    }

    @Override
    protected boolean beforeStartJump(Editor editor, Class<? extends Selector> selectorClass) {
        TextRange sourceRange = EditorUtils.getRangeOf(selectorClass, editor);
        if (sourceRange == null || sourceRange.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "No current argument slice");
            return false;
        }
        return true;
    }
}
