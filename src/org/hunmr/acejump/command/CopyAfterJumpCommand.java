package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import org.hunmr.util.AppUtil;

public class CopyAfterJumpCommand extends CommandAroundJump {
    public CopyAfterJumpCommand(Editor editor) {
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
                _editor.getSelectionModel().removeSelection();
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }
}
