package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.marker.JOffset;

public class SelectAfterJumpCommand extends CommandAroundJump {
    public SelectAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump(final JOffset jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
    }

    @Override
    public void afterJump(final JOffset jumpTargetOffset) {
        selectJumpArea(jumpTargetOffset);
    }
}
