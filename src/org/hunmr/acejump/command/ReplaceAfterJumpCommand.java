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

    public ReplaceAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _addNewLineBeforePaste = false;
        _selectorClass = selectorClass;
    }

    @Override
    public void beforeJump(final int jumpTargetOffset) {
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

                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);
                EditorUtils.selectRangeOf(_selectorClass, _editor);
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }

    public TextRange getTextRangeToReplace()
    {
        return EditorUtils.getRangeOf(_selectorClass, _editor);
    }
}
