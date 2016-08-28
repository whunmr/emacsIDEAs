package org.hunmr.acejump.command;

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
        EditorUtils.selectRangeOf(_selectorClass, _editor);
        _caretOffsetFromSelectRangeStartBeforeJump = getOffsetBeforeJump().offset - _editor.getSelectionModel().getSelectionStart();
        //editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
        getOffsetBeforeJump().restoreCaret();
    }

    @Override
    public void afterJump(final JOffset jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextRange tr = getTextRangeToReplace();
                if (tr != null)
                {
                    _editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                    EditorUtils.deleteRange(tr, _editor);
                }

                if (_addNewLineBeforePaste) {
                    _editor.getDocument().insertString(_editor.getCaretModel().getOffset(), "\n");
                    _editor.getCaretModel().moveToOffset(_editor.getCaretModel().getOffset() + 1);
                }

                TextRange[] textRanges = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);

                if (_config._needSelectTextAfterJump) {
                    int caret = textRanges[0].getStartOffset() + _caretOffsetFromSelectRangeStartBeforeJump;
                    _editor.getCaretModel().moveToOffset(caret);
                    EditorUtils.selectRangeOf(_selectorClass, _editor);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }

    public TextRange getTextRangeToReplace()
    {
        return EditorUtils.getRangeOf(_selectorClass, _editor);
    }
}
