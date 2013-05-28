package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import org.hunmr.util.AppUtil;

public class PasteAfterJumpCommand extends CommandAroundJump {
    private boolean _addNewLineBeforePaste;

    public PasteAfterJumpCommand(Editor editor, boolean addNewLineBeforePaste) {
        super(editor);
        _addNewLineBeforePaste = addNewLineBeforePaste;
    }

    @Override
    public void beforeJump(final int jumpTargetOffset) {
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (_addNewLineBeforePaste) {
                    _editor.getDocument().insertString(_editor.getCaretModel().getOffset(), "\n");
                    _editor.getCaretModel().moveToOffset(_editor.getCaretModel().getOffset() + 1);
                }

                EditorModificationUtil.pasteFromClipboard(_editor);
//                if (tr.getEndOffset() < _editor.getDocument().getTextLength()) {
//                    _editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
//                }
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }
}
