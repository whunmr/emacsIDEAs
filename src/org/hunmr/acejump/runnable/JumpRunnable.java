package org.hunmr.acejump.runnable;

import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.marker.JOffset;

public class JumpRunnable implements Runnable{

    private JOffset _offsetToJump;
    private AceJumpAction _action;

    public JumpRunnable(JOffset _offsetToJump, AceJumpAction _action) {
        this._offsetToJump = _offsetToJump;
        this._action = _action;
    }

    @Override
    public void run() {
        _offsetToJump.editor.getContentComponent().requestFocus();
        _offsetToJump.editor.getCaretModel().moveToOffset(_offsetToJump.offset);
        _offsetToJump.editor.getSelectionModel().removeSelection();
    }
}
