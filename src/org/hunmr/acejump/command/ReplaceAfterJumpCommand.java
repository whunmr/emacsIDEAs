package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
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
    public void beforeJump(final int jumpTargetOffset) {
        super.beforeJump(jumpTargetOffset);
        EditorUtils.selectRangeOf(_selectorClass, _editor);
        _caretOffsetFromSelectRangeStartBeforeJump = getOffsetBeforeJump() - _editor.getSelectionModel().getSelectionStart();
        _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
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

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }

    public TextRange getTextRangeToReplace()
    {
        return EditorUtils.getRangeOf(_selectorClass, _editor);
    }
}
