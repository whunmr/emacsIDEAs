package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class SelectionCommand extends CommandAroundJump {
    public SelectionCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump(final int jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        selectJumpArea(jumpTargetOffset);
    }
}
