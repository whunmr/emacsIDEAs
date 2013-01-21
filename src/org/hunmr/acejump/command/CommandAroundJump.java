package org.hunmr.acejump.command;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;

public class CommandAroundJump {
    protected Editor _editor;
    private int _offsetBeforeJump;

    public CommandAroundJump(Editor editor) {
        _editor = editor;
    }

    public void beforeJump(final int jumpTargetOffset) {
        _offsetBeforeJump = _editor.getCaretModel().getOffset();
    }

    public void afterJump(final int jumpTargetOffset) {

    }

    public int getOffsetBeforeJump() {
        return _offsetBeforeJump;
    }

    protected void selectJumpArea(int jumpTargetOffset) {
        if (getOffsetBeforeJump() < jumpTargetOffset) {
            _editor.getSelectionModel().setSelection(getOffsetBeforeJump(), jumpTargetOffset);
        } else {
            _editor.getSelectionModel().setSelection(jumpTargetOffset, getOffsetBeforeJump());
        }
    }

    protected Runnable getRunnableWrapper(final Runnable runnable) {
        return new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(_editor.getProject(), runnable, "cut", ActionGroup.EMPTY_GROUP);
            }
        };
    }
}
