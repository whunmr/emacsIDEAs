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
    public void afterJump() {
        if (!hasUsableSourceEditor()) {
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor()) {
                    return;
                }

                if (inSameEditor()) {
                    selectJumpArea();

                    _se.getSelectionModel().copySelectionToClipboard();
                    EditorModificationUtil.deleteSelectedText(_se);
                    _se.getSelectionModel().removeSelection();
                }
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
