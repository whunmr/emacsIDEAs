package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.MoveRangeAfterJumpCommand;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.EditorUtils;

public class AceJumpMoveRangeAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null) {
            return;
        }

        String actionId = e.getActionManager().getId(this);
        String selectorClassName = "org.hunmr.common.selector."
                + actionId.substring("emacsIDEAs.AceJumpMove.".length())
                + "Selector";

        try {
            Class<? extends Selector> selectorClass = (Class<? extends Selector>) Class.forName(selectorClassName);
            EditorUtils.copyRange(selectorClass, editor);

            AceJumpAction.getInstance().switchEditorIfNeed(e);
            AceJumpAction.getInstance().addCommandAroundJump(new MoveRangeAfterJumpCommand(editor, selectorClass));
            AceJumpAction.getInstance().performAction(e);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
