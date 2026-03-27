package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CutAfterJumpCommand;
import org.hunmr.common.SimpleEditorAction;

public class AceJumpCutAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null) {
            return;
        }

        AceJumpAction.getInstance().switchEditorIfNeed(e);
        AceJumpAction.getInstance().addCommandAroundJump(new CutAfterJumpCommand(editor));
        AceJumpAction.getInstance().performAction(e);
    }
}
