package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.JOffset;
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
    public void afterJump(final JOffset jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextRange sourceRange = EditorUtils.getRangeOf(_selectorClass, jumpTargetOffset.editor);

                if (inSameEditor(jumpTargetOffset)) {
                    boolean noNeedToMove = sourceRange.contains(getOffsetBeforeJump().offset);
                    if (noNeedToMove) {
                        _editor.getCaretModel().moveToOffset(getOffsetBeforeJump().offset);
                        return;
                    }
                }

                int textSourceStartOffset = sourceRange.getStartOffset();

                EditorUtils.copyRange(_selectorClass, jumpTargetOffset.editor);

                if ( !inSameEditor(jumpTargetOffset) || textSourceStartOffset > getOffsetBeforeJump().offset) {
                    deleteTextSource();
                    pasteClipboardToOffset();
                } else {
                    pasteClipboardToOffset();

                    jumpTargetOffset.restoreCaret();
                    deleteTextSource();
                    getOffsetBeforeJump().restoreCaret();

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
                getOffsetBeforeJump().restoreCaret();

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
