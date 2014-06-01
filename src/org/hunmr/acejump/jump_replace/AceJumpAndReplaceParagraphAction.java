package org.hunmr.acejump.jump_replace;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.command.ReplaceParagraphAfterJumpCommand;
import org.hunmr.util.EditorUtils;

public class AceJumpAndReplaceParagraphAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);
        EditorUtils.copyParagraph(editor);

        AceJumpAction.getInstance().addCommandAroundJump(new ReplaceParagraphAfterJumpCommand(editor));
        AceJumpAction.getInstance().performAction(e);
    }

}
