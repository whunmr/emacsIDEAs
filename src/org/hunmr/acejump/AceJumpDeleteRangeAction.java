package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.DeleteRangeAfterJumpCommand;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.EditorUtils;

public class AceJumpDeleteRangeAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        String actionId = e.getActionManager().getId(this);
        String selectorClassName = "org.hunmr.common.selector."
                + actionId.substring("emacsIDEAs.AceJumpDelete.".length())
                + "Selector";

        try {
            Class<? extends Selector> selectorClass = (Class<? extends Selector>) Class.forName(selectorClassName);
            AceJumpAction.getInstance().switchEditorIfNeed(e);
            AceJumpAction.getInstance().clearCommandAroundJump();
            AceJumpAction.getInstance().addCommandAroundJump(new DeleteRangeAfterJumpCommand(editor, selectorClass));
            AceJumpAction.getInstance().performAction(e);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}