package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.util.AppUtil;

public class PasteAfterJumpCommand extends CommandAroundJump {
    private boolean _addNewLineBeforePaste;

    public PasteAfterJumpCommand(Editor editor) {
        super(editor);
        _addNewLineBeforePaste = false;
    }

    public PasteAfterJumpCommand(Editor editor, boolean addNewLineBeforePaste) {
        super(editor);
        _addNewLineBeforePaste = addNewLineBeforePaste;
    }

    @Override
    public void beforeJump(final JOffset jumpTargetOffset) {
    }

    public TextRange getTextRangeToReplace()
    {
        return null;
    }

    @Override
    public void afterJump() {
        TextRange tr = getTextRangeToReplace();
        if (tr != null)
        {
            _se.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (_addNewLineBeforePaste) {
                    _se.getDocument().insertString(_se.getCaretModel().getOffset(), "\n");
                    _se.getCaretModel().moveToOffset(_se.getCaretModel().getOffset() + 1);
                }

                EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_se);
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
