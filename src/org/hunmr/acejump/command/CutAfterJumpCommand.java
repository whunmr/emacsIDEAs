package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.util.AppUtil;

public class CutAfterJumpCommand extends CommandAroundJump {
    public CutAfterJumpCommand(Editor editor) {
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
                if (inSameEditor(jumpTargetOffset)) {
                    selectJumpArea(jumpTargetOffset);
                    _editor.getSelectionModel().copySelectionToClipboard();
                    EditorModificationUtil.deleteSelectedText(_editor);
                    _editor.getSelectionModel().removeSelection();
                }
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
