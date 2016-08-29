package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class ObtainAndPasteRangeAfterJumpCommand extends CommandAroundJump {

    private final Class<? extends Selector> _selectorClass;

    public ObtainAndPasteRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }

    @Override
    public void afterJump(final JOffset jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EditorUtils.copyRange(_selectorClass, jumpTargetOffset.editor);
                getOffsetBeforeJump().restoreCaret();

                EditorUtils.selectRangeOf(_selectorClass, _editor);
                EditorUtils.deleteRange(_selectorClass, _editor);
                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);

                if (_config._needSelectTextAfterJump) {
                    EditorUtils.selectTextRange(_editor, tr);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
