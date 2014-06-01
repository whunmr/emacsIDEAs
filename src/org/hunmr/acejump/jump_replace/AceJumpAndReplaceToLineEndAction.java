package org.hunmr.acejump.jump_replace;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.command.ReplaceToLineEndAfterJumpCommand;
import org.hunmr.util.EditorUtils;

public class AceJumpAndReplaceToLineEndAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        EditorUtils.copyToLineEnd(editor);
        AceJumpAction.getInstance().addCommandAroundJump(new ReplaceToLineEndAfterJumpCommand(editor));
        AceJumpAction.getInstance().performAction(e);
    }
}