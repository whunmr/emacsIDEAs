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
    public void afterJump() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                TextRange sourceRange = EditorUtils.getRangeOf(_selectorClass, _te);

                if (inSameEditor()) {
                    boolean noNeedToMove = sourceRange.contains(_soff);
                    if (noNeedToMove) {
                        _se.getCaretModel().moveToOffset(_soff);
                        return;
                    }
                }

                int textSourceStartOffset = sourceRange.getStartOffset();

                EditorUtils.copyRange(_selectorClass, _te);

                if ( !inSameEditor() || textSourceStartOffset > _soff) {
                    deleteTextSource(_te);

                    pasteClipboardToOffset();
                } else {
                    pasteClipboardToOffset();

                    focusTargetCaret();
                    deleteTextSource(_te);
                    focusSourceCaret();

                    int cur_offset = _se.getCaretModel().getOffset();

                    if (_config._needSelectTextAfterJump) {
                        EditorUtils.selectTextRange(_se, cur_offset - _length, cur_offset);
                    }
                }
            }

            private void deleteTextSource(Editor editor) {
                EditorUtils.selectRangeOf(_selectorClass, editor);
                EditorModificationUtil.deleteSelectedText(editor);
            }

            private void pasteClipboardToOffset() {
                focusSourceCaret();

                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_se);
                if (_config._needSelectTextAfterJump) {
                    EditorUtils.selectTextRange(_se, tr);
                }
                _length = tr[0].getEndOffset() - tr[0].getStartOffset();
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
