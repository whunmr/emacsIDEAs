package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.ex.EditorEx;

public class CutAfterJumpCommand extends CommandAroundJump {
    public CutAfterJumpCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump() {
        ((EditorEx)_editor).setStickySelection(true);
    }

    @Override
    public void afterJump() {
        _editor.getSelectionModel().copySelectionToClipboard();
        EditorModificationUtil.deleteSelectedText(_editor);
    }
}
