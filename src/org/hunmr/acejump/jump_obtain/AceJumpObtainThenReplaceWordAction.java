package org.hunmr.acejump.jump_obtain;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.command.ObtainAndPasteRangeAfterJumpCommand;
import org.hunmr.copycutwithoutselection.selector.WordSelector;
import org.hunmr.util.EditorUtils;

public class AceJumpObtainThenReplaceWordAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getData(PlatformDataKeys.EDITOR);

        EditorUtils.copyWord(editor);
        AceJumpAction.getInstance().addCommandAroundJump(new ObtainAndPasteRangeAfterJumpCommand(editor, WordSelector.class));
        AceJumpAction.getInstance().performAction(e);
    }
}
