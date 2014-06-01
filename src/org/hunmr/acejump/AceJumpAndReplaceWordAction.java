package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.ReplaceWordAfterJumpCommand;
import org.hunmr.util.EditorUtils;

public class AceJumpAndReplaceWordAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        EditorUtils.copyWord(editor);
        AceJumpAction.getInstance().addCommandAroundJump(new ReplaceWordAfterJumpCommand(editor));
        AceJumpAction.getInstance().performAction(e);
    }

}
