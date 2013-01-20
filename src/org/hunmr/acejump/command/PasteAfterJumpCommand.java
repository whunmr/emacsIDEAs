package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;

public class PasteAfterJumpCommand extends CommandAroundJump {
    public PasteAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump() {
    }

    @Override
    public void afterJump() {
        EditorModificationUtil.pasteFromClipboard(_editor);
    }
}
