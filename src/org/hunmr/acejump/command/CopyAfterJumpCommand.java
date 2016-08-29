package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
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

                SelectionModel selectionModel = jumpTargetOffset.editor.getSelectionModel();
                selectionModel.copySelectionToClipboard();
                selectionModel.removeSelection();
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
