package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import org.hunmr.util.AppUtil;

public class CutAfterJumpCommand extends CommandAroundJump {
    public CutAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump(final int jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                selectJumpArea(jumpTargetOffset);
                _editor.getSelectionModel().copySelectionToClipboard();
                EditorModificationUtil.deleteSelectedText(_editor);
                _editor.getSelectionModel().removeSelection();
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
