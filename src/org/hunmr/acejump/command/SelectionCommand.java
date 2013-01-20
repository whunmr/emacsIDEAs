package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;

public class SelectionCommand extends CommandAroundJump {
    public SelectionCommand(Editor editor) {
        super(editor);
    }

    @Override
    public void beforeJump() {
        ((EditorEx)_editor).setStickySelection(true);
    }

    @Override
    public void afterJump() {
    }
}
