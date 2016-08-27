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
    private int _length;

    public MoveRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
        _length = 0;
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
                    _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
                    int cur_offset = _editor.getCaretModel().getOffset();

                    if (_config._needSelectTextAfterJump) {
                        EditorUtils.selectTextRange(_editor, cur_offset - _length, cur_offset);
                    }
                }
            }

            private void deleteTextSource() {
                EditorUtils.selectRangeOf(_selectorClass, _editor);
                EditorModificationUtil.deleteSelectedText(_editor);
            }

            private void pasteClipboardToOffset() {
                _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());
                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);
                if (_config._needSelectTextAfterJump) {
                    EditorUtils.selectTextRange(_editor, tr);
                }
                _length = tr[0].getEndOffset() - tr[0].getStartOffset();
            }
        };

        AppUtil.runWriteAction(runnable, _editor);
    }
}
