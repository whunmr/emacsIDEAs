package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public abstract class CommandAroundJump {
    protected Editor _editor;

    public CommandAroundJump(Editor editor) {
        _editor = editor;
    }

    public abstract void beforeJump();
    public abstract void afterJump();
}
