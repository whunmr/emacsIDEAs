package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class MoveRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;

    public MoveRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }


    @Override
    public void afterJump(final int jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextRange sourceRange = EditorUtils.getRangeOf(_selectorClass, _editor);

                boolean noNeedToMove = sourceRange.contains(getOffsetBeforeJump());
                if (noNeedToMove) {
                    _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
                    return;
                }

                int textSourceStartOffset = sourceRange.getStartOffset();

                EditorUtils.copyRange(_selectorClass, _editor);
                if (textSourceStartOffset > getOffsetBeforeJump()) {
                    deleteTextSource();
                    pasteClipboardToOffset();
                } else {
                    pasteClipboardToOffset();
                    _editor.getCaretModel().moveToOffset(jumpTargetOffset);
                    deleteTextSource();
                    _editor.getCaretModel().moveToOffset(getOffsetBeforeJump() - sourceRange.getLength());
                    EditorUtils.selectRangeOf(_selectorClass, _editor);
                }
            }

            private void deleteTextSource() {
                EditorUtils.selectRangeOf(_selectorClass, _editor);
                EditorModificationUtil.deleteSelectedText(_editor);
            }

            private void pasteClipboardToOffset() {
                _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);
                EditorUtils.selectTextRange(_editor, tr);
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }
}
