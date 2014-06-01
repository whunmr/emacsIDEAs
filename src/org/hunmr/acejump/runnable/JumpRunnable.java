package org.hunmr.acejump.runnable;

import org.hunmr.acejump.AceJumpAction;

public class JumpRunnable implements Runnable{

    private int _offsetToJump;
    private AceJumpAction _action;

    public JumpRunnable(int _offsetToJump, AceJumpAction _action) {
        this._offsetToJump = _offsetToJump;
        this._action = _action;
    }

    @Override
    public void run() {
        _action.getEditor().getCaretModel().moveToOffset(_offsetToJump);
        _action.getEditor().getSelectionModel().removeSelection();
    }
}
