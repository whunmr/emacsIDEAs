package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;

public class PasteAfterJumpCommand extends CommandAroundJump {
    public PasteAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump(final int jumpTargetOffset) {
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EditorModificationUtil.pasteFromClipboard(_editor);
            }
        };

        ApplicationManager.getApplication().runWriteAction(getRunnableWrapper(runnable));
    }
}
