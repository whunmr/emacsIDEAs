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
        getOffsetBeforeJump().restoreCaret();
    }

    @Override
    public void afterJump(final JOffset jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Editor targetEditor = jumpTargetOffset.editor;

                TextRange tr = getTextRangeToReplace(targetEditor);
                if (tr != null)
                {
                    targetEditor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                    EditorUtils.deleteRange(tr, targetEditor);
                }

                if (_addNewLineBeforePaste) {
                    targetEditor.getDocument().insertString(targetEditor.getCaretModel().getOffset(), "\n");
                    targetEditor.getCaretModel().moveToOffset(targetEditor.getCaretModel().getOffset() + 1);
                }

                TextRange[] textRanges = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(targetEditor);

                if (_config._needSelectTextAfterJump) {
                    int caret = textRanges[0].getStartOffset() + _caretOffsetFromSelectRangeStartBeforeJump;
                    targetEditor.getCaretModel().moveToOffset(caret);
                    EditorUtils.selectRangeOf(_selectorClass, targetEditor);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }

    public TextRange getTextRangeToReplace(Editor targetEditor)
    {
        return EditorUtils.getRangeOf(_selectorClass, targetEditor);
    }
}
