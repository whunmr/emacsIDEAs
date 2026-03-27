package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.hunmr.common.SimpleEditorAction;

public class AceJumpCharAction extends SimpleEditorAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        AceJumpAction.getInstance().performAction(e);
    }
}
