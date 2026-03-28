package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.options.PluginConfig;

public class CommandAroundJump {
    protected Editor _se; /*source editor*/
    protected int _soff;

    protected Editor _te; /*target editor*/
    protected int _toff;

    final PluginConfig _config = PluginConfig.getInstance();

    public CommandAroundJump(Editor editor) {
        _se = editor;
    }

    public void beforeJump(final JOffset jumpTargetOffset) {
        if (!hasUsableSourceEditor()) {
            return;
        }

        _soff = _se.getCaretModel().getOffset();
    }

    public void preAfterJump(final JOffset jumpTargetOffset) {
        if (jumpTargetOffset == null || !isUsableEditor(jumpTargetOffset.editor)) {
            _te = null;
            return;
        }

        _te = jumpTargetOffset.editor;
        _toff = jumpTargetOffset.offset;
    }

    public void afterJump() {
    }

    public void focusSourceCaret() {
        if (!hasUsableSourceEditor()) {
            return;
        }

        _se.getContentComponent().requestFocus();
        _se.getCaretModel().moveToOffset(_soff);
    }

    public void focusTargetCaret() {
        if (!hasUsableTargetEditor()) {
            return;
        }

        _te.getContentComponent().requestFocus();
        _te.getCaretModel().moveToOffset(_toff);
    }

    protected void selectJumpArea() {
        if (inSameEditor() && hasUsableSourceEditor()) {
            if (_soff < _toff)
                _se.getSelectionModel().setSelection(_soff, _toff);
            else
                _se.getSelectionModel().setSelection(_toff, _soff);
        }
    }

    public boolean inSameEditor() {
        return _se == _te;
    }

    protected boolean hasUsableSourceEditor() {
        return isUsableEditor(_se);
    }

    protected boolean hasUsableTargetEditor() {
        return isUsableEditor(_te);
    }

    protected boolean isUsableEditor(Editor editor) {
        return editor != null && !editor.isDisposed();
    }

    protected boolean shouldSelectAfterJump() {
        return _config != null && _config._needSelectTextAfterJump;
    }
}
