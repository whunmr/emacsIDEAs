package org.hunmr.acejump.runnable;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.options.PluginConfig;

public class JumpRunnable implements Runnable {

    private JOffset _offsetToJump;
    private AceJumpAction _action;
    private final PluginConfig _config = ServiceManager.getService(PluginConfig.class);


    public JumpRunnable(JOffset _offsetToJump, AceJumpAction _action) {
        this._offsetToJump = _offsetToJump;
        this._action = _action;
    }

    @Override
    public void run() {
        _offsetToJump.editor.getContentComponent().requestFocus();
        if (_config._jumpBehind && !isLineEndOrLineStartOffset(_offsetToJump.offset)) {
            _offsetToJump.editor.getCaretModel().moveToOffset(_offsetToJump.offset + 1);
        } else {
            _offsetToJump.editor.getCaretModel().moveToOffset(_offsetToJump.offset);
        }
        _offsetToJump.editor.getSelectionModel().removeSelection();
    }

    private boolean isLineEndOrLineStartOffset(int offset) {
        Document _document = _offsetToJump.editor.getDocument();
        int lineNumber = _document.getLineNumber(offset);
        int lineStartOffset = _document.getLineStartOffset(lineNumber);
        int lineEndOffset = _document.getLineEndOffset(lineNumber);
        return lineEndOffset == _offsetToJump.offset || lineStartOffset == _offsetToJump.offset;
    }
}
