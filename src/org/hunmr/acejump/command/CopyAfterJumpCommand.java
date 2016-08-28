package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.util.AppUtil;

public class CopyAfterJumpCommand extends CommandAroundJump {
    public CopyAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump(final JOffset jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
    }

    @Override
    public void afterJump(final JOffset jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                selectJumpArea(jumpTargetOffset);
                _editor.getSelectionModel().copySelectionToClipboard();
                _editor.getSelectionModel().removeSelection();
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
