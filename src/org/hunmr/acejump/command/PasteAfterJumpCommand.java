package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorCopyPasteHelper;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
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
    public void beforeJump(final int jumpTargetOffset) {
    }

    public TextRange getTextRangeToReplace()
    {
        return null;
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        TextRange tr = getTextRangeToReplace();
        if (tr != null)
        {
            _editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (_addNewLineBeforePaste) {
                    _editor.getDocument().insertString(_editor.getCaretModel().getOffset(), "\n");
                    _editor.getCaretModel().moveToOffset(_editor.getCaretModel().getOffset() + 1);
                }

                EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }
}
