package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.ObtainAndPasteRangeAfterJumpCommand;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.common.selector.Selector;

public class AceJumpObtainThenReplaceRangeAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null) {
            return;
        }

        if (e.getActionManager() == null) {
            return;
        }

        String actionId = e.getActionManager().getId(this);
        if (actionId == null || !actionId.startsWith("emacsIDEAs.AceJumpObtainThenReplace.")) {
            return;
        }

        String selectorClassName = "org.hunmr.common.selector."
                                   + actionId.substring("emacsIDEAs.AceJumpObtainThenReplace.".length())
                                   + "Selector";

        try {
            Class<? extends Selector> selectorClass = (Class<? extends Selector>) Class.forName(selectorClassName);
            AceJumpAction.getInstance().switchEditorIfNeed(e);
            AceJumpAction.getInstance().addCommandAroundJump(new ObtainAndPasteRangeAfterJumpCommand(editor, selectorClass));
            AceJumpAction.getInstance().performAction(e);
        } catch (ClassNotFoundException e1) {
            e1.printStackTrace();
        }
    }
}
