package org.hunmr.acejump.command;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class ReplaceAfterJumpCommand extends CommandAroundJump {
    private boolean _addNewLineBeforePaste;
    protected final Class<? extends Selector> _selectorClass;

    private int _caretOffsetFromSelectRangeStartBeforeJump;

    public ReplaceAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _addNewLineBeforePaste = false;
        _selectorClass = selectorClass;
    }

    @Override
    public void beforeJump(final JOffset jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
        EditorUtils.selectRangeOf(_selectorClass, _se);
        _caretOffsetFromSelectRangeStartBeforeJump = _soff - _se.getSelectionModel().getSelectionStart();
        focusSourceCaret();
    }

    @Override
    public void afterJump() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                TextRange tr = EditorUtils.getRangeOf(_selectorClass, _te);
                if (tr != null)
                {
                    _te.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                    EditorUtils.deleteRange(tr, _te);
                }

                CaretModel targetCaret = _te.getCaretModel();

                if (_addNewLineBeforePaste) {
                    _te.getDocument().insertString(targetCaret.getOffset(), "\n");
                    targetCaret.moveToOffset(targetCaret.getOffset() + 1);
                }

                TextRange[] textRanges = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_te);

                if (_config._needSelectTextAfterJump) {
                    int caret = textRanges[0].getStartOffset() + _caretOffsetFromSelectRangeStartBeforeJump;
                    targetCaret.moveToOffset(caret);
                    EditorUtils.selectRangeOf(_selectorClass, _te);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
