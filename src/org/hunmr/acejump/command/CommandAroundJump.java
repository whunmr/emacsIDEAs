package org.hunmr.acejump.command;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.options.PluginConfig;

public class CommandAroundJump {
    protected Editor _editor;
    private JOffset _offsetBeforeJump;

    final PluginConfig _config = ServiceManager.getService(PluginConfig.class);

    public CommandAroundJump(Editor editor) {
        _editor = editor;
    }

    public void beforeJump(final JOffset jumpTargetOffset) {
        _offsetBeforeJump = new JOffset(_editor, _editor.getCaretModel().getOffset());
    }

    public void afterJump(final JOffset jumpTargetOffset) {

    }

    public JOffset getOffsetBeforeJump() {
        return _offsetBeforeJump;
    }

    protected void selectJumpArea(JOffset jumpTargetOffset) {
        if (_editor == jumpTargetOffset.editor) {
            if (getOffsetBeforeJump().offset < jumpTargetOffset.offset) {
                _editor.getSelectionModel().setSelection(getOffsetBeforeJump().offset, jumpTargetOffset.offset);
            } else {
                _editor.getSelectionModel().setSelection(jumpTargetOffset.offset, getOffsetBeforeJump().offset);
            }
        }
    }

    public boolean inSameEditor(JOffset jumpTargetOffset) {
        return _offsetBeforeJump.editor == jumpTargetOffset.editor;
    }
}

