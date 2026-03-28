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
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableTargetEditor()) {
                    return;
                }

                selectJumpArea();
                _te.getSelectionModel().copySelectionToClipboard();
                _te.getSelectionModel().removeSelection();
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
