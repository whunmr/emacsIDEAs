package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;

public class CopyAfterJumpCommand extends CommandAroundJump {
    public CopyAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump() {
        ((EditorEx)_editor).setStickySelection(true);
    }

    @Override
    public void afterJump() {
        _editor.getSelectionModel().copySelectionToClipboard();
        ((EditorEx)_editor).setStickySelection(false);
    }
}
